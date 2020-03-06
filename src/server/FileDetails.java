package server;

import java.io.File;

public class FileDetails {
	private File file;
	private User userOwner;
	private User.Access fileAccess;



	public FileDetails(File file, User userOwner, User.Access fileAccess){
		this.file = file;
		this.userOwner = userOwner;
		this.fileAccess = fileAccess;
	}

	public File getFile() {
		return file;
	}

	public String getFileName(){
		return file.getPath();
	}

	public User getUserOwner(){
		return userOwner;
	}

	public User.Access getFileAccess() {
		return fileAccess;
	}

	public boolean isFile(String filename, String fileDir){
		String fileLocation = file.getPath();
		String realFileName = fileLocation.replace(fileDir, "");
		System.out.println(realFileName);
		return filename.equals(realFileName);
	}

	public boolean canAccess(User.Access fileAccess){
		if(this.fileAccess == User.Access.PUBLIC){
			return true;
		}
		else if(this.fileAccess == User.Access.ADMIN) {
			return this.fileAccess == fileAccess;
		}
		return false;
	}

	public boolean changeAccess(User user){
		try {
			if (this.userOwner.equals(user) && user.getAccess() == User.Access.ADMIN) {
				if (this.fileAccess == User.Access.PUBLIC) {
					System.out.println("File is public. Making private...");
					this.fileAccess = User.Access.ADMIN;
					String fileDir = file.getParentFile().getPath() + "\\private\\" + file.getName();
					System.out.println(fileDir);
					boolean moveToNew = file.renameTo(new File(fileDir));
					this.file = new File(fileDir);
					return moveToNew;
				} else if (this.fileAccess == User.Access.ADMIN) {
					System.out.println("File is private, making public...");
					this.fileAccess = User.Access.PUBLIC;
					String fileDir = file.getParentFile().getParentFile().getPath() + "\\" + file.getName();
					System.out.println(fileDir);
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
