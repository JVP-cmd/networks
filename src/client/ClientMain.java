package client;


import java.io.*;
import java.net.*;
import java.util.*;

public class ClientMain {

	private static final String downloadPath = System.getProperty("user.home") + "\\Downloads\\YeetLoader\\";
	private static PrintWriter clientMessenger;
	private static boolean admin;
	private static boolean contributor; // When a public connection is detected, but the user doesn't have an account [So they cannot upload]
	private static Socket clientSocket;
	private static Scanner inp = new Scanner(System.in);
	private static Scanner serverMessageScanner;
	private static String accessString;
	private static String username;

 // Start code section
	private static boolean logIn(){
		try{
			while(true) {
				boolean cantConnect = Boolean.parseBoolean(serverMessageScanner.nextLine());
				System.out.print(serverMessageScanner.nextLine());
				if(cantConnect){ // Exits program if the servers are "full"
					System.out.println(". Send any input to close the program");
					inp.nextLine();
					System.exit(0);
				}
				System.out.println();
				String userName = inp.nextLine().trim();
				clientMessenger.println(userName);
				// Server asks user for password (or sends user off if they decide to quit the program)
				if(userName.toUpperCase().equals("PUBLIC")){
					contributor = false;
				}
				else {
					if (clientSocket.isConnected()) {
						System.out.println(serverMessageScanner.nextLine());
					} else {
						System.out.println("Connection to server lost =(");
						System.exit(0);
					}
					if (userName.equals("quit")) {
						System.exit(0);
					}
					String password = inp.nextLine();
					clientMessenger.println(password);
					boolean success = Boolean.parseBoolean(serverMessageScanner.nextLine()); // Receives message from server as to whether or not
					System.out.println(serverMessageScanner.nextLine()); // Notifies user whether or not the login attempt was successful

					if (success) {
						String perms = serverMessageScanner.nextLine();
						username = userName;
						contributor = false;
						if (perms.equals("ADMIN")) {
							accessString = perms;
							admin = true;
						} else {
							accessString = perms;
							admin = false;
						}
						return true;
					}
				}
			}
		}
		catch (Exception e){
			e.printStackTrace();
			return false;
		}
	} // Login works buttery smooth

	private static void viewFiles(){
		int numFiles = Integer.parseInt(serverMessageScanner.nextLine());
		if(numFiles < 1){
			System.out.println("No files have been saved to the server yet (None have been logged yet)");
		}
		else {
			System.out.println("Total number of accessible files on server: " + numFiles + "\n");
			for (int i = 0; i < numFiles; i++) {
				String b = serverMessageScanner.nextLine();
				System.out.println(b);
			}
		}
		//clientMessenger.println("done");
	}

	private static void downloadFile(String filename){
		// Controls here

		// NB!!!! Make sure that only the correct operations can be ran at a time

		try {
			clientMessenger.println(filename);
			if (!(serverMessageScanner.nextLine().equals("EXIST"))) { // checks exists message from server and then prints it
				System.out.println("File does not exist. Please make sure that you write out the full directory of the file [Use \"VIEW\" operation to see which files are available for download");
			} else {
				DataInputStream din = new DataInputStream(clientSocket.getInputStream());
				DataOutputStream dout = new DataOutputStream(clientSocket.getOutputStream());


				String str = " ";

				while (!str.equals("stop")) {
					str = "bam";
					dout.writeUTF(str);
					dout.flush();
					filename = din.readUTF();
					String downLocation = downloadPath + filename;
					File f = new File(downLocation);

					if(!f.exists()){
						boolean dirsmade = f.getParentFile().mkdirs();
						if(dirsmade){
							System.out.println("Directory made successfully.");
						}
					}
					str = din.readUTF();
					long sz = Long.parseLong(din.readUTF());
					System.out.println(sz);
					byte b[] = new byte[1]; // Reads 1 byte of data from din at a time

					FileOutputStream fos = new FileOutputStream(f, false);
					long bytesRead = 1;
					long downSize = 0;
					System.out.println("Downloading file: " + f.getName());

					try {
						do {
							bytesRead = din.read(b, 0, b.length);
							fos.write(b, 0, b.length);
							downSize++;
						}
						while (downSize < sz);
						//end of file exception here

						fos.close();
						dout.flush();
						System.out.println("Download to Client complete. Check the " + f.getParent() + " folder in your downloads folder");
					}
					catch (Exception e) {
						System.out.println(e);
					}

				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void uploadFile(String filename) {
		//while (true) {
		File file = new File(filename);
		if(!file.exists()){
			clientMessenger.println("DNEXIST");
			System.out.println("File does not exist. Please make sure that the path that you have entered is correct");
		}
		else if(file.isDirectory()){
			clientMessenger.println("DNEXIST");
			System.out.println("Cannot send this file. This is a directory");
		}
		else {
			clientMessenger.println("EXISTS");
			try {
				DataInputStream din = new DataInputStream(clientSocket.getInputStream());
				DataOutputStream dout = new DataOutputStream(clientSocket.getOutputStream());
				String Data;
				Data = din.readUTF();
				System.out.println(Data);
				dout.writeUTF(filename);
				dout.flush();
				dout.flush();
				if (din.readUTF().toUpperCase().equals("CANRECEIVE")) {
					System.out.println("Can upload file to server");
					FileInputStream fileIn = new FileInputStream(file);
					long fileSize = file.length();
					byte b[] = new byte[1];
					int read;
					dout.writeUTF("stop");
					dout.writeUTF(Long.toString(fileSize));
					dout.flush();
					System.out.println("Uploading File: " + filename.split("/")[filename.split("/").length - 1]);
					while ((read = fileIn.read(b)) > 0) {
						dout.write(b, 0, read);
						dout.flush();
					}
					fileIn.close();
					dout.flush();
					System.out.println(serverMessageScanner.nextLine());
				}
				else{
					System.out.println("Cannot interact with file. File is in use");
				}
			}catch (FileNotFoundException e) {
				System.out.println("The file that has been entered cannot be found");
			} catch (Exception e) {
				e.printStackTrace();
				//break;
			}
		}
	}

	public static void changePerms(String filename){

	}

	public static void main(String[] args){
		try {

			// arg[0] : ip Address of server
			// arg[1] : Server port number

			File f = new File(downloadPath);
			if(!f.exists()){
				System.out.println("Download path for YeetLoader hasn't been made. Generating download path..");
				boolean downPathMade = f.mkdirs();
				if(downPathMade){
					System.out.println("Directory made successfully");
				}
				else{
					System.out.println("Directory not accessed. Please make sure that you, the user, have permission to access the folder");
					System.exit(0);
				}
			}
			boolean autoOp = false;
			String ip = InetAddress.getLocalHost().getHostAddress();
			System.out.println("Current ip address is: " + ip);

			clientSocket = new Socket(args[0], Integer.parseInt(args[1]));
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
					while (true) {
						serverMessageScanner = new Scanner(clientSocket.getInputStream());
						boolean serverState = Boolean.parseBoolean(serverMessageScanner.nextLine());
						if(serverState){
							System.out.println("Server is offline, thanks for using us! :D");
							break;
						}
						System.out.println("\nAccess level: " + accessString);
						System.out.println("Select an action: \n<View> - To view files on server\n<Upload> - Upload a file to the server\n<Download> - To download a file");
						if(admin) {
							System.out.println("<Perm> - Set Permissions");
						}
						System.out.println("<Quit> - Quit");
						String operationInput = inp.nextLine();
						clientMessenger.println(operationInput); // Sends operation to server
						if(operationInput.toUpperCase().trim().equals("VIEW")){
							System.out.println("Initiating VIEW operation...");
							// Prompts user to do stuff
							//serverMessageScanner.nextLine(); // Sends operation message to server (Prints happen on this side)
							viewFiles();
						}
						else if(operationInput.toUpperCase().trim().equals("UPLOAD")){
							System.out.println("Enter the full path name of the file that you would like to upload:");
							String file = inp.nextLine();
							uploadFile(file);
						}
						else if(operationInput.toUpperCase().trim().equals("DOWNLOAD")){
							System.out.println("Enter the name of the file that you would like to download:");
							String file = inp.nextLine();
							downloadFile(file);

						}
						else if(operationInput.toUpperCase().trim().equals("PERM")){
							if(admin) {
								System.out.println("Enter the name of the file that you would like to control the permissions of");
								String file = inp.nextLine();
								changePerms(file);
							}
							else{
								System.out.println("Operation queried does not exist");
							}
							//changePerms();
						}
						else if(operationInput.toUpperCase().trim().equals("QUIT")){
							System.out.println("Thank you, have a nice day! =D");
							break;
						}
						else{
							System.out.println("Operation queried does not exist");
						}
					}
					clientSocket.close();
				}
			}
			else{
				System.out.println("An error occurred during the login process");
			}
		}
		catch(SocketException e){
			System.out.println("Error with connection to server with ip " + args[0]);
		}
		catch(NoSuchElementException e){
			System.out.println("Connection with server lost");
		}
		catch(Exception e){
			e.printStackTrace();
			System.exit(0);
		}
	}
}
