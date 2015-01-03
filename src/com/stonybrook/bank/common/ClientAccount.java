package com.stonybrook.bank.common;

/**
 * The Class ClientAccount.
 */
public class ClientAccount {

	/** The client name. */
	private String clientName = null;

	/** The account number. */
	private Integer accountNumber = null;

	/** The balance. */
	private Float balance = null;

	/**
	 * Instantiates a new client account.
	 *
	 * @param clientName
	 *            the client name
	 * @param accountNumber
	 *            the account number
	 * @param balance
	 *            the balance
	 */
	public ClientAccount(String clientName, Integer accountNumber, Float balance) {
		this.clientName = clientName;
		this.accountNumber = accountNumber;
		this.balance = balance;
	}

	/**
	 * Gets the account number.
	 *
	 * @return the account number
	 */
	public Integer getAccountNumber() {
		return accountNumber;
	}

	/**
	 * Gets the balance.
	 *
	 * @return the balance
	 */
	public Float getBalance() {
		return balance;
	}

	/**
	 * Gets the client name.
	 *
	 * @return the client name
	 */
	public String getClientName() {
		return clientName;
	}

	/**
	 * Sets the account number.
	 *
	 * @param accountNumber
	 *            the new account number
	 */
	public void setAccountNumber(Integer accountNumber) {
		this.accountNumber = accountNumber;
	}

	/**
	 * Sets the balance.
	 *
	 * @param balance
	 *            the new balance
	 */
	public void setBalance(Float balance) {
		this.balance = balance;
	}

	/**
	 * Sets the client name.
	 *
	 * @param clientName
	 *            the new client name
	 */
	public void setClientName(String clientName) {
		this.clientName = clientName;
	}
}
