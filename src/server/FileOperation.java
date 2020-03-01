package server;

public class FileOperation {
	private User user;
	private String fileName;
	public enum FileOp {UPLOAD, DOWNLOAD}
	private FileOp fileOp;

	public FileOperation(User user, String fileName, FileOp fileOp){
		this.user = user;
		this.fileName = fileName;
		this.fileOp = fileOp;
	}

	public User getUser() {
		return user;
	}

	public FileOp getFileOp() {
		return fileOp;
	}

	public String getFileName() {
		return fileName;
	}
}
