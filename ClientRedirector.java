import java.net.*;

public class ClientRedirector {

	private Thread thread;
	private Connector connector;
	private Connector redirectConnector = new Connector();

	public ClientRedirector (Connector connector) {
		this.connector = connector;
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
		thread.interrupt();
	}
}
