package main;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.io.*;
import java.net.*;

public class Controller {
    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    private static String RECEIVE_DIRECTORY = "."+ File.separator+"Receive File"+File.separator;
    private static int PORT = 5000;

    private boolean serverRunning;
    private boolean clientRunning;
    private static Socket clientSocket;

    static ServerSocket serverSocket;
    static Socket socket;


    @FXML TextArea textPath, logSend, logReceive;
    @FXML TextField textName;
    @FXML Label labelIp, labelStatus;
    @FXML Button browseBtn, sendBtn, receiveBtn;


    @FXML
    public void initialize() {
        try(DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            labelIp.setText(socket.getLocalAddress().getHostAddress());
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
        }

        serverRunning = false;
        clientRunning = false;
        labelStatus.setText("Ready to Send or Receive");
        System.out.println("Ready to Send or Receive");
    }

    public void Receive() {
        if(!serverRunning){
            openServer();
            if(serverRunning) {
                try {
                    int amountFiles = dataInputStream.readInt();
                    receiveFiles(amountFiles);
                    closeConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void send() {
        if(!clientRunning) {
            connectSever();
            if(clientRunning) {
                sendFiles();
                closeConnection();
            }
        }
    }


    public void openServer() {
        try{
            serverSocket = new ServerSocket(PORT);
            System.out.println("===== Running Server =====");
            System.out.println("<Server> Waiting Client Connect");

            System.out.println("<Server> listening to port:"+PORT);
            serverSocket.setSoTimeout(10000);
            clientSocket = serverSocket.accept();

            System.out.println("<Server> "+clientSocket+" connected.");
            dataInputStream = new DataInputStream(clientSocket.getInputStream());
            dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());

            serverRunning = true;
        }
        catch (SocketTimeoutException e){
            labelStatus.setText("Server Accept timed out");
            System.out.println("<Server> Accept timed out");
            System.out.println("===== Closed Server =====");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void connectSever() {
        String serverIP = textName.getText().toLowerCase().trim();
        try {
            socket = new Socket(serverIP,PORT);
            System.out.println("===== Connected to Server =====");
            //labelStatus.setText("Connected to Server");
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            clientRunning = true;
        }
        catch (ConnectException e){
            labelStatus.setText("Connection refused");
            System.out.println("!!!!! Connection refused !!!!!");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


    public void closeConnection() {
        try {
            dataInputStream.close();
            dataOutputStream.close();
            if(serverRunning) {
                serverSocket.close();
                clientSocket.close();
                serverRunning = false;
                System.out.println("===== Closed Server =====");
            }
            if(clientRunning) {
                clientRunning = false;
                System.out.println("===== Disconnected from Server =====");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        labelStatus.setText("Ready to Send or Receive");
        System.out.println("Ready to Send or Receive");
    }


    private void sendFile(String path) throws Exception{
        int bytes = 0;
        File file = new File(path);
        FileInputStream fileInputStream = new FileInputStream(file);


        // send file name
        dataOutputStream.writeUTF(file.getName());

        // send file size
        dataOutputStream.writeLong(file.length());
        // break file into chunks
        byte[] buffer = new byte[4*1024];
        while ((bytes=fileInputStream.read(buffer))!=-1){
            dataOutputStream.write(buffer,0,bytes);
            dataOutputStream.flush();
        }
        fileInputStream.close();

        logSend.setText(file.getName()+"\n"+logSend.getText());
        System.out.println("<Client> Complete Send: "+file.getName());
    }

    private void receiveFile() throws Exception{
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

        logReceive.setText(fileName+"\n"+logReceive.getText());
        System.out.println("<Server> Complete Receive: "+fileName);
    }

    public void sendFiles() {
        String paths = textPath.getText().trim();
        String[] pathsSplit = paths.split(", ");

        try {
            dataOutputStream.writeInt(pathsSplit.length);

            for(String path : pathsSplit) {
                try {
                    sendFile(path);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveFiles(int amountFiles) {
        while (amountFiles > 0) {
            try {
                receiveFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
            amountFiles--;
        }
    }

    public void copyIP() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(labelIp.getText());
        clipboard.setContent(content);
    }
}
