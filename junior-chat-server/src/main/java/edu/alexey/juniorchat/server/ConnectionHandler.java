package edu.alexey.juniorchat.server;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionHandler implements Runnable, Closeable, Serializable {

	private static final long serialVersionUID = 1L; // to conform JavaBeans Spec.
	private static final int MAX_ERRORS = 10;

	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final Integer id;
	private final String remoteAddress;

	private final Socket clientSocket;
	private final BufferedReader reader;
	private final PrintWriter writer;
	private final BiConsumer<ConnectionHandler, String> messageConsumer;

	private volatile boolean isClosing;
	private boolean isClosed;

	public ConnectionHandler(Socket clientSocket, BiConsumer<ConnectionHandler, String> messageConsumer)
			throws IOException {

		this.messageConsumer = Objects.requireNonNull(messageConsumer);
		this.clientSocket = Objects.requireNonNull(clientSocket);

		this.remoteAddress = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
		InputStream is = clientSocket.getInputStream();
		this.reader = new BufferedReader(new InputStreamReader(is));
		OutputStream os = clientSocket.getOutputStream();
		this.writer = new PrintWriter(os, true);
		this.id = IdFactory.instance().getAsInt();
		this.isClosed = false;
		this.isClosing = false;
	}

	public ConnectionHandler() throws IOException {

		this.isClosed = true;
		this.isClosing = true;
		this.id = 0;
		this.remoteAddress = "";
		this.clientSocket = null;
		this.reader = null;
		this.writer = null;
		this.messageConsumer = null;
	}

	public Integer getId() {
		return id;
	}

	public String getRemoteAddress() {
		return remoteAddress;
	}

	public boolean isClosed() {
		return isClosed;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		ensureReadyState();
		pcs.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

	@Override
	public void close() {

		if (isClosing) {
			return;
		}
		synchronized (this) {
			if (isClosing) {
				return;
			}
			isClosing = true;
		}

		logger.info("Connection to {} is closing...", remoteAddress);

		if (!clientSocket.isClosed()) {
			try {

				clientSocket.close();
				if (!isClosed) {
					isClosed = true;
					pcs.firePropertyChange("isClosed", false, true);
					logger.info("Connection closed with {}.", remoteAddress);
				}

			} catch (IOException e) {
				logger.error("Error occurred on ConnectionHandler close attempt:", e);
			}
		}
	}

	@Override
	public void run() {
		ensureReadyState();

		int errorsCount = 0;
		try {

			String message;
			while (!isClosing && (message = reader.readLine()) != null) {
				messageConsumer.accept(this, message);
			}
		} catch (SocketException e) {
			logger.info("ClientSocket is closing...");
		} catch (IOException e) {
			logger.error("Error occurred in ConnectionHandler lifecycle:", e);
			if (++errorsCount >= MAX_ERRORS) {
				close();
			}
		}

		if (!isClosed) {
			close();
		}
	}

	public synchronized void acceptMessage(String message) {
		if (!isClosing) {
			writer.println(message);
		}
	}

	private void ensureReadyState() throws IllegalStateException {
		if (isClosing) {
			throw new IllegalStateException();
		}
	}

}
