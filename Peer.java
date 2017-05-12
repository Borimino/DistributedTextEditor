import java.net.*;

public class Peer {

  private final InetAddress inetAddress;
  private final int port;

  public Peer(InetAddress inetAddress, int port) {
	  this.inetAddress = inetAddress;
	  this.port = port;
  }

  public InetAddress getInetAddress(){
  	return this.inetAddress;
  }

  public int getPort(){
  	return this.port;
  }

  @Override
  public int hashCode() { return inetAddress.hashCode() ^ port; }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Peer)) return false;
    Peer peer = (Peer) o;
    return this.inetAddress.equals(peer.getInetAddress()) &&
           this.port == peer.getPort();
  }

}
