import java.net.*;
import java.io.*;

public class ClientConnector {

	/*
	 * Your group should use port number 40HGG, where H is your "hold nummer (1,2 or 3) 
	 * and GG is gruppe nummer 00, 01, 02, ... So, if you are in group 3 on hold 1 you
	 * use the port number 40103. This will avoid the unfortunate situation that you
	 * connect to each others servers.
	 */
	protected static final int portNumber = 40305;  

	protected PrintWriter toServer;
	protected BufferedReader fromServer;
	protected Socket socket;

	public ClientConnector (String serverName) {
		socket = connectToServer(serverName);
		try {
			toServer = new PrintWriter(socket.getOutputStream(), true);
			fromServer = new BufferedReader(new InputStreamReader (socket.getInputStream()));
		} catch (NullPointerException e) {
			System.err.println("Not able to connect to " + serverName + ":" + portNumber);
		} catch (IOException e) {
			// TODO: Should we handle this??
			e.printStackTrace();
		}

	}

	/**
	 *
	 * Connects to the server on IP address serverName and port number portNumber.
	 */
	protected Socket connectToServer(String serverName) {
		Socket res = null;
		try {
			res = new Socket(serverName,portNumber);
		} catch (IOException e) {
			// We return null on IOExceptions
		}
		return res;
	}

	public boolean isConnected() {
		return (socket != null);
	}

	public Socket getSocket() {
		return socket;
	}

	public PrintWriter getPrintWriter() {
		return toServer;
	}

	public BufferedReader getBufferedReader(){
		return fromServer;
	}
}
