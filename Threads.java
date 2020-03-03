import javax.imageio.IIOException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Threads extends Thread{

    private Socket s;
    private ServerSocket ss;
    private String username, filename;

    public Threads(Socket s, String filename, String username){
        this.s = s;
        this.ss = ss;
        this.filename = filename;
        this.username = username;
    }

    public void run() {
        while(true)
        {
            //create server socket on port 5000

            System.out.println("Connected With " + s.getInetAddress().toString());
            DataInputStream din = null;
            try {
                din = new DataInputStream(s.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            DataOutputStream dout = null;
            try {
                dout = new DataOutputStream(s.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            try{
                String str="";

                str=din.readUTF();
                System.out.println("SendGet....Ok");

                if(!str.equals("stop")){

                    System.out.println("Sending File: "+filename);
                    dout.writeUTF(filename);
                    dout.flush();

                    File f=new File("./Server/".concat(username), filename);
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
            try {
                din.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                ss.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
