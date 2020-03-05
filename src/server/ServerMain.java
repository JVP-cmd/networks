package server;

import java.net.InetAddress;
import java.util.Scanner;


public class ServerMain {

	public volatile static boolean endServer;
	protected volatile static boolean pauseServerPrints;
	private static String userDBFile;
	private static String fileRepoDir;
	protected static final String DBDELIMITER = ";,;delim; delim;!end;";

	public static void main(String[] args){ // Server main thread [child threads will be made in server object]
		try {

			// Arguments will be the following: [server port, resource folder(folder where all files will be stored)}
			// args[0] : File Repository Directory
			// args[1] : User Database Directory
			// args[2] : Server port number
			// args[3] : Maximum number of connections that can be made to the server
			pauseServerPrints = false;
			endServer = false;

			fileRepoDir = args[0];

			fileRepoDir = fileRepoDir.replace("/", "\\");

			userDBFile = args[1];

			System.out.println("Creating server at ip address " + InetAddress.getLocalHost().getHostAddress() + " with port number " + args[2] + " having a limit of " + args[3] + " simultaneous connection(s)");

			Server server = new Server(fileRepoDir, userDBFile, Integer.parseInt(args[2]), Integer.parseInt(args[3]));
			server.initialize();
			Thread[] threads = new Thread[2];
			threads[0] = new Thread(new InputThread(server)); // Begins new input thread

			threads[0].start();

			threads[1] = new Thread(new ListenerThread(server)); // Begins server listener thread

			threads[1].start();

			for(int i = 0; i < threads.length; i++){
				threads[i].join();
			}
			System.exit(0);
		}
		catch(Exception e){
			e.printStackTrace();
			System.out.println("An error occurred while running the server");
			System.exit(1);
		}
	}
	///////////////////////////////////////////////////////////////////////////////////// Private thread classes used in main method ///////////////////////////////////////////////////////////////////////////
	private static class ListenerThread implements Runnable{
		Server server;

		public ListenerThread(Server server){
			this.server = server;
		}
		@Override
		public void run() {
			while(!endServer){
				server.listen();
				if(endServer){
					System.out.println("Stuff");
					break;
				}
			}
			System.out.println("Listener thread joined");
		}
	}

	private static class InputThread implements Runnable{
		Scanner serverInput = new Scanner(System.in);
		Server server;
		private static final String servIn = "Send any input in this menu to continue tracking server operations.";

		public InputThread(Server server){
			this.server = server;
		}

		@Override
		public void run() {
			while(!endServer){
				System.out.println("Server is running, send any input to begin interaction with server");
				String stopper = serverInput.nextLine();
				System.out.println("Current server stats:\n - Number of open connections: " + server.numThreads() + "\n - Number of users logged in: " + server.numloggedIn());
				System.out.println("\nCommand list:\n<Print> - Print all user data\n<Add> Add a user to the server\n<End> - End all server operations");
				pauseServerPrints = true;


				String serverOption = serverInput.nextLine();

				if(serverOption.toUpperCase().equals("PRINT")){
					System.out.println("Number of users stored on server - " + server.getNumUsers() + " users:\n");
					for(int i = 0; i < server.getNumUsers(); i++){
						System.out.println(" --> Username: " + server.getUser(i).getUserName() + "; Access Level: " + server.getUser(i).getAccess());
					}
					System.out.println("\n" + servIn);
					stopper=serverInput.nextLine();
					System.out.println("Continuing server tracking operation...\n");
					pauseServerPrints = false;
				}

				// Adds a new user to the user database. Only users can upload files and only admin users can change the privacy of files
				else if(serverOption.toUpperCase().equals("ADD")){

					System.out.println("Enter user name:");
					String username = serverInput.nextLine();
					boolean userNotInServer = true;
					for(int i = 0; i < server.getNumUsers(); i++){
						if(username.toLowerCase().trim().equals(server.getUser(i).getUserName())){
							userNotInServer = false;
						}
					}
					if(userNotInServer) {
						if (username.toLowerCase().contains("private")) {
							System.out.println("User name cannot contain the private");
						} else {

						}
						System.out.println("Enter user password:");
						String password = serverInput.nextLine();

						System.out.println("Enter user access level[PUBLIC/ADMIN]");
						String accessStr = serverInput.nextLine();
						if (!accessStr.toUpperCase().trim().equals("ADMIN") && !accessStr.toUpperCase().trim().equals("PUBLIC")) {
							System.out.println("Invalid user access entered");
						} else {
							User.Access access = User.Access.valueOf(accessStr.toUpperCase());
							boolean newusermade = server.createUser(username, password, access);
							if(newusermade){
								System.out.println("User with name " + username + " and access " + access.toString() + " has been created");
							}
							else{
								System.out.println("An error has occurred while creating user with name " + username + " and access " + access.toString());
							}
						}
					}
					else{
						System.out.println("User with name " + username + " already exists in user database");
					}
					System.out.println(servIn);
					stopper=serverInput.nextLine();
					System.out.println("Continuing server tracking operation...");
					pauseServerPrints = false;
				}
				else if(serverOption.toUpperCase().equals("END")) {
					System.out.println("Are you sure you want to close the server? Any connections made to the server will only be terminated once they have completed their operations.(Y/N)");
					String quit = serverInput.nextLine();
					if (quit.toUpperCase().equals("Y")) {
						System.out.println("Thank you for using our server :D");
						endServer = true;
						server.close();
						break;
					}
					System.out.println("Continuing server tracking operation...");
				}
				else{
					System.out.println("Continuing server tracking operation...");
				}
			}
		}
	}

}
