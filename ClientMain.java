import java.net.*;
import java.io.*;
import java.util.*;

public class ClientMain {

	public static void main(String[] args){
            
		Scanner inp = new Scanner(System.in);
                
		try {
			Client thisClient;
                        
                        // Login procedure here
                        
			String ip = InetAddress.getLocalHost().getHostAddress();
			String machineName = InetAddress.getLocalHost().getHostName();
			System.out.println("Current ip address is: " + ip);
			thisClient = new Client(ip, machineName);
			System.out.println("Current client details:\nClient Name: " + thisClient.getClientName() + "\nClient IP: " + thisClient.getIpAddress());
			Socket socket = new Socket("196.47.228.194", 59090);
                        
                        
			String yeet = "Client " + Client.numClients + " says Hello";
while(true){
            DataOutputStream dout=new DataOutputStream(socket.getOutputStream());
            String input=inp.nextLine();
            dout.writeUTF(input);
            dout.flush();
}
			
				/*Scanner serverin = new Scanner(socket.getInputStream());
				System.out.println(serverin.nextLine());*/
			
		}
		catch(Exception e){
			System.out.println("Error: " + e.getMessage());
			System.exit(0);
		}
	}
     
            
           

    

}
