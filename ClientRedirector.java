import java.net.*;

public class ClientRedirector {

	private Thread thread;
	private Connector connector;
	private Connector redirectConnector;

	public ClientRedirector (Connector connector, Connector redirectConnector) {
		this.connector = connector;
		this.redirectConnector = redirectConnector;
	}
	
	public void start(int portNumber) {
		thread = new Thread (new Runnable() {
			public void run() {
				while (true) {
					redirectConnector.listenForClient(portNumber);
					InetAddress address = connector.getSocket().getInetAddress();
					int port = connector.getSocket().getPort();
					redirectConnector.send(new RedirectMessage(address, port));
				}
			}
		});
		thread.start();
	}

	public void stop() {
		if (thread != null) thread.interrupt();
		Connector.closeServerSocket();
	}
}
