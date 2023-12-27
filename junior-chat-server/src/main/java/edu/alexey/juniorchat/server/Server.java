package edu.alexey.juniorchat.server;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server implements Runnable, Closeable, PropertyChangeListener {

	private static final String USER_ID_FORMAT = "**%d**";//"\0\0%d\0\0";
	private static final String CMD_PFX = "@";
	private static final String TO_ADMIN_CMD = CMD_PFX + "wantBeAdmin";
	private static final String KICK_CMD = CMD_PFX + "kick";
	private static final String QUIT_CMD = CMD_PFX + "quit";

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final ConcurrentHashMap<Integer, ConnectionHandler> connections;
	private Integer admin;

	private ServerSocket serverSocket;
	private boolean isClosing;
	private boolean isClosed;

	public Server() {
		this.connections = new ConcurrentHashMap<Integer, ConnectionHandler>();
		isClosed = false;
		isClosing = false;
	}

	public boolean isClosed() {
		return isClosed;
	}

	@Override
	public void close() throws IOException {

		if (isClosing) {
			return;
		}
		isClosing = true;

		logger.info("Server shutdown...");

		if (serverSocket != null && !serverSocket.isClosed()) {

			try {
				serverSocket.close();
			} catch (IOException e) {
				logger.error("Exception occurred on ServerSocket close attempt:", e);
			}

			for (var connection : connections.values()) {
				connection.close();
			}
		}

		isClosed = true;
	}

	@Override
	public void run() {
		if (isClosing) {
			throw new IllegalStateException("The Server instance is "
					+ (isClosed ? "closed." : "closing."));
		}

		int port = Settings.instance().getPort();
		ExecutorService pool = null;
		try (ServerSocket ss = new ServerSocket(port)) {

			this.serverSocket = ss;
			pool = Executors.newCachedThreadPool();

			logger.info("Server is listening on port {}.", port);

			while (!isClosing) {
				var clientSocket = serverSocket.accept();
				var connection = new ConnectionHandler(clientSocket, this::handleMessage);
				connections.put(connection.getId(), connection);
				connection.addPropertyChangeListener(this);
				if (pool != null) {
					pool.execute(connection);
					welcome(connection);
					logger.info("A new connection established with {}.", connection.getRemoteAddress());
				}
			}

		} catch (SocketException e) {
			logger.info("ServerSocker is closing...");
		} catch (IOException e) {
			logger.error("Error occurred in server lifecycle:", e);
		} finally {
			if (pool != null) {
				try {
					pool.shutdown();
					pool = null;
				} catch (Exception e) {
					logger.error("Error occurred on thread pool shutting down:", e);
				}
			}
		}
	}

	private void welcome(ConnectionHandler connection) {
		connection.acceptMessage(signMessage(connection.getId(), ""));
		connection.acceptMessage(signMessage(0, "Добро пожаловать, Участник №" + connection.getId() + "."));
		broadcast(null, "Участник №" + connection.getId() + " присоединился к чату.", connection.getId());
	}

	private void handleMessage(ConnectionHandler connection, String rawMessage) {

		if (findQuit(rawMessage)) {
			quit(connection);
			return;
		}

		if (admin != null && admin == connection.getId()) {
			// check commands eligible to admin:
			var kickMessage = findKickCommand(rawMessage);
			if (kickMessage.command().isEmpty()) {
				if (findWantBeAdmin(rawMessage)) {
					return;
				}
			} else {
				kick(kickMessage.command().get(), connection);
				return;
			}
		}

		if (findWantBeAdmin(rawMessage)) {
			madeAdmin(connection);
			return;
		}

		var messageWithDestination = findDestination(rawMessage);
		if (messageWithDestination.command().isEmpty()) {
			broadcast(connection, rawMessage, connection.getId());
		} else {
			sendToAnother(messageWithDestination.command().get(),
					connection,
					messageWithDestination.message());
		}
	}

	private void kick(int id, ConnectionHandler adminConnection) {
		var target = connections.get(Integer.valueOf(id));
		if (target == null) {
			adminConnection.acceptMessage(signMessage(0, "Нет участника с таким номером!"));
			return;
		}

		target.acceptMessage(signMessage(0, "Вы отключаетесь от чата по запросу администратора."));
		target.close();
	}

	private void quit(ConnectionHandler connection) {
		connection.acceptMessage(signMessage(0, "Вы покидаете чат. Ждём вас снова!"));
		connection.close();
		broadcast(null, "Участник №" + connection.getId() + " покинул чат.", 0);
	}

	private void madeAdmin(ConnectionHandler connection) {
		admin = connection.getId();
		connection.acceptMessage(signMessage(0, "Вы назначены администатором чата."));
	}

	private void broadcast(ConnectionHandler connection, String message, int excludeId) {
		int id = connection == null ? 0 : connection.getId();
		String signedMessage = signMessage(id, message);

		for (var conn : connections.values()) {
			if (conn != null && !conn.isClosed() && conn.getId() != excludeId) {
				conn.acceptMessage(signedMessage);
			}
		}

		logger.info("Broadcast from {}: {}", id, message);
	}

	private void sendToAnother(int anotherId, ConnectionHandler connection, String message) {
		var another = connections.get(Integer.valueOf(anotherId));
		if (another == null) {
			if (connection != null) {
				connection.acceptMessage(signMessage(0, "Нет участника с таким номером!"));
			}
			return;
		}

		another.acceptMessage(signMessage(connection == null ? 0 : connection.getId(), message));
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String propName = evt.getPropertyName();
		if (propName.equals("isClosed") && (evt.getSource() instanceof ConnectionHandler connection)) {
			if (connection.isClosed()) {
				connections.remove(connection.getId());
			}
			logger.info("Closed connection with {} is removed from connections pool.", connection.getRemoteAddress());
		}
	}

	private String signMessage(int id, String message) {
		String userId = String.format(USER_ID_FORMAT, id);
		return userId + System.lineSeparator() + message;
	}

	private Pair<Integer> findKickCommand(String rawMessage) {
		var extracted = extractFirst(rawMessage, "^\\s*(" + KICK_CMD + "\\s+\\d+)\\s*");
		if (extracted == null) {
			return new Pair<Integer>(Optional.empty(), rawMessage);
		} else {
			int target = Integer.parseInt(extracted[0].strip().substring(KICK_CMD.length()).stripLeading());
			return new Pair<Integer>(Optional.of(target), extracted[1]);
		}
	}

	private Pair<Integer> findDestination(String rawMessage) {
		var extracted = extractFirst(rawMessage, "^\\s*" + CMD_PFX + "(\\d+)\\s*");
		if (extracted == null) {
			return new Pair<Integer>(Optional.empty(), rawMessage);
		} else {
			int dest = Integer.parseInt(extracted[0].strip().substring(1));
			return new Pair<Integer>(Optional.of(dest), extracted[1]);
		}
	}

	private boolean findWantBeAdmin(String rawMessage) {
		return rawMessage.strip().equals(TO_ADMIN_CMD);
	}

	private boolean findQuit(String rawMessage) {
		return rawMessage.strip().equals(QUIT_CMD);
	}

	private String[] extractFirst(String input, String regex) {
		Pattern pattern = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
		Matcher matcher = pattern.matcher(input);
		StringBuilder sbInput = new StringBuilder(input);

		int removedLength = 0;
		if (matcher.find()) {

			String matchStr = matcher.group();

			int start = matcher.start();
			int end = matcher.end();
			int len = end - start;
			start -= removedLength;
			end -= removedLength;
			sbInput.delete(start, end);
			removedLength += len;

			return new String[] { matchStr, sbInput.toString() };
		}
		return null;
	}

	private static record Pair<T>(Optional<T> command, String message) {
	}
}
