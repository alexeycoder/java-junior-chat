package edu.alexey.juniorchat.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Settings {

	public static Settings instance() {
		return Holder.instance;
	}

	// defaults

	public static final Locale LOCALE = Locale.of("ru", "RU");
	public static final Charset CHARSET = StandardCharsets.UTF_8;
	public static final String SERVER_IP = "127.0.0.1";
	public static final int SERVER_PORT = 8082;
	public static final String CHAT_DATETIME_PATTERN = "HH:mm:ss";

	// keys

	private static final String LOCALE_KEY = "server.locale";
	private static final String CHARSET_KEY = "server.charset";
	private static final String SERVER_IP_KEY = "server.ip";
	private static final String SERVER_PORT_KEY = "server.port";
	private static final String CHAT_DATETIME_PATTERN_KEY = "client.chatDatetimePattern";

	// fields

	private Locale locale = LOCALE;
	private Charset charset = CHARSET;
	private String serverIp = SERVER_IP;
	private int serverPort = SERVER_PORT;
	private DateTimeFormatter chatDateTimeFormatter = DateTimeFormatter.ofPattern(CHAT_DATETIME_PATTERN);

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private Properties properties = new Properties();

	private Settings() {
		loadProperties();
	}

	private void loadProperties() {
		try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("application.properties")) {
			properties.load(is);
		} catch (IOException e) {
			logger.error("Exception occurred on reading application.properties:", e);
			throw new RuntimeException(e);
		}

		try {
			if (properties.containsKey(LOCALE_KEY)) {
				locale = Locale.forLanguageTag(properties.getProperty(LOCALE_KEY));
			}
			if (properties.containsKey(CHARSET_KEY)) {
				charset = Charset.forName(properties.getProperty(CHARSET_KEY));
			}
			if (properties.containsKey(SERVER_IP_KEY)) {
				serverIp = properties.getProperty(SERVER_IP_KEY);
			}
			if (properties.containsKey(SERVER_PORT_KEY)) {
				serverPort = Integer.parseInt(properties.getProperty(SERVER_PORT_KEY));
				if (serverPort < 0 || serverPort > (int) Character.MAX_VALUE) {
					throw new RuntimeException("Illegal port number " + serverPort);
				}
			}
			if (properties.containsKey(CHAT_DATETIME_PATTERN_KEY)) {
				chatDateTimeFormatter = DateTimeFormatter.ofPattern(properties.getProperty(CHAT_DATETIME_PATTERN_KEY));
			}
		} catch (Exception e) {
			logger.error("Exception occurred on loading application properties:", e);
			throw new RuntimeException(e);
		}

		logger.info("Loaded Client app settings.");
	}

	public Locale getLocale() {
		return locale;
	}

	public Charset getCharset() {
		return charset;
	}

	public String getServerIp() {
		return serverIp;
	}

	public int getServerPort() {
		return serverPort;
	}

	public DateTimeFormatter getChatDateTimeFormatter() {
		return chatDateTimeFormatter;
	}

	private static class Holder {
		static final Settings instance = new Settings();
	}
}
