package com.stonybrook.bank.master;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;

import org.apache.log4j.Logger;

import com.stonybrook.bank.common.Constants;
import com.stonybrook.bank.server.RunnableTcpListenerFromServers;
import com.stonybrook.bank.server.RunnableUdpListenerFromServers;
import com.stonybrook.bank.server.Server;
import com.stonybrook.bank.utils.ChainReplicationUtil;
import com.stonybrook.bank.utils.ConfigUtil;

/**
 * The Class ChainReplicationMaster.
 */
public class ChainReplicationMaster extends Server {
	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		ChainReplicationMaster master = new ChainReplicationMaster();
		master.start(args[0]);
	}

	/** The servers id list. */
	private List<String> serversIdList = Collections
			.synchronizedList(new ArrayList<String>());

	/** The log. */
	private Logger log = null;

	/** The timer started. */
	private Boolean timerStarted = false;

	/** The chain replication util. */
	private ChainReplicationUtil chainReplicationUtil = new ChainReplicationUtil();
	/** The chains. */
	private HashMap<String, List<String>> chains = new HashMap<String, List<String>>();

	/** The servers alive. */
	private Map<String, Integer> serversAlive = Collections
			.synchronizedMap(new HashMap<String, Integer>());

	/** The timer. */
	private Timer timer = new Timer();

	/**
	 * Identify failure and act.
	 *
	 * @param server
	 *            the server
	 */
	private void identifyFailureAndAct(String server) {
		String failedServerAddress = ConfigUtil
				.getProperty(Constants.BANK_SERVER_ADDRESS + server);
		String failedServerPort = ConfigUtil
				.getProperty(Constants.BANK_SERVER_PORT + server);
		log.info("Failed Server: " + server);
		log.info("failedServerAddress: " + failedServerAddress);
		log.info("failedServerPort: " + failedServerPort);
		log.debug("chains: " + chains.toString());
		Iterator<Entry<String, List<String>>> iterator = chains.entrySet()
				.iterator();
		Boolean detected = false;
		while (iterator.hasNext()) {
			Entry<String, List<String>> entry = iterator.next();
			List<String> data = (List<String>) entry.getValue();
			if (failedServerAddress.equals(data.get(0))
					&& failedServerPort.equals(data.get(1))) {
				log.info("Server Failed: Head. Chain: " + entry.getKey());
				detected = true;
				processHeadFailure(server);
				break;
			}
			if (failedServerAddress.equals(data.get(2))
					&& failedServerPort.equals(data.get(3))) {
				log.info("Server Failed: Tail. Chain: " + entry.getKey());
				detected = true;
				processTailFailure(server);
				break;
			}
		}
		if (!detected) {
			log.info("Server Failed: Internal. Chain: " + server.charAt(0));
			processInternalServerFailure(server);
		}
	}

	/**
	 * Master listen.
	 */
	private void masterListen() {
		Thread serverTcpListener = new Thread(
				new RunnableTcpListenerFromServers(this,
						ConfigUtil.getProperty(Constants.MASTER_PORT)));
		serverTcpListener.start();
		Thread serverUdpListener = new Thread(
				new RunnableUdpListenerFromServers(this,
						ConfigUtil.getProperty(Constants.MASTER_PORT)));
		serverUdpListener.start();
	}

	/**
	 * Prepare chain list.
	 *
	 * @return the list
	 */
	private List<String> prepareChainList() {
		List<String> chainList = new ArrayList<String>();
		Iterator<Entry<String, List<String>>> iterator = chains.entrySet()
				.iterator();
		while (iterator.hasNext()) {
			Entry<String, List<String>> entry = iterator.next();
			chainList.add(entry.getKey().toString());
			List<String> data = (List<String>) entry.getValue();
			for (String s : data) {
				chainList.add(s);
			}
		}
		return chainList;
	}

	/**
	 * Process chain extension.
	 *
	 * @param server
	 *            the server
	 */
	private void processChainExtension(String server) {
		log.info("Process chain extension. New Server: " + server);
		log.info("Chains: " + chains.toString());
		String[] servArr = server.split("\\.");
		List<String> entry = chains.get(servArr[0]);
		chainReplicationUtil.tcpSend(Constants.CMD_TAIL_DEACTIVATED,
				entry.get(2), entry.get(3));
		synchronized (serversIdList) {
			log.info("Adding : " + server);
			serversIdList.add(server);
		}
		sendServersListToAllServers();
		String newServerAddress = ConfigUtil
				.getProperty(Constants.BANK_SERVER_ADDRESS + server);
		String newServerPort = ConfigUtil
				.getProperty(Constants.BANK_SERVER_PORT + server);
		List<String> newServer = new ArrayList<String>();
		newServer.add(newServerAddress);
		newServer.add(newServerPort);
		updateTailInChains(server, newServer);
		sendChainInfoToAllServers();
		sendChainInfoToAllClients();
		String terminateTail = ConfigUtil
				.getProperty(Constants.KILL_CURRENT_TAIL + server);
		if (terminateTail != null && terminateTail.equalsIgnoreCase("true")) {
			log.info("Terminating current Tail: " + terminateTail);
			List<String> predecessor = chainReplicationUtil.getPredecessor(
					server, serversIdList);
			log.info("ServerIdList: " + serversIdList.toString());
			log.info("Server: " + server);
			log.info("Current tail getting terminated before activating new tail."
					+ predecessor.toString());
			chainReplicationUtil.tcpSend(Constants.CMD_TERMINATE,
					predecessor.get(0), predecessor.get(1));
		}
		String testGracefulAbort = ConfigUtil
				.getProperty(Constants.TEST_GRACEFUL_ABORT + server);
		if (testGracefulAbort == null
				|| testGracefulAbort.equalsIgnoreCase("false")) {
			synchronized (serversAlive) {
				serversAlive.put(server, 1);
			}
			chainReplicationUtil.tcpSend(Constants.CMD_TAIL_ACTIVATED,
					newServerAddress, newServerPort);
		} else {
			log.info("Testing Graceful Abort. Killing new server:" + server);
			chainReplicationUtil.tcpSend(Constants.CMD_TERMINATE,
					newServerAddress, newServerPort);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.stonybrook.bank.server.Server#processCommand(java.lang.String)
	 */
	public synchronized void processCommand(String cmdSeq) {
		try {
			log.debug("processCommand Input: " + cmdSeq);
			String[] cmdSeqArr = null;
			cmdSeqArr = cmdSeq.split("\\,");
			switch (cmdSeqArr[0]) {
			case Constants.CMD_NEW_SERVER:
				processChainExtension(cmdSeqArr[1]);
				break;
			case Constants.CMD_SERVER_HEARTBEAT:
				synchronized (serversAlive) {
					serversAlive.put(cmdSeqArr[1], 1);
				}
				if (!timerStarted) {
					timerStarted = true;
					Integer timeout = Integer
							.valueOf(ConfigUtil
									.getProperty(Constants.MASTER_SERVERS_CHECK_TIMEOUT));
					timer.schedule(this, 2000, timeout);
					log.info("Master's timer started");
				}
				break;
			case Constants.CMD_SERVER_LIST_TO_MASTER:
				log.debug("serversIdList.size()" + serversIdList.size());
				synchronized (serversIdList) {
					serversIdList.removeAll(serversIdList);
					for (int i = 1; i < cmdSeqArr.length; i++) {
						serversIdList.add(cmdSeqArr[i]);
					}
					log.info("Master received serversIdList: "
							+ serversIdList.size());
				}
				sendServersListToAllServers();
				break;
			case Constants.CMD_CHAINS_LIST_TO_SERVER:
				for (int i = 1; i < cmdSeqArr.length; i += 5) {
					List<String> data = new ArrayList<String>();
					data.add(cmdSeqArr[i + 1].trim());
					data.add(cmdSeqArr[i + 2].trim());
					data.add(cmdSeqArr[i + 3].trim());
					data.add(cmdSeqArr[i + 4].trim());
					chains.put(cmdSeqArr[i].trim(), data);
					log.debug("Adding " + cmdSeqArr[i] + " data: "
							+ data.toString());
				}
				sendChainInfoToAllServers();
				sendActivateToAllTails();
				// This call will start requests from clients in no time.
				// Make sure pre-requisites are in proper state before making
				// this call.
				sendChainInfoToAllClients();
				break;
			case Constants.CMD_PRINT_SERVER_LIST_IN_MASTER:
				if (null == serversIdList) {
					log.error("serversIdList is null. Fix it.");
				} else {
					log.debug(serversIdList.toString());
				}
				break;
			default:
				break;
			}
		} catch (Exception e) {
			log.error("Exception caught in Master.processCommand: ", e);
		}
	}

	/**
	 * Process head failure.
	 *
	 * @param server
	 *            the server
	 */
	private void processHeadFailure(String server) {
		// identify next head.
		List<String> successor = chainReplicationUtil.getSuccessor(server,
				serversIdList);
		synchronized (serversIdList) {
			log.info("Removing : " + server);
			serversIdList.remove(server);
		}
		// update chain information in master
		updateHeadInChains(server, successor);
		// inform servers about the new living servers list
		sendServersListToAllServers();
		// inform servers
		sendChainInfoToAllServers();
		// inform clients
		sendChainInfoToAllClients();
	}

	/**
	 * Process internal server failure.
	 *
	 * @param server
	 *            the server
	 */
	private void processInternalServerFailure(String server) {
		List<String> predecessor = chainReplicationUtil.getPredecessor(server,
				serversIdList);
		List<String> successor = chainReplicationUtil.getSuccessor(server,
				serversIdList);
		synchronized (serversIdList) {
			log.info("Removing : " + server);
			serversIdList.remove(server);
		}
		sendServersListToAllServers();
		log.info("Informing: " + predecessor.get(0) + ":" + predecessor.get(1));
		chainReplicationUtil.tcpSend(Constants.CMD_PROPAGATE_PENDING,
				predecessor.get(0), predecessor.get(1));
		chainReplicationUtil.tcpSend(Constants.CMD_PROCESS_PENDING,
				successor.get(0), successor.get(1));
	}

	/**
	 * Process tail failure.
	 *
	 * @param server
	 *            the server
	 */
	private void processTailFailure(String server) {
		// identify next tail
		List<String> predecessor = chainReplicationUtil.getPredecessor(server,
				serversIdList);
		synchronized (serversIdList) {
			log.info("Removing : " + server);
			serversIdList.remove(server);
		}
		// update chain information in master
		updateTailInChains(server, predecessor);
		// inform servers about the new living servers list
		sendServersListToAllServers();
		// inform servers about the new head and tails of each chain
		sendChainInfoToAllServers();
		// inform clients about the new head and tails of each chain
		sendChainInfoToAllClients();
		String[] servArr = server.split("\\.");
		List<String> entry = chains.get(servArr[0]);
		chainReplicationUtil.tcpSend(Constants.CMD_TAIL_ACTIVATED,
				entry.get(2), entry.get(3));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.stonybrook.bank.server.Server#run()
	 */
	public synchronized void run() {
		log.info("Periodic Servers Test:");
		Set<String> s = serversAlive.keySet();
		Iterator<String> serversIterator = serversIdList.iterator();
		synchronized (serversAlive) {
			log.info("HeartBeat received from Servers: "
					+ serversAlive.toString());
			// serversAlive.put("5.1", 1);//testing
			// serversAlive.put("5.2", 1);//testing
			String failedServer = null;
			while (serversIterator.hasNext()) {
				String server = serversIterator.next();
				// serversAlive.remove("2.2");//testing
				// log.info("result: " + serversAlive.get("2.1"));
				if (null != serversAlive.get(server)) {
					log.debug("Server Alive: " + server);
					serversAlive.remove(server);
				} else {
					// server has failed. identify the server type from here.
					log.debug("Server Failed: " + server
							+ ". Initiating reconfiguration..");
					failedServer = server;
				}
			}
			if (null != failedServer) {
				identifyFailureAndAct(failedServer);
			}
			s = serversAlive.keySet();
			Iterator<String> i = s.iterator();
			while (i.hasNext()) {
				String entry = i.next();
				log.info("new server: " + entry
						+ ". Initiating chain extension..");
				// serversIdList.add(entry);
				// entry is the new server. process chain extension from here.
				i.remove();
			}
		}
	}

	/**
	 * Send activate to all tails.
	 */
	private void sendActivateToAllTails() {
		Iterator<Entry<String, List<String>>> iterator = chains.entrySet()
				.iterator();
		while (iterator.hasNext()) {
			Entry<String, List<String>> entry = iterator.next();
			List<String> data = (List<String>) entry.getValue();
			chainReplicationUtil.tcpSend(Constants.CMD_TAIL_ACTIVATED,
					data.get(2), data.get(3));
		}
	}

	/**
	 * Send server info to all clients.
	 */
	private void sendChainInfoToAllClients() {
		List<String> chainList = prepareChainList();
		int numOfBanks = Integer.valueOf(ConfigUtil
				.getProperty(Constants.NO_OF_BANKS));
		for (int i = 1; i <= numOfBanks; i++) {
			int bankClientsLength = Integer.valueOf(ConfigUtil
					.getProperty(Constants.NO_OF_CLIENTS + i));
			log.debug("bankClientsLengh: " + bankClientsLength);
			for (int j = 1; j <= bankClientsLength; j++) {
				String clientAddress = ConfigUtil
						.getProperty(Constants.BANK_CLIENT_ADDRESS + i + "."
								+ j);
				log.debug("clientAddress: " + clientAddress);
				String clientPort = ConfigUtil
						.getProperty(Constants.BANK_CLIENT_PORT + i + "." + j);
				log.debug("clientPort: " + clientPort);
				chainReplicationUtil.udpSend(
						Constants.CMD_CHAIN_LIST_TO_CLIENT, chainList,
						clientAddress, clientPort);
			}
		}
	}

	/**
	 * Send server info to all servers.
	 */
	private void sendChainInfoToAllServers() {
		List<String> chainList = prepareChainList();
		for (String s : serversIdList) {
			String serverAddress = ConfigUtil
					.getProperty(Constants.BANK_SERVER_ADDRESS + s);
			String serverPort = ConfigUtil
					.getProperty(Constants.BANK_SERVER_PORT + s);
			log.debug("serverAddress: " + serverAddress);
			log.debug("serverPort" + serverPort);
			chainReplicationUtil.tcpSend(Constants.CMD_CHAINS_LIST_TO_SERVER,
					chainList, serverAddress, serverPort);
		}
	}

	/**
	 * Servers list to all servers.
	 */
	private void sendServersListToAllServers() {
		for (int i = 0; i < serversIdList.size(); i++) {
			String serverAddress = ConfigUtil
					.getProperty(Constants.BANK_SERVER_ADDRESS
							+ serversIdList.get(i));
			String serverPort = ConfigUtil
					.getProperty(Constants.BANK_SERVER_PORT
							+ serversIdList.get(i));
			chainReplicationUtil.tcpSend(Constants.CMD_SERVERS_LIST_TO_SERVERS,
					serversIdList, serverAddress, serverPort);
		}
	}

	/**
	 * Start.
	 *
	 * @param masterId
	 *            the master id
	 */
	public void start(String masterId) {
		try {
			log = chainReplicationUtil.getLogger(Constants.MASTER, "");
			log.info("Master Process Started");
			masterListen();
		} catch (Exception e) {
			log.error("Exception in Master:", e);
		}
	}

	/**
	 * Update head in chains.
	 *
	 * @param server
	 *            the server
	 * @param successor
	 *            the successor
	 */
	private void updateHeadInChains(String server, List<String> successor) {
		String[] servArr = server.split("\\.");
		List<String> entry = chains.get(servArr[0]);
		log.debug("successor: " + successor.toString());
		log.debug("updateHeadInChains before: " + entry.toString());
		entry.remove(0);
		entry.remove(0);
		entry.add(0, successor.get(1));
		entry.add(0, successor.get(0));
		log.debug("updateHeadInChains after: " + entry.toString());
	}

	/**
	 * Update tail in chains.
	 *
	 * @param server
	 *            the server
	 * @param predecessor
	 *            the predecessor
	 */
	private void updateTailInChains(String server, List<String> predecessor) {
		String[] servArr = server.split("\\.");
		List<String> entry = chains.get(servArr[0]);
		log.debug("predecessor: " + predecessor.toString());
		log.debug("updateTailInChains before: " + entry.toString());
		entry.remove(3);
		entry.remove(2);
		entry.add(predecessor.get(0));
		entry.add(predecessor.get(1));
		log.debug("updateTailInChains after: " + entry.toString());
	}
}
