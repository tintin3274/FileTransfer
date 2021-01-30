package main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    private static String RECEIVE_DIRECTORY = "."+File.separator+"Receive File"+File.separator;


    public static void main(String[] args) {
        try(ServerSocket serverSocket = new ServerSocket(5000)){
            System.out.println("listening to port:5000");
            serverSocket.setSoTimeout(10000);
            Socket clientSocket = serverSocket.accept();


            System.out.println(clientSocket+" connected.");
            dataInputStream = new DataInputStream(clientSocket.getInputStream());
            dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());


            receiveFile();
            receiveFile();
            receiveFile();


            dataInputStream.close();
            dataOutputStream.close();
            clientSocket.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void receiveFile() throws Exception{
        int bytes = 0;

        File directory = new File(RECEIVE_DIRECTORY);   // create directory
        directory.mkdir();

        String fileName = dataInputStream.readUTF();    // read file name
        FileOutputStream fileOutputStream = new FileOutputStream(RECEIVE_DIRECTORY+fileName);


        long size = dataInputStream.readLong();     // read file size
        byte[] buffer = new byte[4*1024];
        while (size > 0 && (bytes = dataInputStream.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
            fileOutputStream.write(buffer,0,bytes);
            size -= bytes;      // read upto file size
        }
        fileOutputStream.close();
        dataOutputStream.writeUTF("Complete: "+fileName);
    }
}
