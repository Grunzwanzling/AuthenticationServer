package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ClientThread extends Thread {

	public Socket clientSocket;
	public int clientID;

	long AuthenticationTest;

	OutputStream out;
	InputStream in;
	public BufferedReader breader;

	public ClientThread(Socket s, int i) {
		clientSocket = s;
		clientID = i;
	}

	@SuppressWarnings("resource")
	public void run() {

		Server.log("New Client: ID: " + clientID + " IP: "
				+ clientSocket.getInetAddress().getHostName());

		long authenticationKey = (long) ((Math.random() * (10 ^ 28)) + (Math
				.random() * (10 ^ 14)));
		// Berechnungsaufwand: 2^93
		Server.log("Requesting authentication with key: " + authenticationKey);

		try {
			out = clientSocket.getOutputStream();
			in = clientSocket.getInputStream();

			BufferedReader buffr = new BufferedReader(new InputStreamReader(in));
			DataOutputStream buffw = new DataOutputStream(out);
			// ObjectInputStream ois = new ObjectInputStream(
			// clientSocket.getInputStream());
			// ObjectOutputStream oos = new ObjectOutputStream(
			// clientSocket.getOutputStream());

			String ClientVersion = buffr.readLine();

			if (Server.CompatibleClientVersion.startsWith(ClientVersion)) {

				String username = buffr.readLine();

				// Command: REGISTER

				buffw.writeLong(authenticationKey);

				String password = buffr.readLine();

				if (username != null && password != null) {
					if (Server.certificates.containsKey(username)) {
						if (Server.AuthenticationTest(username,
								authenticationKey, password)) {
							Server.log("Client "
									+ clientID
									+ " sucessfully authenticated with the username "
									+ username);
							buffw.writeBytes("Certificate accepted\n");

							while (true) {

								String command = buffr.readLine();
								if (command != null) {
									// Chat.log("Client " + clientID + " ("
									// + username + ") is sending: \""
									// + command + "\"");
									// Command EXISTS

									// Command: SEND

								}
							}

						} else {

							buffw.writeBytes("Wrong username or certificate!\n");
							clientSocket.close();
							System.out
									.println("Client "
											+ clientID
											+ " disconnected because of wrong username or certificate");
						}

					} else {
						buffw.writeBytes("Wrong username or certificate!\n");
						clientSocket.close();
						Server.log("Client " + clientID
								+ " disconnected because of wrong username");
					}

				} else {
					buffw.writeBytes("Incomplete authentication data!\n");
					clientSocket.close();
					Server.log("Client "
							+ clientID
							+ " disconnected because of incomplete authentication data");
				}

			} else {
				buffw.writeBytes("Only version "
						+ Server.CompatibleClientVersion + " is allowed!\n");
				clientSocket.close();
				Server.log("Client "
						+ clientID
						+ " disconnected because of incompatible ClientVersion "
						+ ClientVersion);
			}

		} catch (SocketException e) {
			Server.log("Client " + clientID + " disconnected");

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
