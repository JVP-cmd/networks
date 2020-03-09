package client;


import java.io.*;
import java.net.*;
import java.util.*;

public class ClientMain {

	private static final String downloadPath = System.getProperty("user.home") +  System.getProperty("file.separator") + "Downloads" +System.getProperty("file.separator") + "Pigeon Pirates" + System.getProperty("file.separator");
	private static PrintWriter clientMessenger;
	private static boolean admin;
	private static Socket clientSocket;
	private static Scanner inp = new Scanner(System.in);
	private static Scanner serverMessageScanner;
	private static String accessString;
	private static String username;

 // Start code section

	/**
	 * Runs user through the login process
	 * @return Success value of the login attempt
	 */
	private static boolean logIn(){
		try{
			boolean cantConnect = Boolean.parseBoolean(serverMessageScanner.nextLine());
			while(true) {
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
					if (perms.equals("ADMIN")) {
						accessString = perms;
						admin = true;
					} else {
						//serverMessageScanner.nextLine(); // This write is here to
						accessString = perms;
						admin = false;
					}
					return true;
				}
			}

		}
		catch (Exception e){
			e.printStackTrace();
			return false;
		}
	} // Login works buttery smooth

	/**
	 * Shows the user on the client side the files that they can access. If there are no accessible files, the user will be informed about
	 */
	private static void viewFiles(){
		int numFiles = Integer.parseInt(serverMessageScanner.nextLine()); // Receives the number of files that can be shared from the server

		if(numFiles < 1){
			// Messages that are printed when there are no files to be shared on the server
			if(admin){
				System.out.println("No files have been stored on the server");
			}
			else {
				System.out.println("No files are available to be accessed (None have been logged yet)");
			}
		}
		else {
			// both the client and the thread maintaining the conneciton loop through this together (Storing the string acts as a "delay" for when the file is received)
			System.out.println("Total number of accessible files on the server: " + numFiles + "\n");
			for (int i = 0; i < numFiles; i++) {
				String b = serverMessageScanner.nextLine();
				System.out.println(b);
			}
		}
	}

	/**
	 *	Sends download request to servers and, once the request is accepted, begins donwloading the specified file from the server to the client
	 * @param filename The name of the file that is being downloaded from the server (this will be the full directory of the file)
	 */
	private static void downloadFile(String filename){
		// Controls here

		// NB!!!! Make sure that only the correct operations can be ran at a time

		clientMessenger.println(filename);

		String availableToDown = serverMessageScanner.nextLine();
		if(availableToDown.equals("CANTDOWN")){
			System.out.println("The file you are trying to download is not available at this moment");
		}
		else {
			try {
				String downExist = serverMessageScanner.nextLine();
				if (!(downExist.equals("EXIST"))) { // checks exists message from server and then prints it
					System.out.println("File does not exist. Please make sure that you write out the full directory of the file [Use \"VIEW\" operation to see which files are available for download");
				}
				else {

					DataInputStream din = new DataInputStream(clientSocket.getInputStream());
					DataOutputStream dout = new DataOutputStream(clientSocket.getOutputStream());

					String str = " ";
					str = "bam";
					dout.writeUTF(str); // Sends header line to server before receiving specified file data
					dout.flush();
					filename = din.readUTF();
					String downLocation = downloadPath + filename; // For client, this location will be their downloads folder (Maybe implement an arg based download path later?)
					File f = new File(downLocation);
					if (!f.exists()) { // creates relevant directories needed for saving file
						f.getParentFile().mkdirs();
					}
					str = din.readUTF(); // reads "bam" header line from server and then initiates file receival from server
					long fileSize = Long.parseLong(din.readUTF()); // receives file size (used mainly for ensuring that file transfer transfers an exact copy of the sent file)
					byte b[] = new byte[1]; // Reads 1 byte of data from din at a time
					FileOutputStream fos = new FileOutputStream(f, false); // writes
					long downSize = 0;
					System.out.println("Downloading file: " + f.getName());
					int downmarker = 0;
					try {
						do {
							din.read(b, 0, b.length);
							fos.write(b, 0, b.length);
							downSize++;
							int a = trackProg(downSize, fileSize, downmarker);
							downmarker = a;
						}
						while (downSize < fileSize);
						//end of file exception here
						fos.close();
						dout.flush();
						System.out.println("Download to Client complete. Check the " + f.getParent() + " folder in your downloads folder");
					} catch (Exception e) {
						System.out.println(e);
					}

					//}
				}
			}
			catch(SecurityException e){
				System.out.println("Security exception detected. Are you sure you have access to this program?");
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Sends upload request to servers and, once accepted, begins uploading the specified file from the client to the server
	 * @param filename The name of the file
	 */
	public static void uploadFile(String filename) {
		System.out.flush();
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
				if(Data.equals("bam")) {
					dout.writeUTF(filename);
					dout.flush();
					dout.flush();
					String receival = din.readUTF();
					if (receival.toUpperCase().equals("CANRECEIVE")) {
						System.out.println("Can upload file to server");
						FileInputStream fileIn = new FileInputStream(file);
						long fileSize = file.length();
						byte b[] = new byte[1];
						int read;
						dout.writeUTF("stop");
						dout.writeUTF(Long.toString(fileSize));
						dout.flush();
						System.out.println("Uploading File: " + filename.split("/")[filename.split("/").length - 1]);
						int downmarker = 0;
						long upSize = 0;
						while ((read = fileIn.read(b)) > 0) {
							dout.write(b, 0, read);
							dout.flush();
							upSize++;
							int a = trackProg(upSize, fileSize, downmarker);
							downmarker = a;
						}
						fileIn.close();
						dout.flush();
						String s = serverMessageScanner.nextLine();
						System.out.println(s);
					}
					else {
						System.out.println("Cannot upload specified file to database. File is currently in use");
					}
				}
			}catch (FileNotFoundException e) {
				System.out.println("The file that has been entered cannot be found");
			} catch (Exception e) {
				e.printStackTrace();
				//break;
			}
		}
	}

	/**
	 * Only available to administrator users. This changes the accessibility of the file (makes a file that's public private and a file that's private public)
	 * @param filename The name of the file whose permissions will be changed. Permissions can only be changed by
	 */
	private static void changePerms(String filename){
		System.out.println("Attempting permission change");
		clientMessenger.println(filename);
		String canMove = serverMessageScanner.nextLine();
		if(!(canMove.toUpperCase().equals("CANTMOVE"))) {
			String success = serverMessageScanner.nextLine();
			if (success.equals("SUCCESS")) {
				System.out.println("Permission change successful");
			} else if (success.equals("UNSUCCESS")) {
				System.out.println("Permission change unsuccessful. Please make sure that you entered the correct file name");
			}
		}
		else{
			System.out.println("The current file cannot be moved (A user is currently accessing the file)");
		}

	}

	private static int trackProg(long size, long fullSize, int marker){

		double downProg = (double)size / (double)fullSize;
		if (downProg > 0.1 && marker < 1) {
			System.out.println("10% complete");
			return 1;
		}
		if (downProg > 0.2 && marker < 2) {
			System.out.println("20% complete");
			return 2;
		}
		if (downProg > 0.3 && marker < 3) {
			System.out.println("30% complete");
			return 3;
		}
		if (downProg > 0.4 && marker < 4) {
			System.out.println("40% complete");
			return 4;
		}
		if (downProg > 0.5 && marker < 5) {
			System.out.println("50% complete");
			return 5;
		}
		if (downProg > 0.6 && marker < 6) {
			System.out.println("60% complete");
			return 6;
		}
		if (downProg > 0.7 && marker < 7) {
			System.out.println("70% complete");
			return 7;
		}
		if (downProg > 0.8 && marker < 8) {
			System.out.println("80% complete");
			return 8;
		}
		if (downProg > 0.9 && marker < 9) {
			System.out.println("90% complete");
			return 9;
		}
		return marker;
	}

	/**
	 * Main Method that is ran on the client-side of the program. The client must be ran after the server is online
	 * @param args args[0]: IP Address of the server; args[1]: server port number
	 */
	public static void main(String[] args){
		try {

			// arg[0] : ip Address of server
			// arg[1] : Server port number

			File f = new File(downloadPath);
			if(!f.exists()){
				System.out.println("Download path for Pigeon Pirates hasn't been made. Generating download path..");
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
				// autoOp = true;
				System.out.println("Operation automation is not possible in this version of the Pigeon Pirates");
			}

			// Runs operations automatically (These operations aren't ran automatically
			serverMessageScanner = new Scanner(clientSocket.getInputStream());
			clientMessenger = new PrintWriter(clientSocket.getOutputStream(), true);


			// User login interaction begins here

			boolean signedIn = logIn();

			if(signedIn) {
				clientMessenger.println(autoOp); // Sends auto operation flag to server
				if (autoOp) {
					// System.out.println("Automatic operations ran");


				}
				else {
					while (true) {
						serverMessageScanner = new Scanner(clientSocket.getInputStream());
						boolean serverState = Boolean.parseBoolean(serverMessageScanner.nextLine());
						if(serverState){
							System.out.println("Server is offline, thanks for using us! :D");
							break;
						}
						System.out.println("\nCurrent user: " + username);
						System.out.println("Access level: " + accessString);
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
							System.out.println("Enter the full path name of the file that you would like to upload [note that folders will not be sent to the server]:");
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
