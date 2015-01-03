package com.stonybrook.bank.server;

import com.stonybrook.bank.common.Outcome;
import com.stonybrook.bank.common.Reply;

/**
 * The Class Account.
 */
public class Account {

	/** The balance. */
	private Float balance = (float) 0;

	/** The acc num. */
	private String accNum = null;

	/**
	 * Instantiates a new account.
	 *
	 * @param accNum
	 *            the acc num
	 * @param balance
	 *            the balance
	 */
	public Account(String accNum, float balance) {
		this.balance = balance;
		this.accNum = accNum;
	}

	/**
	 * Deposit.
	 *
	 * @param reqId
	 *            the req id
	 * @param amount
	 *            the amount
	 * @return the string
	 */
	public String deposit(String reqId, String amount) {
		Float amt = Float.valueOf(amount);
		balance += amt;
		Reply reply = new Reply(reqId, Outcome.Processed, balance);
		return reply.toString();
	}

	/**
	 * Gets the acc num.
	 *
	 * @return the acc num
	 */
	public String getAccNum() {
		return accNum;
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
	 * Gets the balance.
	 *
	 * @param reqId
	 *            the req id
	 * @return the balance
	 */
	public String getBalance(String reqId) {
		Reply reply = new Reply(reqId, Outcome.Processed, balance);
		return reply.toString();
	}

	/**
	 * Sets the acc num.
	 *
	 * @param accNum
	 *            the new acc num
	 */
	public void setAccNum(String accNum) {
		this.accNum = accNum;
	}

	/**
	 * Sets the balance.
	 *
	 * @param balance
	 *            the new balance
	 */
	public void setBalance(float balance) {
		this.balance = balance;
	}

	/**
	 * Withdraw.
	 *
	 * @param reqId
	 *            the req id
	 * @param amount
	 *            the amount
	 * @return the string
	 */
	public String withdraw(String reqId, String amount) {
		Reply reply = new Reply(reqId, Outcome.Processed, balance);
		Float amt = Float.valueOf(amount);
		if (balance >= amt) {
			balance -= amt;
			reply.setBalance(balance);
		} else {
			reply.setOutcome(Outcome.InsufficientFunds);
		}
		return reply.toString();
	}

}
