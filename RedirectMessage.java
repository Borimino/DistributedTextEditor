import java.net.*;
import java.io.*;

public class RedirectMessage extends MyMessage implements Serializable {
	private InetAddress address;
	private int port;

	public RedirectMessage (InetAddress address, int port) {
		this.address = address;
		this.port = port;
	}

	public InetAddress getInetAddress(){
		return this.address;
	}

	public int getPort(){
		return this.port;
	}
}

