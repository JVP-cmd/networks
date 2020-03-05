package server;

import java.io.File;

public class FileDetails {
	private File file;
	private User userOwner;
	public enum FileAccess {PUBLIC, ADMIN}
	private FileAccess fileAccess;

	public FileDetails(File file, User userOwner, FileAccess fileAccess){
		this.file = file;
		this.userOwner = userOwner;
		this.fileAccess = fileAccess;
	}

	public File getFile() {
		return file;
	}

	public String getFileName(){
		return file.getName();
	}

	public User getUserOwner(){
		return userOwner;
	}

	public FileAccess getFileAccess() {
		return fileAccess;
	}

	public boolean canAccess(User.Access userAccess){
		if(fileAccess == FileAccess.PUBLIC){
			return true;
		}
		else if(fileAccess == FileAccess.ADMIN){ // Only admin users can access the file [Only admin users can access private files]
			if(userAccess == User.Access.ADMIN){
				return true;
			}
		}
		return false;
	}

	public boolean changeAccess(String userName, User.Access userAccess, FileAccess fileAccess){
		if(this.userOwner.getUserName().toLowerCase().equals(userName.toLowerCase()) && userAccess == User.Access.ADMIN){
			this.fileAccess = fileAccess;
			return true;
		}
		/*
		if(userAccess == User.Access.ADMIN){
			this.fileAccess = fileAccess;
			return true;
		}*/
		return false;
	}
}
