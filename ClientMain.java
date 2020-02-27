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
            DataInputStream din=new DataInputStream(socket.getInputStream());
            String input=inp.nextLine();
            String checkDownload=din.readUTF();
             
            if(checkDownload.equals("Download File")){
          thisClient.DownloadtoClient(socket);}
            else if(checkDownload.equals("Upload to server")){
                System.out.println("Enter the name of the file to be uploaded");
                
                Scanner s=new Scanner(System.in);
                String filename=s.nextLine();
            thisClient.uploadFile(filename, socket);}
            
            dout.writeUTF(input);
            dout.flush();
         //   dout.close(); maybe need it?
       //     din.close();
   
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