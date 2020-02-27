


import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Server {

	private String ipAddress;
	private String resourceDir;
	public enum Status {Available, Offline, Full};
	private Status status;
	private int socketNo;
	private int numConnections;
	private ServerSocket serverSocket;

	public Server(String ipAddress, String resourceDir, int socketNo){
		this.ipAddress = ipAddress;
		this.resourceDir = resourceDir;
		status = Status.Offline;
		this.socketNo = socketNo;
		numConnections = 0;
	}

	public void initialize(){ // Initializes server socket using specified port number
		try {
			serverSocket = new ServerSocket(this.socketNo);
			System.out.println("Socket initialized");

			// Generate all paths in server [Will have res folder that keeps all files on server]
			//


			status = Status.Available;
		}
		catch(IOException e) {
			System.out.println("Error setting up server on port " + this.socketNo);
		}
	}

	private void printAllAccessible(String username, Socket socket){

	}

	private void DownloadFile(String filename, Socket s){
        while(true){
                try{
 DataInputStream din=new DataInputStream(s.getInputStream());
            DataOutputStream dout=new DataOutputStream(s.getOutputStream());
            
            String Data;
            Data=din.readUTF();
            if(!Data.equals("stop")){
            System.out.println("Downloading File: "+filename);
            dout.writeUTF(filename);
            dout.flush();
            File file=new File(filename);
            FileInputStream fileIn=new FileInputStream(file);
            long sz=(int)file.length();
            byte b[]=new byte[1024];
            int read;
            dout.writeUTF(Long.toString(sz));
            dout.flush();
            while((read=fileIn.read(b))!=1){
            dout.write(b,0,read);
            dout.flush();}
            fileIn.close();
            dout.flush();}
            dout.writeUTF("stop");
            dout.flush();
             din.close();
            //s.close();
            }catch(Exception e){System.out.println(e);}
           
            
            }

	}

	

	public void listen(){
		System.out.println("Listening");
		try(Socket socket = serverSocket.accept()) {
			boolean runOps = false;
			numConnections++;
			System.out.println("Connection from " + socket.getLocalAddress().getHostAddress() + " detected");
                        System.out.println("Sending promts...");
                        PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                        pw.println("Select an action: ");
                         pw.println("<View> - To view files on server");
                          pw.println("<Download> - To download a file");
                           pw.println("<Upload> - To upload a file");
                            pw.println("<Permission> Set Permission");
                        DataInputStream din=new DataInputStream(socket.getInputStream());
                        String userinp=din.readUTF();
                        
                        if(userinp.equals("Download")){
                        pw.println("Please enter the name of the file to be downloaded");
                        }
                            
                        
                        /*
			if(this.status == Status.Offline){
				System.out.println("Connection denied"); // send message to client [Saying servers aren't available]
			}
			else if(this.status == Status.Full){
				System.out.println("Servers are full"); // Send message to clients [Saying server is full and that they
														// should try later

			}
			else {
				Scanner s = new Scanner(socket.getInputStream());
				System.out.println(numConnections);
				System.out.println(s.nextLine());
				PrintWriter msg = new PrintWriter(socket.getOutputStream(), true);
				msg.println("The server says hi back");
				// We create threads here (For now, we just let stuff send items through and stuff)
			}*/
		}

		catch(IOException e){
			System.out.println("Error while listening for connections");
		}

	}

	public ServerSocket getServerSocket(){
		return serverSocket;
	}


	public void receiveConnection(Socket socket){
		try{
			numConnections++;
		}
		catch(Exception e){
			System.out.println("Connection to server was unsuccessful");
		}
	}

	public Status getStatus(){
		return status;
	}

	public String getResourceDir(){
		return resourceDir;
	}

	public String getIpAddress(){
		return ipAddress;
	}

	public int getSocketNo(){
		return socketNo;
	}
	public int getNumConnections(){
		return numConnections;
	}
}