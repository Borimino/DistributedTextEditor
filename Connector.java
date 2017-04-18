import java.net.*;
import java.io.*;

public class Connector {

	protected static final int portNumber = 40305;

	protected Socket socket;

	/**
	 *
	 * Connects to the server on IP address serverName and port number portNumber.
	 */
	public void connectToServer(String serverName) {
		try {
			this.socket = new Socket(serverName,portNumber);
		} catch (IOException e) {
			// We return null on IOExceptions
		}
	}

	public void listenForClient() {
		new Thread(new Runnable() {
			public void run() {
				socket = waitForConnectionFromClient();
			}
		}).start();
	}

    private Socket waitForConnectionFromClient() {
        Socket res = null;
		try {
			ServerSocket serverSocket = new ServerSocket(portNumber);
            res = serverSocket.accept();
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
}
