import java.net.*;
import java.io.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.ArrayList;

public class Connector {

	protected Socket socket;
	protected ObjectInputStream inStream;
	protected ObjectOutputStream outStream;
	protected LinkedBlockingQueue<MyMessage> textEvents = new LinkedBlockingQueue<MyMessage>();
	protected ArrayList<Peer> peers = new ArrayList<Peer>();
	protected DistributedTextEditor dte;

	protected static ServerSocket serverSocket;

	public Connector(DistributedTextEditor dte) {
		this.dte = dte;
	}

	public void connectToServer(InetAddress serverName, int portNumber, int portNumberSelf) {
		try {
			//this.socket = new Socket(serverName,portNumber);
			this.socket = new Socket();
			socket.connect(new InetSocketAddress(serverName, portNumber), 5000);
			this.inStream = new ObjectInputStream(socket.getInputStream());
			this.outStream = new ObjectOutputStream(socket.getOutputStream());
			//System.out.println(socket.getLocalAddress().toString() + ":" + socket.getLocalPort());
			
			send(new ClientAddedEvent(socket.getLocalAddress(), portNumberSelf));
			dte.setLocalAddress(socket.getLocalAddress());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void disconnect() {
		synchronized (this) {
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
	}

	public void listenForClient(int portNumber) {
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
		return (socket != null && inStream != null && outStream != null && socket.isConnected() && !socket.isClosed());
	}

	public Socket getSocket() {
		return socket;
	}

	//Make a thread that collect messages from inStream and treats them appropriately.
	//		MyTextEvent goes in a Queue
	//		RedirectMessage triggers connectToServer on the corresponding server
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
							if (message instanceof RedirectMessage) {
								RedirectMessage msg = (RedirectMessage) message;
								disconnect();
								connectToServer(msg.getInetAddress(), msg.getPort(), dte.getListeningPortNumber());
								if (isConnected()) {
									dte.setTitle("Connected to " + msg.getInetAddress().getHostAddress() + ":" + msg.getPort());
								} else {
									dte.setTitle("Not able to connect to " + msg.getInetAddress().getHostAddress() + ":" + msg.getPort());

								}


							}
							if (message instanceof ClientAddedEvent) {
								ClientAddedEvent clientAddedEvent = (ClientAddedEvent) message;
								Peer peer = new Peer(clientAddedEvent.getInetAddress(), clientAddedEvent.getPort());
								if (!peers.contains(peer)) {
									peers.add(peer);
								}
								textEvents.add(clientAddedEvent);
							}
							if (message instanceof ClientDisconnectedEvent) {
								ClientDisconnectedEvent clientDisconnectedEvent = (ClientDisconnectedEvent) message;
								peers.remove(clientDisconnectedEvent.getPeer());
							}

						} catch (SocketException e) {
							// This will probably occur when disconnecting because of a race-condition
						} catch (EOFException e) {
							// This means that our peer has disconnected, so we should as well
							InetAddress address = socket.getInetAddress();
							int port = socket.getPort();
							Peer peer = new Peer(address, port);
							disconnect();
							textEvents.add(new ClientDisconnectedEvent(peer));
							dte.clientDisconnected(peer);
						} catch (ClassNotFoundException | IOException e) {
							e.printStackTrace();
						}
					}

				}
			}
		}).start();
	}

	public MyMessage take() {
		try {
			return textEvents.take();
		} catch (InterruptedException e) {
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
		synchronized (this) {
			try {
				if (isConnected()) {
					outStream.writeObject(event);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void closeServerSocket() {
		//try {
			//if (serverSocket != null) {
				//serverSocket.close();
				//serverSocket = null;
			//}
		//} catch (IOException e) {
			//e.printStackTrace();
		//}

	}

	public boolean removePeer(Peer peer) {
		return peers.remove(peer);
	}

	public boolean isThisFirstPeer(Peer peer) {
		if (peers.isEmpty()) return false;

		return (peers.get(0).equals(peer));
	}

	public Peer getFirstPeer(){
		return peers.get(0);
	}

}
