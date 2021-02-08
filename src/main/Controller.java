package main;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Controller {
    private static int PORT = 5000;

    private static boolean serverRunning;
    private static boolean clientRunning;

    private static ServerSocket serverSocket;
    private static Socket socket;
    private static SocketThead socketThead;

    public static TextArea TEXT_LOG;
    private static Button OPEN_CONNECTION_BTN, CONNECT_BTN, DISCONNECT_BTN;

    @FXML TextArea textPath, textLog;
    @FXML TextField textName;
    @FXML Label labelIp;
    @FXML Button openConnectionBtn, connectBtn, disconnectBtn, browseBtn, sendBtn, clearBtn;


    @FXML
    public void initialize() throws IOException {
        try(DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            labelIp.setText(socket.getLocalAddress().getHostAddress());
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
        }

        TEXT_LOG = textLog;
        OPEN_CONNECTION_BTN = openConnectionBtn;
        CONNECT_BTN = connectBtn;
        DISCONNECT_BTN = disconnectBtn;

        serverRunning = false;
        clientRunning = false;
        disconnectBtn.setDisable(true);
    }

    public void send() {
        if(socketThead != null) socketThead.sendFiles(textPath.getText());
    }

    public void openServer() {
        try{
            System.out.println("Waiting connect listening to port:"+PORT);
            updateLog("Waiting connect listening to port:"+PORT);

            serverSocket = new ServerSocket(PORT);
            serverRunning = true;
            serverSocket.setSoTimeout(10000);
            socket = serverSocket.accept();

            System.out.println(socket+" connected");
            updateLog(socket+" connected");

            System.out.println("=== Ready to Send or Receive Files ===");
            updateLog("=== Ready to Send or Receive Files ===");

            socketThead = new SocketThead(socket);
            socketThead.start();
            lockButtonConnect();
        }
        catch (SocketTimeoutException e){
            System.out.println("Accept timed out");
            updateLog("Accept timed out");
            closeConnection();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void connectSever() {
        String serverIP = textName.getText().toLowerCase().trim();
        try {
            System.out.println("Connecting to "+serverIP+" port:"+PORT);
            updateLog("Connecting to "+serverIP+" port:"+PORT);

            socket = new Socket(serverIP,PORT);
            clientRunning = true;

            System.out.println(socket+" connected");
            updateLog(socket+" connected");

            System.out.println("=== Ready to Send or Receive Files ===");
            updateLog("=== Ready to Send or Receive Files ===");

            socketThead = new SocketThead(socket);
            socketThead.start();
            lockButtonConnect();
        }
        catch (ConnectException e){
            System.out.println("Connection refused");
            updateLog("Connection refused");
            e.printStackTrace();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void closeConnectionClk() {
        socketThead.stopConnection();
        closeConnection();
    }

    public static void closeConnection() {
        try {
            if(socketThead != null) {
                socketThead.stop();
                System.out.println("=== Closed Connection ===");
                updateLog("=== Closed Connection ===");
            }
            if(serverRunning) {
                serverSocket.close();
                serverRunning = false;
            }
            if(clientRunning) {
                socket.close();
                clientRunning = false;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        unlockButtonConnect();
    }

    public void copyIP() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(labelIp.getText());
        clipboard.setContent(content);
    }

    public void browseFile() {
        FileChooser fileChooser = new FileChooser();
        List<File> chooseFile = fileChooser.showOpenMultipleDialog(new Stage());
        if(chooseFile != null) {
            List<String> path= new ArrayList<>();
            for(File c:chooseFile){
                path.add(c.getAbsolutePath());
            }
            String allPath=String.join(", ",path);
            if(textPath.getText().isEmpty()){
                textPath.setText(allPath);
            }
            else{
                textPath.setText(textPath.getText()+", "+allPath);
            }
        }
    }

    public void clear() {
        textPath.setText("");
    }

    private static void updateLog(String text) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                TEXT_LOG.appendText(text+"\n");
            }
        });
    }

    private void lockButtonConnect() {
        CONNECT_BTN.setDisable(true);
        OPEN_CONNECTION_BTN.setDisable(true);
        DISCONNECT_BTN.setDisable(false);
    }

    private static void unlockButtonConnect() {
        CONNECT_BTN.setDisable(false);
        OPEN_CONNECTION_BTN.setDisable(false);
        DISCONNECT_BTN.setDisable(true);
    }
}
