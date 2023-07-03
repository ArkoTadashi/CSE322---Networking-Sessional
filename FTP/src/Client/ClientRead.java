package src.Client;

import java.io.DataInputStream;
import java.net.Socket;

import src.Upload.USendFile;
import src.Download.DReceiveFile;

public class ClientRead extends Thread {

    Socket socket1;
    Socket socket2;
    String username;

    public ClientRead(String username, Socket socket1, Socket socket2) {
        this.socket1 = socket1;
        this.socket2 = socket2;
        this.username = username;
    }

    private void read() {
        try {
            DataInputStream in = new DataInputStream(socket1.getInputStream());
            String msg;

            while (true) {
                msg = in.readUTF();
                
                if (msg.equalsIgnoreCase("Upload")) {
                    Boolean up = in.readBoolean();
                    if (up) {
                        System.out.println("Uploading");
                        int chunk = in.readInt();
                        Long fID = in.readLong();
                        String fileName = in.readUTF();
                        String fileLocation = "./Desktop/"+username+"/"+fileName;
                        USendFile sendFile = new USendFile(socket2, fileLocation, chunk, fID);
                        sendFile.start();
                        //sendFile.join();
                    }
                }
                else if (msg.equalsIgnoreCase("Download")) {
                    String fileName = in.readUTF();
                    int chunk = in.readInt();
                    byte[] buffer = new byte[chunk];
                    String fileLocation = "./Desktop/"+username+"/"+fileName;
                    DReceiveFile receiveFile = new DReceiveFile(socket2, fileLocation, buffer);
                    receiveFile.start();
                    //receiveFile.join();
                }
                else {
                    System.out.println(msg);
                }


            }

        } catch (Exception e) {
            
        }
    }

    @Override
    public void run() {
        read();
    }
    
}