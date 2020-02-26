package server;

import client.Client;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Server {

	private String ipAddress;
	private String resourceDir;
	public enum Status {Available, Offline, Full};
	private Status status;
	private int socketNo;
	private int numConnections;
	private ServerSocket serverSocket;

	public Server(String ipAddress, String resourceDir, int socketNo){
		this.ipAddress = ipAddress;
		this.resourceDir = resourceDir;
		status = Status.Offline;
		this.socketNo = socketNo;
		numConnections = 0;
	}

	public void initialize(){ // Initializes server socket using specified port number
		try {
			serverSocket = new ServerSocket(this.socketNo);
			System.out.println("Socket initialized");

			// Generate all paths in server [Will have res folder that keeps all files on server]
			//


			status = Status.Available;
		}
		catch(IOException e) {
			System.out.println("Error setting up server on port " + this.socketNo);
		}
	}

	private void printAllAccessible(String username, Socket socket){

	}

	private void uploadFile(String username, Socket socket){

	}

	private void downloadFile(String username, Socket socket){

	}

	public void listen(){
		System.out.println("Listening");
		try(Socket socket = serverSocket.accept()) {
			boolean runOps = false;
			numConnections++;
			System.out.println("Connection from " + socket.getLocalAddress().getHostAddress() + " detected");
			if(this.status == Status.Offline){
				System.out.println("Connection denied"); // send message to client [Saying servers aren't available]
			}
			else if(this.status == Status.Full){
				System.out.println("Servers are full"); // Send message to clients [Saying server is full and that they
														// should try later

			}
			else {
				Scanner s = new Scanner(socket.getInputStream());
				System.out.println(numConnections);
				String userArgs = s.nextLine();
				String operation = s.nextLine();
				// System.out.println(s.nextLine());
				PrintWriter msg = new PrintWriter(socket.getOutputStream(), true);
				msg.println("The server says hi back");
				// We create threads here (For now, we just let stuff send items through and stuff)
				if(operation.equals("UPLOAD")){
					// Client uploading a file to server

				}
				else if(operation.equals("DOWNLOAD")){
					// Client downloading a file from server
				}
				else if(operation.equals("VIEW")){
					// Client
				}
			}
		}

		catch(IOException e){
			System.out.println("Error while listening for connections");
		}

	}

	public ServerSocket getServerSocket(){
		return serverSocket;
	}


	public void receiveConnection(Socket socket){
		try{
			numConnections++;
		}
		catch(Exception e){
			System.out.println("Connection to server was unsuccessful");
		}
	}

	public Status getStatus(){
		return status;
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
		return numConnections;
	}
}
