package server;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

	private String resourceRepo;
	private String userDB;
	public volatile boolean closed;
	public enum Status {AVAILABLE, OFFLINE, FULL}
	public enum Operation {VIEW, UPLOAD, DOWNLOAD, PERMISSION, UNKNOWN, QUIT}
	private volatile Status status;
	private int socketNo;
	private volatile AtomicInteger numConnections;
	private final int maxConnections;
	private ServerSocket serverSocket;
	private Thread threadManager;
	private ArrayList<User> users;
	private ArrayList<Thread> serverThreads;
	private ArrayList<FileOperation> fileOperations;
	private ArrayList<FileDetails> fileRepoArr;

	public Server(String resourceRepo, int socketNo, int maxConnections){
		this.maxConnections = maxConnections;
		this.resourceRepo = resourceRepo;
		status = Status.OFFLINE;
		this.socketNo = socketNo;
		numConnections = new AtomicInteger(0);
	}

	public void initialize(){ // Initializes server socket using specified port number
		try {
			serverSocket = new ServerSocket(this.socketNo);
			users = new ArrayList<>();
			fileOperations = new ArrayList<>();
			fileRepoArr = new ArrayList<>();
			//File userFile = new File(userDB);
			// Used for testing purposes
			users.add(new User("john", "1234qwer", User.Access.PUBLIC));
			users.add(new User("pete", "1234qwer", User.Access.PUBLIC));
			users.add(new User("bob", "1234qwer", User.Access.ADMIN));

			serverThreads = new ArrayList<>();
			System.out.println("Socket initialized");
			System.out.println("Users stored on current server: " + users.size());
			closed = false;
			threadManager = new Thread(new ServerManager(this));
			threadManager.start();

			// Generate all paths in server [Will have res folder that keeps all files on server]
			//

			System.out.println("Server ready to handle connections...");
			status = Status.AVAILABLE;
		}
		catch(IOException e) {
			System.out.println("Error setting up server on port " + this.socketNo);
		}
	}

	public void listen(){
		System.out.println("Listening");
		try{

			Socket socket = serverSocket.accept();
			// Start threading from here
			System.out.println("Connection from " + socket.getLocalAddress().getHostAddress() + " detected");
			PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
			if(this.status == Status.OFFLINE){
				System.out.println("Connection denied"); // send message to client [Saying servers aren't available]
				pw.println("Connection denied. Server is offline");
			}
			else if(this.status == Status.FULL){
				System.out.println("Servers are full"); // Send message to clients [Saying server is full and that they
														// should try later
				pw.println("Connection denied. Server are full");
			}
			else {

				// Begin threading here (User then begins to interact accordingly
				Thread t = new Thread(new ConnectionThread(socket, this));
				// Need to ensure that socket is moved to a different port for communication with server

				serverThreads.add(0, t);
				serverThreads.get(0).start();

				if(numConnections.get() >= maxConnections){
					this.status = Status.FULL;
				}
			}
		}
		catch(SocketException e){
			System.out.println("Socket interrupted");
		}
		catch(IOException e){
			System.out.println("Error while listening for connections");
		}
		catch(Exception e){

		}

	}

	public void close(){
		status = Status.OFFLINE;
		try {
			serverSocket.close();
			threadManager.join();
		}
		catch(Exception e){
			System.out.println("Problem encountered with closing connections");
		}
	}

	// Private methods responsible for data management [These methods will be in connection threads]


	private User getUser(int userIndex){
		return users.get(userIndex);
	}

	private FileOperation.FileOp checkFileOperations(String fileName){
		for(int i = 0; i <fileOperations.size(); i++){
			if(fileOperations.get(i).getFileName().equals(fileName)){
				return fileOperations.get(i).getFileOp();
			}
		}
		return FileOperation.FileOp.NONE;
	}

	private ArrayList<FileOperation> checkFileDownOps(String fileName){
		ArrayList<FileOperation> ops = new ArrayList<>();
		for(int i = 0; i < fileOperations.size(); i++){
			// Add available files to ops [based on fileName argument]
			// Admin gets to make 2 calls (Can access public and private)
			// Some files can only have 1 name since this is an open platform kinda thing
		}
		return ops;
	}

	private boolean checkLogin(InetAddress address, String userName, String password){
		System.out.println("Login attempt detected from " + address.getHostAddress() + "...");
		for(int i = 0; i < users.size(); i++){
			if(users.get(i).getUserName().equals(userName.toLowerCase())){ // Checks if user exists in server
				return users.get(i).logIn(userName, password, address);
			}
		}
		System.out.println("Login attempt detected from " + address.getHostAddress() +" unsuccessful (User does not exist)\n\n"); // Prints this server side (mainly for debugging)
		return false;
	}

	private void logOutUser(int userIndex){
		users.get(userIndex).logOut();
	}

	private int getUserIndex(String userName){ // This method is only ran when a client has successfully logged in
		for(int i = 0; i < users.size(); i++){
			if(users.get(i).getUserName().equals(userName.toLowerCase())){
				return i;
			}
		}
		return -1;
	}

	public int numThreads(){
		return serverThreads.size();
	}

	public int numloggedIn(){
		int a = 0;
		for(int i = 0; i < users.size(); i++) {
			if(users.get(i).loggedIn()){
				a++;
			}
		}
		return a;
	}

	private void addFileOp(FileOperation fileOp){
		fileOperations.add(fileOp);
	}

	///////////////////////////////////////////////////////////////////// Private classes here (used mainly for threading) ////////////////////////////////////////////////////////////////////////////

	private static class ConnectionThread implements Runnable{
		Socket conSock;
		int userIndex; // (We do, however, need it in case connection is lost [sign user out]) User is stored here mainly to ensure that they are signed out when operation is complete
		String filename;
		Operation operation;
		Server server;
		boolean autoOp;
		private PrintWriter serverMessanger;
		private Scanner clientMessageScanner;


		public ConnectionThread(Socket conSock, Server server){
			this.conSock = conSock;
			this.server = server;
			operation = Operation.UNKNOWN;
			autoOp = false;
		}

		// In the case where the client enters arguments into the program by themselves

		@Override
		public void run() {
			try {
				boolean userLoggedIn = signUserIn();
				String a = conSock.getInputStream().toString(); // Here to catch SocketException

				// Still need to test all of this. For now, we just need to ensure that the user can connect to the server (user can connect to server =D)
				if (userLoggedIn) {
					autoOp = Boolean.parseBoolean(clientMessageScanner.nextLine()); // Receives whether or not the user is automating operations
					System.out.println("Auto operation flag read here");
					System.out.println(autoOp);
					if (autoOp) {
						interaction1();
						Thread.sleep(10);
					}
					else {
						System.out.println("Initializing interaction2");
						interaction2(); // See interaction2() method
					}
					server.logOutUser(userIndex);
					server.numConnections.getAndDecrement();
				}
			}
			catch(SocketException e){
				System.out.println("Socket lost");
				server.logOutUser(userIndex);
			}
			catch(IOException e){
				System.out.println("Yeetus deletus");
				server.logOutUser(userIndex);
			}
			catch(Exception e){
				System.out.println("Exception breaking program");
				server.logOutUser(userIndex);
			}
		}

		private void interaction1(){ // Responsible for interactions where user specifies operations they want to do through command line args
			try {
				this.serverMessanger = new PrintWriter(conSock.getOutputStream(), true);
				serverMessanger.println("Initiating " + operation.toString() + " operation.");
				switch(operation.ordinal()){
					case 0:
						// View operation called

						break;
					case 1:
						// Upload operation called (Must check if file already exists on server and ask user if )

						serverMessanger.println("File with name " + filename + "already exists, would you like to overwrite it?(Y/N)");
						String userInput = "";
						userInput = userInput.toUpperCase();
						if(userInput.equals("Y")){
							serverMessanger.println("Initializing upload protocol...");
						}
						else if(userInput.equals("N")){
							serverMessanger.println("Upload operation cancelled");
						}
						else{
							serverMessanger.println("Invalid input was entered. Upload operation has been cancelled");
						}
						break;
					case 2:
						// Download operation called

						break;
					default:
						System.out.println("Invalid operation detected from " + conSock.getInetAddress().getHostAddress());
						serverMessanger.println("Invalid operation entered.");
						break;
				}
			}
			catch(Exception e){

			}

		}

		private boolean signUserIn(){
			try {
				server.numConnections.getAndIncrement();
				boolean loggedIn = false;
				// Figure out a way to prevent the chance of 2 people signing in to the same account at the same time (which would mean there is a rare chance that 2 clients can connect to the same "user account")
				while (true) {
					this.serverMessanger = new PrintWriter(this.conSock.getOutputStream(), true); // Object that sends messages to client

					serverMessanger.println("Enter username (or send \"quit\" to end):");
					this.clientMessageScanner = new Scanner(this.conSock.getInputStream()); // Object that receives messages from client from other side of the socket
					String userName = clientMessageScanner.nextLine(); // Waits for client to send message

					System.out.println(userName);
					if (userName.equals("quit")) {
						serverMessanger.println("Thank you. Have a nice day! =D");
						break;
					}
					serverMessanger.println("Enter password:");
					String password = clientMessageScanner.nextLine();
					System.out.println("userName: " + userName + "\nPassword: " + password);
					boolean correct = server.checkLogin(conSock.getInetAddress(), userName, password);
					serverMessanger.println(correct);
					if (correct) { // Checks if user details are entered correctly
						serverMessanger.println("Login successful! Welcome " + userName);
						userIndex = server.getUserIndex(userName);
						loggedIn = true;
						break;
					} else {
						serverMessanger.println("Login unsuccessful");
					}
				}
				return loggedIn;
			}
			catch(SocketException e){
				System.out.println("Socket timed out");
			}
			catch(IOException e){
				System.out.println("random IOException that occurs");
			}
			return false;
		}

		private void interaction2(){ // Mainly responsible for interactions where user doesn't specify the operations they want to do through command line args
			try{
				conSock.getOutputStream(); // Line of code is here just so that we can catch SocketException
				boolean userQuit = false;
				while (!userQuit) {
					while (true) {
						Thread.sleep(100);
						String option = clientMessageScanner.nextLine().toUpperCase();
						if (option.equals("UPLOAD")) {
							System.out.println("Upload called");
							//serverMessanger.println("ready"); // Ensures that server is ready for operation to complete
							receiveFile(); // Fix communication
						}
						else if (option.equals("DOWNLOAD")) {
							System.out.println("Download called");
							//serverMessanger.println("ready");
							sendToClient(); // Fix communication
						}
						else if (option.equals("VIEW")) {
							System.out.println("VIEW called");
							viewFiles(); // Fix communication
						}
						else if (option.equals("PERM")) {
							serverMessanger.println("PERM called");
							changePerms(); // Fix communication
						}
						else if (option.equals("QUIT")) {
							System.out.println("User " + server.users.get(userIndex).getUserName() + " sent quit command");
							server.logOutUser(userIndex);
							System.out.println("User " + server.getUser(userIndex).getUserName() + " from connection " + conSock.getInetAddress().getHostAddress() + " has signed out successfully");
							userQuit = true;
							break; // only breaking out of 1 loop
						}
					}
				}
			}
			catch(SocketException e){
				System.out.println("Error. Connection with user timed out");
				server.logOutUser(userIndex);
			}
			catch (Exception e){
				e.printStackTrace();
			}

		}

		private void receiveFile(){


			// Add upload controls here


			try{
				DataInputStream din=new DataInputStream(conSock.getInputStream());
				DataOutputStream dout=new DataOutputStream(conSock.getOutputStream());

				String str=""; String filename="";

				System.out.println("upload to Server started");
				str="bam";
				dout.writeUTF(str);
				dout.flush();
				filename=din.readUTF();
				String[] fileArr = filename.split("/");
				String saveFileDir = "src/Server/res/" + fileArr[fileArr.length - 1];
				str=din.readUTF();
				long fileSize=Long.parseLong(din.readUTF());
				byte b[] = new byte[1];
				FileOutputStream fos=new FileOutputStream(new File(saveFileDir),false);
				long downSize = 0;
				do{
					din.read(b,0,b.length);
					fos.write(b,0,b.length);
					downSize++;
				}
				while(downSize<fileSize);
				fos.flush();
				fos.close();
				//dout.close();
				dout.flush();
				System.out.println("File written to server successfully");
				serverMessanger.println("Upload complete");

			}
			catch(SocketException e){

			}
			catch(Exception e){
				e.printStackTrace();
			}
		}

		private void sendToClient() {
			String filename = clientMessageScanner.nextLine();
			//String dirLoc = server.resourceRepo +server.getUser(userIndex).getUserName()+"/public/";

			// Check file status (See if anything is happening with our file)

			boolean fileAvailable = true;
			// Do later

			// End filecheck
			//while (true) {//infinite while loop to wait for the client to be ready to recieve


				try {

					// Does exist check must

					DataInputStream din = new DataInputStream(conSock.getInputStream());
					DataOutputStream dout = new DataOutputStream(conSock.getOutputStream());

					String finalFilePath = server.resourceRepo + filename;
					File file = new File(finalFilePath);
					FileInputStream fileIn = new FileInputStream(file);
					serverMessanger.println("EXIST");
					String Data;
					Data = din.readUTF(); //input recived from the DownloadtoClient method in Client class (recives "bam ")
					System.out.println(Data);

					//while (!Data.equals("stop")) {

						System.out.println("Downloading File: " + filename);

						dout.writeUTF(filename); // Sends file name to
						dout.flush();
						long sz = (int) file.length();
						byte b[] = new byte[1];
						int read;
						dout.writeUTF("stop");
						dout.writeUTF(Long.toString(sz));
						dout.flush();
						while ((read = fileIn.read(b)) > 0) {
							dout.write(b, 0, read);
							dout.flush();
						}
						fileIn.close();
						dout.flush();

					//}
					dout.flush();
					//din.close();
					System.out.println("File sent to client successfully");

				}

				catch(FileNotFoundException e){
					System.out.println("File does not exist on server");
					serverMessanger.println("DNEXIST");
				}
				catch(SocketException e){

				}
				catch (Exception e) {
					e.printStackTrace();
				}

			//}
		}

		private void viewFiles(){
			serverMessanger.println(server.fileRepoArr.size());
			serverMessanger.println(server.fileRepoArr.size());
			System.out.println(server.fileRepoArr.size());
			for(int i = 0; i < 3; i++){
				//FileDetails f = server.fileRepoArr.get(i);
				String viewString = "yeet" /* "File name: " + f.getFileName() + " Access type: " + f.getFileAccess() + " File Owner: " + f.getUserOwner()*/;
				serverMessanger.println(viewString);
			}
		}

		// OK, wtf happened lol?
		private void changePerms(){
			// Receives message from client
			try {
				// serverMessanger.println("Enter the name of the file whose permissions will be changed:"); // Add notes later
				String fileName = clientMessageScanner.nextLine();
				serverMessanger.println("1");
				FileOperation.FileOp fo = server.checkFileOperations(fileName);
				if (fo == FileOperation.FileOp.NONE) {
					serverMessanger.println("canMove");
					server.addFileOp(new FileOperation(server.getUser(userIndex), fileName, FileOperation.FileOp.MOVE));
					if (server.getUser(userIndex).getAccess() == User.Access.PRIVATE) {
						File privateFile = new File(server.resourceRepo + server.getUser(userIndex).getUserName().toLowerCase() + "/private/" + fileName);
						File publicFile = new File(server.resourceRepo + server.getUser(userIndex).getUserName().toLowerCase() + "/public/" + fileName);
						serverMessanger.println("privateUserMove");
						if (publicFile.exists()) {
							if (privateFile.exists()) {
								System.out.println("Duplicate files detected. Making file public by default");
								boolean niceDel = privateFile.delete();
								System.out.println(niceDel);
								if (niceDel) {
									System.out.println("File has been made public successfully");
								} else {
									System.out.println("An error occurred while removing duplicate file");
								}
							} else {
								System.out.println("Making file " + fileName + " private...");
							}
						} else if (privateFile.exists()) {
							serverMessanger.println("Making file " + fileName + " public...");
							Files.copy(publicFile.toPath(), privateFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
						} else {
							System.out.println("Permission change unsuccessful. This user has never uploaded this file to the server (hence, can't make any permission changes)");
						}
					}
					else if(server.getUser(userIndex).getAccess() == User.Access.PUBLIC){
						serverMessanger.println("publicUserMove");
					}
				} else {
					serverMessanger.println("cantMove");
				}
			}
			catch(IOException e){
				serverMessanger.println("An error occurred while changing file permissions");
			}

		}
	}

	private static class ServerManager implements Runnable{
		Server server;

		public ServerManager(Server server){
			this.server = server;
		}

		@Override
		public void run() {
			while(true){
				if(server.closed){
					for (Thread connectionThread : server.serverThreads) {
						try {
							connectionThread.join();
						} catch (InterruptedException e) {
							System.out.println("Error while closing server:\n" + e.getMessage());
						}
					}
					server.serverThreads.clear();
					break;
				}
				else{
					try{
						Thread.sleep(200);
					}
					catch (Exception e){

					}
					//System.out.println(server.serverThreads.size());
					ArrayList<Integer> inactiveThreads = new ArrayList<Integer>();
					for(int i = 0; i < server.serverThreads.size(); i++){
						if(!server.serverThreads.get(i).isAlive()){
							inactiveThreads.add(i);
						}
					}
					for (Integer inactiveThread : inactiveThreads) {
						server.serverThreads.remove(Integer.parseInt(inactiveThread.toString()));
					}
				}
			}
		}
	}
}
