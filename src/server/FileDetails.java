package server;

import java.io.File;

public class FileDetails {
	private File file;
	private User userOwner;
	private User.Access fileAccess;


	/**
	 * Constructor method for FileDetails Object
	 * @param file The file
	 * @param userOwner The owner of the file
	 * @param fileAccess The access type of the file
	 */
	public FileDetails(File file, User userOwner, User.Access fileAccess){
		this.file = file;
		this.userOwner = userOwner;
		this.fileAccess = fileAccess;
	}

	/**
	 * Gets the file that is linked to this object
	 * @return File details that are linked to the object
	 */
	public File getFile() {
		return file;
	}

	/**
	 * gets the path of the file
	 * @return String of the path of the file
	 */
	public String getFilePath(){
		return file.getPath();
	}

	/**
	 * Returns the user object that "owns" the file (The user that uploaded it)
	 * @return The user that owns the file
	 */
	public User getUserOwner(){
		return userOwner;
	}

	public User.Access getFileAccess() {
		return fileAccess;
	}

	/**
	 *	Checks if this file is the file that we are looking for (Through our search query)
	 * @param filename The name of the file that is being queried
	 * @param fileDir The directory of where this file is being stored (In this case, it will be the repository directory)
	 * @return A boolean that says whether or not the file that is being queried is this file
	 */
	public boolean isFile(String filename, String fileDir){
		String fileLocation = file.getPath();
		String realFileName = fileLocation.replace(fileDir, "");
		return filename.equals(realFileName);
	}

	/**
	 * Checks if the specified access rights can access this file
	 * @param fileAccess The access level that is being checked
	 * @return A boolean that dictates whether or no the file is accessible
	 */
	public boolean canAccess(User.Access fileAccess){
		if(this.fileAccess == User.Access.PUBLIC){
			return true;
		}
		else if(this.fileAccess == User.Access.ADMIN) {
			return this.fileAccess == fileAccess;
		}
		return false;
	}

	/**
	 * Changes the access level of the file, only available to the
	 * @param user The user that is being checked in
	 * @return A boolean which pertains to the success of the operation (There are cases where a different admin will try to move items around)
	 */
	public boolean changeAccess(User user){
		try {
			if (this.userOwner.equals(user) && user.getAccess() == User.Access.ADMIN) {
				if (this.fileAccess == User.Access.PUBLIC) {
					System.out.println("File " + file.getName() + " is public. Making private...");
					this.fileAccess = User.Access.ADMIN;
					String fileDir = file.getParentFile().getPath() + System.getProperty("file.separator") + "private" + System.getProperty("file.separator") + file.getName();
					boolean moveToNew = file.renameTo(new File(fileDir));
					this.file = new File(fileDir);
					return moveToNew;
				} else if (this.fileAccess == User.Access.ADMIN) {
					System.out.println("File " + file.getName() + " is private, making public...");
					this.fileAccess = User.Access.PUBLIC;
					String fileDir = file.getParentFile().getParentFile().getPath() + System.getProperty("file.separator") + file.getName();
					boolean moveToNew = file.renameTo(new File(fileDir));
					this.file = new File(fileDir);
					return moveToNew;
				}
				return true;
			}
			return false;
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
}
