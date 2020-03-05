package server;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

	private String fileRepoDirectory;
	private String userDB;
	public volatile boolean closed;
	public enum Status {AVAILABLE, OFFLINE, FULL}
	public enum Operation {VIEW, UPLOAD, DOWNLOAD, PERMISSION, UNKNOWN, QUIT}
	private volatile Status status;
	private int port;
	private volatile AtomicInteger numConnections;
	private final int maxConnections;
	private ServerSocket serverSocket;
	private Thread threadManager;
	private ArrayList<User> users;
	private ArrayList<Thread> serverThreads;
	private ArrayList<FileOperation> fileOperations;
	private ArrayList<FileDetails> fileRepoArr;

	public Server(String fileRepoDirectory, String userDB, int port, int maxConnections){
		this.maxConnections = maxConnections;
		this.fileRepoDirectory = fileRepoDirectory;
		this.userDB = userDB;
		status = Status.OFFLINE;
		this.port = port;
		numConnections = new AtomicInteger(0);
	}

	public void initialize(){ // Initializes server socket using specified port number
		try {
			serverSocket = new ServerSocket(this.port, 0, InetAddress.getLocalHost());
			System.out.println("Socket initialized");
			users = new ArrayList<>();
			fileOperations = new ArrayList<>();
			fileRepoArr = new ArrayList<>();


			FileReader userFile = new FileReader(userDB);

			BufferedReader fileReader = new BufferedReader(userFile);

			String userDetails;

			while((userDetails = fileReader.readLine()) != null){
				String[] userData = userDetails.split(ServerMain.DBDELIMITER);
				// String[] userData = userPassword.split(";,;delim; ");
				String userName = userData[0];
				String userPerms = userData[1];
				String userPassword = "";
				// Reads file here (Loop is here in the off chance that the user ACTUALLY has a password that contains our DB parser delimiter [This is very unlikely, but if they have the DB delimiter in their password a number of times in a row, then they deserve to have a broken password >:^) ]

				for(int i = 2; i < userData.length; i++){
					if(i == 2) {
						userPassword = userPassword.concat(userData[i]);
					}
					else{
						// In the very off chance that the user ACTUALLY has a password that contains our delimiter [This is very unlikely, but if they have the DB delimiter in their password a number of times in a row, then they deserve to have a broken password >:| ]
						userPassword = userPassword.concat(ServerMain.DBDELIMITER + userData[i]);
					}
				}
				users.add(new User(userName, userPassword, User.Access.valueOf(userPerms.toUpperCase())));
			}
			fileReader.close();
			userFile.close();

			System.out.println("Users populated");
			// Used for testing purposes
			initializeRepo();
			serverThreads = new ArrayList<>();
			closed = false;
			threadManager = new Thread(new ServerManager(this));
			threadManager.start();

			// Generate all paths in server [Will have res folder that keeps all files on server]
			//

			System.out.println("Server ready to handle connections...");
			status = Status.AVAILABLE;
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(0);
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
				pw.println("TRUE");
				pw.println("Connection denied. Server is offline"); // Impossible to get this message. But if you get it, well done, you broke the program :^)
			}
			else if(this.status == Status.FULL){
				System.out.println("Servers are full"); // Send message to clients [Saying server is full and that they
														// should try later
				pw.println("TRUE");
				pw.println("Connection denied. Server are full");
			}
			else {
				pw.println("FALSE");
				// Begin threading here (User then begins to interact accordingly
				Thread t = new Thread(new ConnectionThread(socket, this));
				// Need to ensure that socket is moved to a different port for communication with server

				serverThreads.add(0, t);
				serverThreads.get(0).start();
				numConnections.getAndIncrement();

				if(numConnections.get() >= maxConnections){
					System.out.println("Connection load limit reached");
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
			e.printStackTrace();
		}

	}

	public void close(){
		status = Status.OFFLINE;
		try {
			for(int i = 0; i < serverThreads.size(); i++){ // Server waits for all threads to complete their task, and then joins them. Doesn't abruptly interrupt connection between client and server if server is closed via input
				serverThreads.get(i).join();
			}
			Socket s = new Socket(InetAddress.getLocalHost().getHostAddress(), this.port);
			threadManager.join();
			serverSocket.close();
			s.close();
		}
		catch(Exception e){
			System.out.println("Problem encountered with closing connections");
		}
	}

	// Private methods responsible for data management [These methods will be in connection threads]
	// Protected accessor methods
	protected int getNumUsers(){
		return users.size();
	}

	protected User getUser(int userIndex){
		return users.get(userIndex);
	}

	private synchronized FileOperation.FileOp checkFileOperations(String fileName){
		for(int i = 0; i <fileOperations.size(); i++){
			if(fileOperations.get(i).getFile().equals(fileName)){
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

	private void initializeRepo(){
		try {
			System.out.println("Initializing repository...");
			File file = new File(fileRepoDirectory);
			if(!file.exists() || !file.isDirectory()){
				System.out.println("Repository directory does not exist. Would you like to create it? [Break your computer at your own risk lol] (Y/N)");
				String a = new Scanner(System.in).nextLine();
				if(a.toUpperCase().equals("Y")){
					boolean makeDirs = file.mkdirs();
					if(!makeDirs){
						System.out.println("An error occurred while creating directories");
						System.exit(1);
					}
				}
				else{
					System.out.println("Server initialization failed. The program will now exit");
					System.exit(1);
				}
			}
			for (User user : users) {
				File userFolder = new File(fileRepoDirectory + "/" + user.getUserName());
				if (!userFolder.exists() || !userFolder.isDirectory()) {
					boolean mkdir = userFolder.mkdirs();
					if (!mkdir) {
						System.out.println("An error occurred while creating directory belonging to " + user.getUserName());
					}
				}
				File userFolder2 = new File(fileRepoDirectory+"/"+user.getUserName()+"/private");
				if(!userFolder2.exists() || !userFolder2.isDirectory()){
					boolean makePrivFolder = userFolder2.mkdirs();
					if(!makePrivFolder) {
						System.out.println("ok, this shit is broken. Wtf?");
					}
				}
				makeUserFiles(userFolder, user, false, 0, false);
			}

			System.out.println("Repository initialized successfully");
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	protected boolean createUser(String userName, String password, User.Access access){

		// We have this method return something so that the InputThread has to wait for the result of this operation
		// Program begins generating directories for user files
		File userFolder =  new File(fileRepoDirectory + "\\"+userName);
		if(!userFolder.exists() || !userFolder.isDirectory()){
			boolean makedirs = userFolder.mkdirs();
			if(!makedirs){
				System.out.println("Eh");
			}
		}

		File userFolderPriv = new File(userFolder.getPath()+"\\private");
		if(!userFolderPriv.exists() || !userFolderPriv.isDirectory()){
			boolean makePrivDirs = userFolderPriv.mkdirs();
			if(!makePrivDirs){
				System.out.println("Eh, how does this even return false? lol");
				return false;
			}
		}

		// Come here tho.
		try {
			FileWriter userDBFile = new FileWriter(userDB, true);
			String userData = userName + ServerMain.DBDELIMITER + access.toString().toUpperCase() + ServerMain.DBDELIMITER + password;
			userDBFile.write(userData);
			// User is created at the end of all directory generation and file writing
			userDBFile.close();
			User user = new User(userName, password, access);
			boolean add = users.add(user);
			if(!add){
				System.out.println("Yeet, this shit is broken lol");
				return false;
			}
		}
		catch(IOException e){
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private void makeUserFiles(File f, User user, boolean inDir, int lvl, boolean Private){
		lvl++;
		try {
			if (f.list() != null) {
				for (int i = 0; i < f.list().length; i++) {
					String fileDirectory = f.getPath() + "\\" + f.list()[i];
					File subFile = new File(fileDirectory);
					if (subFile.isDirectory()) {
						if(subFile.getName().toUpperCase().equals("PRIVATE") && lvl==1) {
							makeUserFiles(subFile, user, true, lvl, true); // Only sends that file is private if file is in private directory
						}
						else{
							makeUserFiles(subFile, user, true, lvl, Private);
						}
					}
					//else if(inDir){
						// In the case where the file is located in a directory
					//}
					else{
						String actualFilePath = f.getPath() + "\\" +f.list()[i];
						File userFile = new File(actualFilePath);
						FileDetails.FileAccess access;
						if(Private){
							access = FileDetails.FileAccess.ADMIN;
						}
						else{
							access = FileDetails.FileAccess.PUBLIC;
						}
						FileDetails fileEntry = new FileDetails(userFile, user, access);
						fileRepoArr.add(fileEntry);
					}
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}


	private void logOutUser(int userIndex){
		users.get(userIndex).logOut();
		numConnections.getAndDecrement();
		if(this.status == Status.FULL){
			this.status = Status.AVAILABLE;
		}
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

	private void removeFileOp(FileOperation fileOperation){
		fileOperations.remove(fileOperation);
	}

	private void printToServerInterface(String s){
		if(!ServerMain.pauseServerPrints){
			System.out.println(s);
		}
	}

	// <------------------------------------------------------------------------------ Private classes here (used mainly for threading) ////////////////////////////////////////////////////////////////////////////

	private static class ConnectionThread implements Runnable{
		Socket conSock;
		int userIndex; // (We do, however, need it in case connection is lost [sign user out]) User is stored here mainly to ensure that they are signed out when operation is complete
		String filename;
		Operation operation;
		Server server;
		boolean autoOp;
		private PrintWriter serverMessenger;
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
					server.printToServerInterface("Auto operation flag read here");
					if (autoOp) {
						interaction1();
						Thread.sleep(10);
					}
					else {
						System.out.println("Initializing interaction2");
						interaction2(); // See interaction2() method
					}
					server.logOutUser(userIndex);
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

		private void interaction1(){

			// Responsible for interactions where user specifies operations they want to do through command line args

			// NB!!! THIS NEEDS TO BE DONE

			try {
				this.serverMessenger = new PrintWriter(conSock.getOutputStream(), true);
				serverMessenger.println("Initiating " + operation.toString() + " operation.");
				Operation a = Operation.values()[0];
				switch(operation.ordinal()){
					case 0:
						// View operation called

						break;
					case 1:
						// Upload operation called (Must check if file already exists on server and ask user if )

						serverMessenger.println("File with name " + filename + "already exists, would you like to overwrite it?(Y/N)");
						String userInput = "";
						userInput = userInput.toUpperCase();
						if(userInput.equals("Y")){
							serverMessenger.println("Initializing upload protocol...");
						}
						else if(userInput.equals("N")){
							serverMessenger.println("Upload operation cancelled");
						}
						else{
							serverMessenger.println("Invalid input was entered. Upload operation has been cancelled");
						}
						break;
					case 2:
						// Download operation called

						break;
					default:
						System.out.println("Invalid operation detected from " + conSock.getInetAddress().getHostAddress());
						serverMessenger.println("Invalid operation entered.");
						break;
				}
			}
			catch(Exception e){

			}

		}

		private boolean signUserIn(){
			try {
				boolean loggedIn = false;
				// Figure out a way to prevent the chance of 2 people signing in to the same account at the same time (which would mean there is a rare chance that 2 clients can connect to the same "user account")
				while (true) {
					this.serverMessenger = new PrintWriter(this.conSock.getOutputStream(), true); // Object that sends messages to client

					serverMessenger.println("Enter username (or send \"quit\" to end) [Note that user names are not case-sensitive]:");
					this.clientMessageScanner = new Scanner(this.conSock.getInputStream()); // Object that receives messages from client from other side of the socket
					String userName = clientMessageScanner.nextLine(); // Waits for client to send message

					System.out.println(userName);
					if (userName.equals("quit")) {
						serverMessenger.println("Thank you. Have a nice day! =D");
						break;
					}

					// Asking if the user is an admin complicates the whole system tbh [personally not a fan but
					// lmk if you think we should still keep it]


					serverMessenger.println("Enter password:");
					String password = clientMessageScanner.nextLine();
					boolean correct = server.checkLogin(conSock.getInetAddress(), userName, password);
					serverMessenger.println(correct);
					if (correct) { // Checks if user details are entered correctly
						serverMessenger.println("Login successful! Welcome back " + userName);
						userIndex = server.getUserIndex(userName);
						String permissions = server.getUser(userIndex).getAccess().toString();
						serverMessenger.println(permissions);
						loggedIn = true;
						break;
					} else {
						serverMessenger.println("Login unsuccessful");
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
		 /*The interactions that occur when the user doesn't send any automation arguments to the server*/
		private void interaction2(){ // Mainly responsible for interactions where user doesn't specify the operations they want to do through command line args
			try{
				conSock.getOutputStream(); // Line of code is here just so that we can catch SocketException
				boolean userQuit = false;
				while (!userQuit) {
					while (true) {
						Thread.sleep(100);
						if(server.status == Status.OFFLINE){
							serverMessenger.println("True");
							System.out.println("End of all days");
							server.logOutUser(userIndex);
							conSock.close();
							userQuit = true;
							break;
						}
						else{
							serverMessenger.println("false");
						}
						String option = clientMessageScanner.nextLine().toUpperCase();
						if (option.equals("UPLOAD")) {
							System.out.println("UPLOAD called");
							//serverMessanger.println("ready"); // Ensures that server is ready for operation to complete
							receiveFromClient(); // Fix communication
						}
						else if (option.equals("DOWNLOAD")) {
							System.out.println("DOWNLOAD called");
							//serverMessanger.println("ready");
							sendToClient(); // Fix communication
						}
						else if (option.equals("VIEW")) {
							System.out.println("VIEW called");
							viewFiles(); // Fix communication
						}
						else if (option.equals("PERM")) {
							serverMessenger.println("PERM called");
							changePerms(); // Fix communication
						}
						else if (option.equals("QUIT")) {
							System.out.println("User " + server.users.get(userIndex).getUserName() + " sent quit command");
							server.logOutUser(userIndex);
							System.out.println("User " + server.getUser(userIndex).getUserName() + " from connection " + conSock.getInetAddress().getHostAddress() + " has signed out successfully");
							conSock.close();
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

		/* Checks if the file that is being queried is being used. If the file isn't being used, the server downloads the file from the client */
		private void receiveFromClient(){

			// NB!!!! ADD DIFFERENT USER ACCESS PROTOCOLS!

			// Access isn't really necessary. Just add a file operation whatermacallit

			// Add upload controls here

			if(clientMessageScanner.nextLine().equals("DNEXIST")){
				System.out.println("File cannot be received from client. [They fucked up]");
			}
			else {
				try {
					DataInputStream din = new DataInputStream(conSock.getInputStream());
					DataOutputStream dout = new DataOutputStream(conSock.getOutputStream());

					String str = "";
					String filename = "";
					str = "bam";
					dout.writeUTF(str);
					dout.flush();
					filename = din.readUTF();
					FileOperation.FileOp fileOp = server.checkFileOperations(filename);
					if(fileOp == FileOperation.FileOp.NONE) { // No operation is being performed on the file
						System.out.println("No operations being ran on file. File can be uploaded");
						String canreceive = "CANRECEIVE";
						dout.writeUTF(canreceive);
						dout.flush();
						String[] fileArr = filename.split("/");
						String saveFileDir = server.fileRepoDirectory + server.getUser(userIndex).getUserName() + "/" + fileArr[fileArr.length - 1];
						str = din.readUTF();
						long fileSize = Long.parseLong(din.readUTF());
						File f =  new File(saveFileDir);
						byte b[] = new byte[1];
						FileOutputStream fos = new FileOutputStream(f, false);
						long downSize = 0;
						int downmarker = 0;
						do {
							din.read(b, 0, b.length);
							fos.write(b, 0, b.length);
							downSize++;
							double downProg = (double)downSize / (double)fileSize;
							if (downProg > 0.1 && downmarker < 1) {
								System.out.println("10% complete");
								downmarker = 1;
							}
							if (downProg > 0.2 && downmarker < 2) {
								System.out.println("20% complete");
								downmarker = 2;
							}
							if (downProg > 0.3 && downmarker < 3) {
								System.out.println("30% complete");
								downmarker = 3;
							}
							if (downProg > 0.4 && downmarker < 4) {
								System.out.println("40% complete");
								downmarker = 4;
							}
							if (downProg > 0.5 && downmarker < 5) {
								System.out.println("50% complete");
								downmarker = 5;
							}
							if (downProg > 0.6 && downmarker < 6) {
								System.out.println("60% complete");
								downmarker = 6;
							}
							if (downProg > 0.7 && downmarker < 7) {
								System.out.println("70% complete");
								downmarker = 7;
							}
							if (downProg > 0.8 && downmarker < 8) {
								System.out.println("80% complete");
								downmarker = 8;
							}
							if (downProg > 0.9 && downmarker < 9) {
								System.out.println("90% complete");
								downmarker = 9;
							}
						}
						while (downSize < fileSize);
						fos.flush();
						fos.close();

						dout.flush();
						System.out.println("File written to server successfully");
						FileDetails fileDetails = new FileDetails(f, server.getUser(userIndex), FileDetails.FileAccess.PUBLIC);
						server.fileRepoArr.add(fileDetails);
						serverMessenger.println("Upload complete");
					}
				} catch (SocketException e) {

				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}

		/* Checks if queried file exists on the server, and if it does, it checks if the client can access it and if they can, they can download it*/
		private void sendToClient() {
			String filename = clientMessageScanner.nextLine();

			//String dirLoc = server.resourceRepo +server.getUser(userIndex).getUserName()+"/public/";

			// Check file status (See if anything is happening with our file)

			FileOperation.FileOp fileOp = server.checkFileOperations(filename);

			boolean fileAvailable;


			// Do later

			// End filecheck
			//while (true) {//infinite while loop to wait for the client to be ready to recieve


				try {

					// Does exist check must

					DataInputStream din = new DataInputStream(conSock.getInputStream());
					DataOutputStream dout = new DataOutputStream(conSock.getOutputStream());

					String finalFilePath = server.fileRepoDirectory + filename;
					File file = new File(finalFilePath);
					FileInputStream fileIn = new FileInputStream(file);
					serverMessenger.println("EXIST");
					String Data;
					Data = din.readUTF(); //input recived from the DownloadtoClient method in Client class (recives "bam ")
					dout.writeUTF(filename); // Sends file name to
					dout.flush();
					long sz = (int) file.length();
					byte b[] = new byte[1];
					int read;
					dout.writeUTF("stop");
					dout.writeUTF(Long.toString(sz));
					dout.flush();
					System.out.println("Downloading File: " + filename);
					while ((read = fileIn.read(b)) > 0) {
						dout.write(b, 0, read);
						dout.flush();
					}
					fileIn.close();
					dout.flush();

					dout.flush();
					System.out.println("File sent to client successfully");

				}

				catch(FileNotFoundException e){
					System.out.println("File " + filename + " does not exist on server");
					serverMessenger.println("DNEXIST");
				}
				catch(SocketException e){

				}
				catch (Exception e) {
					e.printStackTrace();
				}

			//}
		}

		private void viewFiles(){

			// NB!!! WORK ON THIS
			ArrayList<String> fileNames = new ArrayList<>();
			for(int i = 0; i < server.fileRepoArr.size(); i++){
				FileDetails f = server.fileRepoArr.get(i);
				if(f.getFileAccess() == FileDetails.FileAccess.PUBLIC){
					String fileName = f.getFile().getPath();
					String realFileName = fileName.replace(server.fileRepoDirectory, "");
					System.out.println(realFileName);
					System.out.println(server.fileRepoDirectory);
					fileNames.add(realFileName);
				}
				else if(f.getFileAccess() == FileDetails.FileAccess.ADMIN && server.getUser(userIndex).getAccess() == User.Access.ADMIN){
					String fileName = f.getFile().getPath();
					String realFileName = fileName.replace(server.fileRepoDirectory, "");
					fileNames.add(realFileName);
				}
			}
			serverMessenger.println(fileNames.size());
			for(int i = 0; i < fileNames.size(); i++){
				serverMessenger.println(fileNames.get(i));
			}
		}

		private void changePerms(){
			// Receives message from client

			// NB!!!! COMPLETE ASAP!



			/*
			try {
				// serverMessanger.println("Enter the name of the file whose permissions will be changed:"); // Add notes later
				String fileName = clientMessageScanner.nextLine();
				serverMessanger.println("1");
				FileOperation.FileOp fo = server.checkFileOperations(fileName);
				if (fo == FileOperation.FileOp.NONE) {
					serverMessanger.println("canMove");
					server.addFileOp(new FileOperation(server.getUser(userIndex), fileName, FileOperation.FileOp.MOVE));
					if (server.getUser(userIndex).getAccess() == User.Access.ADMIN) {
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
			}*/


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
				if(server.status == Status.OFFLINE){
					for (Thread connectionThread : server.serverThreads) {
						try {
							connectionThread.join();
						} catch (InterruptedException e) {
							System.out.println("Error while closing server:\n" + e.getMessage());
						}
					}
					server.serverThreads.clear();
					System.out.println("All threads joined");
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

	// <------------------------------------------------------------------------------ End of private classes -------------------------------------------------------------------------------------------> //
}
