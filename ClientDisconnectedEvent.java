public class ClientDisconnectedEvent extends MyMessage {
	private Peer peer;

	public ClientDisconnectedEvent (Peer peer) {
		this.peer = peer;
	}

	public Peer getPeer(){
		return this.peer;
	}
}

