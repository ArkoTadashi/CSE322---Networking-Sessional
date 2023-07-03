package src.Download;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;


public class DReceiveFile extends Thread {

    private Socket socket;
    private DataInputStream in;
    private String fileLocation;
    private byte[] buffer;

    public DReceiveFile(Socket socket, String fileLocation, byte[] buffer) {
        this.socket = socket;
        try {
            this.in = new DataInputStream(this.socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.fileLocation = fileLocation;
        this.buffer = buffer;
    }

    public void receive() {
        try {
            File dir = new File(fileLocation);
            FileOutputStream otp = new FileOutputStream(dir);
            int count = 0;
            ////////////////
            
            while(true) {
                count = in.readInt();
                if (count < 0) break;
                in.read(buffer, 0, count);
                otp.write(buffer, 0, count);
            }
            String status = in.readUTF();
            if (status.equalsIgnoreCase("Success")) {
                System.out.println("Download Done");
            }
            else {
                System.out.println("Download Failed");
            }
        
            otp.close();
        } catch (IOException e) {
            e.printStackTrace();
        } 
    }

    @Override
    public void run() {
        receive();
    }
    
}
