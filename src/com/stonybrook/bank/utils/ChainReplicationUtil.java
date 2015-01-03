package com.stonybrook.bank.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.stonybrook.bank.client.Client;
import com.stonybrook.bank.common.Constants;
import com.stonybrook.bank.common.Status;
import com.stonybrook.bank.server.Server;

/**
 * The Class ChainReplicationUtil.
 */
public class ChainReplicationUtil {

	/**
	 * The Class ServersIdListComparator.
	 */
	private class ServersIdListComparator implements Comparator<String> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(String p, String q) {
			String[] pArr = p.split("\\.");
			String[] qArr = q.split("\\.");
			Integer i = Integer.valueOf(pArr[0]);
			Integer j = Integer.valueOf(pArr[1]);
			Integer k = Integer.valueOf(qArr[0]);
			Integer h = Integer.valueOf(qArr[1]);
			if (i < k) {
				return -1;
			} else if (i > k) {
				return 1;
			} else {
				if (j < h) {
					return -1;
				} else if (j > h) {
					return 1;
				} else {
					return 0;
				}
			}
		}
	}

	/**
	 * The Class TakeAction.
	 */
	private class TakeAction implements Runnable {

		/** The client socket. */
		private Socket clientSocket = null;

		/** The server. */
		private Server server = null;

		/**
		 * Instantiates a new take action.
		 *
		 * @param server
		 *            the server
		 * @param clientSocket
		 *            the client socket
		 */
		public TakeAction(Server server, Socket clientSocket) {
			this.server = server;
			this.clientSocket = clientSocket;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			PrintWriter socketWriter = null;
			BufferedReader socketReader = null;
			try {
				socketWriter = new PrintWriter(clientSocket.getOutputStream(),
						true);
				socketReader = new BufferedReader(new InputStreamReader(
						clientSocket.getInputStream()));
				String inData;
				StringBuilder inputBuilder = new StringBuilder();
				while ((inData = socketReader.readLine()) != null) {
					if (inData.equals("done")) {
						String cmdSeq = inputBuilder.toString();
						cmdSeq = cmdSeq.substring(0, cmdSeq.length() - 1);
						socketWriter.println(cmdSeq);
						server.processCommand(cmdSeq.trim());
						break;
					}
					inputBuilder.append(inData + ",");
				}
			} catch (IOException e) {
				// log.error("Error while communicationg with other servers");
				e.printStackTrace();
			} finally {
				if (null != socketWriter)
					socketWriter.close();
				try {
					if (null != socketReader)
						socketReader.close();
					if (null != clientSocket)
						clientSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/** The log. */
	private Logger log = null;

	/** The servers id list comparator. */
	private ServersIdListComparator serversIdListComparator = new ServersIdListComparator();

	/**
	 * Deposit.
	 *
	 * @param client
	 *            the client
	 * @param reqId
	 *            the req id
	 * @param accNum
	 *            the acc num
	 * @param amount
	 *            the amount
	 */
	public void deposit(Client client, String reqId, String accNum,
			String amount) {
		List<String> params = new ArrayList<String>();
		params.add(reqId);
		params.add(accNum);
		params.add(amount);
		sendRequestToServer(Constants.CMD_DEPOSIT_FROM_CLIENT, client, params);
	}

	/**
	 * Gets the balance.
	 *
	 * @param client
	 *            the client
	 * @param reqId
	 *            the req id
	 * @param accNum
	 *            the acc num
	 * @return the balance
	 */
	public void getBalance(Client client, String reqId, String accNum) {
		List<String> params = new ArrayList<String>();
		params.add(reqId);
		params.add(accNum);
		sendRequestToServer(Constants.CMD_GET_BALANCE_FROM_CLIENT, client,
				params);
	}

	/**
	 * Gets the head of chain.
	 *
	 * @param chain
	 *            the chain
	 * @param serversIdList
	 *            the servers id list
	 * @return the head of chain
	 */
	public List<String> getHeadOfChain(String chain, List<String> serversIdList) {
		List<String> result = new ArrayList<String>();
		Collections.sort(serversIdList, serversIdListComparator);
		Iterator<String> iterator = serversIdList.iterator();
		String entry = null;
		Integer found = -1;
		while (iterator.hasNext()) {
			entry = iterator.next();
			String[] entryArr = entry.split("\\.");
			if (entryArr[0].equalsIgnoreCase(chain)) {
				if (found == -1)
					found = 1;
			} else {
				if (found == 1) {
					String head = serversIdList.get(serversIdList
							.indexOf(entry) - 1);
					log.info("Chain Head: " + head);
					result.add(ConfigUtil
							.getProperty(Constants.BANK_SERVER_ADDRESS + head));
					result.add(ConfigUtil
							.getProperty(Constants.BANK_SERVER_PORT + head));
					break;
				}
			}
		}
		if (found == 1 && result.isEmpty()) {
			String head = serversIdList.get(serversIdList.indexOf(entry));
			log.info("Chain Head: " + head);
			result.add(ConfigUtil.getProperty(Constants.BANK_SERVER_ADDRESS
					+ head));
			result.add(ConfigUtil
					.getProperty(Constants.BANK_SERVER_PORT + head));
		}
		return result;
	}

	/**
	 * Gets the logger.
	 *
	 * @param name
	 *            the name
	 * @param id
	 *            the id
	 * @return the logger
	 */
	public Logger getLogger(String name, String id) {
		Logger logger = Logger.getLogger(name + "." + id);
		String logFileName = null;
		if (!id.equals("")) {
			logFileName = name + "." + id + ".log";
		} else {
			logFileName = name + ".log";
		}
		Properties prop = new Properties();
		String appenderName = id.toUpperCase() + "LOG";

		prop.setProperty("log4j.logger." + name + "." + id, "DEBUG,"
				+ appenderName);

		// prop.setProperty("log4j.rootLogger","DEBUG, consoleAppender, " +
		// appenderName);

		prop.setProperty("log4j.appender." + appenderName,
				"org.apache.log4j.FileAppender");
		prop.setProperty("log4j.appender." + appenderName + ".File",
				Constants.LOG_FOLDER + File.separator + logFileName);
		prop.setProperty("log4j.appender." + appenderName + ".layout",
				"org.apache.log4j.PatternLayout");
		prop.setProperty("log4j.appender." + appenderName + ".append", "false");
		prop.setProperty("log4j.appender." + appenderName
				+ ".layout.ConversionPattern", "%d %-5p (%13F:%L) - %m%n");
		prop.setProperty("log4j.appender." + appenderName + ".Threshold",
				"INFO");

		// prop.setProperty("log4j.appender.consoleAppender",
		// "org.apache.log4j.ConsoleAppender");
		// prop.setProperty("log4j.appender.consoleAppender.layout",
		// "org.apache.log4j.PatternLayout");
		// prop.setProperty("log4j.appender.consoleAppender.Threshold", "ALL");
		// prop.setProperty("log4j.appender.consoleAppender.layout.ConversionPattern",
		// "%d{yyyy-MM-dd HH:mm:ss} %-5p :%L > %m%n");

		PropertyConfigurator.configure(prop);
		log = logger;
		return logger;
	}

	/**
	 * Gets the predecessor.
	 *
	 * @param server
	 *            the server
	 * @param serversIdList
	 *            the servers id list
	 * @return the predecessor
	 */
	public List<String> getPredecessor(String server, List<String> serversIdList) {
		List<String> result = new ArrayList<String>();
		String predecessorAddress = null;
		String predecessorPort = null;
		String[] servArr = server.split("\\.");
		String bankId = servArr[0];
		Collections.sort(serversIdList, serversIdListComparator);
		log.debug("Sorted serversIdList: " + serversIdList.toString());
		Integer index = serversIdList.indexOf(server);
		Integer lastIndex = serversIdList.size() - 1;
		Integer predIndex = null;
		if (lastIndex != index) {
			String nextServer = serversIdList.get(index + 1);
			String[] nextArr = nextServer.split("\\.");
			if (nextArr[0].equals(bankId)) {
				predIndex = index + 1;
			}
		}
		if (predIndex != null) {
			predecessorAddress = ConfigUtil
					.getProperty(Constants.BANK_SERVER_ADDRESS
							+ serversIdList.get(predIndex));
			predecessorPort = ConfigUtil.getProperty(Constants.BANK_SERVER_PORT
					+ serversIdList.get(predIndex));
		}
		log.info("PredecessorAddress: " + predecessorAddress
				+ " PredecessorPort: " + predecessorPort);
		result.add(predecessorAddress);
		result.add(predecessorPort);
		return result;
	}

	/**
	 * Gets the servers id list comparator.
	 *
	 * @return the servers id list comparator
	 */
	public ServersIdListComparator getServersIdListComparator() {
		return serversIdListComparator;
	}

	/**
	 * Gets the successor.
	 *
	 * @param server
	 *            the server
	 * @param serversIdList
	 *            the servers id list
	 * @return the successor
	 */
	public List<String> getSuccessor(String server, List<String> serversIdList) {
		List<String> result = new ArrayList<String>();
		String successorAddress = null;
		String successorPort = null;
		String[] servArr = server.split("\\.");
		String bankId = servArr[0];
		Collections.sort(serversIdList, serversIdListComparator);
		Integer index = serversIdList.indexOf(server);
		Integer succIndex = null;
		if (0 != index) {
			String prevServer = serversIdList.get(index - 1);
			String[] prevArr = prevServer.split("\\.");
			if (prevArr[0].equals(bankId)) {
				succIndex = index - 1;
			}
		}

		if (succIndex != null) {
			successorAddress = ConfigUtil
					.getProperty(Constants.BANK_SERVER_ADDRESS
							+ serversIdList.get(succIndex));
			successorPort = ConfigUtil.getProperty(Constants.BANK_SERVER_PORT
					+ serversIdList.get(succIndex));
		}
		log.info("SuccessorAddress: " + successorAddress + " SuccessorPort: "
				+ successorPort);
		result.add(successorAddress);
		result.add(successorPort);
		return result;
	}

	/**
	 * Gets the tail of chain.
	 *
	 * @param chain
	 *            the chain
	 * @param serversIdList
	 *            the servers id list
	 * @return the tail of chain
	 */
	public List<String> getTailOfChain(String chain, List<String> serversIdList) {
		log.info("ChainReplicationUtil.getTailOfChain(): chain:" + chain
				+ " serversIdList:" + serversIdList.toString());
		List<String> result = new ArrayList<String>();
		Collections.sort(serversIdList, serversIdListComparator);
		Iterator<String> iterator = serversIdList.iterator();
		String entry = null;
		while (iterator.hasNext()) {
			entry = iterator.next();
			String[] entryArr = entry.split("\\.");
			if (entryArr[0].equalsIgnoreCase(chain)) {
				String tail = serversIdList.get(serversIdList.indexOf(entry));
				log.info("Chain Tail: " + tail);
				result.add(ConfigUtil.getProperty(Constants.BANK_SERVER_ADDRESS
						+ tail));
				result.add(ConfigUtil.getProperty(Constants.BANK_SERVER_PORT
						+ tail));
				break;
			}
		}
		return result;
	}

	/**
	 * Send request to server.
	 *
	 * @param cmd
	 *            the cmd
	 * @param client
	 *            the client
	 * @param params
	 *            the params
	 */
	private void sendRequestToServer(String cmd, Client client,
			List<String> params) {
		HashMap<String, List<String>> chains = client.getChains();
		List<String> data = chains.get(client.getBankId());
		String chainAddress = null;
		String chainPort = null;
		if (cmd.equals(Constants.CMD_GET_BALANCE_FROM_CLIENT)) {
			chainAddress = data.get(2);
			chainPort = data.get(3);
		} else {
			chainAddress = data.get(0);
			chainPort = data.get(1);
		}

		String waitTime = ConfigUtil
				.getProperty(Constants.BANK_CLIENT_WAIT_TIME
						+ client.getClient());
		String noOfRetries = ConfigUtil
				.getProperty(Constants.BANK_CLIENT_NO_OF_RETRIES
						+ client.getClient());
		log.debug("chainAddress: " + chainAddress + " chainPort: " + chainPort);
		udpSend(cmd, params, chainAddress, chainPort, waitTime, noOfRetries);
	}

	/**
	 * Sets the servers id list comparator.
	 *
	 * @param serversIdListComparator
	 *            the new servers id list comparator
	 */
	public void setServersIdListComparator(
			ServersIdListComparator serversIdListComparator) {
		this.serversIdListComparator = serversIdListComparator;
	}

	/**
	 * Tcp listen.
	 *
	 * @param server
	 *            the server
	 * @param port
	 *            the port
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void tcpListen(Server server, String port) throws IOException {
		int portNum = Integer.valueOf(port);
		ServerSocket tcpSocket = new ServerSocket(portNum);
		try {
			while (true) {
				Thread serviceThread = new Thread(new TakeAction(server,
						tcpSocket.accept()));
				serviceThread.start();
			}
		} finally {
			tcpSocket.close();
		}
	}

	/**
	 * Tcp send.
	 *
	 * @param command
	 *            the command
	 * @param data
	 *            the data
	 * @param address
	 *            the address
	 * @param port
	 *            the port
	 * @return the status
	 */
	public Status tcpSend(String command, List<String> data, String address,
			String port) {
		int portNum = Integer.valueOf(port);
		List<String> input = new ArrayList<String>();
		input.add(command);
		if (null != data) {
			input.addAll(data);
		}
		Status retStatus = Status.FAILURE;
		Socket tcpSocket = null;
		PrintWriter tcpOut = null;
		BufferedReader tcpIn = null;
		try {
			// log.debug("tcpSend: address:" + address + ":port:" + portNum);
			tcpSocket = new Socket(address, portNum);
			tcpSocket.setSoTimeout(500);
			tcpOut = new PrintWriter(tcpSocket.getOutputStream(), true);
			tcpIn = new BufferedReader(new InputStreamReader(
					tcpSocket.getInputStream()));
			input.add("done");
			for (String s : input) {
				// log.debug("Input: " + s);
				tcpOut.println(s);
				if (s.equals("done")) {
					// log.debug("Response: " + tcpIn.readLine());
					break;
				}
			}
			retStatus = Status.SUCCESS;
		} catch (Exception e) {
			// log.error("Error while communicating with Server", e);
			e.printStackTrace();
		} finally {
			if (null != tcpOut)
				tcpOut.close();
			try {
				if (null != tcpIn)
					tcpIn.close();
				if (null != tcpSocket)
					tcpSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return retStatus;
	}

	/**
	 * Tcp send.
	 *
	 * @param command
	 *            the command
	 * @param address
	 *            the address
	 * @param port
	 *            the port
	 * @return the status
	 */
	public Status tcpSend(String command, String address, String port) {
		return tcpSend(command, null, address, port);
	}

	/**
	 * Transfer.
	 *
	 * @param client
	 *            the client
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
	public void transfer(Client client, String reqId, String accNum,
			String amount, String destBank, String destAccNum) {
		List<String> params = new ArrayList<String>();
		params.add(reqId);
		params.add(accNum);
		params.add(amount);
		params.add(destBank);
		params.add(destAccNum);
		sendRequestToServer(Constants.CMD_TRANSFER_FROM_CLIENT, client, params);
	}

	/**
	 * Udp listen.
	 *
	 * @param object
	 *            the object
	 * @param clientPort
	 *            the client port
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void udpListen(Object object, String clientPort) throws IOException {
		if (object instanceof Client) {
			// log = ((Client)object).getLogger();
			// log.debug("Client listening: UDP: ChainReplicationUtil.udpListen()");
		} else {
			// log.debug("Server listening: UDP: ChainReplicationUtil.udpListen()");
		}

		try {
			// log.debug("clientPort: " + clientPort);
			@SuppressWarnings("resource")
			DatagramSocket serverSocket = new DatagramSocket(
					Integer.valueOf(clientPort));

			byte[] receiveData;
			byte[] sendData;

			while (true) {

				receiveData = new byte[1024];
				sendData = new byte[1024];

				DatagramPacket receivePacket = new DatagramPacket(receiveData,
						receiveData.length);

				// log.debug("Waiting for datagram packet on port: " +
				// clientPort);

				serverSocket.receive(receivePacket);

				// log.debug("Received UDP message from: "
				// + receivePacket.getAddress() + ":"
				// + receivePacket.getPort());
				String receivedData = new String(receivePacket.getData());

				if (object instanceof Client) {
					((Client) object).processCommand(receivedData.trim());
				} else {
					((Server) object).processCommand(receivedData.trim());
				}

				String udpResponse = "udpAck";
				sendData = udpResponse.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData,
						sendData.length, receivePacket.getAddress(),
						receivePacket.getPort());
				serverSocket.send(sendPacket);

			}
		} catch (SocketException e) {
			// log.error("SocketException in udpListen.", e);
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Udp send.
	 *
	 * @param command
	 *            the command
	 * @param data
	 *            the data
	 * @param address
	 *            the address
	 * @param port
	 *            the port
	 * @return the status
	 */
	public Status udpSend(String command, List<String> data, String address,
			String port) {
		// return udpSend(command, data, address, port, "1", "3");
		return udpSend(command, data, address, port, null, null);
	}

	/**
	 * Udp send.
	 *
	 * @param command
	 *            the command
	 * @param data
	 *            the data
	 * @param address
	 *            the address
	 * @param port
	 *            the port
	 * @param waitTime
	 *            the wait time
	 * @param noOfRetries
	 *            the no of retries
	 * @return the status
	 */
	public Status udpSend(String command, List<String> data, String address,
			String port, String waitTime, String noOfRetries) {
		int portNum = Integer.valueOf(port);
		Integer numOfRetries = 0;
		Integer soTimeout = 1;
		List<String> input = new ArrayList<String>();
		input.add(command);
		if (null != data) {
			input.addAll(data);
		}
		Status retStatus = Status.FAILURE;

		try {
			DatagramSocket clientSocket = new DatagramSocket();

			InetAddress IPAddress = InetAddress.getByName(address);
			// log.debug("Attempting to connect to " + IPAddress);

			byte[] sendData = new byte[1024];
			byte[] receiveData = new byte[1024];

			String sentence = input.toString();
			sendData = sentence.getBytes();

			// log.debug("Sending data to " + sendData.length
			// + " bytes to server.");
			DatagramPacket sendPacket = new DatagramPacket(sendData,
					sendData.length, IPAddress, portNum);
			if (noOfRetries != null && waitTime != null) {
				numOfRetries = Integer.valueOf(noOfRetries);
				soTimeout = Integer.valueOf(waitTime);
			}
			do {
				clientSocket.send(sendPacket);

				DatagramPacket receivePacket = new DatagramPacket(receiveData,
						receiveData.length);
				clientSocket.setSoTimeout(soTimeout);

				try {
					clientSocket.receive(receivePacket);
					String udpResponse = new String(receivePacket.getData());
					udpResponse = udpResponse.trim();
					if (log != null) {
						log.debug("udpSend: received response:" + udpResponse);
					}
					if (!udpResponse.equals("")) {
						break;
					}
				} catch (SocketTimeoutException e) {
					if (log != null && numOfRetries > 0) {
						log.debug("udpSend SocketTimeoutException. Attemp retry.");
					}
				}
				numOfRetries = 0;// testing
			} while (numOfRetries-- > 0);
			clientSocket.close();
		} catch (UnknownHostException e) {
			// log.error("UnknownHostException in udpSend", e);
			e.printStackTrace();
		} catch (IOException e) {
			// log.error("IOException in udpSend", e);
			e.printStackTrace();
		}

		return retStatus;
	}

	/**
	 * Withdraw.
	 *
	 * @param client
	 *            the client
	 * @param reqId
	 *            the req id
	 * @param accNum
	 *            the acc num
	 * @param amount
	 *            the amount
	 */
	public void withdraw(Client client, String reqId, String accNum,
			String amount) {
		List<String> params = new ArrayList<String>();
		params.add(reqId);
		params.add(accNum);
		params.add(amount);
		sendRequestToServer(Constants.CMD_WITHDRAW_FROM_CLIENT, client, params);
	}
}
