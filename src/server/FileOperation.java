package server;

public class FileOperation {
	private User user;
	private FileDetails file;
	public enum FileOp {UPLOAD, DOWNLOAD, MOVE, NONE}
	private FileOp fileOp;

	public FileOperation(User user, FileDetails file, FileOp fileOp){
		this.user = user;
		this.file = file;
		this.fileOp = fileOp;
	}

	public User getUser() {
		return user;
	}

	public FileOp getFileOp() {
		return fileOp;
	}

	public FileDetails getFile() {
		return file;
	}

}
