package edu.alexey.juniorchat.server;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
	private static final Logger logger = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) {

		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				logger.error("Unhandled exception caught.", e);
			}
		});

		logger.info("Application started.");

		try (Server server = new Server()) {

			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						logger.info("Forced shutdown...");
						server.close();

					} catch (IOException e) {
						Thread.currentThread().interrupt();
						e.printStackTrace();
					}
				}
			});

			server.run();

		} catch (IOException e) {
			logger.error("Error in server lifecycle:", e);
		}

		logger.info("Application finished." + System.lineSeparator());
	}
}
