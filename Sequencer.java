import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class Sequencer {

	private ArrayList<Connector> clients = new ArrayList<Connector>();
	private LinkedBlockingQueue<MyMessage> eventHistory = new LinkedBlockingQueue<MyMessage>();
	private DistributedTextEditor distributedTextEditor;

	public Sequencer (DistributedTextEditor distributedTextEditor) {
		this.distributedTextEditor = distributedTextEditor;
	}


	public void listenForClients(int portNumber) {
		new Thread (new Runnable() {
			public void run() {
				while (true) {
					Connector connector = new Connector();
					connector.listenForClient(portNumber);
					connector.startReceiveThread();
					addClient(connector);
				}
			}
		}).start();
	}

	private void addClient(Connector newClient) {
		clients.add(newClient);

		new Thread(new Runnable() {
			public void run() {
				String copyText = distributedTextEditor.getTextAreaSyncronizer().getGuarantiedText();
				newClient.send(new TextInsertEvent(0, copyText));
        // Send client list to the new client
        for(Connector client : clients) {
          newClient.send(new ClientAddedEvent(
                client.getSocket().getInetAddress(),
                client.getSocket().getPort()
                ));
        }
        // Send new client to all the old clients
        eventHistory.add(new ClientAddedEvent(
                newClient.getSocket().getInetAddress(),
                newClient.getSocket().getPort()
                ));
				while (true) {
					MyTextEvent event = newClient.take();
					if (event != null) {
						eventHistory.add(event);
					} else {
						if (!newClient.isConnected()) {
							System.out.println("Client is no longer connected");

							// Send ClientRemovedEvent to all other clients

							clients.remove(newClient);
							break;
						}
					}
				}
			}
		}).start();
	}

	public void startSendThread() {
		new Thread(new Runnable() {
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
		}).start();
	}

}
