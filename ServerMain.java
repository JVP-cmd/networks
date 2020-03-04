import java.net.*;
import java.io.*;


public class ServerMain {
    public static void main(String[] args) { // Server main thread [child threads will be made in server object]
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            String fileLoc = "res";
            Server server = new Server(ip, fileLoc, 59090);
            server.initialize();

            while(true){
                server.listen();
                System.out.println("Connection received");
            }
        }

        catch(Exception e){
            System.out.println("An error occurred while running the server: " + e.getMessage());
            System.exit(1);
        }
    }
}
