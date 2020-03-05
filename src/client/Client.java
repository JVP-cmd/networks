package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {

	private Socket clientSocket;
	private String clientName;
	private String downloadPath;


	public Client(Socket clientSocket, String clientName, String downloadPath){
		this.clientSocket = clientSocket;
		this.clientName = clientName;
		this.downloadPath = downloadPath;
	}


	public String getClientName(){
		return clientName;
	}

	public boolean setPerms(String filename, Scanner serverMessengerScanner, PrintWriter clientMessenger){
		try{
			clientMessenger.println(filename);
			serverMessengerScanner.nextLine(); // Waiting for server to receive and respond to message
			System.out.println("Checking file usage");
			String tryPermResult = serverMessengerScanner.nextLine();
			if(tryPermResult.equals("canMove")){

			}
			else{
				System.out.println("File cannot be moved at the moment. (File is still in use)");
			}
		}
		catch(Exception e){

		}

		return false;
	}


	public void viewFiles(Scanner serverMessengerScanner, PrintWriter clientMessenger){
		System.out.println(serverMessengerScanner.hasNextLine());
		int numFiles = 3;
		System.out.println(numFiles);
		if(numFiles < 1){
			System.out.println("No files have been saved to the server yet (None have been logged yet)");
		}
		else {
			System.out.println("Total number of files on server: " + numFiles + "\n");
			for (int i = 0; i < numFiles; i++) {
				System.out.println(serverMessengerScanner.nextLine());
			}
		}
		clientMessenger.println("done");
	}

	public boolean downloadFile(String filename2, Scanner serverMessengerScanner, PrintWriter clientMessenger){

		// check if file is present in the server

		try{
			DataInputStream din=new DataInputStream(clientSocket.getInputStream());
			DataOutputStream dout=new DataOutputStream(clientSocket.getOutputStream());
			BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
			String str=" "; String filename="";
			String saveLoc = downloadPath;
			//while(!str.equals("stop")){
				str="bam";

				dout.writeUTF(str);
				dout.flush();

				str=din.readUTF();
				filename=din.readUTF();
				File f = new File(saveLoc);
				System.out.println(f.length());
				saveLoc+=din.readUTF();
				long sz=Long.parseLong(din.readUTF());
				byte b[]=new byte [2];
				FileOutputStream fos=new FileOutputStream(f,true);
				long bytesRead=1;
				System.out.println("Download to Client complete ");
				try{
					do{
						bytesRead=din.read(b,0,b.length);

						//str=din.readUTF();
						fos.write(b,0,b.length);
					}
					while(!(bytesRead<2));{//end of file exception here
					fos.close();
					dout.close();
				}
				}
				catch(Exception e){
					e.printStackTrace();
				}

			//}
		}
		catch(Exception e){System.out.println(e);}
		return false;
	}


	public boolean uploadToServer(String fileName){

		// Client Server communication here (File already exists in server, overwrite?)
		while(true) {
			try {
				DataInputStream din = new DataInputStream(clientSocket.getInputStream());
				DataOutputStream dout = new DataOutputStream(clientSocket.getOutputStream());

				String Data;

				System.out.println("reading UTF");

				Data = din.readUTF();

				if (!Data.equals("stop")) {
					System.out.println("Link to server created. Uploading file...");
					dout.writeUTF(fileName);
					dout.flush();
					File file = new File(fileName);
					FileInputStream fileIn = new FileInputStream(file);
					long sz = (int) file.length();
					byte b[] = new byte[1024];
					int read;
					dout.writeUTF(Long.toString(sz));
					dout.flush();
					while ((read = fileIn.read(b)) != 1) {
						dout.write(b, 0, read);
						dout.flush();
					}
					fileIn.close();
					dout.flush();
				}
				dout.writeUTF("stop");
				dout.flush();
				din.close();
			} catch (ArrayIndexOutOfBoundsException e) {
				return true;
			}
			catch(EOFException e){
				return true;
			}
			catch (Exception e) {
				return false;
			}
		}
	}

	// So for some odd reason, when I smoke, I can't concentrate on anything. Especially when I'm tired, guess I
	// shouldn't smoke when I'm in the zone.


	// Alright, so we removing the client.java class from the project. Tyty.
}