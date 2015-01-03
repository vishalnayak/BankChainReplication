package com.stonybrook.bank.common;

/**
 * The Interface Constants.
 */
public interface Constants {

	/** The itemized. */
	String ITEMIZED = "itemized";

	/** The random. */
	String RANDOM = "random";

	/** The inconsistent. */
	String INCONSISTENT = "inconsistent";

	/** The duplicate. */
	String DUPLICATE = "duplicate";

	/** The master. */
	String MASTER = "master";

	/** The initiator. */
	String INITIATOR = "initiator";

	/** The client. */
	String CLIENT = "client";

	/** The server. */
	String SERVER = "server";

	/** The log folder. */
	String LOG_FOLDER = "logs";

	/** The receive. */
	String RECEIVE = "receive";

	/** The send. */
	String SEND = "send";

	/** The get balance. */
	String GET_BALANCE = "getBalance";

	/** The deposit. */
	String DEPOSIT = "deposit";

	/** The withdraw. */
	String WITHDRAW = "withdraw";

	/** The transfer. */
	String TRANSFER = "transfer";

	/** The transfer withdraw. */
	String TRANSFER_WITHDRAW = "transfer_withdraw";

	/** The transfer deposit. */
	String TRANSFER_DEPOSIT = "transfer_deposit";

	/** The init address. */
	String INIT_ADDRESS = "init.address";

	/** The init port. */
	String INIT_PORT = "init.port";

	/** The config file. */
	String CONFIG_FILE = "/config.properties";

	/** The log config. */
	String LOG_CONFIG = "/log.properties";

	/** The client class. */
	String CLIENT_CLASS = "com.stonybrook.bank.client.ChainReplicationClient";

	/** The server class. */
	String SERVER_CLASS = "com.stonybrook.bank.server.ChainReplicationServer";

	/** The master class. */
	String MASTER_CLASS = "com.stonybrook.bank.master.ChainReplicationMaster";

	// Commands
	/** The cmd server list to master. */
	String CMD_SERVER_LIST_TO_MASTER = "cmd_server_list_to_master";

	/** The cmd print server list in master. */
	String CMD_PRINT_SERVER_LIST_IN_MASTER = "cmd_print_server_list_in_master";

	/** The cmd chains list to server. */
	String CMD_CHAINS_LIST_TO_SERVER = "cmd_chains_list_to_server";

	/** The cmd servers list to servers. */
	String CMD_SERVERS_LIST_TO_SERVERS = "cmd_servers_list_to_servers";

	/** The cmd chain list to client. */
	String CMD_CHAIN_LIST_TO_CLIENT = "cmd_chain_list_to_client";

	/** The cmd get balance from client. */
	String CMD_GET_BALANCE_FROM_CLIENT = "cmd_get_balance_from_client";

	/** The cmd update get balance. */
	String CMD_UPDATE_GET_BALANCE = "cmd_update_get_balance";

	/** The cmd get balance result to client. */
	String CMD_GET_BALANCE_RESULT_TO_CLIENT = "cmd_get_balance_result_to_client";

	/** The cmd withdraw from client. */
	String CMD_WITHDRAW_FROM_CLIENT = "cmd_withdraw_from_client";

	/** The cmd update withdraw. */
	String CMD_UPDATE_WITHDRAW = "cmd_update_withdraw";

	/** The cmd withdraw result to client. */
	String CMD_WITHDRAW_RESULT_TO_CLIENT = "cmd_withdraw_result_to_client";

	/** The cmd update deposit. */
	String CMD_UPDATE_DEPOSIT = "cmd_update_deposit";

	/** The cmd deposit from client. */
	String CMD_DEPOSIT_FROM_CLIENT = "cmd_deposit_from_client";

	/** The cmd deposit result to client. */
	String CMD_DEPOSIT_RESULT_TO_CLIENT = "cmd_deposit_result_to_client";

	/** The cmd update transfer withdraw. */
	String CMD_UPDATE_TRANSFER_WITHDRAW = "cmd_update_transfer_withdraw";

	/** The cmd transfer from client. */
	String CMD_TRANSFER_FROM_CLIENT = "cmd_transfer_from_client";

	/** The cmd transfer result to client. */
	String CMD_TRANSFER_RESULT_TO_CLIENT = "cmd_transfer_result_to_client";

	/** The cmd update transfer deposit. */
	String CMD_UPDATE_TRANSFER_DEPOSIT = "cmd_update_transfer_deposit";

	/** The cmd transfer deposit. */
	String CMD_TRANSFER_DEPOSIT = "cmd_transfer_deposit";

	/** The cmd transfer deposit acknowledgement. */
	String CMD_TRANSFER_DEPOSIT_ACKNOWLEDGEMENT = "cmd_transfer_deposit_acknowledgement";

	/** The cmd server heartbeat. */
	String CMD_SERVER_HEARTBEAT = "cmd_server_heartbeat";

	/** The cmd acknowledgement. */
	String CMD_ACKNOWLEDGEMENT = "cmd_acknowledgement";

	/** The cmd tail activated. */
	String CMD_TAIL_ACTIVATED = "cmd_tail_activated";

	/** The cmd tail deactivated. */
	String CMD_TAIL_DEACTIVATED = "cmd_tail_deactivated";

	/** The cmd propagate pending. */
	String CMD_PROPAGATE_PENDING = "cmd_propagate_pending";

	/** The cmd process pending. */
	String CMD_PROCESS_PENDING = "cmd_process_pending";

	/** The cmd new server. */
	String CMD_NEW_SERVER = "cmd_new_server";

	/** The cmd terminate. */
	String CMD_TERMINATE = "cmd_terminate";

	// Properties related to Master
	/** The master address. */
	String MASTER_ADDRESS = "master.address";

	/** The master port. */
	String MASTER_PORT = "master.port";

	/** The master servers check timeout. */
	String MASTER_SERVERS_CHECK_TIMEOUT = "master.servers.check.timeout";

	// Properties related to Clients
	/** The bank client address. */
	String BANK_CLIENT_ADDRESS = "bank.client.address.";

	/** The bank client port. */
	String BANK_CLIENT_PORT = "bank.client.port.";

	/** The bank client request. */
	String BANK_CLIENT_REQUEST = "bank.client.request.";

	/** The bank client request length. */
	String BANK_CLIENT_REQUEST_LENGTH = "bank.client.request.length.";

	/** The bank client request random. */
	String BANK_CLIENT_REQUEST_RANDOM = "bank.client.request.random.";

	/** The bank client request type. */
	String BANK_CLIENT_REQUEST_TYPE = "bank.client.request.type.";

	/** The bank client wait time. */
	String BANK_CLIENT_WAIT_TIME = "bank.client.wait_time.";

	/** The bank client no of retries. */
	String BANK_CLIENT_NO_OF_RETRIES = "bank.client.no_of_retries.";

	// Properties related to Banks
	/** The bank server address. */
	String BANK_SERVER_ADDRESS = "bank.server.address.";

	/** The bank server port. */
	String BANK_SERVER_PORT = "bank.server.port.";

	/** The no of banks. */
	String NO_OF_BANKS = "bank.no_of_banks";

	/** The bank chain length. */
	String BANK_CHAIN_LENGTH = "bank.chain.length.";

	/** The no of clients. */
	String NO_OF_CLIENTS = "bank.no_of_clients.";

	/** The bank server startup delay. */
	String BANK_SERVER_STARTUP_DELAY = "bank.server.startup_delay.";

	/** The bank server lifetime unbounded. */
	String BANK_SERVER_LIFETIME_UNBOUNDED = "bank.server.lifetime.unbounded.";

	/** The bank server lifetime random. */
	String BANK_SERVER_LIFETIME_RANDOM = "bank.server.lifetime.random.";

	/** The bank server lifetime send. */
	String BANK_SERVER_LIFETIME_SEND = "bank.server.lifetime.send.";

	/** The bank server lifetime receive. */
	String BANK_SERVER_LIFETIME_RECEIVE = "bank.server.lifetime.receive.";

	/** The bank server heartbeat timeout. */
	String BANK_SERVER_HEARTBEAT_TIMEOUT = "bank.server.heartbeat.timeout";

	/** The subfail. */
	String SUBFAIL = "bank.server.subfail.";

	/** The kill current tail. */
	String KILL_CURRENT_TAIL = "bank.server.kill_current_tail.";

	/** The test graceful abort. */
	String TEST_GRACEFUL_ABORT = "bank.server.test_graceful.";

	/** The test transfer deposit fail. */
	String TEST_TRANSFER_DEPOSIT_FAIL = "bank.server.transfer.deposit.fail.";

	/** The test transfer withdraw fail. */
	String TEST_TRANSFER_WITHDRAW_FAIL = "bank.server.transfer.withdraw.fail.";

	/** The simulate message loss. */
	String SIMULATE_MESSAGE_LOSS = "bank.client.simulate_message_loss.";
}
