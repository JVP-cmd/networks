import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

class Client{
    public static void main(String args[])throws Exception{
      
        Scanner sc=new Scanner(System.in);
Socket socket=new Socket("localhost",5000);

 DataInputStream din=new DataInputStream(socket.getInputStream());
            DataOutputStream dout=new DataOutputStream(socket.getOutputStream());
            Scanner outputs = new Scanner(socket.getInputStream());
            System.out.println(outputs.nextLine());
            System.out.println(outputs.nextLine());
            while(!din.readBoolean()){
             
                String username=sc.nextLine();
            dout.writeUTF(username);
            if(!din.readBoolean()) {System.out.println(outputs.nextLine());}}
            System.out.println(outputs.nextLine());
            String perm=sc.nextLine();
            dout.writeUTF(perm);
            if(perm.equals("Admin")){
            System.out.println(outputs.nextLine());
            while(!din.readBoolean()){
            String pass=sc.nextLine();
            dout.writeUTF(pass);
            if(!din.readBoolean()){System.out.println(outputs.nextLine());}}
                System.out.println(outputs.nextLine());}
            String request = "";
            while(!request.equals("Q")){
                System.out.println(outputs.nextLine());
             System.out.println(outputs.nextLine());
             System.out.println(outputs.nextLine());
             boolean ad=din.readBoolean();
            if(ad)
            {System.out.println(outputs.nextLine());}
              System.out.println(outputs.nextLine());
            request=sc.nextLine();
            dout.writeUTF(request);
            boolean acccess=din.readBoolean();
          if(request.equals("P")&&acccess){
              System.out.println(outputs.nextLine());
          String filename=sc.nextLine();
          dout.writeUTF(filename);
          System.out.println(outputs.nextLine());
          String p=sc.nextLine();
          dout.writeUTF(p);}
          else if(request.equals("Download")){
              System.out.println(outputs.nextLine());
          String filename=sc.nextLine();
          dout.writeUTF(filename);}
            }
        //create the socket on port 5000
   
       
    }

    public static void move(String file){
        File f = new File(file);
        File makePrivate = new File(".\\Client\\" + file);
        try{
            Files.copy(f.toPath(), makePrivate.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }catch(IOException e){}
        f.delete();
    }
                   private static void DownloadtoClient( Socket s){
                    try{
                DataInputStream din=new DataInputStream(s.getInputStream());
        DataOutputStream dout=new DataOutputStream(s.getOutputStream());
      
     
        String str=" "; String filename="";
 
                    while(!str.equals("stop")){
                       
                     
                    str="bam";
                
                    dout.writeUTF(str);
                    dout.flush();
                    filename=din.readUTF();
                    filename+=" client's";
                     str=din.readUTF();
                    long sz=Long.parseLong(din.readUTF());
                     byte b[]=new byte [1024];
                    FileOutputStream fos=new FileOutputStream(new File(filename),true);
                    long bytesRead=1;
                     System.out.println("Download to Client complete ");
                    try{do{
                    bytesRead=din.read(b,0,b.length);
                    
                   
                    fos.write(b,0,b.length);
                    }
                    
                    while(!(bytesRead<1024));{//end of file exception here
                        move(filename);
                        
                     fos.close();
                     dout.close();
                     s.close();
                  }}catch(Exception e){System.out.println(e);}
                    
                   }}
                    catch(Exception e){System.out.println(e);}
}

}