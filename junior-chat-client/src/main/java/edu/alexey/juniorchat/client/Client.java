package edu.alexey.juniorchat.client;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.OptionalInt;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client implements Runnable, Consumer<String>, Closeable {

	private static final String USER_ID_PFX_SFX = "**";

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final Socket socket;
	private final BufferedReader reader;
	private final PrintWriter writer;

	private int id;

	private boolean isClosing;
	private boolean isClosed;

	public Client() throws UnknownHostException, IOException {
		isClosing = isClosed = false;

		String ip = Settings.instance().getServerIp();
		int port = Settings.instance().getServerPort();

		socket = new Socket(ip, port);
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		writer = new PrintWriter(socket.getOutputStream(), true);

		handshake();

		if (id == 0) {
			String error = "Error: Incorrect behavior detected. Had not received ID on handshake.";
			logger.error(error);
			throw new RuntimeException(error);
		}
	}

	public boolean isClosed() {
		return isClosed;
	}

	private void handshake() {
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				OptionalInt myIdOpt = findId(line);
				if (myIdOpt.isPresent()) {
					id = myIdOpt.getAsInt();
					return;
				}
			}

		} catch (IOException e) {
			logger.error("Error occurred during handshake:", e);
		}
	}

	private OptionalInt findId(String line) {
		if (line.startsWith(USER_ID_PFX_SFX) && line.startsWith(USER_ID_PFX_SFX)) {
			line = line.substring(2, line.length() - 2);
			try {
				int id = Integer.parseInt(line);
				return OptionalInt.of(id);
			} catch (NumberFormatException e) {}
		}
		return OptionalInt.empty();
	}

	@Override
	public void close() {
		if (isClosing) {
			return;
		}
		isClosing = true;

		if (socket != null && !socket.isClosed()) {

			try {
				socket.close();
			} catch (IOException e) {
				logger.error("Error occurred on closing:", e);
			}
		}

		isClosed = true;

	}

	@Override
	public void run() {
		if (isClosing) {
			return;
		}

		Thread t = new Thread(this::incomingMessagesHandler);
		t.start();
	}

	private void incomingMessagesHandler() {
		String line;
		try {
			while (!isClosing && (line = reader.readLine()) != null) {

				var fromIdOpt = findId(line);
				if (fromIdOpt.isPresent()) {
					String from;
					if (fromIdOpt.getAsInt() == this.id) {
						from = "Вы:";
					} else if (fromIdOpt.getAsInt() == 0) {
						from = "Сервер:";
					} else {
						from = "Участник №" + fromIdOpt.getAsInt() + ":";
					}
					printToChat(from);
				} else {
					printToChat(line);
				}

			}
		} catch (SocketException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void printToChat(String message) {
		System.out.println(message);
	}

	@Override
	public void accept(String message) {
		if (isClosing) {
			throw new IllegalStateException();
		}
		if (message == null) {
			throw new NullPointerException("message");
		}

		if (!message.isBlank()) {
			writer.println(message);
		}
	}

}
