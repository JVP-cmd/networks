import java.net.*;
import java.io.*;
import java.util.*;

public class ClientMain {

    public static void main(String[] args) {

        Scanner inp = new Scanner(System.in);

        try {
            Client thisClient=new Client("apples","bannnas");
 Socket socket = new Socket("localhost", 59090);
    DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
            DataInputStream din = new DataInputStream(socket.getInputStream());
           Scanner output=new Scanner(socket.getInputStream());
            Scanner scinput=new Scanner(System.in);
            System.out.println("Enter your username");
            boolean correctUser=false;
            while(correctUser==false){
                String username=scinput.nextLine();
                
            
            dout.writeUTF(username);
            correctUser=din.readBoolean();
            if(!correctUser){System.out.println("Uknown username, please try again");}}
            String accessLevel;
            System.out.println("Please enter your access level<Public/Admin>");
            accessLevel=scinput.nextLine();
            boolean loop=false;//boolean used to keep the loop going until the user puts the correct user input in.
            while(!loop){
            if(!(accessLevel.equals("Public")||accessLevel.equals("Admin"))){
            System.out.println("incorrect user input,please try again");
            accessLevel=scinput.nextLine();}
            loop=true;}
            dout.writeUTF(accessLevel);
               boolean pas=false;
            if(accessLevel.equals("Admin")){
                //checks the admins password with the textfile stored on server
            System.out.println("Please enter in your password");
         
            while(!pas){
            String password=scinput.nextLine();
            dout.writeUTF(password);
            pas=din.readBoolean();
            
            if(!pas){
                System.out.println("Incorrect password, try again");}}}
            
            
          
            

String input=" ";
            while(!input.equals("Stop")){ 
               
            //prompts of user action sent from the Server class
            System.out.println("Select an action: ");
          
            System.out.println("<View> - To view files on server");
            System.out.println("<Download> - To download a file");
            System.out.println("<Upload> - To upload a file");
            if(pas){System.out.println("<Permission> Set Permission");}
            System.out.println("<Stop> to end the connection betweeen server and client");
            
                input = inp.nextLine();
            
         
            dout.writeUTF(input);

            if (input.equals("Download")) {
                
                System.out.println("Please enter the name of the file to be downloaded");
                dout.writeUTF(inp.nextLine());
                thisClient.DownloadtoClient(socket);
            } 
            else if (input.equals("Upload")) {
                System.out.println("Enter the name of the file to be uploaded");
                String filename = inp.nextLine();
                thisClient.uploadFile(filename, socket);
            }
            else if (input.equals("View")&&pas==true){
            dout.writeUTF("Admin View");
            System.out.println(output.nextLine());
            System.out.println(output.nextLine());}
            else if(input.equals("View")&&pas==false){
            dout.writeUTF("Public View");
            System.out.println(output.nextLine());}
            else if (pas==true&&input.equals("Permission")){
                
            System.out.println("Would you like to move a file to <Public/Private>");
            String moveFileTo=scinput.nextLine();
            dout.writeUTF(moveFileTo);
            System.out.println("Enter the name of the file");
            String filename=scinput.nextLine();
            dout.writeUTF(filename);
            System.out.println("Moved file");}
                
              }

        

            /*Scanner serverin = new Scanner(socket.getInputStream());
				System.out.println(serverin.nextLine());*/
        } catch (Exception e) {
            System.out.println("Error:  " + e.getMessage());
            System.exit(0);
        }
    }

}