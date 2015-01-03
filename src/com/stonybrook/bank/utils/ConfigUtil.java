package com.stonybrook.bank.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.stonybrook.bank.common.Constants;

/**
 * The Class ConfigUtil.
 */
public class ConfigUtil {

	/**
	 * Gets the property.
	 *
	 * @param key
	 *            the key
	 * @return the property
	 */
	public static String getProperty(String key) {
		if (!loaded) {
			loadProperties();
		}
		String property = prop.getProperty(key);
		if (property != null) {
			property = property.trim();
		}
		return property;
	}

	/**
	 * Load properties.
	 */
	public static void loadProperties() {
		InputStream in = ConfigUtil.class
				.getResourceAsStream(Constants.CONFIG_FILE);
		try {
			prop.load(in);
			in.close();
			loaded = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Prints the properties.
	 */
	public static void printProperties() {
		if (!loaded) {
			loadProperties();
		}
		prop.list(System.out);
	}

	/** The prop. */
	private static Properties prop = new Properties();

	/** The loaded. */
	private static Boolean loaded = false;
}
