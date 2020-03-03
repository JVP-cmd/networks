import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Threadmanager extends Thread{
    private final int port = 5000;
    private ServerSocket ss = null;
    private Thread threads = null;
    private boolean stopped = false;
    private Socket s;
    private String username, filename;


    public Threadmanager(int port, String filename, String username){
        this.filename = filename;
        this.username = username;
    }

    public void run(){
        synchronized(this){
            this.threads = Thread.currentThread();
        }
        openServerSocket();
        while(! isStopped()){
            Socket clientSocket = null;
            try {
                clientSocket = this.ss.accept();
            } catch (IOException e) {
                if(isStopped()) {
                    System.out.println("Server Stopped.") ;
                    return;
                }
                throw new RuntimeException(
                        "Error accepting client connection", e);
            }
            new Thread(
                    new Threads(ss, clientSocket, filename, username)
            ).start();
        }
        System.out.println("Server Stopped.") ;
    }

    public synchronized boolean isStopped(){
        return stopped;
    }


    public synchronized void stopT(){
        this.stopped = true;
        try {
            this.ss.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    public synchronized void openServerSocket(){
        try {
            this.ss = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
