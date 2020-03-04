import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Server {

    private String ipAddress;
    private String resourceDir;
    public enum Status {Available, Offline, Full};
    private Status status;
    private final int socketNo;
    private Socket s;
    private int numConnections;
    private ServerSocket serverSocket;


    /**
     * Constructor takes in the parameters essential to establish the connection
     * @param ipAddress
     * @param resourceDir
     * @param socketNo
     */
    public Server(String ipAddress, String resourceDir, int socketNo){
        this.ipAddress = ipAddress;
        this.resourceDir = resourceDir;
        status = Status.Offline;
        this.socketNo = socketNo;
        numConnections = 0;
    }


    /**
     * initialize the server socket with the port number
     */
    public void initialize(){ // Initializes server socket using specified port number
        try {
            serverSocket = new ServerSocket(this.socketNo);

            System.out.println("Socket initialized");

            // Generate all paths in server [Will have res folder that keeps all files on server]
            //

            status = Status.Available;
        }
        catch(IOException e) {
            System.out.println("Error setting up server on port " + this.socketNo);
        }
    }

    /**
     * Uploads file from client to server
     * @param username
     * @param s
     */
    private void uploadToServer(String username, Socket s){
        try{
            DataInputStream din=new DataInputStream(s.getInputStream());
            DataOutputStream dout=new DataOutputStream(s.getOutputStream());

            String str=""; String filename="";

            System.out.println("upload to Server started");
            while(!str.equals("stop")){

                str="bam";
                dout.writeUTF(str);
                dout.flush();
                filename=din.readUTF();

                str=din.readUTF();
                long sz=Long.parseLong(din.readUTF());
                byte b[]=new byte [1024];
                FileOutputStream fos=new FileOutputStream(new File(filename),true);
                long bytesRead;

                do{
                    bytesRead=din.read(b,0,b.length);
                    fos.write(b,0,b.length);
                }
                while(!(bytesRead<1024));{
                    move(username,filename);
                    fos.close();
                    dout.close();
                    s.close();
                }
            }
            System.out.println("Upload complete");}
        catch(Exception e){System.out.println(e);}}

    /**
     * Takes in filename, username and socket object to download a file from the server to the user.
     * @param filename
     * @param username
     * @param s
     */
    private void DownloadFile(String filename,String username, Socket s){

        while(true){//infinite while loop to wait for the client to be ready to recieve
            try{


                DataInputStream din=new DataInputStream(s.getInputStream());
                DataOutputStream dout=new DataOutputStream(s.getOutputStream());

                String Data;
                Data=din.readUTF(); //input recived from the DownloadtoClient method in Client class (recives "bam ")

                while(!Data.equals("stop")){

                    System.out.println("Downloading File: "+filename);
                    dout.writeUTF(filename);
                    dout.flush();
                    File file=new File("./Server/".concat(username), filename);
                    FileInputStream fileIn=new FileInputStream(file);
                    long sz=(int)file.length();
                    byte b[]=new byte[1024];
                    int read;
                    dout.writeUTF("stop");
                    dout.writeUTF(Long.toString(sz));
                    dout.flush();

                    while((read=fileIn.read(b))!=1){
                        dout.write(b,0,read);
                        dout.flush();
                    }
                    move(filename, username);
                    fileIn.close();
                    dout.flush();

                }
                dout.flush();
                din.close();
                s.close();

            }catch(ArrayIndexOutOfBoundsException e){System.out.println("Download complete");break;}catch(Exception e){System.out.println(e.getMessage());break;}
        }
    }


    /**
     * Manages the messages being parsed from ClientMain to Server. Listens to the inputs and calls the necessary methods
     */
    public void listen(){
        System.out.println("Listening");
        try(Socket socket = serverSocket.accept()) {
            DataInputStream din=new DataInputStream(socket.getInputStream());
            DataOutputStream dout=new DataOutputStream(socket.getOutputStream());

            boolean RealUser=false;
            String username=" ";
            while(RealUser==false){
                username=din.readUTF();
                RealUser=users(username);
                dout.writeBoolean(RealUser);
            }
            String accessLevel=din.readUTF();
            boolean pas=true;
            if(accessLevel.equals("Admin")){
                pas=false;
                System.out.println(pas);}
            while(!pas){
                String password=din.readUTF();
                pas=password(username,password);
                dout.writeBoolean(pas);}


            numConnections++;
            System.out.println("Connection from " + socket.getLocalAddress().getHostAddress() + " detected");
            System.out.println("Sending promts...");
            String userinp=" ";
            while(!(userinp.equals("Stop"))){


                userinp=din.readUTF();


                if(userinp.equals("Download")){



                    userinp =din.readUTF();

                    DownloadFile(userinp,username, socket);}

                else if(userinp.equals("Upload")){
                    uploadToServer(username,socket);
                }

                else if(userinp.equals("View")){
                    String viewer=din.readUTF();
                    if(viewer.equals("Admin View")){
                        view("./Server/".concat(username)+"/Private", socket);
                        view("./Server/".concat(username), socket);
                    }
                    else view("./Server/".concat(username), socket);

                }


                else if(userinp.equals("Permission")){
                    String perm=din.readUTF();
                    String filename=din.readUTF();

                    if(perm.equals("Public")){
                        File myfile=new File("./Server/".concat(username)+"/Private",filename);
                        permPublic(myfile, username, filename);
                    }

                    else{
                        File myfile = new File("./Server/".concat(username), filename);
                        permPrivate(myfile, username, filename);
                    }
                }
            }


        }

        catch(IOException e){
            System.out.println("Error while listening for connections");
        }

    }


    /**
     * Return the file path and name of folders in the users folder on the server
     * @param folderPath
     * @param s
     */
    public static void view(String folderPath, Socket s){
        Path path = Paths.get(folderPath);


        try(Stream<Path> subPaths = Files.walk(path,1)){
            PrintWriter pw=new PrintWriter(s.getOutputStream(),true);
            List<String> subPathList = subPaths.filter(Files::isRegularFile)
                    .map(Objects::toString)
                    .collect(Collectors.toList());
            pw.println(subPathList);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Is used by the upload method, when file is downloaded from the client, it is moved to their specific folder on the server
     * @param username
     * @param file
     */
    private static void move(String username,String file){
        File f = new File(file);
        File makePrivate = new File("./Server/"+username+"/"+ file);
        try{
            Files.copy(f.toPath(), makePrivate.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }catch(IOException e){}
        f.delete();
    }



    /**
     * Checks if password corresponds with the previously given username, if it does, return true and false otherwise.
     * @param username
     * @param password
     * @return
     * @throws FileNotFoundException
     */
    private static boolean password(String username, String password) throws FileNotFoundException {
        boolean bool = false;
        File file = new File("userInfo.txt");
        Scanner scan = new Scanner(file);
        while(scan.hasNext()){
            if(scan.next().equals(username) && scan.next().equals(password)){
                bool = true;
            }
        }
        return bool;
    }

    /**
     * Checks if user exists in the records, if they do, returns true, and false otherwise.
     * @param username
     * @return
     * @throws FileNotFoundException
     */
    public static boolean users(String username) throws FileNotFoundException {
        boolean bool = false;
        File file = new File("userInfo.txt");
        Scanner scan = new Scanner(file);
        while(scan.hasNext()){
            if(scan.next().equals(username)){
                bool = true;
            }
            scan.next();
        }
        return bool;
    }

    /**
     * Takes in the file to be made private and moves it to the private folder
     * @param ofile
     * @param username
     * @param fname
     */
    public static void permPrivate(File ofile, String username, String fname){
        File makePrivate = new File("./Server/".concat(username)+"/Private/" + fname);
        try{
            Files.copy(ofile.toPath(), makePrivate.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }catch(IOException e){}
        ofile.delete();         //deletes old file
        System.out.println("Moved to private folder");
    }

    /**
     * Makes a given file private by moving it to a private folder where only the owner can view it
     * @param ofile
     * @param username
     * @param fname
     */
    public static void permPublic(File ofile, String username, String fname){
        File makePublic = new File("./Server/".concat(username) +"/"+ fname);
        try{
            Files.copy(ofile.toPath(), makePublic.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }catch(IOException e){}
        ofile.delete();//deletes old file
        System.out.println("Moved to public folder");
    }}
