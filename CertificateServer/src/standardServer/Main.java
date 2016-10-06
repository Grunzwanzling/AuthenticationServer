/**
 * 
 */
package standardServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;

import main.ClientThread;
import essentials.SimpleLog;

/**
 * @author Maximilian
 *
 */
public class Main {

	static final String ServerVersion = "0.3.4";
	static final String CompatibleClientVersion = "0.2.0";
	public final int port = 1337;

	int id;

	static Properties passwords = new Properties();

	static Properties settings = new Properties();

	public static Map<Integer, String> usernames = new HashMap<Integer, String>();

	public ServerSocket server;
	static SimpleLog log = new SimpleLog(new File(Main.class.getResource(
			"/settings.xml").toString()), true, true);

	public Main() throws IOException, URISyntaxException {

		// Load settings.config
		log.debug("Reading settings.xml");
		try {
			File file = new File(Main.class.getResource("/settings.xml")
					.toString());
			if (file.canRead()) {

				try {
					settings.loadFromXML(new FileInputStream(file));
					log.debug("Settings loaded");
				} catch (InvalidPropertiesFormatException e) {
					System.out
							.println("ERROR: Wrong Properties Format in settings.xml Resseting setting.xml to default values.");
					settings.setProperty("databaseIP", "127.0.0.1");
					settings.setProperty("databaseUser", "root");
					settings.setProperty("databasePass", "root");
					settings.setProperty("databaseName", "sas");
					settings.setProperty("databaseTable", "election1");
					settings.setProperty("port", "1337");
					settings.storeToXML(new FileOutputStream(file), null);
					new Main();
				}

			} else {
				if (file.exists()) {
					log.fatal("Can't read settings.xml");
					System.exit(1);
				} else {
					log.warning("settings.xml doesnn't exist! Trying to create default settings.xml");
					file.createNewFile();
					settings.setProperty("databaseIP", "127.0.0.1");
					settings.setProperty("databaseUser", "root");
					settings.setProperty("databasePass", "root");
					settings.setProperty("databaseName", "sas");
					settings.setProperty("databaseTable", "election1");
					settings.setProperty("port", "1337");
					settings.storeToXML(new FileOutputStream(file), null);
					new Main();
				}
			}

		} catch (FileNotFoundException e) {

			log.fatal("FileNotFoundException while reading settings.xml");
			log.logStackTrace(e);
			System.exit(1);

		} catch (IOException e) {
			log.fatal("IOException while reading settings.xml");
			log.logStackTrace(e);
			System.exit(1);
		}

		connect();
	}

	public static void main(String[] args) throws IOException,
			URISyntaxException {
		new Main();
	}

	public void connect() throws IOException {
		log.log("******************************************************");
		log.log("Starting MUDGOO Chat Server Version " + ServerVersion
				+ " on Port " + settings.getProperty("port"));
		log.log("******************************************************");
		try {
			@SuppressWarnings("resource")
			ServerSocket m_ServerSocket = new ServerSocket(
					Integer.parseInt((String) settings.setProperty("port",
							"1337")));
			log.info("Waiting for Clients...");
			id = 0;
			while (true) {
				Socket clientSocket = m_ServerSocket.accept();
				ClientThread cliThread = new ClientThread(clientSocket, id++);
				cliThread.start();
			}
		} catch (BindException e) {
			log.fatal("Port in use. Is the Server already running? Close every process that uses port "
					+ settings.setProperty("port", "1337")
					+ " or select an other port in settings.xml");
			System.exit(1);
		}

	}

}
