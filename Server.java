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
    public static void main(String args[])throws Exception {
        dialogues();
    }

    public static void dialogues() throws Exception{
        Scanner sc=new Scanner(System.in);
        boolean excess = false;
        boolean success = false;
        String userName= "";

        System.out.println("Welcome to the server");
        System.out.printf("User name: ");

        while(!success) {               // Checks if username is in the records and repeats the prompt if it's incorrect
            userName = sc.nextLine();
            success = users(userName);
            if(!success) System.out.println("Unknown user, try again.");
        }

        System.out.println("Excess level? (Public/Admin): ");
        success = false;
        String ex = sc.nextLine();
        if(ex.equals("Public")){
            excess = false;
        }

        else if(ex.equals("Admin")){
            System.out.printf("Password: ");

            while(!success){
                String pass = sc.nextLine();
                success  = password(userName, pass);
                if(!success) System.out.println("Try again");
            }
            excess = true;
            System.out.println("Welcome back "+userName+"!!");
        }


        System.out.println("Select an action: ");
        System.out.println("<View> - To view files on server");
        System.out.println("<Download> - To download a file");
        if(excess)      System.out.println("<P> - To set Permissions");


        String request = sc.nextLine();

        if(request.equals("View")){
            if(excess) {
                view("./Server/".concat(userName)+"/Private");
                view("./Server/".concat(userName));
            }
            else view("./Server/".concat(userName));
        }

        else if(request.equals("P") && excess){
            System.out.println("Type filename:");
            String filename=sc.nextLine();
            System.out.println("Make Private? (Y/N)");
            String p = sc.nextLine();
            File f = new File("./Server/".concat(userName), filename);
            perm(f, userName, filename);
        }

        else if (request.equals("Download")){
            System.out.println("Give the filename");
            String filename=sc.nextLine();
            //ServerSocket s = new ServerSocket(5000);

                //Socket serverthread = s.accept();

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
