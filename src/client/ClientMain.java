package client;

import java.net.*;
import java.io.*;
import java.util.*;

public class ClientMain {

	public static void main(String[] args){
		Scanner inp = new Scanner(System.in);
		try {
			Client currentClient;
			String ip = InetAddress.getLocalHost().getHostAddress();
			String machineName = InetAddress.getLocalHost().getHostName();
			System.out.println("Current ip address is: " + ip);
			currentClient = new Client(ip, machineName);
			System.out.println("Current client details:\nClient Name: " + currentClient.getClientName() + "\nClient IP: " + currentClient.getIpAddress());
			Socket socket = new Socket("192.168.100.104", 59090);
			String yeet = "Client " + Client.numClients + " says Hello";
			PrintWriter message = new PrintWriter(socket.getOutputStream(), true);
			message.println(yeet);
			while(true){
				/*Scanner serverin = new Scanner(socket.getInputStream());
				System.out.println(serverin.nextLine());*/
			}
		}
		catch(Exception e){
			System.out.println("Error: " + e.getMessage());
			System.exit(0);
		}
	}
}
