package com.stonybrook.bank.server;

import java.io.IOException;

import com.stonybrook.bank.utils.ChainReplicationUtil;

/**
 * The Class RunnableTcpListenerFromServers.
 */
public class RunnableTcpListenerFromServers implements Runnable {

	/** The server. */
	Server server = null;

	/** The port. */
	String port = null;

	/** The server util. */
	ChainReplicationUtil serverUtil = new ChainReplicationUtil();

	/**
	 * Instantiates a new runnable tcp listener from servers.
	 *
	 * @param server
	 *            the server
	 * @param port
	 *            the port
	 */
	public RunnableTcpListenerFromServers(Server server, String port) {
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
			serverUtil.tcpListen(server, port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
