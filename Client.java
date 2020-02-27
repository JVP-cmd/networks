
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;



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
            if(!Data.equals("stop")){
            System.out.println("Uploading File: "+filename);
            dout.writeUTF(filename);
            dout.flush();
            File file=new File(filename);
            FileInputStream fileIn=new FileInputStream(file);
            long sz=(int)file.length();
            byte b[]=new byte[1024];
            int read;
            dout.writeUTF(Long.toString(sz));
            dout.flush();
            while((read=fileIn.read(b))!=1){
            dout.write(b,0,read);
            dout.flush();}
            fileIn.close();
            dout.flush();}
            dout.writeUTF("stop");
            dout.flush();
             din.close();
            //s.close();
            }catch(Exception e){System.out.println(e);}
           
            
            }}
                public void DownloadtoClient( Socket s){
                    try{
                DataInputStream din=new DataInputStream(s.getInputStream());
        DataOutputStream dout=new DataOutputStream(s.getOutputStream());
        BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
        String str=""; String filename="";
                    while(!str.equals("stop")){
                    str=br.readLine();
                    dout.writeUTF(str);
                    dout.flush();
                    filename=din.readUTF();
                    long sz=Long.parseLong(din.readUTF());
                     byte b[]=new byte [1024];
                    FileOutputStream fos=new FileOutputStream(new File(filename),true);
                    long bytesRead;
                    do{
                    bytesRead=din.read(b,0,b.length);
                    fos.write(b,0,b.length);}
                    while(!(bytesRead<1024));{
                     fos.close();
                     dout.close();
                     s.close();}}}
                    catch(Exception e){System.out.println();}
}
}