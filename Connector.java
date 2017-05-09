import java.net.*;
import java.io.*;
import java.util.concurrent.LinkedBlockingQueue;

public class Connector {

	protected static final int portNumber = 40305;

	protected Socket socket;
	protected ObjectInputStream inStream;
	protected ObjectOutputStream outStream;
	protected LinkedBlockingQueue<MyTextEvent> textEvents = new LinkedBlockingQueue<MyTextEvent>();

	protected static ServerSocket serverSocket;

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
		if (serverSocket == null) {
			try {
				serverSocket = new ServerSocket(portNumber);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		//new Thread(new Runnable() {
			//public void run() {
				try {
					socket = waitForConnectionFromClient();
					outStream = new ObjectOutputStream(socket.getOutputStream());
					inStream = new ObjectInputStream(socket.getInputStream());
				} catch (IOException e) {
					// We do nothing on IOExceptions
				}

			//}
		//}).start();
	}

    private Socket waitForConnectionFromClient() {
        Socket res = null;
		try {
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

	//Make a thread that collect messages from inStream and treats them appropriately.
	//		MyTextEvent goes in a Queue
	//		SequencerRedirect triggers connectToServer on the corresponding server
	public void startReceiveThread() {
		new Thread (new Runnable() {
			public void run () {
				while (true) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					if (isConnected()) {
						try {
							MyMessage message = (MyMessage) inStream.readObject();
							if (message instanceof MyTextEvent) {
								textEvents.add( (MyTextEvent) message);
							}
						} catch (SocketException e) {
							// This will probably occur when disconnecting because of a race-condition
						} catch (EOFException e) {
							// This means that our peer has disconnected, so we should as well
							disconnect();
						} catch (ClassNotFoundException | IOException e) {
							e.printStackTrace();
						}
					}

				}
			}
		}).start();
	}

	/*
	public MyTextEvent take() {
		//TODO: Get rid of the Thread.sleep call.
		//Could maybe be done by calling socket.isClosed and stream.isReady in isConnected.
		//Could maybe be done by locking the method.
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (isConnected()) {
			//System.out.println("Taking from inStream");

			try {
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
	*/
	//TODO: Take events from a Queue instead of from inStream
	public MyTextEvent take() {
		try {
			return textEvents.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
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
							//System.out.println("Sending event");

							outStream.writeObject(event);
						}
					} catch (InterruptedException | IOException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	public void send(MyMessage event) {
		try {
			if (isConnected()) {
				outStream.writeObject(event);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
