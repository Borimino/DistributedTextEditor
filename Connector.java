import java.net.*;
import java.io.*;

public class Connector {

	protected static final int portNumber = 40305;

	protected Socket socket;
	protected ObjectInputStream inStream;
	protected ObjectOutputStream outStream;


	/**
	 *
	 * Connects to the server on IP address serverName and port number portNumber.
	 */
	public void connectToServer(String serverName) {
		try {
			this.socket = new Socket(serverName,portNumber);
			this.inStream = new ObjectInputStream(socket.getInputStream());
			this.outStream = new ObjectOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			// We do nothing on IOExceptions
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

	public MyTextEvent take() {
		try {
			return (MyTextEvent) inStream.readObject();
		} catch (ClassNotFoundException | IOException e) {
			// We return null on exceptions
			return null;
		}
	}

	public void startSendThread(DocumentEventCapturer dec) {
		new Thread (new Runnable() {
			public void run() {
				while (true) {
					try {
						MyTextEvent event = dec.take();
						if (isConnected()) {
							outStream.writeObject(event);
						}
					} catch (InterruptedException | IOException e) {
					}
				}
			}
		}).start();
	}

}
