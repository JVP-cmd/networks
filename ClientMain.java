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
			Socket socket = new Socket("196.42.108.182", 59090);
                        
                        
			String yeet = "Client " + Client.numClients + " says Hello";
                        Scanner inputs=new Scanner(socket.getInputStream());
                        System.out.println(inputs.nextLine());
                         System.out.println(inputs.nextLine());
                          System.out.println(inputs.nextLine());
                           System.out.println(inputs.nextLine());
                            System.out.println(inputs.nextLine());
                        

         
            String input=inp.nextLine();
               DataOutputStream dout=new DataOutputStream(socket.getOutputStream());
            DataInputStream din=new DataInputStream(socket.getInputStream());
            dout.writeUTF(input);
            String checkDownload=din.readUTF();
             
            if(checkDownload.equals("Download File")){
                System.out.println("Please enter the name of the file to be downloaded");
                dout.writeUTF(inp.nextLine());
              
              
          thisClient.DownloadtoClient(socket);}
            else if(checkDownload.equals("Upload to server")){
                System.out.println("Enter the name of the file to be uploaded");
                
                
                String filename=inp.nextLine();
                dout.writeUTF("prep server");
            thisClient.uploadFile(filename, socket);}
            
            dout.writeUTF(input);
            dout.flush();
          
   

			
				/*Scanner serverin = new Scanner(socket.getInputStream());
				System.out.println(serverin.nextLine());*/
			
		}
		catch(Exception e){
			System.out.println("Error: " + e.getMessage());
			System.exit(0);
		}
	}
     
            
           

    

}