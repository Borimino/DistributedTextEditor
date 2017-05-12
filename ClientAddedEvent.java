import java.net.InetAddress;
import java.io.Serializable;

public class ClientAddedEvent extends MyMessage implements Serializable {
  private InetAddress inetAdress;
  private int port;

  public ClientAddedEvent(InetAddress inetAdress, int port) {
    this.inetAdress = inetAdress;
    this.port = port;
  }

  public InetAddress getInetAddress() {
    return inetAdress;
  }

  public int getPort() {
    return port;
  }
}

