package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;

public class Server {

	static final String ServerVersion = "0.3.4";
	static final String CompatibleClientVersion = "0.2.0";
	public final int Port = 56789;
	public static String Path = "C://Server//SimpleCreditTransfer//";

	int id;

	static HashMap<String, PublicKey> publicKeys = new HashMap<String, PublicKey>();

	static PrivateKey privateKey;

	static Properties settings = new Properties();

	public ServerSocket server;

	public Server() throws IOException, URISyntaxException {

		// Load settings.config
		System.out.println("Reading settings.xml");
		try {
			File file = new File("settings.xml");
			if (file.canRead()) {

				try {
					settings.loadFromXML(new FileInputStream(file));
					System.out.println("Settings loaded");
				} catch (InvalidPropertiesFormatException e) {
					System.out
							.println("ERROR: Wrong Properties Format in settings.xml Resseting setting.xml to default values.");
					settings.setProperty("port", "56789");
					settings.setProperty("privateKey", "private.key");
					settings.setProperty("PublicKeysDir", "publicKeys");
					settings.setProperty("logFile", "log.txt");
					settings.setProperty("bankAccountFile",
							"bankAccounts.config");
					settings.storeToXML(new FileOutputStream(file), null);
					new Server();
				}

			} else {
				if (file.exists()) {
					System.out.println("FATAL ERROR: Can't read settings.xml");
					System.exit(1);
				} else {
					System.out
							.println("ERROR: settings.xml doesnn't exist! Trying to create default settings.xml");
					file.createNewFile();
					settings.setProperty("port", "56789");
					settings.setProperty("privateKey", "private.key");
					settings.setProperty("PublicKeysDir", "publicKeys");
					settings.setProperty("logFile", "log.txt");
					settings.setProperty("bankAccountFile",
							"bankAccounts.config");
					settings.storeToXML(new FileOutputStream(file), null);
					new Server();
				}
			}

		} catch (FileNotFoundException e) {

			System.out
					.println("ERROR: FileNotFoundException while reading settings.xml "
							+ e.getMessage());
			System.exit(1);

		} catch (IOException e) {
			System.out.println("ERROR: IOException while reading settings.xml "
					+ e.getMessage());
			System.exit(1);
		}
		loadCertificates();
		//
		// // Load users.config
		// try {
		// Path user = Paths.get(new File(settings
		// .getProperty("certificateFile")).getCanonicalPath());
		// InputStream in = Files.newInputStream(user);
		// certificates.load(in);
		// in.close();
		//
		// } catch (NoSuchFileException e) {
		// System.out
		// .println("ERROR: Certificate-file doesn't exists! Creating default Certificate-file at "
		// + settings.getProperty("certificateFile"));
		// OutputStream out = Files.newOutputStream(Paths.get(settings
		// .getProperty("userFile")));
		// certificates
		// .store(out,
		// "Certificate-file of SimpleCreditTransfer Server.\nCreate new certificates like clientID=certificate");
		// out.close();
		//
		// }
		// Server.log("Certificate-file loaded.");
		// if (certificates.isEmpty()) {
		// Server.log("ERROR: Certificate-file is empty. Add some certificates to "
		// + settings.getProperty("certificateFile")
		// + " and start the Server again.");
		// }

		connect();
	}

	public static void main(String[] args) throws IOException,
			URISyntaxException {
		new Server();
	}

	public static String Hash(String s) {

		try {
			MessageDigest md = MessageDigest.getInstance("SHA");
			String s2 = new String(md.digest(s.getBytes("UTF-8")), "UTF-8");
			return s2;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return ("");
	}

	public static void loadCertificates() throws URISyntaxException {

		KeyFactory keyFactory;
		try {
			keyFactory = KeyFactory.getInstance("RSA");

			// Load Public Keys from Clients
			File Dir = new File(Server.class.getProtectionDomain()
					.getCodeSource().getLocation().toURI().getPath()
					+ "\\" + settings.getProperty("PublicKeysDir"));
			File[] SubDirs = Dir.listFiles();
			System.out.println(Dir.getAbsolutePath());
			FileInputStream fis;
			for (File file : SubDirs) {

				try {

					fis = new FileInputStream(file);
					byte[] encodedPublicKey = new byte[(int) file.length()];
					fis.read(encodedPublicKey);
					fis.close();

					X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
							encodedPublicKey);
					PublicKey publicKey = keyFactory
							.generatePublic(publicKeySpec);
					publicKeys.put(
							file.getName().substring(0,
									file.getName().indexOf(".")), publicKey);
					Server.log("Loaded key "
							+ file.getName().substring(0,
									file.getName().indexOf(".")));
				} catch (IOException e) {
					Server.log("Can't read PublicKey in File " + file.getName());
					e.printStackTrace();
				} catch (InvalidKeySpecException e) {
					Server.log("InvalidKeySpecException!");
					e.printStackTrace();
				}

			}

			// Load Private Key from Server

			File filePrivateKey;
			try {
				filePrivateKey = new File(Server.class.getProtectionDomain()
						.getCodeSource().getLocation().toURI().getPath()
						+ "\\" + settings.getProperty("privateKey"));
				fis = new FileInputStream(filePrivateKey);
				byte[] encodedPrivateKey = new byte[(int) filePrivateKey
						.length()];
				fis.read(encodedPrivateKey);
				fis.close();

				PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
						encodedPrivateKey);
				privateKey = keyFactory.generatePrivate(privateKeySpec);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeySpecException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (NoSuchAlgorithmException e) {
			Server.log("NoSuchAlgorithmException! " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void log(String s) {
		try {
			SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss");
			Timestamp time = new Timestamp(System.currentTimeMillis());
			File log = new File(settings.getProperty("logFile"));

			if (!log.exists())
				log.createNewFile();

			FileWriter out = new FileWriter(log, true);
			out.append((CharSequence) df.format(time) + "  " + s);
			out.append('\n');
			out.close();
			System.out.println(df.format(time) + "  " + s);

		} catch (IOException e) {

		}
	}

	public void connect() throws IOException {
		Server.log("********************************************************");
		Server.log("Starting SimpleCreditTransfer Server | Version "
				+ ServerVersion + " on Port " + Port);
		Server.log("********************************************************");
		try {
			@SuppressWarnings("resource")
			ServerSocket m_ServerSocket = new ServerSocket(Port);
			Server.log("Waiting for Clients...");
			id = 0;
			while (true) {
				Socket clientSocket = m_ServerSocket.accept();
				ClientThread cliThread = new ClientThread(clientSocket, id++);
				cliThread.start();
			}
		} catch (BindException e) {
			Server.log("FATAL ERROR: Port in use. Is the Server already running? Close every process that uses port "
					+ Port + " or select an other port in settings.xml");
			System.exit(1);
		}

	}

}
