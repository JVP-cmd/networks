package client;

public class Client {

	public static int numClients = 0;
	private String ipAddress;
	private String clientName;


	public Client(String ipAddress, String clientName){
		numClients++;
		this.ipAddress = ipAddress;
		this.clientName = clientName;
	}


	public String getIpAddress(){
		return ipAddress;
	}

	public String getClientName(){
		return clientName;
	}
}
