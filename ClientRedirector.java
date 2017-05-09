import java.net.*;

public class ClientRedirector {

	private Thread thread;
	private Connector connector;
	private Connector redirectConnector = new Connector();

	public ClientRedirector (Connector connector) {
		this.connector = connector;
	}
	
	public void start() {
		thread = new Thread (new Runnable() {
			public void run() {
				while (true) {
					redirectConnector.listenForClient();
					InetAddress address = connector.getSocket().getInetAddress();
					redirectConnector.send(new RedirectMessage(address));
				}
			}
		});
		thread.start();
	}

	public void stop() {
		thread.interrupt();
	}
}
