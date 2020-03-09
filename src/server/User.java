package server;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class User {

	private InetAddress connectionAddress;
	public enum Access { ADMIN, PUBLIC}
	private Access access;
	private final String userName;
	private final String password;
	private AtomicBoolean loggedIn;

	/**
	 * Constructor for User object
	 * @param userName The name of the user
	 * @param password The password that is required to login as this user
	 * @param access The access level that the user has
	 */
	public User(String userName, String password, Access access){
		this.userName = userName;
		this.password = password;
		this.loggedIn = new AtomicBoolean(false);
		this.access = access;
	}

	/**
	 * Getter that returns the name of the user
	 * @return A string which details the name of the user
	 */
	public String getUserName(){
		return userName;
	}

	/**
	 * Getter that checks if a client has already logged in as this user
	 * @return A boolean which details the state of the user account being accessed (false means that the user has not logged in yet)
	 */
	public boolean loggedIn(){
		return loggedIn.get();
	}

	/**
	 * Getter that returns the access level of this user
	 * @return An Access enum that details the access level of this user
	 */

	public Access getAccess(){
		return access;
	}

	/**
	 * Synchronized method that lets a client sign in as a specific user
	 * @param userName The user name that was passed in by the client
	 * @param password The password that was passed in by the client
	 * @param connectionAddress The connection address of the user
	 * @return A boolean that details the success of the login attempt by a client
	 */
	public synchronized boolean logIn(String userName, String password, InetAddress connectionAddress){
		System.out.println("Attempting login..");
		if(!loggedIn.get()) { // User is not logged in
			if (this.userName.equals(userName.toLowerCase()) && this.password.equals(password)) { // Checks if the correct user details were entered
				System.out.println("User login successful");
				System.out.println();
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

	/**
	 * Signs a client out of this user
	 */
	public void logOut(){
		connectionAddress = null;
		loggedIn.set(false);
	}

	/**
	 * equals method that checks if this user is equivalent to the parameterized object
	 * @param o Object that is being checked for equality
	 * @return Boolean that details the equality between this user and the parameterized o object
	 */
	public boolean equals(Object o){
		if(o.getClass() == User.class){
			User u = (User)o;
			return (this.userName.equals(u.getUserName()));
		}
		return false;
	}
}
