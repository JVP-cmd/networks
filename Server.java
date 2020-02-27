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
    public static void main(String args[])throws Exception{

        Scanner sc=new Scanner(System.in);

        System.out.println("Welcome to the server");
        System.out.println("Select an action: ");
        System.out.println("<View> - To view files on server");
        System.out.println("<Download> - To download a file");
        System.out.println("<Upload> - To upload a file");
        System.out.println("<Permission> Set Permission");

        String request = sc.nextLine();
        if(request.equals("View")){view();}

        else if(request.equals("Perm")){
            System.out.println("Type filename:");
            String filename=sc.nextLine();
            System.out.println("Make readable? (Y/N)");
            String p = sc.nextLine();

            if(p.equals("N"))   perm(filename, false);
            System.out.println("Make private? (Y/N)");
            if(sc.nextLine().equals("Y")){
                perm(filename);
            }
        }

        else if (request.equals("Download")){
            System.out.println("Give the filename");
            String filename=sc.nextLine();
            download(filename);}

    }

    public static void view(){
        Path path = Paths.get("/home/j/jlxkhu003/Documents/Networks/Server");

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

    public static void perm(String file, boolean bool){
      File f = new File(file);
      if(f.exists()){
          if(f.setReadable(bool)){
              System.out.println("Permissions Changed");
          }
      }
      else{
          System.out.println("File does not exist");
      }
    }

    public static void perm(String file){
        File f = new File("/home/j/jlxkhu003/Documents/Networks");
        File makePrivate = new File("/home/j/jlxkhu003/Documents/Networks/Server/Private");
        try{
            Files.copy(f.toPath(), makePrivate.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }catch(IOException e){}
        System.out.println("File is now private");
    }



    public static void download(String filename) throws IOException {

        while(true)
        {
            //create server socket on port 5000
            ServerSocket ss=new ServerSocket(5000);
            System.out.println ("Waiting for request");
            Socket s=ss.accept();
            System.out.println ("Connected With "+s.getInetAddress().toString());
            DataInputStream din=new DataInputStream(s.getInputStream());
            DataOutputStream dout=new DataOutputStream(s.getOutputStream());
            try{
                String str="";

                str=din.readUTF();
                System.out.println("SendGet....Ok");

                if(!str.equals("stop")){

                    System.out.println("Sending File: "+filename);
                    dout.writeUTF(filename);
                    dout.flush();

                    File f=new File(filename);
                    FileInputStream fin=new FileInputStream(f);
                    long sz=(int) f.length();

                    byte b[]=new byte [1024];

                    int read;

                    dout.writeUTF(Long.toString(sz));
                    dout.flush();

                    System.out.println ("Size: "+sz);
                    System.out.println ("Buf size: "+ss.getReceiveBufferSize());

                    while((read = fin.read(b)) != -1){
                        dout.write(b, 0, read);
                        dout.flush();
                    }
                    fin.close();

                    System.out.println("..ok");
                    dout.flush();
                }
                dout.writeUTF("stop");
                System.out.println("Send Complete");
                dout.flush();
            }
            catch(Exception e)
            {
                e.printStackTrace();
                System.out.println("An error occured");
            }
            din.close();
            s.close();
            ss.close();
        }
    }
}
