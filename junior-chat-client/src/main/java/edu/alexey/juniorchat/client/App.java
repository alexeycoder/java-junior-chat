package edu.alexey.juniorchat.client;

import java.io.IOException;
import java.util.Scanner;

public class App {

	static final String QUIT_CMD = "@quit";

	protected static final Scanner scanner = new Scanner(System.in, Settings.instance().getCharset());

	public static void main(String[] args) {

		System.out.println("Starting Client application...");

		try (Client client = new Client()) {

			client.run();

			boolean quit = false;
			while (!quit && !client.isClosed()) {

				String input = scanner.nextLine();
				quit = input.equals(QUIT_CMD);
				client.accept(input);

			}

			Thread.sleep(1000);

		} catch (IOException e) {
			System.err.println("Unable to connect.");
		} catch (InterruptedException ignore) {}

		System.out.println("Application is closing...");
	}

}
