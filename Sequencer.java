import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class Sequencer {

	private ArrayList<Connector> clients = new ArrayList<Connector>();
	private ArrayList<Peer> peers = new ArrayList<Peer>();
	private LinkedBlockingQueue<MyMessage> eventHistory = new LinkedBlockingQueue<MyMessage>();
	private DistributedTextEditor distributedTextEditor;
	private ArrayList<Thread> threads = new ArrayList<Thread>();

	public Sequencer (DistributedTextEditor distributedTextEditor) {
		this.distributedTextEditor = distributedTextEditor;
	}


	public void listenForClients(int portNumber) {
		Thread t = new Thread (new Runnable() {
			public void run() {
				while (true) {
					Connector connector = new Connector(distributedTextEditor);
					connector.listenForClient(portNumber);
					connector.startReceiveThread();
					addClient(connector);
				}
			}
		});
		threads.add(t);
		t.start();
	}

	private void addClient(Connector newClient) {
		clients.add(newClient);

		Thread t = new Thread(new Runnable() {
			public void run() {
				String copyText = distributedTextEditor.getTextAreaSyncronizer().getGuarantiedText();
				newClient.send(new TextInsertEvent(0, copyText));
				ClientAddedEvent greeting = (ClientAddedEvent) newClient.take();
				peers.add(new Peer(newClient.getSocket().getInetAddress(), greeting.getPort()));
				for (Peer peer : peers) {
					newClient.send(new ClientAddedEvent(peer.getInetAddress(), peer.getPort()));
				}
				eventHistory.add(new ClientAddedEvent(newClient.getSocket().getInetAddress(),
													  greeting.getPort()));
				while (true) {
					MyMessage msg = newClient.take();
					if (msg instanceof MyTextEvent) {
						MyTextEvent event = (MyTextEvent) msg;
						eventHistory.add(event);
					} else if (msg instanceof ClientDisconnectedEvent) {
						if (!newClient.isConnected()) {
							System.out.println("Client is no longer connected");

							// Send ClientDisconnectedEvent to all other clients
							clients.remove(newClient);

							for (Connector client : clients) {
								client.send(msg);
							}


							break;
						}
					}
				}
			}
		});
		threads.add(t);
		t.start();
	}

	public void startSendThread() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						MyMessage event = eventHistory.take();
						for (Connector con : clients) {
							con.send(event);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		threads.add(t);
		t.start();
	}

	public void stop() {
		for (Thread t : threads) {
			t.interrupt();
		}
		Connector.closeServerSocket();
	}


}
