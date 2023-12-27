package edu.alexey.juniorchat.server;

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
	public static final int PORT = 8082;
	public static final String CHAT_DATETIME_PATTERN = "HH:mm:ss";

	// keys

	private static final String LOCALE_KEY = "server.locale";
	private static final String CHARSET_KEY = "server.charset";
	private static final String PORT_KEY = "server.port";
	private static final String CHAT_DATETIME_PATTERN_KEY = "server.chatDatetimePattern";

	// fields

	private Locale locale = LOCALE;
	private Charset charset = CHARSET;
	private int port = PORT;
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
			if (properties.containsKey(PORT_KEY)) {
				port = Integer.parseInt(properties.getProperty(PORT_KEY));
				if (port < 0 || port > (int) Character.MAX_VALUE) {
					throw new RuntimeException("Illegal port number " + port);
				}
			}
			if (properties.containsKey(CHAT_DATETIME_PATTERN_KEY)) {
				chatDateTimeFormatter = DateTimeFormatter.ofPattern(properties.getProperty(CHAT_DATETIME_PATTERN_KEY));
			}
		} catch (Exception e) {
			logger.error("Exception occurred on loading application properties:", e);
			throw new RuntimeException(e);
		}

		logger.info("Loaded Server app settings.");
	}

	public Locale getLocale() {
		return locale;
	}

	public Charset getCharset() {
		return charset;
	}

	public int getPort() {
		return port;
	}

	public DateTimeFormatter getChatDateTimeFormatter() {
		return chatDateTimeFormatter;
	}

	private static class Holder {
		static final Settings instance = new Settings();
	}
}
