package src.Upload;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

import src.Server.Server;
import src.Server.User;

public class UReceiveFile extends Thread {

    private Socket socket;
    private int fSize;
    private Long fileID;
    private String fileName;
    private DataOutputStream out;
    private DataInputStream in;
    private int currentBufferSize;
    private String fileLocation;
    private String clientName;
    private Boolean priv;
    

    public UReceiveFile(Socket socket, String clientName, String fileName, String fileLocation, Boolean priv, int fSize, int currentBufferSize) {
        this.socket = socket;
        try {
            this.out = new DataOutputStream(this.socket.getOutputStream());
            this.in = new DataInputStream(this.socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.fileName = fileName;
        this.fileLocation = fileLocation;
        this.fSize = fSize;
        this.currentBufferSize = currentBufferSize;
        this.priv = priv;
        this.clientName = clientName;
    }

    public void receive() {
        int bufferStart = currentBufferSize;
        try {
            File dir = new File(fileLocation);
            FileOutputStream otp = new FileOutputStream(dir);
            int count = 0;
            ////////////////
            
            while(true) {
                count = in.readInt();
                if (count < 0) break;
                in.read(Server.BUFFER, currentBufferSize, count);
                out.writeBoolean(true);
                otp.write(Server.BUFFER, currentBufferSize, count);
                currentBufferSize += count;
            }
            String status = in.readUTF();
            if (status.equalsIgnoreCase("Success")) {
                if (fSize == currentBufferSize-bufferStart) {
                    out.writeUTF("Upload Done\n");
                    if (Server.REQUEST_ID.containsKey(fileName)) {
                        Long reqID = Server.REQUEST_ID.get(fileName);
                        List<String> reqList = Server.REQUEST_USER.get(reqID);
                        for (String client : reqList) {
                            Server.USER_LIST.get(client).addInbox(fileName + " uploaded by " + clientName + " with FileID: " + fileID);
                        }
                    }

                    Server.FILES_LOCATION.put(Server.fileID, fileLocation);
                    Server.FILES_ID.put(fileLocation, Server.fileID);
                    Server.FILES_NAME.put(Server.fileID, fileName);
                    if (!priv) {
                        Server.PUBLIC_FILE_ID.add(fileID);
                    }

                    User user = Server.USER_LIST.get(clientName);
                    user.addFileID(fileID);
                }
                else {
                    out.writeUTF("Upload Failed, chunk sizes didn't add up\n");
                    dir.delete();
                }
            }
            else {
                out.writeUTF("Upload Failed\n");
                dir.delete();
            }
            
            otp.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Server.fileID++;
            for (int i = bufferStart+fSize+1; i < Server.MAX_BUFFER_SIZE; i++) {
                Server.BUFFER[i-fSize] = Server.BUFFER[i];
            }
            Server.bufferSize -= fSize;
        } 
    }

    @Override
    public void run() {
        receive();
    }
    
}
