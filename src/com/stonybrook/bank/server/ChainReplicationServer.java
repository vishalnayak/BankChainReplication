package com.stonybrook.bank.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.stonybrook.bank.common.Constants;
import com.stonybrook.bank.common.Outcome;
import com.stonybrook.bank.utils.ChainReplicationUtil;
import com.stonybrook.bank.utils.ConfigUtil;

/**
 * Server process for each bank. May act as head or tail or an intermediate
 * server. Maintains own instance of all the accounts.
 */
public class ChainReplicationServer extends Server {

	/**
	 * The Class StartupDelayTimerTask.
	 */
	private class StartupDelayTimerTask extends TimerTask {

		/** The timer. */
		private Timer timer = null;

		/**
		 * Instantiates a new startup delay timer task.
		 *
		 * @param timer
		 *            the timer
		 */
		public StartupDelayTimerTask(Timer timer) {
			super();
			this.timer = timer;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.TimerTask#run()
		 */
		@Override
		public void run() {
			log.info("Startup Delay Timer Expired");
			timer.cancel();
			String masterAddress = ConfigUtil
					.getProperty(Constants.MASTER_ADDRESS);
			String masterPort = ConfigUtil.getProperty(Constants.MASTER_PORT);
			List<String> params = new ArrayList<String>();
			params.add(server);
			chainReplicationUtil.tcpSend(Constants.CMD_NEW_SERVER, params,
					masterAddress, masterPort);
		}
	}

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		ChainReplicationServer client = new ChainReplicationServer();
		client.start(args[0]);
	}

	/** The log. */
	private Logger log = null;

	/** The server id. */
	private String serverId = null;

	/** The bank id. */
	private String bankId = null;

	/** The server. */
	private String server = null;

	/** The receive count. */
	private Integer receiveCount = -1;

	/** The send count. */
	private Integer sendCount = -1;

	/** The timer started. */
	private Boolean timerStarted = false;

	/** The servers id list. */
	private List<String> serversIdList = new ArrayList<String>();

	/** The tail activated. */
	private Boolean tailActivated = false;

	/** The chains. */
	private HashMap<String, List<String>> chains = new HashMap<String, List<String>>();

	/** The accounts. */
	private HashMap<String, Account> accounts = new HashMap<String, Account>();

	/** The sent trans. */
	private List<String> sentTrans = Collections
			.synchronizedList(new ArrayList<String>());

	/** The transfer request record. */
	private HashMap<String, Integer> transferRequestRecord = new HashMap<String, Integer>();

	/** The processed trans. */
	private HashMap<String, List<String>> processedTrans = new HashMap<String, List<String>>();

	/** The chain replication util. */
	ChainReplicationUtil chainReplicationUtil = new ChainReplicationUtil();

	/** The heart beat timer. */
	private Timer heartBeatTimer = new Timer();

	/** The transfer timer. */
	private Timer transferTimer = new Timer();

	/** The successor address. */
	private String successorAddress = null;

	/** The successor port. */
	private String successorPort = null;

	/** The predecessor address. */
	private String predecessorAddress = null;

	/** The predecessor port. */
	private String predecessorPort = null;

	/** The propagate pending. */
	private Boolean propagatePending = false;

	/** The server list received. */
	private Boolean serverListReceived = false;

	/** The updated server list received. */
	private Boolean updatedServerListReceived = false;

	/** The chain list received. */
	private Boolean chainListReceived = false;

	/** The process pending. */
	private boolean processPending = false;

	/** The j. */
	private int j = 0;

	/** The start up timer. */
	private Timer startUpTimer = new Timer();

	/**
	 * Deposit.
	 *
	 * @param reqId
	 *            the req id
	 * @param accNum
	 *            the acc num
	 * @param amount
	 *            the amount
	 */
	private void deposit(String reqId, String accNum, String amount) {
		log.debug("processing deposit");
		String result = getResult(reqId, Constants.DEPOSIT);
		if (result == null) {
			result = getAccount(accNum).deposit(reqId, amount);
		} else {
			if (result.contains(Outcome.InconsistentWithHistory.toString())) {
				log.info("Replying InconsistentWithHistory for reqId: " + reqId);
			} else {
				log.info("Duplicate deposit for reqId: " + reqId
						+ " Returning the same reply:" + result);
			}
		}
		log.debug("deposit result: " + result);
		List<String> params = new ArrayList<String>();
		params.add(reqId);
		params.add(result);
		params.add(Constants.DEPOSIT);
		params.add(accNum);
		params.add(amount);
		processedTrans.put(reqId, params);
		if (-1 == sentTrans.lastIndexOf(reqId)) {
			sentTrans.add(reqId);
		}
		propagateRequest(reqId);
	}

	/**
	 * Gets the account.
	 *
	 * @param accNum
	 *            the acc num
	 * @return the account
	 */
	private Account getAccount(String accNum) {
		Account account = accounts.get(accNum);
		if (account == null) {
			account = new Account(accNum, 0);
			accounts.put(accNum, account);
		}
		return account;
	}

	/**
	 * Gets the balance.
	 *
	 * @param reqId
	 *            the req id
	 * @param accNum
	 *            the acc num
	 * @return the balance
	 */
	private void getBalance(String reqId, String accNum) {
		log.debug("processing getBalance");
		String result = getResult(reqId, Constants.GET_BALANCE);
		if (result == null) {
			result = getAccount(accNum).getBalance(reqId);
		} else {
			if (result.contains(Outcome.InconsistentWithHistory.toString())) {
				log.info("Replying InconsistentWithHistory for reqId: " + reqId);
			} else {
				log.info("Duplicate getBalanceRequest for reqId: " + reqId
						+ " Returning the same reply:" + result);
			}
		}
		log.debug("getBalanceResult: " + result);
		List<String> params = new ArrayList<String>();
		params.add(reqId);
		params.add(result);
		params.add(Constants.GET_BALANCE);
		params.add(accNum);
		processedTrans.put(reqId, params);
		if (-1 == sentTrans.lastIndexOf(reqId)) {
			sentTrans.add(reqId);
		}
		propagateRequest(reqId);
	}

	/**
	 * Gets the client command.
	 *
	 * @param cmd
	 *            the cmd
	 * @return the client command
	 */
	private String getClientCommand(String cmd) {
		String command = null;
		if (cmd.equals(Constants.GET_BALANCE)) {
			command = Constants.CMD_UPDATE_GET_BALANCE;
		} else if (cmd.equals(Constants.DEPOSIT)) {
			command = Constants.CMD_UPDATE_DEPOSIT;
		} else if (cmd.equals(Constants.WITHDRAW)) {
			command = Constants.CMD_UPDATE_WITHDRAW;
		} else if (cmd.equals(Constants.TRANSFER_WITHDRAW)) {
			command = Constants.CMD_UPDATE_TRANSFER_WITHDRAW;
		} else if (cmd.equals(Constants.TRANSFER_DEPOSIT)) {
			command = Constants.CMD_UPDATE_TRANSFER_DEPOSIT;
		}
		return command;
	}

	/**
	 * Gets the result.
	 *
	 * @param reqId
	 *            the req id
	 * @param cmd
	 *            the cmd
	 * @return the result
	 */
	private String getResult(String reqId, String cmd) {
		String result = null;
		List<String> tran = null;
		tran = processedTrans.get(reqId);
		log.debug("processedTrans: " + processedTrans.toString());
		if (tran != null) {// matching reqId
			if (tran.get(2).equals(cmd)) {
				// same transaction
				result = tran.get(1);
			} else {
				// different transaction. inconsistent with history.
				result = tran.get(1);
				String[] resultArr = result.split("\\;");
				resultArr[1] = Outcome.InconsistentWithHistory.toString();
				result = resultArr[0] + ";" + resultArr[1] + ";" + resultArr[2];
			}
		}
		return result;
	}

	/**
	 * Identify neighbor servers.
	 */
	private void identifyNeighborServers() {
		List<String> predecessor = chainReplicationUtil.getPredecessor(server,
				serversIdList);
		List<String> successor = chainReplicationUtil.getSuccessor(server,
				serversIdList);
		predecessorAddress = predecessor.get(0);
		predecessorPort = predecessor.get(1);
		successorAddress = successor.get(0);
		successorPort = successor.get(1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.stonybrook.bank.server.Server#processCommand(java.lang.String)
	 */
	public synchronized void processCommand(String cmdSeq) {
		if (cmdSeq.charAt(0) == "[".charAt(0)) {
			cmdSeq = cmdSeq.substring(1, cmdSeq.length() - 1);
		}
		log.debug("processCommand in Server: " + cmdSeq);
		String[] cmdSeqArr = null;
		cmdSeqArr = cmdSeq.split("\\,");
		switch (cmdSeqArr[0]) {
		case Constants.CMD_TRANSFER_DEPOSIT_ACKNOWLEDGEMENT:
			log.info("Acknowledgement for transferDeposit received.");
			transferRequestRecord.put(cmdSeqArr[1], 1);
			break;
		case Constants.CMD_UPDATE_TRANSFER_DEPOSIT:
			log.info("updateTransferDeposit(" + cmdSeqArr[1].trim() + ","
					+ cmdSeqArr[2].trim() + "," + cmdSeqArr[4].trim() + ","
					+ cmdSeqArr[5].trim() + "," + cmdSeqArr[6].trim() + ")");
			log.debug("Reducing count from cmd_update_transfer_deposit: receive");
			reduceCount(Constants.RECEIVE);
			updateTransferDeposit(cmdSeqArr[1].trim(), cmdSeqArr[2].trim(),
					cmdSeqArr[4].trim(), cmdSeqArr[5].trim(),
					cmdSeqArr[6].trim());
			break;
		case Constants.CMD_TRANSFER_DEPOSIT:
			log.info("Transfer: transferDeposit(" + cmdSeqArr[1].trim() + ","
					+ cmdSeqArr[2].trim() + "," + cmdSeqArr[3].trim() + ","
					+ cmdSeqArr[4].trim() + ")");
			log.debug("Reducing count from cmd_transfer_deposit: receive");
			reduceCount(Constants.RECEIVE);
			String testTransferDeposit = ConfigUtil
					.getProperty(Constants.TEST_TRANSFER_DEPOSIT_FAIL + server);
			if (testTransferDeposit != null
					&& testTransferDeposit.equalsIgnoreCase("true")) {
				System.exit(0);
			}
			transferDeposit(cmdSeqArr[1].trim(), cmdSeqArr[2].trim(),
					cmdSeqArr[3].trim(), cmdSeqArr[4].trim());
			break;
		case Constants.CMD_TRANSFER_FROM_CLIENT:
			log.info("transfer(" + cmdSeqArr[1].trim() + ","
					+ cmdSeqArr[2].trim() + "," + cmdSeqArr[3].trim()
					+ cmdSeqArr[4].trim() + "," + cmdSeqArr[5].trim() + ")");
			log.debug("Reducing count from cmd_transfer_from_client: receive");
			reduceCount(Constants.RECEIVE);
			transferWithdraw(cmdSeqArr[1].trim(), cmdSeqArr[2].trim(),
					cmdSeqArr[3].trim(), cmdSeqArr[4].trim(),
					cmdSeqArr[5].trim());
			break;
		case Constants.CMD_UPDATE_TRANSFER_WITHDRAW:
			log.info("updateTransfer(" + cmdSeqArr[1].trim() + ","
					+ cmdSeqArr[2].trim() + "," + cmdSeqArr[4].trim() + ","
					+ cmdSeqArr[5].trim() + "," + cmdSeqArr[6].trim() + ","
					+ cmdSeqArr[7].trim() + ")");
			log.debug("Reducing count from cmd_update_transfer_withdraw: receive");
			reduceCount(Constants.RECEIVE);
			updateTransferWithdraw(cmdSeqArr[1].trim(), cmdSeqArr[2].trim(),
					cmdSeqArr[4].trim(), cmdSeqArr[5].trim(),
					cmdSeqArr[6].trim(), cmdSeqArr[7].trim());
			break;
		case Constants.CMD_TERMINATE:
			log.info("Server Failed: Terminating Server: " + server);
			System.exit(0);
			break;
		case Constants.CMD_PROPAGATE_PENDING:
			log.info("PropagatePending: PredecessorAddress: "
					+ predecessorAddress + " PredecessorPort: "
					+ predecessorPort);
			propagatePending = true;
			break;
		case Constants.CMD_PROCESS_PENDING:
			log.info("ProcessPending: SuccessorAddress: " + successorAddress
					+ " SuccessorPort: " + successorPort);
			processPending = true;
			break;
		case Constants.CMD_TAIL_DEACTIVATED:
			tailActivated = false;
			break;
		case Constants.CMD_TAIL_ACTIVATED:
			tailActivated = true;
			break;
		case Constants.CMD_ACKNOWLEDGEMENT:
			log.info("Acknowledgement received for reqId:" + cmdSeqArr[1]);
			if (processedTrans.get(cmdSeqArr[1]).get(2)
					.equals(Constants.TRANSFER_DEPOSIT)) {
				log.info("Acknowledgement for transferDeposit received in destination bank. Replying to source bank.");
				String sourceServer = processedTrans.get(cmdSeqArr[1]).get(5);
				String sourceBankTailAddress = chains.get(
						sourceServer.split("\\.")[0]).get(2);
				String sourceBankTailPort = chains.get(
						sourceServer.split("\\.")[0]).get(3);
				chainReplicationUtil.tcpSend(
						Constants.CMD_TRANSFER_DEPOSIT_ACKNOWLEDGEMENT,
						processedTrans.get(cmdSeqArr[1]),
						sourceBankTailAddress, sourceBankTailPort);
			}
			synchronized (sentTrans) {
				sentTrans.remove(cmdSeqArr[1].trim());
			}
			sendAcknowledgement(Constants.CMD_ACKNOWLEDGEMENT,
					processedTrans.get(cmdSeqArr[1]));
			break;
		case Constants.CMD_CHAINS_LIST_TO_SERVER:
			for (int i = 1; i < cmdSeqArr.length; i += 5) {
				List<String> data = new ArrayList<String>();
				data.add(cmdSeqArr[i + 1].trim());
				data.add(cmdSeqArr[i + 2].trim());
				data.add(cmdSeqArr[i + 3].trim());
				data.add(cmdSeqArr[i + 4].trim());
				chains.put(cmdSeqArr[i].trim(), data);
			}
			if (!chainListReceived) {
				chainListReceived = true;
				log.info("Received Chain list from Master: Data: "
						+ chains.toString());
			} else {
				log.info("Received 'Updated' Chain list from Master: Data: "
						+ chains.toString());
			}
			break;
		case Constants.CMD_SERVERS_LIST_TO_SERVERS:
			serversIdList.removeAll(serversIdList);
			for (int i = 1; i < cmdSeqArr.length; i++) {
				serversIdList.add(cmdSeqArr[i]);
			}
			if (!timerStarted) {
				log.info("HeartBeat timer starting for server:" + server);
				timerStarted = true;
				Integer timeout = Integer.valueOf(ConfigUtil
						.getProperty(Constants.BANK_SERVER_HEARTBEAT_TIMEOUT));
				heartBeatTimer.schedule(this, 1000, timeout);
			}
			if (!serverListReceived) {
				serverListReceived = true;
				log.info("Received Servers list from Master: Updated Servers data: "
						+ serversIdList.toString());
			} else {
				updatedServerListReceived = true;
				log.info("Received 'Updated' Servers list from Master: Updated Servers data: "
						+ serversIdList.toString());
			}
			identifyNeighborServers();
			break;
		case Constants.CMD_GET_BALANCE_FROM_CLIENT:
			if (tailActivated) {
				log.info("getBalance(" + cmdSeqArr[1].trim() + ","
						+ cmdSeqArr[2].trim() + ")");
				getBalance(cmdSeqArr[1].trim(), cmdSeqArr[2].trim());
				log.debug("Reducing count from cmd_get_balance_from_client: receive");
				reduceCount(Constants.RECEIVE);
			}
			break;
		case Constants.CMD_UPDATE_GET_BALANCE:
			log.info("updateGetBalance(" + cmdSeqArr[1].trim() + ","
					+ cmdSeqArr[2].trim() + "," + cmdSeqArr[4].trim() + ")");
			log.debug("Reducing count from cmd_update_get_balance: receive");
			reduceCount(Constants.RECEIVE);
			updateGetBalance(cmdSeqArr[1].trim(), cmdSeqArr[2].trim(),
					cmdSeqArr[4].trim());
			break;
		case Constants.CMD_WITHDRAW_FROM_CLIENT:
			log.info("withdraw(" + cmdSeqArr[1].trim() + ","
					+ cmdSeqArr[2].trim() + "," + cmdSeqArr[3].trim() + ")");
			log.debug("Reducing count from cmd_withdraw_from_client: receive");
			reduceCount(Constants.RECEIVE);
			withdraw(cmdSeqArr[1].trim(), cmdSeqArr[2].trim(),
					cmdSeqArr[3].trim());
			break;
		case Constants.CMD_UPDATE_WITHDRAW:
			log.info("updateWithdraw(" + cmdSeqArr[1].trim() + ","
					+ cmdSeqArr[2].trim() + "," + cmdSeqArr[4].trim() + ","
					+ cmdSeqArr[5].trim() + ")");
			log.debug("Reducing count from cmd_update_withdraw: receive");
			reduceCount(Constants.RECEIVE);
			updateWithdraw(cmdSeqArr[1].trim(), cmdSeqArr[2].trim(),
					cmdSeqArr[4].trim(), cmdSeqArr[5].trim());
			break;
		case Constants.CMD_DEPOSIT_FROM_CLIENT:
			log.info("deposit(" + cmdSeqArr[1].trim() + ","
					+ cmdSeqArr[2].trim() + "," + cmdSeqArr[3].trim() + ")");
			log.debug("Reducing count from cmd_deposit_from_client: receive");
			reduceCount(Constants.RECEIVE);
			deposit(cmdSeqArr[1].trim(), cmdSeqArr[2].trim(),
					cmdSeqArr[3].trim());
			break;
		case Constants.CMD_UPDATE_DEPOSIT:
			log.info("updateDeposit(" + cmdSeqArr[1].trim() + ","
					+ cmdSeqArr[2].trim() + "," + cmdSeqArr[4].trim() + ","
					+ cmdSeqArr[5].trim() + ")");
			log.debug("Reducing count from cmd_update_deposit: receive");
			reduceCount(Constants.RECEIVE);
			updateDeposit(cmdSeqArr[1].trim(), cmdSeqArr[2].trim(),
					cmdSeqArr[4].trim(), cmdSeqArr[5].trim());
			break;
		default:
			break;
		}
	}

	/**
	 * Process transfer deposit.
	 *
	 * @param parameters
	 *            the parameters
	 */
	private void processTransferDeposit(List<String> parameters) {
		// Sample 'parameters': [1.1.5,<1.1.5;Processed;1900.0>,11,100,2,21]
		log.debug("transferDeposit: parameters: " + parameters.toString());
		List<String> params = new ArrayList<String>();
		params.add(parameters.get(0));
		params.add(parameters.get(6));
		params.add(parameters.get(4));
		params.add(server);
		String destAddress = chains.get(parameters.get(5)).get(0);
		String destPort = chains.get(parameters.get(5)).get(1);
		log.info("Sending the transfer request to Destination Bank with parameters: "
				+ params.toString());
		String testTransferWithdraw = ConfigUtil
				.getProperty(Constants.TEST_TRANSFER_WITHDRAW_FAIL + server);
		if (testTransferWithdraw != null
				&& testTransferWithdraw.equalsIgnoreCase("true")) {
			System.exit(0);
		}
		chainReplicationUtil.tcpSend(Constants.CMD_TRANSFER_DEPOSIT, params,
				destAddress, destPort);
	}

	/**
	 * Propagate request.
	 *
	 * @param reqId
	 *            the req id
	 */
	private void propagateRequest(String reqId) {
		String request = null;
		int i = 0;
		List<String> parameters = null;
		String command = null;
		if (successorAddress != null && successorPort != null) {
			if (processPending) {
				j++;
				String subfail = ConfigUtil.getProperty(Constants.SUBFAIL
						+ server);
				if (j > 2 && subfail != null
						&& subfail.equalsIgnoreCase("true")) {
					log.info("Server Failed: After processing 1 of many pending requests.");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					System.exit(0);
				}
				if (j > 1) {
					log.info("Processing pending request:" + (j - 1));
				}
			}
			if (propagatePending && updatedServerListReceived) {
				i = 0;
				synchronized (sentTrans) {
					Iterator<String> iterator = sentTrans.iterator();
					while (iterator.hasNext()) {
						i++;
						String subfail = ConfigUtil
								.getProperty(Constants.SUBFAIL + server);
						if (i > 2 && subfail != null
								&& subfail.equalsIgnoreCase("true")) {
							log.info("Server Failed: After propagating 1 of many pending requests.");
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							System.exit(0);
						}
						if (i > 1) {
							log.info("Propagating pending request:" + (i - 1));
						}
						request = iterator.next();
						parameters = processedTrans.get(request);
						command = getClientCommand(parameters.get(2));
						chainReplicationUtil.tcpSend(command, parameters,
								successorAddress, successorPort);
						log.debug("Reducing count from propagateRequest: send");
						reduceCount(Constants.SEND);
					}
				}
				propagatePending = false;
				updatedServerListReceived = false;
			} else {
				parameters = processedTrans.get(reqId);
				command = getClientCommand(parameters.get(2));
				chainReplicationUtil.tcpSend(command, parameters,
						successorAddress, successorPort);
			}
		} else if (successorAddress == null && successorPort == null
				&& tailActivated) {
			synchronized (sentTrans) {
				String transferReqId = null;
				while (sentTrans.size() > 0) {
					request = sentTrans.get(0);
					parameters = processedTrans.get(request);
					command = getClientCommand(parameters.get(2));
					log.debug("Reached tail for bank: " + bankId);
					if (command.equals(Constants.CMD_UPDATE_TRANSFER_WITHDRAW)) {
						if (parameters.get(1).contains(
								Outcome.Processed.toString())) {

							trackTransferReqId(reqId);
							processTransferDeposit(parameters);
						} else {
							log.info("transferWithdraw failed. Not sending the request to destination bank.");
							respondToClient(command, parameters.get(0),
									parameters.get(1));
						}
					} else if (command
							.equals(Constants.CMD_UPDATE_TRANSFER_DEPOSIT)) {
						log.info("Transfer operation complete in both source and destination bank servers.");
						respondToClient(command, parameters.get(0),
								parameters.get(1));
					} else {
						respondToClient(command, parameters.get(0),
								parameters.get(1));
					}
					// remove from sent list and send acknowledgement
					log.debug("sentTrans before. size:" + sentTrans.size()
							+ " " + sentTrans.toString());
					if (parameters.get(2).equals(Constants.TRANSFER_WITHDRAW)) {
						transferReqId = parameters.get(0);
					}
					sentTrans.remove(parameters.get(0));
					log.debug("sentTrans after. size:" + sentTrans.size() + " "
							+ sentTrans.toString());
					log.info("Sending acknowledgement for reqId:" + request);
					if (!parameters.get(2).equals(Constants.TRANSFER_WITHDRAW)) {
						sendAcknowledgement(Constants.CMD_ACKNOWLEDGEMENT,
								parameters);
					}
				}
				// if (transferReqId != null) {
				// sentTrans.add(transferReqId);
				// }
			}
		} else {
			log.error("--");
		}
	}

	/**
	 * Track transfer req id.
	 *
	 * @param reqId the req id
	 */
	private void trackTransferReqId(String reqId) {
		reqId = reqId.trim();
		transferRequestRecord.put(reqId, 0);
		transferTimer = new Timer();
		long waitTime = 2000;
		Integer noOfRetries = 10;
		log.debug("Tracking the transfer request:" + reqId + " noOfRetries:"
				+ noOfRetries);
		transferTimer.schedule(new TransferRequestTimerTask(reqId,
				transferTimer, noOfRetries), waitTime, waitTime);
	}

	/**
	 * The Class TransferRequestTimerTask.
	 */
	private class TransferRequestTimerTask extends TimerTask {

		/** The req id. */
		private String reqId = null;

		/** The timer. */
		private Timer timer = null;

		/** The no of retries. */
		private Integer noOfRetries = null;

		/**
		 * Instantiates a new client request timer task.
		 *
		 * @param reqId
		 *            the req id
		 * @param timer
		 *            the timer
		 * @param noOfRetries
		 *            the no of retries
		 */
		public TransferRequestTimerTask(String reqId, Timer timer,
				Integer noOfRetries) {
			super();
			this.reqId = reqId.trim();
			this.timer = timer;
			this.noOfRetries = noOfRetries;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.TimerTask#run()
		 */
		@Override
		public void run() {
			if (transferRequestRecord.get(reqId) == 1) {
				log.debug("Response received for reqId: " + reqId);
				timer.cancel();
			} else {
				log.debug("retry transferDeposit: " + reqId
						+ " pending retries: " + noOfRetries);
				if (noOfRetries-- > 0) {
					processTransferDeposit(processedTrans.get(reqId));
				} else {
					log.info("Exceeded number of retries: " + reqId);
					timer.cancel();
				}
			}
		}
	}

	/**
	 * Reduce count.
	 *
	 * @param type
	 *            the type
	 */
	private void reduceCount(String type) {
		if (type.equals(Constants.RECEIVE)) {
			receiveCount--;
		} else {
			sendCount--;
		}
		if (receiveCount == -1 && sendCount == -1) {
			return;
		}
		if (receiveCount == 0) {
			log.info("Server Failed: receiveCount is zero: Server:" + server);
			System.exit(0);
		}
		if (sendCount == 0) {
			log.info("Server Failed: sendCount is zero: Server:" + server);
			System.exit(0);
		}
	}

	/**
	 * Respond to client.
	 *
	 * @param cmd
	 *            the cmd
	 * @param reqId
	 *            the req id
	 * @param result
	 *            the result
	 */
	private void respondToClient(String cmd, String reqId, String result) {
		String[] reqIdArr = reqId.split("\\.");
		String clientId = reqIdArr[0].trim() + "." + reqIdArr[1].trim();
		log.info("Responding to client: " + clientId);
		ChainReplicationUtil chainReplicationUtil = new ChainReplicationUtil();
		String clientAddress = ConfigUtil
				.getProperty(Constants.BANK_CLIENT_ADDRESS + clientId);
		String clientPort = ConfigUtil.getProperty(Constants.BANK_CLIENT_PORT
				+ clientId);
		List<String> params = new ArrayList<String>();
		params.add(result);
		String clientCommand = null;
		// to be enhanced in next phase.
		if (Constants.CMD_UPDATE_GET_BALANCE.equals(cmd)) {
			clientCommand = Constants.CMD_GET_BALANCE_RESULT_TO_CLIENT;
		} else if (Constants.CMD_UPDATE_DEPOSIT.equals(cmd)) {
			clientCommand = Constants.CMD_DEPOSIT_RESULT_TO_CLIENT;
		} else if (Constants.CMD_UPDATE_TRANSFER_DEPOSIT.equals(cmd)) {
			clientCommand = Constants.CMD_TRANSFER_RESULT_TO_CLIENT;
		} else if (Constants.CMD_UPDATE_TRANSFER_WITHDRAW.equals(cmd)) {
			clientCommand = Constants.CMD_TRANSFER_RESULT_TO_CLIENT;
		} else if (Constants.CMD_UPDATE_WITHDRAW.equals(cmd)) {
			clientCommand = Constants.CMD_WITHDRAW_RESULT_TO_CLIENT;
		}
		chainReplicationUtil.udpSend(clientCommand, params, clientAddress,
				clientPort);
		log.debug("Reducing count from respondToClient: send");
		reduceCount(Constants.SEND);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.stonybrook.bank.server.Server#run()
	 */
	@Override
	public void run() {
		log.info("HeartBeat..");
		String masterAddress = ConfigUtil.getProperty(Constants.MASTER_ADDRESS);
		String masterPort = ConfigUtil.getProperty(Constants.MASTER_PORT);
		List<String> params = new ArrayList<String>();
		params.add(server);
		chainReplicationUtil.tcpSend(Constants.CMD_SERVER_HEARTBEAT, params,
				masterAddress, masterPort);
	}

	/**
	 * Send acknowledgement.
	 *
	 * @param cmd
	 *            the cmd
	 * @param params
	 *            the params
	 */
	private void sendAcknowledgement(String cmd, List<String> params) {
		if (predecessorAddress != null && predecessorPort != null) {
			chainReplicationUtil.tcpSend(cmd, params, predecessorAddress,
					predecessorPort);
		}
	}

	/**
	 * Server listen.
	 */
	private void serverListen() {
		Thread serverListener = new Thread(new RunnableTcpListenerFromServers(
				this, ConfigUtil.getProperty(Constants.BANK_SERVER_PORT
						+ server)));
		serverListener.start();
		Thread serverUdpListener = new Thread(
				new RunnableUdpListenerFromServers(this,
						ConfigUtil.getProperty(Constants.BANK_SERVER_PORT
								+ server)));
		serverUdpListener.start();
	}

	/**
	 * Start.
	 *
	 * @param id
	 *            the id
	 */
	private void start(String id) {
		try {
			String[] idArr = id.split("\\.");
			this.bankId = idArr[0];
			this.serverId = idArr[1];
			this.server = bankId + "." + serverId;
			log = chainReplicationUtil.getLogger(Constants.SERVER, server);
			log.info("Server: Address: "
					+ ConfigUtil.getProperty(Constants.BANK_SERVER_ADDRESS
							+ server)
					+ " Port: "
					+ ConfigUtil.getProperty(Constants.BANK_SERVER_PORT
							+ server));
			ConfigUtil.getProperty(Constants.BANK_SERVER_PORT + server);
			log.debug("Server Process Init: " + server + " Completed");
			Integer serverDelay = Integer.valueOf(ConfigUtil
					.getProperty(Constants.BANK_SERVER_STARTUP_DELAY + server));
			log.info("Server Delay: " + serverDelay);
			if (serverDelay > 0) {
				startUpTimer.schedule(new StartupDelayTimerTask(startUpTimer),
						serverDelay);
			}
			receiveCount = Integer.valueOf(ConfigUtil
					.getProperty(Constants.BANK_SERVER_LIFETIME_RECEIVE
							+ server));
			sendCount = Integer.valueOf(ConfigUtil
					.getProperty(Constants.BANK_SERVER_LIFETIME_SEND + server));
			Boolean random = Boolean
					.valueOf(ConfigUtil
							.getProperty(Constants.BANK_SERVER_LIFETIME_RANDOM
									+ server));
			Boolean unbounded = Boolean.valueOf(ConfigUtil
					.getProperty(Constants.BANK_SERVER_LIFETIME_UNBOUNDED
							+ server));
			log.info("Before: receiveCount:" + receiveCount + " sendCount:"
					+ sendCount + " random:" + random.toString()
					+ " unbounded:" + unbounded.toString());
			log.debug("server: " + server + " serverDelay:" + serverDelay);
			if (unbounded) {
				receiveCount = -1;
				sendCount = -1;
			} else if (random) {
				Random rand = new Random();
				receiveCount = (rand.nextInt(10) + 10) * 10;
				sendCount = (rand.nextInt(10) + 10) * 10;
			}
			log.info("After: receiveCount:" + receiveCount + " sendCount:"
					+ sendCount + " random:" + random.toString()
					+ " unbounded:" + unbounded.toString());
			serverListen();
			log.debug("Server Process Init: " + server + " Completed");
		} catch (Exception e) {
			log.error("Exception in Server: ", e);
		}
	}

	/**
	 * Transfer deposit.
	 *
	 * @param reqId
	 *            the req id
	 * @param accNum
	 *            the acc num
	 * @param amount
	 *            the amount
	 * @param sourceServer
	 *            the source server
	 */
	private void transferDeposit(String reqId, String accNum, String amount,
			String sourceServer) {
		log.debug("processing transferDeposit");
		String result = getResult(reqId, Constants.TRANSFER_DEPOSIT);
		if (result == null) {
			result = getAccount(accNum).deposit(reqId, amount);
		} else {
			if (result.contains(Outcome.InconsistentWithHistory.toString())) {
				log.info("Replying InconsistentWithHistory for reqId: " + reqId);
			} else {
				log.info("Duplicate transferDeposit for reqId: " + reqId
						+ " Returning the same reply:" + result);
			}
		}
		log.debug("transferDeposit result: " + result);
		List<String> params = new ArrayList<String>();
		params.add(reqId);
		params.add(result);
		params.add(Constants.TRANSFER_DEPOSIT);
		params.add(accNum);
		params.add(amount);
		params.add(sourceServer);
		processedTrans.put(reqId, params);
		if (-1 == sentTrans.lastIndexOf(reqId)) {
			sentTrans.add(reqId);
		}
		propagateRequest(reqId);
	}

	/**
	 * Transfer withdraw.
	 *
	 * @param reqId
	 *            the req id
	 * @param accNum
	 *            the acc num
	 * @param amount
	 *            the amount
	 * @param destBank
	 *            the dest bank
	 * @param destAccNum
	 *            the dest acc num
	 */
	private void transferWithdraw(String reqId, String accNum, String amount,
			String destBank, String destAccNum) {
		log.info("processing transfer (withdraw at source bank)");
		String result = getResult(reqId, Constants.TRANSFER_WITHDRAW);
		if (result == null) {
			result = getAccount(accNum).withdraw(reqId, amount);
		} else {
			if (result.contains(Outcome.InconsistentWithHistory.toString())) {
				log.info("Replying InconsistentWithHistory for reqId: " + reqId);
			} else {
				log.info("Duplicate transferWithdraw for reqId: " + reqId
						+ " Propagating the same reply:" + result);
			}
		}
		log.info("transfer (withdraw) result: " + result);
		List<String> params = new ArrayList<String>();
		params.add(reqId);
		params.add(result);
		params.add(Constants.TRANSFER_WITHDRAW);
		params.add(accNum);
		params.add(amount);
		params.add(destBank);
		params.add(destAccNum);
		processedTrans.put(reqId, params);
		if (-1 == sentTrans.lastIndexOf(reqId)) {
			sentTrans.add(reqId);
		}
		propagateRequest(reqId);
	}

	/**
	 * Update deposit.
	 *
	 * @param reqId
	 *            the req id
	 * @param result
	 *            the result
	 * @param accNum
	 *            the acc num
	 * @param amount
	 *            the amount
	 */
	private void updateDeposit(String reqId, String result, String accNum,
			String amount) {
		log.debug("storing deposit result " + result + " in server: " + bankId
				+ "." + serverId);
		List<String> params = new ArrayList<String>();
		params.add(reqId);
		params.add(result);
		params.add(Constants.DEPOSIT);
		params.add(accNum);
		params.add(amount);
		String[] resultArr = result.split("\\;");
		log.debug("Saving the Deposit result into replica");
		getAccount(accNum).setBalance(
				Float.valueOf(resultArr[2].substring(0,
						resultArr[2].length() - 1)));
		processedTrans.put(reqId, params);
		if (-1 == sentTrans.lastIndexOf(reqId)) {
			sentTrans.add(reqId);
		}
		propagateRequest(reqId);
	}

	/**
	 * Update get balance.
	 *
	 * @param reqId
	 *            the req id
	 * @param result
	 *            the result
	 * @param accNum
	 *            the acc num
	 */
	private void updateGetBalance(String reqId, String result, String accNum) {
		log.debug("storing getBalance result " + result + " in server: "
				+ bankId + "." + serverId);
		List<String> params = new ArrayList<String>();
		params.add(reqId);
		params.add(result);
		params.add(Constants.GET_BALANCE);
		params.add(accNum);
		String[] resultArr = result.split("\\;");
		log.debug("Saving the getBalance result into replica");
		getAccount(accNum).setBalance(
				Float.valueOf(resultArr[2].substring(0,
						resultArr[2].length() - 1)));
		processedTrans.put(reqId, params);
		if (-1 == sentTrans.lastIndexOf(reqId)) {
			sentTrans.add(reqId);
		}
		propagateRequest(reqId);
	}

	/**
	 * Update transfer deposit.
	 *
	 * @param reqId
	 *            the req id
	 * @param result
	 *            the result
	 * @param accNum
	 *            the acc num
	 * @param amount
	 *            the amount
	 * @param sourceServer
	 *            the source server
	 */
	private void updateTransferDeposit(String reqId, String result,
			String accNum, String amount, String sourceServer) {
		log.debug("storing updateTransferDeposit result " + result
				+ " in server: " + bankId + "." + serverId);
		List<String> params = new ArrayList<String>();
		params.add(reqId);
		params.add(result);
		params.add(Constants.TRANSFER_DEPOSIT);
		params.add(accNum);
		params.add(amount);
		params.add(sourceServer);
		String[] resultArr = result.split("\\;");
		log.debug("Saving the updateTransferDeposit result into replica");
		getAccount(accNum).setBalance(
				Float.valueOf(resultArr[2].substring(0,
						resultArr[2].length() - 1)));
		processedTrans.put(reqId, params);
		if (-1 == sentTrans.lastIndexOf(reqId)) {
			sentTrans.add(reqId);
		}
		propagateRequest(reqId);
	}

	/**
	 * Update transfer withdraw.
	 *
	 * @param reqId
	 *            the req id
	 * @param result
	 *            the result
	 * @param accNum
	 *            the acc num
	 * @param amount
	 *            the amount
	 * @param destBank
	 *            the dest bank
	 * @param destAccNum
	 *            the dest acc num
	 */
	private void updateTransferWithdraw(String reqId, String result,
			String accNum, String amount, String destBank, String destAccNum) {
		log.debug("storing transfer (withdraw) result " + result
				+ " in server: " + bankId + "." + serverId);
		List<String> params = new ArrayList<String>();
		params.add(reqId);
		params.add(result);
		params.add(Constants.TRANSFER_WITHDRAW);
		params.add(accNum);
		params.add(amount);
		params.add(destBank);
		params.add(destAccNum);
		String[] resultArr = result.split("\\;");
		log.info("Saving the Transfer (Withdraw) result into replica");
		getAccount(accNum).setBalance(
				Float.valueOf(resultArr[2].substring(0,
						resultArr[2].length() - 1)));
		processedTrans.put(reqId, params);
		if (-1 == sentTrans.lastIndexOf(reqId)) {
			sentTrans.add(reqId);
		}
		propagateRequest(reqId);
	}

	/**
	 * Update withdraw.
	 *
	 * @param reqId
	 *            the req id
	 * @param result
	 *            the result
	 * @param accNum
	 *            the acc num
	 * @param amount
	 *            the amount
	 */
	private void updateWithdraw(String reqId, String result, String accNum,
			String amount) {
		log.debug("storing withdraw result " + result + " in server: " + bankId
				+ "." + serverId);
		List<String> params = new ArrayList<String>();
		params.add(reqId);
		params.add(result);
		params.add(Constants.WITHDRAW);
		params.add(accNum);
		params.add(amount);
		String[] resultArr = result.split("\\;");
		log.debug("Saving the Withdraw result into replica");
		getAccount(accNum).setBalance(
				Float.valueOf(resultArr[2].substring(0,
						resultArr[2].length() - 1)));
		processedTrans.put(reqId, params);
		if (-1 == sentTrans.lastIndexOf(reqId)) {
			sentTrans.add(reqId);
		}
		propagateRequest(reqId);
	}

	/**
	 * Withdraw.
	 *
	 * @param reqId
	 *            the req id
	 * @param accNum
	 *            the acc num
	 * @param amount
	 *            the amount
	 */
	private void withdraw(String reqId, String accNum, String amount) {
		log.debug("processing withdraw");
		String result = getResult(reqId, Constants.WITHDRAW);
		if (result == null) {
			result = getAccount(accNum).withdraw(reqId, amount);
		} else {
			if (result.contains(Outcome.InconsistentWithHistory.toString())) {
				log.info("Replying InconsistentWithHistory for reqId: " + reqId);
			} else {
				log.info("Duplicate withdrawRequest for reqId: " + reqId
						+ " Returning the same reply:" + result);
			}
		}
		log.debug("withdraw result: " + result);
		List<String> params = new ArrayList<String>();
		params.add(reqId);
		params.add(result);
		params.add(Constants.WITHDRAW);
		params.add(accNum);
		params.add(amount);
		processedTrans.put(reqId, params);
		if (-1 == sentTrans.lastIndexOf(reqId)) {
			sentTrans.add(reqId);
		}
		propagateRequest(reqId);
	}
}
