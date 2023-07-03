package src.Client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ClientWrite extends Thread {
    
    Socket socket1;
    Socket socket2;
    String username;

    public ClientWrite(String username, Socket socket1, Socket socket2) {
        this.username = username;
        this.socket1 = socket1;
        this.socket2 = socket2;
    }

    private void write() {
        try {
            DataOutputStream out = new DataOutputStream(socket1.getOutputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                System.out.println("You can do the following: \n 1. Check Client List \n 2. Check your files \n 3. Check others' files \n 4. Upload a file \n 5. Download a file \n 6. Request a file \n 7. Check Upload Request List \n 8. Check Inbox \n 9. Clear Inbox \n");
                
                String o = br.readLine();
                out.writeUTF(o);

                if (o.equalsIgnoreCase("4")) {
                    Boolean priv = false;
                    System.out.print("Upload privately?(Y/N): ");
                    String p = br.readLine();
                    if (p.equalsIgnoreCase("Y")) {
                        priv = true;
                    }
                    String fileName;
                    System.out.print("File to upload: ");
                    fileName = br.readLine();
                    int fileSize = (int)Files.size(Paths.get("./Desktop/"+username+"/"+fileName));
                    out.writeBoolean(priv);
                    out.writeInt(fileSize);
                    out.writeUTF(fileName);
                }
                if (o.equalsIgnoreCase("5")) {
                    Long fileID;
                    System.out.print("File to download(ID): ");
                    fileID = Long.parseLong(br.readLine());
                    out.writeLong(fileID);
                }
                if (o.equalsIgnoreCase("6")) {
                    String fileName;
                    String description;
                    System.out.print("Requesting File: ");
                    fileName = br.readLine();
                    System.out.print("Description: ");
                    description = br.readLine();
                    out.writeUTF(fileName);
                    out.writeUTF(description);
                }
            }


        } catch (Exception e) {

        }
    }

    @Override
    public void run() {
        write();
    }
}
