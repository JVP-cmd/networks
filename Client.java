import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;



public class Client {

	public static int numClients = 0;
	private String ipAddress;
	private String clientName;


	public Client(String ipAddress, String clientName){
		numClients++;
		this.ipAddress = ipAddress;
		this.clientName = clientName;
	}


	public String getIpAddress(){
		return ipAddress;
	}

	public String getClientName(){
		return clientName;
	}
                public void uploadFile(String filename, Socket s){
            while(true){
                try{
 DataInputStream din=new DataInputStream(s.getInputStream());
            DataOutputStream dout=new DataOutputStream(s.getOutputStream());
            
            String Data;
            Data=din.readUTF();
            while(!Data.equals("stop")){
            System.out.println("Uploading File: "+filename);
            dout.writeUTF(filename);
            dout.flush();
            File file=new File(".\\Client", filename);
            FileInputStream fileIn=new FileInputStream(file);
            long sz=(int)file.length();
            byte b[]=new byte[1024];
            int read;
            dout.writeUTF("stop");
            dout.writeUTF(Long.toString(sz));
            dout.flush();
            while((read=fileIn.read(b))!=1){
            dout.write(b,0,read);
            dout.flush();}
            fileIn.close();
             }
           
            dout.flush();
             din.close();
            //s.close();
            }catch(ArrayIndexOutOfBoundsException e){System.out.println("Upload complete");break;}catch(EOFException e){System.out.println("Upload complete");break;}catch(Exception e){System.out.println(e);break;}
           
            
            }}
                public void DownloadtoClient( Socket s){
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
}    public static void move(String file){
        File f = new File(file);
        File makePrivate = new File(".\\Client\\" + file);
        try{
            Files.copy(f.toPath(), makePrivate.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }catch(IOException e){}
        f.delete();
    }
}