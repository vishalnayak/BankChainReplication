package com.stonybrook.bank.common;

/**
 * The Enum Outcome.
 */
public enum Outcome {

	/** The Processed. */
	Processed("Processed"),
	/** The Inconsistent with history. */
	InconsistentWithHistory("InconsistentWithHistory"),
	/** The Insufficient funds. */
	InsufficientFunds("InsufficientFunds");

	/** The outcome. */
	private final String outcome;

	/**
	 * Instantiates a new outcome.
	 *
	 * @param outcome
	 *            the outcome
	 */
	private Outcome(String outcome) {
		this.outcome = outcome;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Enum#toString()
	 */
	public String toString() {
		return outcome;
	}
}