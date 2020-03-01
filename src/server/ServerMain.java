package server;

import java.net.*;
import java.io.*;
import java.util.Scanner;


public class ServerMain {

	public volatile static boolean endServer;

	public static void main(String[] args){ // Server main thread [child threads will be made in server object]
		try {

			// Arguments will be the following: [server port, resource folder(folder where all files will be stored)}

			endServer = false;
			String ip = InetAddress.getLocalHost().getHostAddress();
			String fileLoc = "res";
			Server server = new Server(ip, fileLoc, 59090, 200);
			server.initialize();

			Thread[] threads = new Thread[3];
			threads[0] = new Thread(new InputThread(server)); // Begins new input thread

			threads[0].start();

			threads[1] = new Thread(new ListenerThread(server)); // Begins server listener thread

			threads[1].start();

			threads[2] = new Thread(new TrackerThread(server)); // Begins tracking number of active threads
			threads[2].start();

			System.out.println();
			while(!endServer){
				// Keeps the server running
			}
			for(int i = 0; i < threads.length; i++){
				threads[i].join();
			}
			System.exit(0);
		}
		catch(Exception e){
			System.out.println("An error occurred while running the server: " + e.getMessage());
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
		}
	}

	private static class InputThread implements Runnable{
		Scanner ServerInput = new Scanner(System.in);
		Server server;

		public InputThread(Server server){
			this.server = server;
		}

		@Override
		public void run() {
			System.out.println("Yes");
			while(!endServer){
				String stopper = ServerInput.nextLine();
				System.out.println("Are you sure you want to close the server?(Y/N)");
				String quit = ServerInput.nextLine();
				if(quit.toUpperCase().equals("Y")){
					System.out.println("Thank you for using our server :D");
					endServer = true;
					server.close();
					break;
				}
				System.out.println("Continuing server operations");
			}

		}
	}

	private static class TrackerThread implements Runnable{
		Server server;

		public TrackerThread(Server server){
			this.server = server;
		}
		@Override
		public void run() {
			while(!endServer){
				try{
					System.out.println("Number of open threads: " + server.numThreads() + " connection(s)");
					System.out.println("Number of users logged in: " + server.numloggedIn() + " user(s)");
					Thread.sleep(20000);
				}
				catch (Exception e){

				}
			}
		}
	}
}
