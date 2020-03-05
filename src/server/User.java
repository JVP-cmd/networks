package server;

import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class User {

	private InetAddress connectionAddress;
	public enum Access { ADMIN, PUBLIC}
	private Access access;
	private final String userName;
	private final String password;
	private AtomicBoolean loggedIn;
	private ArrayList<FileDetails> userFileLogs;

	public User(String userName, String password, Access access){
		this.userName = userName;
		this.password = password;
		this.loggedIn = new AtomicBoolean(false);
		this.access = access;
		this.userFileLogs = new ArrayList<>();
	}

	public String getConnectionAddress(){
		return connectionAddress.getHostAddress();
	}

	public ArrayList<FileDetails> getUserFileLogs(){
		return userFileLogs;
	}

	public String getUserName(){
		return userName;
	}

	public boolean loggedIn(){
		return loggedIn.get();
	}

	public Access getAccess(){
		return access;
	}

	public synchronized boolean logIn(String userName, String password, InetAddress connectionAddress){
		System.out.println("Attempting login..");
		if(!loggedIn.get()) { // User is not logged in
			if (this.userName.equals(userName.toLowerCase()) && this.password.equals(password)) { // Checks if the correct user details were entered
				System.out.println("User login successful");
				// serverMessage = 0; // User login successful
				loggedIn.set(true);
				this.connectionAddress = connectionAddress;
				return true;
			} else { // Username correct, but password is incorrect
				System.out.println("Login attempt from " + connectionAddress.getHostAddress() + " unsuccessful (Incorrect Password)");
			}
		}
		if(loggedIn.get()){
			System.out.println("Duplicate login detected from " + connectionAddress.getHostAddress() + " detected");
		}
		return false;
	}

	public void logOut(){
		connectionAddress = null;
		loggedIn.set(false);
	}
}
