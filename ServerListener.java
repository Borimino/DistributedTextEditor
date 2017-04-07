import java.net.*;
import java.io.*;

public class ServerListener implements Runnable {

    protected ServerSocket serverSocket;
	protected Socket socket;
	protected BufferedReader fromClient;
	protected PrintWriter toClient;
    private int portNumber;

    public ServerListener(int portNumber) {
        this.portNumber = portNumber;
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(portNumber);
            while(true) {
                Socket socket = waitForConnectionFromClient();
                if (socket != null) {
					fromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					toClient = new PrintWriter(socket.getOutputStream(), true);
                    // TODO: Handle stuff
                }
            }
        } catch(IOException e) {
            // TODO: Handle exception
        }
    }

    protected Socket waitForConnectionFromClient() {
        Socket res = null;
        try {
            res = serverSocket.accept();
        } catch (IOException e) {
            // We return null on IOExceptions
        }
        return res;
    }

	public boolean isConected() {
		return (socket != null);
	}

	public Socket getSocket() {
		return socket;
	}

	public PrintWriter getPrintWriter(){
		return toClient;
	}

	public BufferedReader getBufferedReader(){
		return fromClient;
	}
}
