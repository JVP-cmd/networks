package server;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

	private String ipAddress;
	private String resourceDir;
	public volatile boolean closed;
	public enum Status {Available, Offline, Full}
	public enum Operation {VIEW, UPLOAD, DOWNLOAD, PERMISSION, UNKNOWN, QUIT}
	private volatile Status status;
	private int socketNo;
	private volatile AtomicInteger numConnections;
	private final int maxConnections;
	private ServerSocket serverSocket;
	private Thread threadManager;
	private ArrayList<User> users;
	private ArrayList<Thread> serverThreads;

	public Server(String ipAddress, String resourceDir, int socketNo, int maxConnections){
		this.maxConnections = maxConnections;
		this.ipAddress = ipAddress;
		this.resourceDir = resourceDir;
		status = Status.Offline;
		this.socketNo = socketNo;
		numConnections = new AtomicInteger(0);
	}

	public void initialize(){ // Initializes server socket using specified port number
		try {
			serverSocket = new ServerSocket(this.socketNo);
			users = new ArrayList<>();

			// Used for testing purposes
			users.add(new User("john", "1234qwer"));
			users.add(new User("pete", "1234qwer"));
			users.add(new User("bob", "1234qwer"));

			serverThreads = new ArrayList<>();
			System.out.println("Socket initialized");
			System.out.println("Users stored on current server: " + users.size());
			closed = false;
			threadManager = new Thread(new ServerManager(this));
			threadManager.start();

			// Generate all paths in server [Will have res folder that keeps all files on server]
			//

			System.out.println("Server ready to handle connections...");
			status = Status.Available;
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
			if(this.status == Status.Offline){
				System.out.println("Connection denied"); // send message to client [Saying servers aren't available]
				pw.println("Connection denied. Server is offline");
			}
			else if(this.status == Status.Full){
				System.out.println("Servers are full"); // Send message to clients [Saying server is full and that they
														// should try later
				pw.println("Connection denied. Server are full");
			}
			else {

				// Begin threading here (User then begins to interact accordingly
				Thread t = new Thread(new ConnectionThread(socket, this));
				serverThreads.add(0, t);
				serverThreads.get(0).start();

				if(numConnections.get() >= maxConnections){
					this.status = Status.Full;
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
		status = Status.Offline;
		try {
			serverSocket.close();
		}
		catch(Exception e){
			System.out.println("Problem encountered with closing connections");
		}
	}

	// Private methods responsible for sending and receiving data [These methods will be in connection threads]


	private void printAllAccessible(String username, Socket socket){

	}

	private synchronized void addClient(User client){
		users.add(client);
	}

	private void uploadFile(String username, Socket socket){

	}

	private void downloadFile(String username, Socket socket){

	}

	private boolean newConnection(Socket socket){
		return true;
	}

	protected boolean handleLogin(User client){

		return true;
	}

	protected boolean checkLogin(InetAddress address, String userName, String password){
		System.out.println("Login attempt detected from " + address.getHostAddress() + "...");
		for(int i = 0; i < users.size(); i++){
			if(users.get(i).getUserName().equals(userName.toLowerCase())){ // Checks if user exists in server
				return users.get(i).logIn(userName, password, address);
			}
		}
		System.out.println("Login attempt detected from " + address.getHostAddress() +" unsuccessful (User does not exist)\n\n"); // Prints this server side (mainly for debugging)
		return false;
	}

	public Status getStatus(){
		return status;
	}

	public int numThreads(){
		return serverThreads.size();
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
		return numConnections.get();
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

	///////////////////////////////////////////////////////////////////// Private classes here (used mainly for threading) ////////////////////////////////////////////////////////////////////////////

	private static class ConnectionThread implements Runnable{
		Socket conSock;
		User user; // (We do, however, need it in case connection is lost [sign user out]) User is stored here mainly to ensure that
		String filename;
		Operation operation;
		Server server;
		boolean autoOp;


		public ConnectionThread(Socket conSock, Server server){
			this.conSock = conSock;
			this.server = server;
			operation = Operation.UNKNOWN;
			autoOp = false;
		}

		// In the case where the client enters arguments into the program by themselves

		public ConnectionThread(Socket conSock, String filename, Operation operation){
			this.conSock = conSock;
			this.filename = filename;
			this.operation = operation;
			autoOp = true;
		}

		@Override
		public void run() {
			try {
				server.numConnections.getAndIncrement();
				boolean loggedIn = false;
				// Figure out a way to prevent the chance of 2 people signing in to the same account at the same time (which would mean there is a rare chance that 2 clients can connect to the same "user account")
				while (true) {

					PrintWriter serverWriter = new PrintWriter(conSock.getOutputStream(), true); // Object that sends messages to client

					serverWriter.println("Enter username (or send \"quit\" to end):");
					Scanner ClientMessageScanner = new Scanner(conSock.getInputStream()); // Object that receives messages from client from other side of the socket
					String userName = ClientMessageScanner.nextLine(); // Waits for client to send message

					System.out.println(userName);
					boolean done = false;
					if(userName.equals("quit")){
						serverWriter.println("Thank you. Have a nice day! =D");
						break;
					}
					serverWriter.println("Enter password:");
					String password = ClientMessageScanner.nextLine();
					System.out.println("userName: " + userName + "\nPassword: " + password);
					boolean correct = server.checkLogin(conSock.getInetAddress(), userName, password);
					System.out.println(correct);
					serverWriter.println(correct);
					if (correct) { // Checks if user details are entered correctly
						serverWriter.println("Login successful! Welcome " + userName);
						loggedIn = true;
						break;
					}
					else{
						serverWriter.println("Login unsuccessful");
					}

				}
				// Still need to test all of this. For now, we just need to ensure that the user can connect to the server (user can connect to server =D)
				if (loggedIn) {

					if (autoOp) {
						interaction1();
						Thread.sleep(10);
					}

					else {
						while (true) {
							interaction2();
							// Code below will go into interaction2
							String userInput = new Scanner(System.in).next();
							userInput = userInput.toUpperCase();
							if (userInput.equals("VIEW")) {
								operation = Operation.VIEW;
							} else if (userInput.equals("UPLOAD")) {
								operation = Operation.UPLOAD;
							} else if (userInput.equals("DOWNLOAD")) {
								operation = Operation.DOWNLOAD;
							} else if (userInput.equals("PERM")) {
								operation = Operation.PERMISSION;
							} else if (userInput.equals("QUIT")) {
								operation = Operation.QUIT;
							}

							// Need a failsafe that monitors other user file requests {So if one user uploads a file while another one downloads it, the uploading user must wait for the downloader to finish their operation before they can
							// begin uploading their file to the server. Peer-to-Peer would have been great hey! But here we are, stuck with a client-server architecture. FML. That time I also have a C++ assignment due on Monday. Yoh, getting into Honors is
							// gonna be ROFF. Can't be playing around anymore.


							switch (operation.ordinal()) {
								case 0:
									break;
								case 1:
									break;
								case 2:
									break;
								case 3:
									break;
								default:
									break;
							}

							if (operation == Operation.VIEW) {

							} else if (operation == Operation.UPLOAD) {

							} else if (operation == Operation.DOWNLOAD) {

							} else if (operation == Operation.QUIT) {
								// Sends message back to client
								break;
							} else {
								System.out.println("Unknown operation entered");
							}
						}
					}
					server.numConnections.getAndDecrement();
				}
			}
			catch(SocketException e){
				System.out.println("Connection with client " + conSock.getInetAddress().getHostAddress() + "lost:\n" + e.getMessage());
				for(int i = 0; i < server.users.size(); i++){
					if(server.users.get(i).getUserName().toLowerCase().equals(this.user.getUserName().toLowerCase())){
						server.users.get(i).logOut();
						break;
					}
				}
			}
			catch(IOException e){
				System.out.println("Error detected with connection from " + conSock.getInetAddress().getHostAddress() + ":\n" + e.getMessage());
			}
			catch(InterruptedException e){

			}
			catch(NoSuchElementException e){
				System.out.println("Connection to client " + conSock.getInetAddress().getHostAddress() + " lost");
			}
		}

		private void interaction1(){ // Responsible for interactions where user specifies operations they want to do through command line args
			try {
				PrintWriter pw = new PrintWriter(conSock.getOutputStream(), true);
				pw.println("Initiating " + operation.toString() + " operation.");
				switch(operation.ordinal()){
					case 0:
						// View operation called

						break;
					case 1:
						// Upload operation called (Must check if file already exists on server and ask user if )

						pw.println("File with name " + filename + "already exists, would you like to overwrite it?(Y/N)");
						String userInput = "";
						userInput = userInput.toUpperCase();
						if(userInput.equals("Y")){
							pw.println("Initializing upload protocol...");
						}
						else if(userInput.equals("N")){
							pw.println("Upload operation cancelled");
						}
						else{
							pw.println("Invalid input was entered. Upload operation has been cancelled");
						}
						break;
					case 2:
						// Download operation called

						break;
					default:
						System.out.println("Invalid operation detected from " + conSock.getInetAddress().getHostAddress());
						pw.println("Invalid operation entered.");
						break;
				}
			}
			catch(Exception e){

			}

		}

		private void interaction2(){ // Mainly responsible for interactions where user doesn't specify the operations they want to do through command line args

			try{
				PrintWriter pw = new PrintWriter(conSock.getOutputStream(), true);
				pw.println("Menu menu. Use this menu yeet");
			}
			catch(SocketException e){

			}
			catch(IOException e){

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

	private static class TestThread implements Runnable{

		Socket socket;
		Server server;

		public TestThread(Socket socket, Server server){
			this.socket = socket;
			this.server = server;
		}

		@Override
		public void run(){
			try{
				// Must test user login operation here
			}
			catch(Exception e){

			}
		}
	}

}
