package server;

import java.io.File;

public class FileDetails {
	private File file;
	private String userOwner;
	private String password;
	public enum FileAccess {PUBLIC, PRIVATE, ADMIN}
	private FileAccess fileAccess;

	public FileDetails(File file, String userOwner, String password, FileAccess fileAccess){
		this.file = file;
		this.userOwner = userOwner;
		this.password = password;
		this.fileAccess = fileAccess;
	}

	public File getFile() {
		return file;
	}

	public String getFileName(){
		return file.getName();
	}

	public String getUserOwner(){
		return userOwner;
	}

	public FileAccess getFileAccess() {
		return fileAccess;
	}

	public boolean canAccess(String userName, String password, User.Access userAccess){
		if(fileAccess == FileAccess.PUBLIC){
			return true;
		}
		else if(fileAccess == FileAccess.PRIVATE){ // Can access if you're admin, if you have the password to access the file or if you're the owner of the file
			if(userAccess == User.Access.ADMIN){
				return true;
			}
			else{
				if(userName.equals(this.userOwner)){
					return true;
				}
				if(password.equals(this.password)){
					return true;
				}
			}
			return false;
		}
		else if(fileAccess == FileAccess.ADMIN){ // Only admin users can access the file
			if(userAccess == User.Access.ADMIN){
				return true;
			}
		}
		return false;
	}

	public boolean changeAccess(String userName, User.Access userAccess, FileAccess fileAccess){
		if(this.userOwner.equals(userName)){
			this.fileAccess = fileAccess;
			return true;
		}
		else if(userAccess == User.Access.ADMIN){
			this.fileAccess = fileAccess;
			return true;
		}
		return false;
	}
}
