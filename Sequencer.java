import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class Sequencer {

	private ArrayList<Connector> clients = new ArrayList<Connector>();
	private LinkedBlockingQueue<MyTextEvent> eventHistory = new LinkedBlockingQueue<MyTextEvent>();
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

	private void addClient(Connector connector) {
		clients.add(connector);
		new Thread(new Runnable() {
			public void run() {
				String copyText = distributedTextEditor.getTextAreaSyncronizer().getGuarantiedText();
				connector.send(new TextInsertEvent(0, copyText));
				while (true) {
					MyTextEvent event = connector.take();
					if (event != null) {
						eventHistory.add(event);
					} else {
						if (!connector.isConnected()) {
							System.out.println("Client is no longer connected");

							// Send ClientRemovedEvent to all other clients

							clients.remove(connector);
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
						MyTextEvent event = eventHistory.take();
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
