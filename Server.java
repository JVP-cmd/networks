import javax.imageio.IIOException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Server{

    /**
     * Main method to be run first before the client class
     * @param args
     * @throws Exception
     */
    public static void main(String args[])throws Exception {
        dialogues();
    }

    public static void dialogues() throws Exception {
        ServerSocket ss=new ServerSocket(5000);
        Socket socket=ss.accept();
         DataInputStream din=new DataInputStream(socket.getInputStream());
            DataOutputStream dout=new DataOutputStream(socket.getOutputStream());
              PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
        Scanner sc = new Scanner(System.in);
        boolean acccess = false;
        boolean success = false;
        String userName = "";

        pw.println("Welcome to the server");
        pw.println("User name: ");    // Ask user for their login information
System.out.println("Waiting for user input"); 
        dout.writeBoolean(acccess);

        while (!success) {               // Checks if username is in the records and repeats the prompt if it's incorrect
            userName = din.readUTF();
            System.out.println(userName);
            success = users(userName);
            System.out.println("Sending boolean success");
             dout.writeBoolean(success);
            if (!success)
               
                pw.println("Unknown user, try again.");        //Prompt to re-enter the username if intial was incorrect
        }

        pw.println("Excess level? (Public/Admin): ");       // Prompt user to specify access level
        success = false;
        String ex = din.readUTF();

        if (ex.equals("Public")) {
            acccess = false;
        } else if (ex.equals("Admin")) {
            pw.println("Password: ");
            System.out.println("Sending success boolean");
dout.writeBoolean(success);
            while (!success) {
                String pass = din.readUTF();
                success = password(userName, pass);
                dout.writeBoolean(success);
                if (!success) pw.println("Try again");
            }
            acccess = true;
            pw.println("Welcome back " + userName + "!!");
        }
        String request = "";

        while (!(request.equals("Q"))) {
            pw.println("Select an action: ");
            pw.println("<View> - To view files on server");
            pw.println("<Download> - To download a file");
            System.out.println("Check if the user has admin rights");
            dout.writeBoolean(acccess);
            if (acccess){ pw.println("<P> - To set Permissions");}
            pw.println("<Q> To quit");

            request = din.readUTF();
 dout.writeBoolean(acccess);
            if (request.equals("View")) {
               
                if (acccess) {
                    view("./Server/".concat(userName) + "/Private");
                    view("./Server/".concat(userName));
                } else view("./Server/".concat(userName));
            } else if (request.equals("P") && acccess) {
                pw.println("Type filename:");
                String filename = din.readUTF();
                pw.println("Make Private? (Y/N)");
                String p = din.readUTF();
                File f = new File("./Server/".concat(userName), filename);
                perm(f, userName, filename);
            } else if (request.equals("Download")) {
               
                pw.println("Give the filename");
                
                String filename = din.readUTF();

                Threadmanager server = new Threadmanager(5000, filename, userName);
                new Thread(server).start();

                try {
                    Thread.sleep(20 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Stopping Server");
                server.stopT();

            }
        }
    }


    /**
     * View contents in the server
     */

    public static void view(String folderPath){
        Path path = Paths.get(folderPath);

        try(Stream<Path> subPaths = Files.walk(path,1)){

            List<String> subPathList = subPaths.filter(Files::isRegularFile)
                    .map(Objects::toString)
                    .collect(Collectors.toList());
            System.out.println(subPathList);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if password is correct for the given username
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
     * Checks if given user name is known to the server
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
     * Makes file private or invisible to users using public access
     * @param ofile
     * @param username
     * @param fname
     */
    public static void perm(File ofile, String username, String fname){
        File makePrivate = new File("./Server/".concat(username)+"/Private/" + fname);
        try{
            Files.copy(ofile.toPath(), makePrivate.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }catch(IOException e){}
        //ofile.delete();
        System.out.println("Moved to private folder");
    }


}