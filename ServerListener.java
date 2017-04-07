import java.net.*;
import java.io.*;

public class ServerListener implements Runnable {
    protected ServerSocket serverSocket;
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
}
