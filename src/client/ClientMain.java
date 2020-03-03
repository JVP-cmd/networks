package client;


import java.io.*;
import java.net.*;
import java.util.*;

public class ClientMain {

	private static Client thisClient;
	private static final String downloadPath = System.getProperty("user.home") + "/Downloads/";
	private static PrintWriter clientMessenger;
	private static Socket clientSocket;
	private static Scanner inp = new Scanner(System.in);
	private static Scanner serverMessageScanner;

 // Start code section
	private static boolean logIn(){
		try{
			while(true) {
				System.out.println(serverMessageScanner.nextLine());
				String userName = inp.nextLine().trim();
				clientMessenger.println(userName);
				// Server asks user for password (or sends user off if they decide to quit the program)
				if(clientSocket.isConnected()) {
					System.out.println(serverMessageScanner.nextLine());
				}
				else{
					System.out.println("Connection to server lost =(");
					System.exit(0);
				}
				if(userName.equals("quit")){
					System.exit(0);
				}
				String password = inp.nextLine();
				clientMessenger.println(password);
				boolean success = Boolean.parseBoolean(serverMessageScanner.nextLine()); // Receives message from server as to whether or not
				System.out.println(serverMessageScanner.nextLine()); // Notifies user whether or not the login attempt was successful

				if(success){
					return true;
				}
			}
		}
		catch (Exception e){
			return false;
		}
	} // Login works buttery smooth

	private static void viewFiles(){
		int numFiles = 3;
		String a = serverMessageScanner.nextLine();
		if(numFiles < 1){
			System.out.println("No files have been saved to the server yet (None have been logged yet)");
		}
		else {
			System.out.println("Total number of files on server: " + numFiles + "\n");
			for (int i = 0; i < numFiles; i++) {
				System.out.println(serverMessageScanner.nextLine());
			}
		}
		clientMessenger.println("done");
	}

	private static void downloadFile(String filename){
		// Controls here

		try {
			clientMessenger.println(filename);
			if (!(serverMessageScanner.nextLine().equals("EXIST"))) { // checks exists message from server and then prints it
				System.out.println("File does not exist");
			} else {
				DataInputStream din = new DataInputStream(clientSocket.getInputStream());
				DataOutputStream dout = new DataOutputStream(clientSocket.getOutputStream());


				String str = " ";

				while (!str.equals("stop")) {
					System.out.println("Downloading file: " + filename);
					str = "bam";
					dout.writeUTF(str);
					dout.flush();
					filename = din.readUTF();
					String downLocation = downloadPath + filename;
					str = din.readUTF();
					long sz = Long.parseLong(din.readUTF());
					System.out.println(sz);
					byte b[] = new byte[1];

					FileOutputStream fos = new FileOutputStream(new File(downLocation), false);
					long bytesRead = 1;
					long downSize = 0;

					try {
						do {
							bytesRead = din.read(b, 0, b.length);
							fos.write(b, 0, b.length);
							downSize++;
						}
						while (downSize < sz);
						{//end of file exception here

							fos.close();
							dout.flush();
						}
						System.out.println("Download to Client complete ");
					} catch (Exception e) {
						System.out.println(e);
					}

				}
			}
		}
		catch(Exception e){
			System.out.println(e);
		}
	}

	public static void uploadFile(String filename) {
		//while (true) {
			try {
				File file = new File(filename);
				DataInputStream din = new DataInputStream(clientSocket.getInputStream());
				DataOutputStream dout = new DataOutputStream(clientSocket.getOutputStream());

				String Data;
				Data = din.readUTF();
				System.out.println(Data);
					System.out.println("Uploading File: " + filename.split("/")[filename.split("/").length - 1]);
					dout.writeUTF(filename);
					dout.flush();
					dout.flush();
					FileInputStream fileIn = new FileInputStream(file);
					long fileSize = file.length();
					byte b[] = new byte[1];
					int read;
					dout.writeUTF("stop");
					dout.writeUTF(Long.toString(fileSize));
					dout.flush();
					while ((read = fileIn.read(b)) > 0) {
						dout.write(b, 0, read);
						dout.flush();
					}
					fileIn.close();
				//}

				dout.flush();
				//din.close();
				//dout.close();
				System.out.println(serverMessageScanner.nextLine());
				//s.close();
			}
			catch(FileNotFoundException e){
				System.out.println("The file that has been entered cannot be found");
			}
			catch (Exception e) {
				e.printStackTrace();
				//break;
			}
		//}
	}

	public static void main(String[] args){
		try {
			// arg[0] :
			// arg[1] :
			// arg[2] :
			// arg[3] :
			// arg[4] :
			boolean autoOp = false;
			String ip = InetAddress.getLocalHost().getHostAddress();
			String machineName = InetAddress.getLocalHost().getHostName();
			System.out.println("Current ip address is: " + ip);
			thisClient = new Client(clientSocket, machineName, downloadPath);

			clientSocket = new Socket("196.47.228.194", 59090);
			if(args.length > 3){
				autoOp = true;
			}

			// Runs operations automatically (These operations aren't ran automatically
			serverMessageScanner = new Scanner(clientSocket.getInputStream());
			clientMessenger = new PrintWriter(clientSocket.getOutputStream(), true);

			// User login interaction begins here

			boolean signedIn = logIn();

			if(signedIn) {
				clientMessenger.println(autoOp); // Sends auto operation flag to server
				if (autoOp) {
					System.out.println("Automatic operations ran");
				}
				else {
					System.out.println("Welcome to the server!");
					while (true) {
						serverMessageScanner = new Scanner(clientSocket.getInputStream());
						System.out.println("Select an action: \n<View> - To view files on server\n<Upload> - Upload a file to the server\n<Download> - To download a file\n<Perm> Set Permissions\n<Quit> - Quit");
						String operationInput = inp.nextLine();
						clientMessenger.println(operationInput); // Sends operation to server
						if(operationInput.toUpperCase().trim().equals("VIEW")){
							System.out.println("Initiating VIEW operation...");
							// Prompts user to do stuff
							serverMessageScanner.nextLine(); // Sends operation message to server (Prints happen on this side)
							viewFiles();
						}
						if(operationInput.toUpperCase().trim().equals("UPLOAD")){
							System.out.println("Enter the full path name of the file that you would like to upload:");
							String file = inp.nextLine();
							uploadFile(file);
						}
						if(operationInput.toUpperCase().trim().equals("DOWNLOAD")){
							System.out.println("Enter the name of the file that you would like to download:");
							String file = inp.nextLine();
							downloadFile(file);

						}
						if(operationInput.toUpperCase().trim().equals("PERM")){
							serverMessageScanner.nextLine();
							//changePerms();
						}
						if(operationInput.toUpperCase().trim().equals("QUIT")){
							System.out.println("Thank you, have a nice day! =D");
							break;
						}
					}
				}
			}
			else{
				System.out.println("An error occurred during the login process");
			}
		}
		catch(SocketException e){
			e.printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
			System.exit(0);
		}
	}
}
