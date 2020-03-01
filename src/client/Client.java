package client;

import java.net.Socket;

public class Client {

	private String ipAddress;
	private String clientName;


	public Client(String ipAddress, String clientName){
		this.ipAddress = ipAddress;
		this.clientName = clientName;
	}


	public String getIpAddress(){
		return ipAddress;
	}

	public String getClientName(){
		return clientName;
	}

	public void uploadFile(String downloadPath, String filename, Socket s){

	}

	// So for some odd reason, when I smoke, I can't concentrate on anything. Especially when I'm tired, guess I
	// shouldn't smoke when I'm in the zone.
}