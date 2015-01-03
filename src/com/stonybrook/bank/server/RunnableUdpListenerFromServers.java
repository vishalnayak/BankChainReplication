package com.stonybrook.bank.server;

import java.io.IOException;

import com.stonybrook.bank.utils.ChainReplicationUtil;

/**
 * The Class RunnableUdpListenerFromServers.
 */
public class RunnableUdpListenerFromServers implements Runnable {

	/** The server. */
	private Server server = null;

	/** The port. */
	private String port = null;

	/** The server util. */
	ChainReplicationUtil serverUtil = new ChainReplicationUtil();

	/**
	 * Instantiates a new runnable udp listener from servers.
	 *
	 * @param server
	 *            the server
	 * @param port
	 *            the port
	 */
	public RunnableUdpListenerFromServers(Server server, String port) {
		this.server = server;
		this.port = port;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		try {
			serverUtil.udpListen(server, port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
