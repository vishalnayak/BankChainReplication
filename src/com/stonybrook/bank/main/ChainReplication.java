package com.stonybrook.bank.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.stonybrook.bank.common.Constants;
import com.stonybrook.bank.common.ProcessType;
import com.stonybrook.bank.utils.ChainReplicationUtil;
import com.stonybrook.bank.utils.ConfigUtil;

/**
 * The Class ChainReplication. This initiates the whole system. Creates
 * processes for bank servers. Creates process for master. Creates process for
 * client. (Client process spawns threads for each client) Communicates the
 * server list to Master.
 */
public class ChainReplication {
	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		ChainReplication main = new ChainReplication();
		main.start();
	}

	/** The log. */
	private Logger log = null;
	/** The clients. */
	private HashMap<String, Process> clients = new HashMap<String, Process>();

	/** The chain replication util. */
	private ChainReplicationUtil chainReplicationUtil = new ChainReplicationUtil();

	/** The servers id list. */
	private List<String> serversIdList = Collections
			.synchronizedList(new ArrayList<String>());

	/**
	 * Chains list to master.
	 */
	private void chainsListToMaster() {
		int numOfBanks = Integer.valueOf(ConfigUtil
				.getProperty(Constants.NO_OF_BANKS));
		List<String> chainList = new ArrayList<String>();
		for (int i = 1; i <= numOfBanks; i++) {
			// bankId = chainName
			String chain = String.valueOf(i);
			chainList.add(chain);
			// head = server with highest server id
			List<String> head = chainReplicationUtil.getHeadOfChain(chain,
					serversIdList);
			chainList.add(head.get(0));
			chainList.add(head.get(1));
			List<String> tail = chainReplicationUtil.getTailOfChain(chain,
					serversIdList);
			chainList.add(tail.get(0));
			chainList.add(tail.get(1));
		}
		chainReplicationUtil.tcpSend(Constants.CMD_CHAINS_LIST_TO_SERVER,
				chainList, ConfigUtil.getProperty(Constants.MASTER_ADDRESS),
				ConfigUtil.getProperty(Constants.MASTER_PORT));
	}

	/**
	 * Creates the bank clients.
	 */
	private void createBankClients() {
		int numOfBanks = Integer.valueOf(ConfigUtil
				.getProperty(Constants.NO_OF_BANKS));
		for (int i = 1; i <= numOfBanks; i++) {
			List<String> args = new ArrayList<String>();
			args.add(String.valueOf(i));
			Process process = createProcess(ProcessType.CLIENT, args);
			clients.put(String.valueOf(i), process);
		}
	}

	/**
	 * Creates the bank servers.
	 */
	private void createBankServers() {
		int numOfBanks = Integer.valueOf(ConfigUtil
				.getProperty(Constants.NO_OF_BANKS));
		for (int i = 1; i <= numOfBanks; i++) {
			HashMap<String, Process> servers = new HashMap<String, Process>();
			int bankChainLength = Integer.valueOf(ConfigUtil
					.getProperty(Constants.BANK_CHAIN_LENGTH + i));
			for (int j = 1; j <= bankChainLength; j++) {
				List<String> args = new ArrayList<String>();
				args.add(String.valueOf(i) + "." + String.valueOf(j));
				Process process = createProcess(ProcessType.SERVER, args);
				servers.put(String.valueOf(i) + "." + String.valueOf(j),
						process);
				Integer startupDelay = Integer.valueOf(ConfigUtil
						.getProperty(Constants.BANK_SERVER_STARTUP_DELAY
								+ String.valueOf(i) + "." + String.valueOf(j)));
				if (startupDelay == 0) {
					synchronized (serversIdList) {
						serversIdList.add(String.valueOf(i) + "."
								+ String.valueOf(j));
					}
				}
			}
		}
	}

	/**
	 * Creates the process.
	 *
	 * @param processType
	 *            the process type
	 * @param args
	 *            the args
	 * @return the process
	 */
	private Process createProcess(String processType, List<String> args) {
		Process retProcess = null;
		String className = null;
		try {
			switch (processType) {
			case ProcessType.CLIENT:
				className = Class.forName(Constants.CLIENT_CLASS).getName();
				break;
			case ProcessType.SERVER:
				className = Class.forName(Constants.SERVER_CLASS).getName();
				break;
			case ProcessType.MASTER:
				className = Class.forName(Constants.MASTER_CLASS).getName();
				break;
			default:
				log.error("Invalid ClassName");
				return null;
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		List<String> pArgs = new ArrayList<String>();
		pArgs.add(System.getProperty("java.home") + File.separator + "bin"
				+ File.separator + "java");
		pArgs.add("-classpath");
		pArgs.add(System.getProperty("java.class.path"));
		pArgs.add(className);
		pArgs.addAll(args);
		ProcessBuilder pBuilder = new ProcessBuilder(pArgs);
		try {
			pBuilder.redirectErrorStream(true);
			pBuilder.redirectOutput();
			pBuilder.redirectError();
			retProcess = pBuilder.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return retProcess;
	}

	/**
	 * Servers list to master.
	 */
	private void serversListToMaster() {
		chainReplicationUtil.tcpSend(Constants.CMD_SERVER_LIST_TO_MASTER,
				serversIdList,
				ConfigUtil.getProperty(Constants.MASTER_ADDRESS),
				ConfigUtil.getProperty(Constants.MASTER_PORT));
	}

	/**
	 * Start.
	 */
	public void start() {
		try {
			log = chainReplicationUtil.getLogger(Constants.INITIATOR, "");
			log.info("------------------------Chain Replication-------------------------");
			ConfigUtil.printProperties();
			List<String> args = new ArrayList<String>();
			args.add("1");
			createProcess(ProcessType.MASTER, args);
			createBankClients();
			createBankServers();
			Thread.sleep(2000);// Wait till all the servers stabilize.
			serversListToMaster();
			chainsListToMaster();
		} catch (Exception e) {
			// log.error("Something went wrong!" + e);
			e.printStackTrace();
		}
	}
}
// wmic process where "name like '%java%'" delete