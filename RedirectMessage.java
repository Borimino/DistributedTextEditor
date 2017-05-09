import java.net.*;
import java.io.*;

public class RedirectMessage extends MyMessage implements Serializable {
	private InetAddress address;

	public RedirectMessage (InetAddress address) {
		this.address = address;
	}

	public InetAddress getInetAddress(){
		return this.address;
	}

}

