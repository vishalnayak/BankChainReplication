package com.stonybrook.bank.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.stonybrook.bank.common.Constants;
import com.stonybrook.bank.utils.ChainReplicationUtil;
import com.stonybrook.bank.utils.ConfigUtil;

/**
 * The Class ChainReplicationClient. Client process which generates all the
 * client threads for all banks.
 */
public class ChainReplicationClient {
	/**
	 * The Class RunnableClient.
	 */
	class RunnableClient extends Client implements Runnable {

		/**
		 * The Class ClientRequestTimerTask.
		 */
		private class ClientRequestTimerTask extends TimerTask {

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
			public ClientRequestTimerTask(String reqId, Timer timer,
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
				if (requestRecord.get(reqId) == 1) {
					log.debug("Response received for reqId: " + reqId);
					timer.cancel();
				} else {
					log.debug("retry for reqId: " + reqId
							+ " pending retries: " + noOfRetries);
					if (noOfRetries-- > 0) {
						String request = ConfigUtil
								.getProperty(Constants.BANK_CLIENT_REQUEST
										+ reqId);
						processSingleItemRetry(reqId, request);
					} else {
						log.info("Exceeded number of retries: " + reqId);
						timer.cancel();
					}
				}
			}
		}

		/** The initiated requests. */
		private Boolean initiatedRequests = false;

		/** The log. */
		private Logger log = null;

		/** The client id. */
		private String clientId;

		/** The bank id. */
		private String bankId;

		/** The client. */
		private String client;

		/** The seq num. */
		private int seqNum = 1;

		/** The chains. */
		private HashMap<String, List<String>> chains = new HashMap<String, List<String>>();

		/** The request record. */
		private HashMap<String, Integer> requestRecord = new HashMap<String, Integer>();

		/** The timer. */
		private Timer timer = null;

		/** The chain replication util. */
		ChainReplicationUtil chainReplicationUtil = new ChainReplicationUtil();

		/**
		 * Instantiates a new runnable client.
		 *
		 * @param bankId
		 *            the bank id
		 * @param clientId
		 *            the client id
		 */
		public RunnableClient(String bankId, String clientId) {
			client = bankId + "." + clientId;
			log = chainReplicationUtil.getLogger(Constants.CLIENT, client);
			this.bankId = bankId;
			this.clientId = clientId;
		}

		/**
		 * Client listen.
		 */
		private void clientListen() {
			String clientAddress = ConfigUtil
					.getProperty(Constants.BANK_CLIENT_ADDRESS + client);
			String clientPort = ConfigUtil
					.getProperty(Constants.BANK_CLIENT_PORT + client);
			log.info("Address: " + clientAddress + " Port: " + clientPort);
			try {
				chainReplicationUtil.udpListen(this, clientPort);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.stonybrook.bank.client.Client#generateReqId()
		 */
		public String generateReqId() {
			return client + "." + String.valueOf(seqNum++);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.stonybrook.bank.client.Client#getBankId()
		 */
		public String getBankId() {
			return bankId;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.stonybrook.bank.client.Client#getChains()
		 */
		public HashMap<String, List<String>> getChains() {
			return chains;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.stonybrook.bank.client.Client#getClient()
		 */
		public String getClient() {
			return client;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.stonybrook.bank.client.Client#getClientId()
		 */
		public String getClientId() {
			return clientId;
		}

		/**
		 * Initiate requests.
		 */
		private void initiateRequests() {
			log.info("initiatingRequests");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				log.error("Client Thread interrupted while waiting:", e);
			}
			String reqType = ConfigUtil
					.getProperty(Constants.BANK_CLIENT_REQUEST_TYPE + client);
			if (Constants.ITEMIZED.equals(reqType)) {
				processItemizedRequests(false);
			} else if (Constants.DUPLICATE.equals(reqType)) {
				processItemizedRequests(false);
				processItemizedRequests(false);
			} else if (Constants.INCONSISTENT.equals(reqType)) {
				processItemizedRequests(true);
			} else {
				log.debug("initiating randomized requests");
				String randEntry = ConfigUtil
						.getProperty(Constants.BANK_CLIENT_REQUEST_RANDOM
								+ client);
				log.debug("randEntry: " + randEntry);
				String[] randEntryArr = randEntry.split("\\,");
				int numOfReq = Integer.valueOf(randEntryArr[0].trim());
				log.debug("numOfReq: " + numOfReq);
				int getBalanceBound = (int) (Float.valueOf(randEntryArr[1]
						.trim()) * 100);
				log.debug("getBalanceBound: " + getBalanceBound);
				int depositBound = (int) ((Float
						.valueOf(randEntryArr[1].trim()) + Float
						.valueOf(randEntryArr[2].trim())) * 100);
				log.debug("depositBound: " + depositBound);
				int withdrawBound = (int) ((Float.valueOf(randEntryArr[1]
						.trim()) + Float.valueOf(randEntryArr[2].trim()) + Float
						.valueOf(randEntryArr[3].trim())) * 100);
				log.debug("withdrawBound: " + withdrawBound);
				Random randNum = new Random();
				for (int i = 0; i < numOfReq; i++) {
					String accNum = bankId + clientId;
					String amount = String.valueOf(randNum.nextInt(500));
					final int show = randNum.nextInt(100);
					String reqId = generateReqId();
					if (show <= getBalanceBound) {
						log.info("getBalance (" + reqId + "," + accNum + ")");
						chainReplicationUtil.getBalance(this, reqId, accNum);
						trackReqId(reqId);
					} else if (show < depositBound) {
						log.info("deposit (" + reqId + "," + accNum + ","
								+ amount + ")");
						chainReplicationUtil.deposit(this, reqId, accNum,
								amount);
						trackReqId(reqId);
					} else if (show < withdrawBound) {
						log.info("withdraw (" + reqId + "," + accNum + ","
								+ amount + ")");
						chainReplicationUtil.withdraw(this, reqId, accNum,
								amount);
						trackReqId(reqId);
					} else {
						String destBank = "3";
						String destAccNum = "31";
						if (bankId.equals("3")) {
							destBank = "1";
							destAccNum = "11";
						}
						log.info("transfer (" + reqId + "," + accNum + ","
								+ amount + "," + destBank + "," + destAccNum
								+ ")");
						chainReplicationUtil.transfer(this, reqId, accNum,
								amount, destBank, destAccNum);
					}
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.stonybrook.bank.client.Client#processCommand(java.lang.String)
		 */
		public synchronized void processCommand(String cmdSeq) {
			cmdSeq = cmdSeq.trim();
			if (cmdSeq.charAt(0) == "[".charAt(0)) {
				cmdSeq = cmdSeq.substring(1, cmdSeq.length() - 1);
			}
			log.debug("Command received by client thread: " + cmdSeq
					+ " length: " + cmdSeq.length());
			String[] cmdSeqArr = null;
			cmdSeqArr = cmdSeq.split("\\,");
			String[] resultArr = cmdSeqArr[1].split("\\;");
			String reqId = resultArr[0].trim();
			reqId = reqId.substring(1, reqId.length());
			String simulateMessageLoss = null;
			switch (cmdSeqArr[0]) {
			case Constants.CMD_TRANSFER_RESULT_TO_CLIENT:
				simulateMessageLoss = ConfigUtil
						.getProperty(Constants.SIMULATE_MESSAGE_LOSS + client);
				if (simulateMessageLoss == null
						|| simulateMessageLoss.equalsIgnoreCase("false")) {
					// if (requestRecord.get(reqId) == 0) {
					log.info("transferResult: " + cmdSeqArr[1]);
					// }
					requestRecord.put(reqId, 1);
				}
				break;
			case Constants.CMD_GET_BALANCE_RESULT_TO_CLIENT:
				simulateMessageLoss = ConfigUtil
						.getProperty(Constants.SIMULATE_MESSAGE_LOSS + client);
				if (simulateMessageLoss == null
						|| simulateMessageLoss.equalsIgnoreCase("false")) {
					// if (requestRecord.get(reqId) == 0) {
					log.info("getBalanceResult: " + cmdSeqArr[1]);
					// }
					requestRecord.put(reqId, 1);
				}
				break;
			case Constants.CMD_DEPOSIT_RESULT_TO_CLIENT:
				simulateMessageLoss = ConfigUtil
						.getProperty(Constants.SIMULATE_MESSAGE_LOSS + client);
				if (simulateMessageLoss == null
						|| simulateMessageLoss.equalsIgnoreCase("false")) {
					// if (requestRecord.get(reqId) == 0) {
					log.info("depositResult: " + cmdSeqArr[1]);
					// }
					requestRecord.put(reqId, 1);
				}
				break;
			case Constants.CMD_WITHDRAW_RESULT_TO_CLIENT:
				simulateMessageLoss = ConfigUtil
						.getProperty(Constants.SIMULATE_MESSAGE_LOSS + client);
				if (simulateMessageLoss == null
						|| simulateMessageLoss.equalsIgnoreCase("false")) {
					// if (requestRecord.get(reqId) == 0) {
					log.info("withdrawResult: " + cmdSeqArr[1]);
					// }
					requestRecord.put(reqId, 1);
				}
				break;
			case Constants.CMD_CHAIN_LIST_TO_CLIENT:
				for (int i = 1; i < cmdSeqArr.length; i += 5) {
					List<String> data = new ArrayList<String>();
					data.add(cmdSeqArr[i + 1].trim());
					data.add(cmdSeqArr[i + 2].trim());
					data.add(cmdSeqArr[i + 3].trim());
					data.add(cmdSeqArr[i + 4].trim());
					chains.put(cmdSeqArr[i].trim(), data);
				}
				if (!initiatedRequests) {
					log.info("Client Thread received chain information from Master: "
							+ chains.toString());
					initiatedRequests = true;
					initiateRequests();
				} else {
					log.info("Client Thread received 'updated' Chain information from Master."
							+ chains.toString());
				}
				break;
			default:
				break;
			}
		}

		/**
		 * Process itemized requests.
		 *
		 * @param inconsistent
		 *            the inconsistent
		 */
		private void processItemizedRequests(Boolean inconsistent) {
			int reqLength = Integer
					.valueOf(ConfigUtil
							.getProperty(Constants.BANK_CLIENT_REQUEST_LENGTH
									+ client));
			String request = null;
			for (int i = 1; i <= reqLength; i++) {
				request = ConfigUtil.getProperty(Constants.BANK_CLIENT_REQUEST
						+ client + "." + i);
				String reqId = client + "." + i;
				processSingleItem(reqId, request, inconsistent);
			}
		}

		/**
		 * Process single item.
		 *
		 * @param reqId
		 *            the req id
		 * @param request
		 *            the request
		 * @param inconsistent
		 *            the inconsistent
		 */
		private void processSingleItem(String reqId, String request,
				Boolean inconsistent) {
			String[] requestArr = request.split("\\,");
			String accNum = requestArr[1].trim();
			String amount = null;
			switch (requestArr[0].trim()) {
			case Constants.TRANSFER:
				amount = requestArr[2].trim();
				String destBank = requestArr[3].trim();
				String destAccNum = requestArr[4].trim();
				log.info("transfer (" + reqId + "," + accNum + "," + amount
						+ "," + destBank + "," + destAccNum + ")");
				chainReplicationUtil.transfer(this, reqId, accNum, amount,
						destBank, destAccNum);
				trackReqId(reqId);
				if (inconsistent) {
					log.info("Inconsistent request: withdraw (" + reqId + ","
							+ accNum + "," + "1" + ")");
					chainReplicationUtil.withdraw(this, reqId, accNum, "1");
				}
				break;
			case Constants.GET_BALANCE:
				log.info("getBalance (" + reqId + "," + accNum + ")");
				chainReplicationUtil.getBalance(this, reqId, accNum);
				trackReqId(reqId);
				break;
			case Constants.DEPOSIT:
				amount = requestArr[2].trim();
				log.info("deposit (" + reqId + "," + accNum + "," + amount
						+ ")");
				chainReplicationUtil.deposit(this, reqId, accNum, amount);
				trackReqId(reqId);
				if (inconsistent) {
					log.info("Inconsistent request: withdraw (" + reqId + ","
							+ accNum + "," + "1" + ")");
					chainReplicationUtil.withdraw(this, reqId, accNum, "1");
				}
				break;
			case Constants.WITHDRAW:
				amount = requestArr[2].trim();
				log.info("withdraw (" + reqId + "," + accNum + "," + amount
						+ ")");
				chainReplicationUtil.withdraw(this, reqId, accNum, amount);
				trackReqId(reqId);
				if (inconsistent) {
					log.info("Inconsistent request: deposit (" + reqId + ","
							+ accNum + "," + "1" + ")");
					chainReplicationUtil.deposit(this, reqId, accNum, "1");
				}
				break;
			default:
				break;
			}

		}

		/**
		 * Process single item retry.
		 *
		 * @param reqId
		 *            the req id
		 * @param request
		 *            the request
		 */
		private void processSingleItemRetry(String reqId, String request) {
			String[] requestArr = request.split("\\,");
			String accNum = requestArr[1].trim();
			String amount = null;
			switch (requestArr[0].trim()) {
			case Constants.GET_BALANCE:
				log.info("Retry getBalance (" + reqId + "," + accNum + ")");
				chainReplicationUtil.getBalance(this, reqId, accNum);
				break;
			case Constants.DEPOSIT:
				amount = requestArr[2].trim();
				log.info("Retry deposit (" + reqId + "," + accNum + ","
						+ amount + ")");
				chainReplicationUtil.deposit(this, reqId, accNum, amount);
				break;
			case Constants.WITHDRAW:
				amount = requestArr[2].trim();
				log.info("Retry withdraw (" + reqId + "," + accNum + ","
						+ amount + ")");
				chainReplicationUtil.withdraw(this, reqId, accNum, amount);
				break;
			case Constants.TRANSFER:
				amount = requestArr[2].trim();
				String destBank = requestArr[3].trim();
				String destAccNum = requestArr[4].trim();
				log.info("Retry transfer (" + reqId + "," + accNum + ","
						+ amount + "," + destBank + "," + destAccNum + ")");
				chainReplicationUtil.transfer(this, reqId, accNum, amount,
						destBank, destAccNum);
				break;
			default:
				break;
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			log.debug("BankChildThreadLog: " + clientId);
			clientListen();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.stonybrook.bank.client.Client#setBankId(java.lang.String)
		 */
		public void setBankId(String bankId) {
			this.bankId = bankId;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.stonybrook.bank.client.Client#setChains(java.util.HashMap)
		 */
		public void setChains(HashMap<String, List<String>> chains) {
			this.chains = chains;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.stonybrook.bank.client.Client#setClient(java.lang.String)
		 */
		public void setClient(String client) {
			this.client = client;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.stonybrook.bank.client.Client#setClientId(java.lang.String)
		 */
		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		/**
		 * Track req id.
		 *
		 * @param reqId
		 *            the req id
		 */
		private void trackReqId(String reqId) {
			reqId = reqId.trim();
			requestRecord.put(reqId, 0);
			timer = new Timer();
			long waitTime = Long.valueOf(ConfigUtil
					.getProperty(Constants.BANK_CLIENT_WAIT_TIME + client));
			Integer noOfRetries = Integer.valueOf(ConfigUtil
					.getProperty(Constants.BANK_CLIENT_NO_OF_RETRIES + client));
			log.debug("Tracking reqId: waitTime:" + waitTime + " noOfRetries:"
					+ noOfRetries);
			timer.schedule(
					new ClientRequestTimerTask(reqId, timer, noOfRetries),
					waitTime, waitTime);
		}
	}

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		ChainReplicationClient client = new ChainReplicationClient();
		client.start(args[0]);
	}

	/**
	 * Start.
	 *
	 * @param bankId
	 *            the bank id
	 */
	private void start(String bankId) {
		// log.debug("Client Process Init: " + bankId + " Started");
		int numOfClients = Integer.valueOf(ConfigUtil
				.getProperty(Constants.NO_OF_CLIENTS + bankId));
		for (int i = 1; i <= numOfClients; i++) {
			Thread thread = new Thread(new RunnableClient(bankId,
					String.valueOf(i)));
			thread.start();
		}
		// log.debug("Client Process Init: " + bankId + " Completed");
	}
}
