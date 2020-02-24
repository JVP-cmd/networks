import java.util.Scanner;
import java.net.Socket;
import java.io.IOException;
import java.net.InetAddress;

/**
 * A command line client for the date server. Requires the IP address of the
 * server as the sole argument. Exits after printing the response.
 */
public class Client {
    public static void main(String[] args) throws IOException {
    
        Socket socket = new Socket("196.42.105.143", 59090);
        Scanner in = new Scanner(socket.getInputStream());
        System.out.println("Server response: " + in.nextLine());
    }
}