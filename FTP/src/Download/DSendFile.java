package src.Download;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;

public class DSendFile extends Thread {

    private Socket socket;
    private DataOutputStream out;
    private String fileLocation;
    private int chunk;

    public DSendFile(Socket socket, String fileLocation, int chunk) {
        this.socket = socket;
        try {
            this.out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.fileLocation = fileLocation;
        this.chunk = chunk;
    }

    public void send() {
        try {
            File dir = new File(fileLocation);
            FileInputStream inp = new FileInputStream(dir);
            byte[] buffer = new byte[chunk];
            int count = 0;

            try {
                while (true) {
                    count = inp.read(buffer);
                    out.writeInt(count);
                    if (count < 0) break;
                    out.write(buffer, 0, count);
                }
            }  catch (IOException e) {
                System.out.println("Download error for ");
            }

            if (count == -1) {
                out.writeUTF("Success");
            }
            else {
                out.writeUTF("Fail");
            }


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
