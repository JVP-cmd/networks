package server;

public class FileOperation {
	private User user;
	private FileDetails file;
	public enum FileOp {UPLOAD, DOWNLOAD, MOVE, NONE}
	private FileOp fileOp;

	/**
	 * Constructor used to create a FileOperation Object
	 * @param user The user that is doing the operation
	 * @param file The file that the operation is being performed on
	 * @param fileOp The type of operation that is being performed on the file
	 */
	public FileOperation(User user, FileDetails file, FileOp fileOp){
		this.user = user;
		this.file = file;
		this.fileOp = fileOp;
	}

	/**
	 * Gets the user
	 * @return The user that is currently linked with the current file operation
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Gets the operation type that is currently being used by a specific user
	 * @return The operation type on a specific file
	 */
	public FileOp getFileOp() {
		return fileOp;
	}

	/**
	 * Gets the details of the file that is currently being worked on
	 * @return FileDetails object which contains a lot of data regarding the file
	 */
	public FileDetails getFile() {
		return file;
	}

}
