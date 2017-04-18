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

	public void disconnect() {
		try {
			inStream.close();
			outStream.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		inStream = null;
		outStream = null;
		socket = null;
	}

	public void listenForClient() {
		new Thread(new Runnable() {
			public void run() {
				try {
					socket = waitForConnectionFromClient();
					outStream = new ObjectOutputStream(socket.getOutputStream());
					inStream = new ObjectInputStream(socket.getInputStream());
				} catch (IOException e) {
					// We do nothing on IOExceptions
				}

			}
		}).start();
	}

    private Socket waitForConnectionFromClient() {
        Socket res = null;
		try {
			ServerSocket serverSocket = new ServerSocket(portNumber);
            res = serverSocket.accept();
        } catch (IOException e) {
			e.printStackTrace();
        }
        return res;
    }

	public boolean isConnected() {
		return (socket != null && inStream != null && outStream != null);
	}

	public Socket getSocket() {
		return socket;
	}

	public MyTextEvent take() {
		//System.out.println("Trying to take");
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (isConnected()) {
			try {
				//System.out.println("Trying to receive");
				return (MyTextEvent) inStream.readObject();
			} catch (SocketException e) {
				// This will probably occur when disconnecting because of a race-condition
				return null;
			} catch (EOFException e) {
				// This means that our peer has disconnected, so we should as well
				disconnect();
				return null;
			} catch (ClassNotFoundException | IOException e) {
				// We return null on exceptions
				e.printStackTrace();
				return null;
			}
		} else {
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
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

}
