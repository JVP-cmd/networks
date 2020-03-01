package server;

import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class User {

	private InetAddress connectionAddress;
	private final String userName;
	private final String password;
	private AtomicBoolean loggedIn;

	public User(String userName, String password){
		this.userName = userName;
		this.password = password;
		this.loggedIn = new AtomicBoolean(false);
	}

	public String getConnectionAddress(){
		return connectionAddress.getHostAddress();
	}

	public String getUserName(){
		return userName;
	}

	public boolean loggedIn(){
		return loggedIn.get();
	}

	public synchronized boolean logIn(String userName, String password, InetAddress connectionAddress){
		System.out.println("Attempting login..");
		if(!loggedIn.get()) { // User is not logged in
			if (this.userName.toLowerCase().equals(userName) && this.password.equals(password)) { // Correct details entered
				System.out.println("User login successful");
				//serverMessage = 0; // User login successful
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
