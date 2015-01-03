package com.stonybrook.bank.common;

/**
 * The Class Reply.
 */
public class Reply {

	/** The req id. */
	private String reqID;

	/** The outcome. */
	private Outcome outcome;

	/** The balance. */
	private float balance;

	/**
	 * Instantiates a new reply.
	 *
	 * @param reqId
	 *            the req id
	 * @param processed
	 *            the processed
	 * @param balance
	 *            the balance
	 */
	public Reply(String reqId, Outcome processed, float balance) {
		this.reqID = reqId;
		this.outcome = processed;
		this.balance = balance;
	}

	/**
	 * Gets the balance.
	 *
	 * @return the balance
	 */
	public float getBalance() {
		return balance;
	}

	/**
	 * Gets the outcome.
	 *
	 * @return the outcome
	 */
	public Outcome getOutcome() {
		return outcome;
	}

	/**
	 * Gets the req id.
	 *
	 * @return the req id
	 */
	public String getReqID() {
		return reqID;
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
	 * Sets the outcome.
	 *
	 * @param outcome
	 *            the new outcome
	 */
	public void setOutcome(Outcome outcome) {
		this.outcome = outcome;
	}

	/**
	 * Sets the req id.
	 *
	 * @param reqID
	 *            the new req id
	 */
	public void setReqID(String reqID) {
		this.reqID = reqID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "<" + reqID + ";" + outcome + ";" + balance + ">";
	}
}
