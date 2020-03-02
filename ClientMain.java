
import java.net.*;
import java.io.*;
import java.util.*;

public class ClientMain {

    public static void main(String[] args) {

        Scanner inp = new Scanner(System.in);

        try {
            Client thisClient=new Client("apples","bannnas");

            // Login procedure here
          /*  String ip = InetAddress.getLocalHost().getHostAddress();
            String machineName = InetAddress.getLocalHost().getHostName();
            System.out.println("Current ip address is: " + ip);
            thisClient = new Client(ip, machineName);
            System.out.println("Current client details:\nClient Name: " + thisClient.getClientName() + "\nClient IP: " + thisClient.getIpAddress());
            Socket socket = new Socket("196.42.109.119", 59090);

            String yeet = "Client " + Client.numClients + " says Hello";*/
          
String input=" ";
            while(!input.equals("Stop")){ 
                Socket socket = new Socket("196.42.109.119", 59090);
            Scanner inputs = new Scanner(socket.getInputStream());//prompts of user action sent from the Server class
            System.out.println(inputs.nextLine());
            System.out.println(inputs.nextLine());
            System.out.println(inputs.nextLine());
            System.out.println(inputs.nextLine());
            System.out.println(inputs.nextLine());
            System.out.println("<Stop> to end the connection betweeen server and client");
                input = inp.nextLine();
            
            DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
            DataInputStream din = new DataInputStream(socket.getInputStream());
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
                
              }

        

            /*Scanner serverin = new Scanner(socket.getInputStream());
				System.out.println(serverin.nextLine());*/
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(0);
        }
    }

}
