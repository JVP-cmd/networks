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

	/**
	 * Server class that keeps track of
	 * @param fileRepoDirectory The directory that all files can be found
	 * @param userDB The name of the user database file that will store all user data
	 * @param port The port number of the Connection Socket
	 * @param maxConnections The maximum number of users that the server can take
	 */
	public Server(String fileRepoDirectory, String userDB, int port, int maxConnections){
		this.maxConnections = maxConnections;
		this.fileRepoDirectory = fileRepoDirectory;
		this.userDB = userDB;
		status = Status.OFFLINE;
		this.port = port;
		numConnections = new AtomicInteger(0);
	}

	/**
	 * Initializes the server by creating the server socket at specified port number, and goes through the necessary steps in order for the server to be functional
	 */
	public void initialize(){ // Initializes server socket using specified port number
		try {
			serverSocket = new ServerSocket(this.port, 0, InetAddress.getLocalHost());
			printToServerInterface("Socket initialized", ServerMain.pauseServerPrints);
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

			printToServerInterface("Users populated", ServerMain.pauseServerPrints);
			// Used for testing purposes
			initializeRepo();
			serverThreads = new ArrayList<>();
			closed = false;
			threadManager = new Thread(new ServerManager(this));
			threadManager.start();

			// Generate all paths in server [Will have res folder that keeps all files on server]
			//

			printToServerInterface("Server ready to handle connections...", ServerMain.pauseServerPrints);
			status = Status.AVAILABLE;
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	/**
	 * Listens for a connection attempts to the specified server socket port number. When a connection is accepted, a new thread is created which is used to allow clients to communicate
	 * with the server
	 */
	public void listen(){
		printToServerInterface("Listening", ServerMain.pauseServerPrints);
		try{
			Socket socket = serverSocket.accept();
			// Start threading from here
			printToServerInterface("Connection from " + socket.getLocalAddress().getHostAddress() + " detected", ServerMain.pauseServerPrints);
			PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
			if(this.status == Status.OFFLINE){
				printToServerInterface("Connection denied", ServerMain.pauseServerPrints); // send message to client [Saying servers aren't available]
				pw.println("TRUE");
				pw.println("Connection denied. Server is offline"); // Impossible to get this message. But if you get it, well done, you broke the program :^)
			}
			else if(this.status == Status.FULL){
				printToServerInterface("Servers are full", ServerMain.pauseServerPrints); // Send message to clients [Saying server is full and that they
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
					printToServerInterface("Connection load limit reached", ServerMain.pauseServerPrints);
					this.status = Status.FULL;
				}
			}
		}
		catch(SocketException e){
			printToServerInterface("Socket interrupted", ServerMain.pauseServerPrints);
		}
		catch(IOException e){
			printToServerInterface("Error while listening for connections", ServerMain.pauseServerPrints);
		}
		catch(Exception e){
			e.printStackTrace();
		}

	}

	/**
	 * Waits for all other user operations to complete and then closes the server. Any new connections that try to connect to the server won't be able to connect
	 */
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
			printToServerInterface("Problem encountered with closing connections", ServerMain.pauseServerPrints);
		}
	}

	/**
	 * Number of users
	 * @return the number of user accounts that are stored on the server
	 */
	protected int getNumUsers(){
		return users.size();
	}

	/**
	 * Returns a user at a specifically given index. Used mainly in the ConnectionThread class
	 * @param userIndex the index number of the user
	 * @return the user at the given index number
	 */
	protected User getUser(int userIndex){
		return users.get(userIndex);
	}

	/**
	 * Checks if a specific file operation is valid.
	 * @param fileName the name of the file whose operation we want to see
	 * @return the Operation type of a specific File Operation
	 */
	private synchronized FileOperation.FileOp checkFileOperations(String fileName){
		for(int i = 0; i <fileOperations.size(); i++){
			if(fileOperations.get(i).getFile() != null) { // Checks if the operation has a file linked to it (At some point in time, we create a null File Operation object
				if (fileOperations.get(i).getFile().equals(getFileDetails(fileName))) {
				}
			}
		}
		return FileOperation.FileOp.NONE;
	}

	/**
	 *
	 * @param address
	 * @param userName
	 * @param password
	 * @return
	 */
	private boolean checkLogin(InetAddress address, String userName, String password){
		printToServerInterface("Login attempt detected from " + address.getHostAddress() + "...", ServerMain.pauseServerPrints);
		for(int i = 0; i < users.size(); i++){
			if(users.get(i).getUserName().equals(userName.toLowerCase())){ // Checks if user exists in server
				return users.get(i).logIn(userName, password, address);
			}
		}
		printToServerInterface("Login attempt detected from " + address.getHostAddress() +" unsuccessful (User does not exist)\n\n", ServerMain.pauseServerPrints); // Prints this server side (mainly for debugging)
		return false;
	}

	/**
	 *
	 */

	private void initializeRepo(){
		try {
			printToServerInterface("Initializing repository...", ServerMain.pauseServerPrints);
			File file = new File(fileRepoDirectory);
			if(!file.exists() || !file.isDirectory()){
				printToServerInterface("Repository directory does not exist. Would you like to create it? [Break your computer at your own risk lol] (Y/N)", ServerMain.pauseServerPrints);
				String a = new Scanner(System.in).nextLine();
				if(a.toUpperCase().equals("Y")){
					boolean makeDirs = file.mkdirs();
					if(!makeDirs){
						printToServerInterface("An error occurred while creating directories", ServerMain.pauseServerPrints);
						System.exit(1);
					}
				}
				else{
					printToServerInterface("Server initialization failed. The program will now exit", ServerMain.pauseServerPrints);
					System.exit(1);
				}
			}
			for (User user : users) {
				File userFolder = new File(fileRepoDirectory + "/" + user.getUserName());
				if (!userFolder.exists() || !userFolder.isDirectory()) {
					boolean mkdir = userFolder.mkdirs();
					if (!mkdir) {
						printToServerInterface("An error occurred while creating directory belonging to " + user.getUserName(), ServerMain.pauseServerPrints);
					}
				}
				File userFolder2 = new File(fileRepoDirectory+"/"+user.getUserName()+"/private");
				if(!userFolder2.exists() || !userFolder2.isDirectory()){
					boolean makePrivFolder = userFolder2.mkdirs();
					if(!makePrivFolder) {
						printToServerInterface("ok, this shit is broken. Wtf?", ServerMain.pauseServerPrints);
					}
				}
				makeUserFiles(userFolder, user, false, 0, false);
			}

			printToServerInterface("Repository initialized successfully", ServerMain.pauseServerPrints);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 *
	 * @param userName
	 * @param password
	 * @param access
	 * @return
	 */
	protected boolean createUser(String userName, String password, User.Access access){

		// We have this method return something so that the InputThread has to wait for the result of this operation
		// Program begins generating directories for user files
		File userFolder =  new File(fileRepoDirectory + "\\"+userName);
		if(!userFolder.exists() || !userFolder.isDirectory()){
			boolean makedirs = userFolder.mkdirs();
			if(!makedirs){
				printToServerInterface("Eh", ServerMain.pauseServerPrints);
			}
		}

		File userFolderPriv = new File(userFolder.getPath()+"\\private");
		if(!userFolderPriv.exists() || !userFolderPriv.isDirectory()){
			boolean makePrivDirs = userFolderPriv.mkdirs();
			if(!makePrivDirs){
				printToServerInterface("Eh, how does this even return false? lol", ServerMain.pauseServerPrints);
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
				printToServerInterface("Yeet, this shit is broken lol", ServerMain.pauseServerPrints);
				return false;
			}
		}
		catch(IOException e){
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 *
	 * @param f
	 * @param user
	 * @param inDir
	 * @param lvl
	 * @param Private
	 */

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
						User.Access access;
						if(Private){
							access = User.Access.ADMIN;
						}
						else{
							access = User.Access.PUBLIC;
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

	/**
	 *
	 * @param filename
	 * @return
	 */
	private FileDetails getFileDetails(String filename){
		for(int i = 0; i < fileRepoArr.size(); i++){
			if(fileRepoArr.get(i).isFile(filename, fileRepoDirectory)){
				return fileRepoArr.get(i);
			}
		}
		return new FileDetails(null, null, null);
	}

	/**
	 *
	 * @param userIndex
	 */
	private void logOutUser(int userIndex){
		users.get(userIndex).logOut();
		numConnections.getAndDecrement();
		if(this.status == Status.FULL){
			this.status = Status.AVAILABLE;
		}
	}

	/**
	 *
	 * @param userName
	 * @return
	 */
	private int getUserIndex(String userName){ // This method is only ran when a client has successfully logged in
		for(int i = 0; i < users.size(); i++){
			if(users.get(i).getUserName().equals(userName.toLowerCase())){
				return i;
			}
		}
		return -1;
	}

	/**
	 *
	 * @return
	 */
	public int numThreads(){
		return serverThreads.size();
	}

	/**
	 *
	 * @return
	 */
	public int numloggedIn(){
		int a = 0;
		for(int i = 0; i < users.size(); i++) {
			if(users.get(i).loggedIn()){
				a++;
			}
		}
		return a;
	}

	/**
	 *
	 * @param filename
	 * @param user
	 * @param fileOp
	 * @return
	 */
	private synchronized FileOperation addFileOp(String filename, User user, FileOperation.FileOp fileOp){
		FileDetails a = getFileDetails(filename);
		FileOperation operation = new FileOperation(user, a, fileOp);
		boolean canOperate = true;
		for(int i = 0; i < fileOperations.size(); i++){
			if(operation.getFile().equals(fileOperations.get(i).getFile())){
				canOperate = false;
				break;
			}
		}
		if(canOperate) {
			fileOperations.add(operation);
			return operation;
		}
		else{
			return null;
		}
	}

	/**
	 *
	 * @param fileOperation
	 */
	private void removeFileOp(FileOperation fileOperation){
		fileOperations.remove(fileOperation);
	}

	/**
	 * Prints items to screen. This method solely exists to act as an "override" of the System.out.println() method. This has the added feature that
	 * @param o The object that is going to be printed to the console
	 */
	private static void printToServerInterface(Object o, boolean b){
		if(!b){
			System.out.println(o.toString());
		}
	}

	// <------------------------------------------------------------------------------ Private classes here (used mainly for threading) ////////////////////////////////////////////////////////////////////////////

	/**
	 * Private ConnectionThread Class that keeps track of individual user connections.
	 */
	private static class ConnectionThread implements Runnable{
		Socket conSock;
		int userIndex; // (We do, however, need it in case connection is lost [sign user out]) User is stored here mainly to ensure that they are signed out when operation is complete
		String filename;
		Operation operation;
		Server server;
		boolean autoOp;
		private PrintWriter serverMessenger;
		private Scanner clientMessageScanner;

		/**
		 *
		 * @param conSock Connection Socket between client and server
		 * @param server Server that the thread is communicating with
		 */
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
					server.printToServerInterface("Auto operation flag read here", ServerMain.pauseServerPrints);
					if (autoOp) {
						interaction1();
						Thread.sleep(10);
					}
					else {
						printToServerInterface("Initializing interaction2", ServerMain.pauseServerPrints);
						interaction2(); // See interaction2() method
					}
					server.logOutUser(userIndex);
				}
			}
			catch(SocketException e){
				printToServerInterface("Socket lost", ServerMain.pauseServerPrints);
				server.logOutUser(userIndex);
			}
			catch(IOException e){
				printToServerInterface("Yeetus deletus", ServerMain.pauseServerPrints);
				server.logOutUser(userIndex);
			}
			catch(Exception e){
				printToServerInterface("Exception breaking program", ServerMain.pauseServerPrints);
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
						printToServerInterface("Invalid operation detected from " + conSock.getInetAddress().getHostAddress(), ServerMain.pauseServerPrints);
						serverMessenger.println("Invalid operation entered.");
						break;
				}
			}
			catch(Exception e){

			}

		}

		/**
		 * Responsible for interacting with the client to procure user data
		 * @return The details pertaining to the success of the user's login attempt ("true" if successful, "false" if unsuccessful)
		 */
		private boolean signUserIn(){
			try {
				boolean loggedIn = false;
				this.serverMessenger = new PrintWriter(this.conSock.getOutputStream(), true); // Object that sends messages to client
				// Figure out a way to prevent the chance of 2 people signing in to the same account at the same time (which would mean there is a rare chance that 2 clients can connect to the same "user account")

				while (true) {
					serverMessenger.println("Enter username (or send \"quit\" to end) [Note that user names are not case-sensitive]:");
					this.clientMessageScanner = new Scanner(this.conSock.getInputStream()); // Object that receives messages from client from other side of the socket
					String userName = clientMessageScanner.nextLine(); // Waits for client to send message

					printToServerInterface(userName, ServerMain.pauseServerPrints);
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
						this.userIndex = server.getUserIndex(userName);
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
				printToServerInterface("Socket timed out", ServerMain.pauseServerPrints);
			}
			catch(IOException e){
				printToServerInterface("random IOException that occurs", ServerMain.pauseServerPrints);
			}
			return false;
		}
		 /**
		  * The interactions that occur when the user doesn't send any automation arguments to the server
		  *
		  * */
		private void interaction2(){ // Mainly responsible for interactions where user doesn't specify the operations they want to do through command line args
			try{
				conSock.getOutputStream(); // Line of code is here just so that we can catch SocketException
				boolean userQuit = false;
				while (!userQuit) {
					while (true) {
						Thread.sleep(100);
						if(server.status == Status.OFFLINE){
							serverMessenger.println("True");
							printToServerInterface("End of all days", ServerMain.pauseServerPrints);
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
							printToServerInterface("UPLOAD called", ServerMain.pauseServerPrints);
							//serverMessanger.println("ready"); // Ensures that server is ready for operation to complete
							receiveFromClient(); // Fix communication
						}
						else if (option.equals("DOWNLOAD")) {
							printToServerInterface("DOWNLOAD called", ServerMain.pauseServerPrints);
							//serverMessanger.println("ready");
							sendToClient(); // Fix communication
						}
						else if (option.equals("VIEW")) {
							printToServerInterface("VIEW called", ServerMain.pauseServerPrints);
							viewFiles(); // Fix communication
						}
						else if (option.equals("PERM")) {
							printToServerInterface("PERM called", ServerMain.pauseServerPrints);
							printToServerInterface("Yes", ServerMain.pauseServerPrints);
							changePerms(); // Fix communication
						}
						else if (option.equals("QUIT")) {
							printToServerInterface("User " + server.users.get(userIndex).getUserName() + " sent quit command", ServerMain.pauseServerPrints);
							server.logOutUser(userIndex);
							printToServerInterface("User " + server.getUser(userIndex).getUserName() + " from connection " + conSock.getInetAddress().getHostAddress() + " has signed out successfully", ServerMain.pauseServerPrints);
							conSock.close();
							userQuit = true;
							break; // only breaking out of 1 loop
						}
					}
				}
			}
			catch(SocketException e){
				printToServerInterface("Error. Connection with user timed out", ServerMain.pauseServerPrints);
				server.logOutUser(userIndex);
			}
			catch (Exception e){
				e.printStackTrace();
			}

		}

		/**
		 *  Checks if the file that is being queried is being used. If the file isn't being used, the server downloads the file from the client.
		 *  */
		private void receiveFromClient(){
			FileOperation op = null;
			// NB!!!! ADD DIFFERENT USER ACCESS PROTOCOLS!

			// Access isn't really necessary. Just add a file operation whatermacallit

			// Add upload controls here


			String fileExistString = clientMessageScanner.nextLine();
			if(fileExistString.equals("DNEXIST")){
				printToServerInterface("File cannot be received from client. [They fucked up]", ServerMain.pauseServerPrints);
			}
			else {
				// Check is here

				try {
					DataInputStream din = new DataInputStream(conSock.getInputStream());
					DataOutputStream dout = new DataOutputStream(conSock.getOutputStream());

					String str = "";
					String filename = "";
					str = "bam";
					dout.writeUTF(str);
					dout.flush();
					filename = din.readUTF(); // Reads in file name
					FileOperation.FileOp fileOp = server.checkFileOperations(filename);
					printToServerInterface(fileOp, ServerMain.pauseServerPrints);

					if(fileOp == FileOperation.FileOp.NONE) { // No operation is being performed on the file
						op = server.addFileOp(filename, server.users.get(userIndex), FileOperation.FileOp.UPLOAD);
						if (op == null) {
							dout.writeUTF("CANTRECIEVE");
						} else {
							String canreceive = "CANRECEIVE";
							dout.writeUTF(canreceive);
							dout.flush();
							String filenameFinal = filename.replace("\\", "/"); // Changes directory dividers to a different character (makes it easier to parse the text properly and work with data from there)
							String[] fileArr = filenameFinal.split("/");
							String saveFileDir = server.fileRepoDirectory + server.getUser(userIndex).getUserName() + "\\" + fileArr[fileArr.length - 1];
							String saveFileCopy = saveFileDir + ".copy"; // Maybe create a copy of the file while it's uploading, and then replace file once complete (This requires a queue [Priority queue would be good])
							printToServerInterface(server.users.get(userIndex).getUserName() + " attempt to upload file " + fileArr[fileArr.length - 1] + " to servers has been accepted. Writing...", ServerMain.pauseServerPrints);
							str = din.readUTF(); // Waits for "bam" header line from client before beginning file transfer operation
							long fileSize = Long.parseLong(din.readUTF());
							File f = new File(saveFileDir);
							byte b[] = new byte[1];
							FileOutputStream fos = new FileOutputStream(f, false);
							long downSize = 0;
							do {
								din.read(b, 0, b.length);
								fos.write(b, 0, b.length);
								downSize++;
							}
							while (downSize < fileSize);
							fos.flush();
							fos.close();

							dout.flush();
							printToServerInterface(server.users.get(userIndex).getUserName() + " has written file name " + fileArr[fileArr.length - 1] + " to server successfully", ServerMain.pauseServerPrints);
							boolean newToServer = true;
							for (int i = 0; i < server.fileRepoArr.size(); i++) {
								File oldFile = server.fileRepoArr.get(i).getFile();
								if (f.equals(oldFile)) {
									newToServer = false;
								}
							}
							if (newToServer) {
								FileDetails fileDetails = new FileDetails(f, server.getUser(userIndex), User.Access.PUBLIC);
								server.fileRepoArr.add(fileDetails);
							}
							serverMessenger.println("Upload complete");
							server.removeFileOp(op);
							printToServerInterface(server.fileOperations.size(), ServerMain.pauseServerPrints);
						}
					}
					else{
						dout.writeUTF("CANTRECEIVE");
					}
				} catch (SocketException e) {
					if(op != null){
						server.removeFileOp(op);
					}
				} catch (Exception e) {
					e.printStackTrace();
					if(op != null){
						server.removeFileOp(op);
					}
				}

			}
		}

		/**
		 *  Checks if queried file exists on the server, and if it does, it checks if the client can access it and if they can, they can download it.
		 *  */
		private void sendToClient() {
			String filename = clientMessageScanner.nextLine();

			//String dirLoc = server.resourceRepo +server.getUser(userIndex).getUserName()+"/public/";

			// Check file status (See if anything is happening with our file)

			FileOperation.FileOp fileOp = server.checkFileOperations(filename); // Checks if a user is doing anything with the file. If the file is e.g. being uploaded, then the user can't download the file
			if(fileOp == FileOperation.FileOp.MOVE || fileOp == FileOperation.FileOp.UPLOAD){
				serverMessenger.println("CANTDOWN");
			}
			else {
				FileOperation op = server.addFileOp(filename, server.getUser(userIndex), FileOperation.FileOp.DOWNLOAD); // Adds new operation to thingymabob
				printToServerInterface("Sending download request to servers...", ServerMain.pauseServerPrints);
				boolean fileAvailable = false;
				for(int i = 0; i < server.fileRepoArr.size(); i++){
					// checks permissions on each file in repo
					boolean a = server.fileRepoArr.get(i).canAccess(server.users.get(userIndex).getAccess());
					if(a){
						fileAvailable = true;
					}
				}
				printToServerInterface(server.fileRepoArr.get(0).getFileAccess(), ServerMain.pauseServerPrints);
				printToServerInterface(server.users.get(userIndex).getAccess(), ServerMain.pauseServerPrints);
				printToServerInterface(server.fileRepoArr.get(0).canAccess(server.users.get(userIndex).getAccess()), ServerMain.pauseServerPrints);
				printToServerInterface(server.fileRepoArr.size(), ServerMain.pauseServerPrints);
				printToServerInterface(userIndex, ServerMain.pauseServerPrints);
				printToServerInterface(op, ServerMain.pauseServerPrints);
				if(op != null && fileAvailable) { // Checks if FileOperation has been created (if it didn't exist) and if the file is available for download
					serverMessenger.println("CANDOWN"); // Can Download message is sent

					try {

						DataInputStream din = new DataInputStream(conSock.getInputStream());
						DataOutputStream dout = new DataOutputStream(conSock.getOutputStream());

						String finalFilePath = server.fileRepoDirectory + filename;
						File file = new File(finalFilePath);
						FileInputStream fileIn = new FileInputStream(file);
						serverMessenger.println("EXIST");
						String Data;
						Data = din.readUTF(); //input recived from the DownloadtoClient method in Client class (receives "bam" header line)
						if (Data.equals("bam")) {
							dout.writeUTF(filename); // Sends file name to
							dout.flush();
							long sz = (int) file.length();
							byte b[] = new byte[1];
							int read;
							dout.writeUTF("stop");
							dout.writeUTF(Long.toString(sz));
							dout.flush();
							printToServerInterface("Downloading File: " + filename, ServerMain.pauseServerPrints);
							while ((read = fileIn.read(b)) > 0) {
								dout.write(b, 0, read);
								dout.flush();
							}
							fileIn.close();
							dout.flush();
							dout.flush();
							printToServerInterface("File sent to client successfully", ServerMain.pauseServerPrints);
							printToServerInterface(server.fileOperations.size(), ServerMain.pauseServerPrints);
							server.removeFileOp(op);
							printToServerInterface(server.fileOperations.size(), ServerMain.pauseServerPrints);
						}
					} catch (FileNotFoundException e) {
						printToServerInterface("Error 404: File " + filename + " does not exist", ServerMain.pauseServerPrints);
						server.removeFileOp(op);
						serverMessenger.println("DNEXIST");
					} catch (SocketException e) {
						printToServerInterface("An error occurred while sending file to client", ServerMain.pauseServerPrints);
						server.removeFileOp(op);
					} catch (Exception e) {
						e.printStackTrace();
						server.removeFileOp(op);
					}
				}
				else{
					serverMessenger.println("CANTDOWN");
				}
			}
			//}
		}

		/**
		 * Shows the client all the files that they can view. If there are no available files for them to see, the program informs them of that.
		 */
		private void viewFiles(){

			ArrayList<String> fileNames = new ArrayList<>();
			for(int i = 0; i < server.fileRepoArr.size(); i++){
				FileDetails f = server.fileRepoArr.get(i);
				if(f.getFileAccess() == User.Access.PUBLIC){
					String fileName = f.getFile().getPath();
					String realFileName = fileName.replace(server.fileRepoDirectory, "");
					fileNames.add(realFileName);
				}
				else if(f.getFileAccess() == User.Access.ADMIN && server.getUser(userIndex).getAccess() == User.Access.ADMIN){
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

		/**
		 * Checks if queried file exists on server, and if it does, changes the visibility of the file based on the previous visibility settings of the file
		 * (So e.g. if a file was private, it would make it public and vice versa)
		 * 
		 */
		private void changePerms(){
			
			// Receives message from client
			String filename = clientMessageScanner.nextLine();
			FileOperation.FileOp fileOp = server.checkFileOperations(filename);
			
			if(fileOp == FileOperation.FileOp.DOWNLOAD || fileOp == FileOperation.FileOp.UPLOAD){
				printToServerInterface("Cannot move file", ServerMain.pauseServerPrints);
				serverMessenger.println("CANTMOVE");
			}
			else {
				printToServerInterface("Can move file", ServerMain.pauseServerPrints);
				serverMessenger.println("CANMOVE");
				FileOperation op = server.addFileOp(filename, server.users.get(userIndex), FileOperation.FileOp.MOVE);
				while(true){
					if(1 == 2){
						break;
					}
				}
				boolean success;
				boolean found = false;
				int filePos = -1;
				// FileDetails changeFile = new FileDetails(null, null, null); // Create null details (This is to prevent
				for (int i = 0; i < server.fileRepoArr.size(); i++) {
					if (server.getUser(userIndex).equals(server.fileRepoArr.get(i).getUserOwner())) {
						String fileDetailsFileName = server.fileRepoArr.get(i).getFileName(); // File name from FileDetails that we were looking for
						fileDetailsFileName = fileDetailsFileName.replace(server.fileRepoDirectory, "");
						if (fileDetailsFileName.equals(filename)) {
							// changeFile = server.fileRepoArr.get(i);
							filePos = i;
							found = true;
						}
					}
				}
				if (found) {
					// success = changeFile.changeAccess(server.users.get(userIndex));
					success = server.fileRepoArr.get(filePos).changeAccess(server.users.get(userIndex));

					if (success) {
						serverMessenger.println("SUCCESS");
						server.removeFileOp(op);
					} else {
						serverMessenger.println("UNSUCCESS");
						server.removeFileOp(op);
					}
				} else {
					serverMessenger.println("UNSUCCESS");
					server.removeFileOp(op);
				}
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
				if(server.status == Status.OFFLINE){ // Server is offline
					for (Thread connectionThread : server.serverThreads) {
						try {
							connectionThread.join();
						} catch (InterruptedException e) {
							printToServerInterface("Error while closing server:\n" + e.getMessage(), ServerMain.pauseServerPrints);
						}
					}
					server.serverThreads.clear();
					printToServerInterface("All threads joined", ServerMain.pauseServerPrints);
					break;
				}
				else{
					try{
						Thread.sleep(200);
					}
					catch (Exception e){

					}
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
