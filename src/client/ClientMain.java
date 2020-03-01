package client;

import java.io.PrintWriter;
import java.net.*;
import java.util.*;

public class ClientMain {

	public static void main(String[] args){

		Scanner inp = new Scanner(System.in);

		try {
			// arg[0] :
			// arg[1] :
			// arg[2] :
			// arg[3] :
			// arg[4] :

			String downloadPath = System.getProperty("user.home") + "/Downloads/YeetLoader/";
			Client currentClient;
			boolean autoOp = false;
			String ip = InetAddress.getLocalHost().getHostAddress();
			String machineName = InetAddress.getLocalHost().getHostName();
			System.out.println("Current ip address is: " + ip);
			currentClient = new Client(ip, machineName);
			System.out.println("Current client details:\nClient Name: " + currentClient.getClientName() + "\nClient IP: " + currentClient.getIpAddress());
			Socket socket = new Socket("196.47.228.194", 59090);
			if(args.length > 3){
				autoOp = true;
			}

			// Runs operations automatically (These operations aren't ran automatically
			Scanner serverInputs = new Scanner(socket.getInputStream());
			PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
			while(true) {
				System.out.println(serverInputs.nextLine());
				String userName = inp.nextLine().trim();
				pw.println(userName);
				// Server asks user for password (or sends user off if they decide to quit the program)
				System.out.println(serverInputs.nextLine());
				if(userName.equals("quit")){
					System.exit(0);
				}
				String password = inp.nextLine();
				pw.println(password);
				boolean success = Boolean.parseBoolean(serverInputs.nextLine()); // Receives message from server as to whether or not
				System.out.println(serverInputs.nextLine()); // Notifies user whether or not the login attempt was successful

				if(success){
					break;
				}
			}
			// Server


			if(autoOp){
			}
			else {
				while (true) {
					serverInputs = new Scanner(socket.getInputStream());
					System.out.println("Yeet");
				}
			}
		}
		catch(Exception e){
			System.out.println("Error: " + e.getMessage());
			System.exit(0);
		}
	}
}
