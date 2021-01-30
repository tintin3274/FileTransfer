package main;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Controller {
    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    private static String RECEIVE_DIRECTORY = "."+ File.separator+"Receive File"+File.separator;
    private static int PORT = 5000;

    private static boolean serverRunning;
    private static boolean clientRunning;
    private static Socket clientSocket;

    private static Thread thread;
    private static AtomicBoolean threadRunning = new AtomicBoolean(false);
    private static AtomicBoolean waitingCommand = new AtomicBoolean(false);

    @FXML TextField textName, textPath;
    @FXML Label labelIp, labelStatus, labelSend, labelReceive;
    @FXML Button connectToBtn, disconnectToBtn, connectStandBtn, disconnectStandBtn, browseBtn, sendBtn;


    @FXML
    public void initialize() {
        try(DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            labelIp.setText(socket.getLocalAddress().getHostAddress());
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
        }

        labelStatus.setText("READY TO: ConnectServer or StandbyServer");


        serverRunning = false;
        clientRunning = false;

        disconnectToBtn.setDisable(true);
        disconnectStandBtn.setDisable(true);
        browseBtn.setDisable(true);
        sendBtn.setDisable(true);
    }

    private void createThread() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (threadRunning.get()) {
                    try {
                        if(dataInputStream.available() > 0 && waitingCommand.get() == true) {
                            int amountFiles = dataInputStream.readInt();
                            receiveFiles(amountFiles);
                            waitingCommand.set(false);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        threadRunning.set(true);
        waitingCommand.set(true);
        thread = new Thread(runnable);
        thread.start();
    }

    public void openServer() {
        connectToBtn.setDisable(true);
        connectStandBtn.setDisable(true);

        System.out.println("Waiting Client Connect");
        //labelStatus.setText("Waiting Client Connect");

        try(ServerSocket serverSocket = new ServerSocket(PORT)){
            System.out.println("listening to port:"+PORT);
            serverSocket.setSoTimeout(10000);
            clientSocket = serverSocket.accept();

            System.out.println(clientSocket+" connected.");
            dataInputStream = new DataInputStream(clientSocket.getInputStream());
            dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
            createThread();
            serverRunning = true;
        } catch (Exception e){
            e.printStackTrace();
        }

        if(serverRunning) {
            disconnectStandBtn.setDisable(false);
            browseBtn.setDisable(false);
            sendBtn.setDisable(false);
        }
        else {
            System.out.println("Timeout Waiting");
            labelStatus.setText("Timeout Waiting");
            connectToBtn.setDisable(false);
            connectStandBtn.setDisable(false);
        }
    }

    public void connectSever() {
        connectToBtn.setDisable(true);
        connectStandBtn.setDisable(true);

        System.out.println("Connecting to Server");
        //labelStatus.setText("Connecting to Server");

        String serverIP = textName.getText();
        try(Socket socket = new Socket(serverIP,PORT)) {
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            createThread();
            clientRunning = true;
        }catch (Exception e){
            e.printStackTrace();
        }

        if(clientRunning) {
            disconnectToBtn.setDisable(false);
            browseBtn.setDisable(false);
            sendBtn.setDisable(false);
        }
        else {
            System.out.println("Connected Fail");
            labelStatus.setText("Connected Fail");
            connectToBtn.setDisable(false);
            connectStandBtn.setDisable(false);
        }
    }

    public void closeConnection() {
        try {
            dataInputStream.close();
            dataOutputStream.close();
            threadRunning.set(false);
            if(serverRunning) {
                clientSocket.close();
                serverRunning = false;
            }
            if(clientRunning) {
                clientRunning = false;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
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
        System.out.println(dataInputStream.readUTF());
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
        dataOutputStream.writeUTF("Complete: "+fileName);
    }

    public void sendFiles() {
        String paths = textPath.getText();
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
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            amountFiles--;
        }
        waitingCommand.set(true);
    }
}
