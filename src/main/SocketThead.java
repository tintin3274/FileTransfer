package main;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketThead implements Runnable {
    private Thread worker;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean sending = new AtomicBoolean(false);
    private final int interval = 1000;

    private static String RECEIVE_DIRECTORY = "."+ File.separator+"Receive File"+File.separator;
    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private String paths;

    public void start() {
        worker = new Thread(this);
        worker.start();
    }

    public void stop() {
        running.set(false);
    }

    public synchronized void stopConnection() {
        try {
            dataOutputStream.writeInt(-1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendFiles(String paths) {
        this.paths = paths;
        sending.set(true);
    }

    @Override
    public void run() {
        running.set(true);
        while (running.get()) {
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
                System.out.println("Thread was interrupted, Failed to complete operation");
            }
            try {
                if(sending.get()) {
                    sendFiles();
                    sending.set(false);
                }
                else if(!socket.isClosed() && dataInputStream.available() > 0) {
                    int amountFiles = dataInputStream.readInt();
                    if(amountFiles == -1) Controller.closeConnection();
                    else receiveFiles(amountFiles);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public SocketThead(Socket socket) throws IOException {
        this.socket = socket;
        this.dataInputStream = new DataInputStream(socket.getInputStream());
        this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
    }


    private synchronized void sendFile(File file) throws Exception{
        int bytes = 0;
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

        String message = dataInputStream.readUTF();
        System.out.println(message);
        updateLog(message);
    }

    private synchronized void receiveFile() throws Exception{
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

        String message = "Complete Send: "+fileName;
        dataOutputStream.writeUTF(message);

        message = "Received: "+fileName;
        System.out.println(message);
        updateLog(message);
    }

    private void sendFiles() {
        String[] pathsSplit = paths.split(", ");

        List<File> files = new ArrayList<>();
        for(String path : pathsSplit) {
            File file = new File(path);
            if(file.exists()) files.add(file);
        }
        try {
            dataOutputStream.writeInt(files.size());
            for(File file : files) {
                sendFile(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
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

    private void updateLog(String text) {
        Controller.TEXT_LOG.appendText(text+"\n");
    }
}
