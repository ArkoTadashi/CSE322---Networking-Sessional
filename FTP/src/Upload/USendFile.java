package src.Upload;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class USendFile extends Thread {

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private String fileLocation;
    private Long fID;
    private int chunk;
    private String msg;

    public USendFile(Socket socket, String fileLocation, int chunk, Long fID) {
        this.socket = socket;
        try {
            this.out = new DataOutputStream(socket.getOutputStream());
            this.in = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.fileLocation = fileLocation;
        this.chunk = chunk;
        this.fID = fID;
    }

    public void send() {
        try {
            File dir = new File(fileLocation);
            FileInputStream inp = new FileInputStream(dir);
            
            byte[] buffer = new byte[chunk];
            int count = 0;
            Boolean ack = true;
            socket.setSoTimeout(30000);

            try {
                while (ack) {
                    count = inp.read(buffer);
                    out.writeInt(count);
                    if (count < 0) break;
                    out.write(buffer, 0, count);
                    ack = in.readBoolean();
                }
            }  catch (SocketTimeoutException e) {
                System.out.println("Upload Timeout");
            }

            if (count == -1) {
                out.writeUTF("Success");
            }
            else {
                out.writeUTF("Fail");
            }

            msg = in.readUTF();
            System.out.println(msg);

            inp.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    @Override
    public void run() {
        send();
    }
    
}
