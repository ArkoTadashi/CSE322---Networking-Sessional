package src.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import src.Download.DSendFile;
import src.Upload.UReceiveFile;


public class Server {

    public static HashMap<String, User> USER_LIST;
    HashMap<User, Socket> CLIENT_LIST;
    public static final int MAX_BUFFER_SIZE = 1000000, MIN_CHUNK_SIZE = 100, MAX_CHUNK_SIZE = 100000;
    public volatile static int bufferSize;
    int currentBufferSize;
    public static byte[] BUFFER;
    ServerSocket serverSocket1, serverSocket2;
    public static HashMap<Long, String> FILES_LOCATION, FILES_NAME;
    public static HashMap<Long, List<String>> REQUEST_USER;
    public static HashMap<String, Long> FILES_ID, REQUEST_ID;
    volatile Long fileID, requestID;

    public Server() {
        CLIENT_LIST = new HashMap<>();
        USER_LIST = new HashMap<>();
        FILES_LOCATION = new HashMap<>();
        FILES_NAME = new HashMap<>();
        FILES_ID = new HashMap<>();
        REQUEST_ID = new HashMap<>();
        REQUEST_USER = new HashMap<>();
        bufferSize = currentBufferSize = 0;
        fileID = requestID = 0L;
        BUFFER = new byte[MAX_BUFFER_SIZE];
    }

    private void start(int port1, int port2) {
        try {
            serverSocket1 = new ServerSocket(port1);
            serverSocket2 = new ServerSocket(port2);

            while(true) {
                System.out.println("Waiting for connection...");
                Socket socket1 = serverSocket1.accept();
                Socket socket2 = serverSocket2.accept();
                System.out.println("Connection established");

                Thread worker = new Worker(socket1, socket2);
                worker.start();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    private void close() {
        for (Map.Entry<User, Socket> v : CLIENT_LIST.entrySet()) {
            try {
                v.getValue().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            serverSocket1.close();
            serverSocket2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /// Helper Functions
    void newDirectory(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    String listClients(String msg) {
        msg += "List of clients:\n";
        for (Map.Entry<String, User> v : USER_LIST.entrySet()) {
            msg += v.getKey();
            if (CLIENT_LIST.containsKey(v.getValue())) {
                msg += " - Online";
            }
            else {
                msg += " - Offline";
            }
            msg += "\n";
        }
        msg += "\n";
        
        return msg;
    }
    String listFiles(String msg, Path dir, String username, Boolean priv) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path file: stream) {
                String fileName = "";
                fileName += file.getFileName();
                msg += fileName;
                String fileLocation;
                if (priv) {
                    fileLocation = "./Clients/" + username + "/Private/" + fileName;
                }
                else {
                    fileLocation = "./Clients/" + username + "/Public/" + fileName;
                }
                Long fileID = FILES_ID.get(fileLocation);
                msg += " - ID: ";
                msg += fileID;
                msg += "\n";
            }
            msg += "\n";
        } catch (IOException | DirectoryIteratorException e) {
            e.printStackTrace();
        }

        return msg;
    }
    void broadcast(User user, String fileName, String description) {
        DataOutputStream out;
        try {
            for (Map.Entry<User, Socket> v : CLIENT_LIST.entrySet()) {
                if (v.getKey() != user) {
                    out = new DataOutputStream(v.getValue().getOutputStream());
                    out.writeUTF(user.getUserName() + " has requested for file " + fileName + "\n" + description);
                    User to = v.getKey();
                    if (!to.getRequestedFiles().contains(fileName)) {
                        to.addRequest(fileName);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private class Worker extends Thread {
        Socket socket1, socket2;
        String clientName;
    
        public Worker(Socket socket1, Socket socket2) {
            this.socket1 = socket1;
            this.socket2 = socket2;
        }
    
        public void run() {
            try {
                DataOutputStream out = new DataOutputStream(this.socket1.getOutputStream());
                DataInputStream in = new DataInputStream(this.socket1.getInputStream());
                

                clientName = in.readUTF();
                if (USER_LIST.containsKey(clientName) && CLIENT_LIST.containsKey(USER_LIST.get(clientName))) {
                    out.writeBoolean(false);
                    out.writeUTF("User already logged in. Connection is closing.\n");
                    this.socket1.close();
                    this.socket2.close();
                }
                else {
                    out.writeBoolean(true);
                    User user;
                    if (USER_LIST.containsKey(clientName)) {
                        user = USER_LIST.get(clientName);
                    }
                    else {
                        user = new User(clientName);
                        USER_LIST.put(clientName, user);
                    }

                    CLIENT_LIST.put(user, socket1); 

                    File dir = new File("./Clients/" + clientName);
                    newDirectory(dir);
                    dir = new File("./Clients/" + clientName + "/Private");
                    newDirectory(dir);
                    dir = new File("./Clients/" + clientName + "/Public");
                    newDirectory(dir);

                    out.writeUTF("Connection Established. Your personal folder is named " + clientName + "\n");
    


                    while (true) {
                        String rd = in.readUTF();
                        System.out.println(rd);
                        String msg = "";

                        // LIST OF CLIENTS
                        if (rd.equalsIgnoreCase("1")) {
                            msg = listClients(msg);

                            out.writeUTF(msg);
                        }
                        // LIST OF PERSONAL FILES
                        if (rd.equalsIgnoreCase("2")) {
                            msg = "Private Files\n";
                            Path diri = Paths.get("./Clients/" + clientName + "/Private");
                            msg = listFiles(msg, diri, clientName, true);

                            msg += "Public Files\n";
                            diri = Paths.get("./Clients/" + clientName + "/Public");
                            msg = listFiles(msg, diri, clientName, false);

                            out.writeUTF(msg);

                        }
                        // LIST PUBLIC FILES OF OTHERS
                        if (rd.equalsIgnoreCase("3")) {
                            for (Map.Entry<String, User> v : USER_LIST.entrySet()) {
                                String username = v.getKey();
                                if (!username.equalsIgnoreCase(clientName)) {
                                    msg += "User: " + username + "\n";
                                    Path diri = Paths.get("./Clients/" + username + "/Public");
                                    msg = listFiles(msg, diri, username, false);
                                }
                            }
                            out.writeUTF(msg);
                        }
                        // UPLOADING A FILE
                        if (rd.equalsIgnoreCase("4")) {
                            Boolean priv = in.readBoolean();
                            int fileSize = in.readInt();
                            String fileName = in.readUTF();
                            out.writeUTF("Upload");
                            if (fileSize + bufferSize > MAX_BUFFER_SIZE) {
                                msg += "Cannot upload right now, please try again later\n";
                                out.writeBoolean(false);
                                out.writeUTF(msg);
                            }
                            else {
                                bufferSize += fileSize;
                            
                                Random random = new Random();
                                int size = random.nextInt(MAX_CHUNK_SIZE-MIN_CHUNK_SIZE) + MIN_CHUNK_SIZE;
                                out.writeBoolean(true);

                                out.writeInt(size);
                                out.writeLong(fileID);
                                out.writeUTF(fileName);
                                String fileLocation;
                                if (priv) {
                                    fileLocation = "./Clients/" + clientName + "/Private/" + fileName;
                                }
                                else {
                                    fileLocation = "./Clients/" + clientName + "/Public/" + fileName;
                                }
                                FILES_LOCATION.put(fileID, fileLocation);
                                FILES_ID.put(fileLocation, fileID);
                                FILES_NAME.put(fileID, fileName);
                                fileID++;

                                UReceiveFile receiveFile = new UReceiveFile(socket2, clientName, fileName, fileLocation, fileID, fileSize, bufferSize-fileSize);
                                receiveFile.start();
                            }

                            
                        }
                        // DOWNLOADING A FILE
                        if (rd.equalsIgnoreCase("5")) {
                            Long fileID = in.readLong();
                            out.writeUTF("Download");
                            String fileName = FILES_NAME.get(fileID);
                            out.writeUTF(fileName);
                            out.writeInt(MAX_CHUNK_SIZE);

                            String fileLocation = FILES_LOCATION.get(fileID);
                            DSendFile sendFile = new DSendFile(socket2, fileLocation, MAX_CHUNK_SIZE);
                            sendFile.start();
                        }

                        if (rd.equalsIgnoreCase("6")) {
                            String fileName = in.readUTF();
                            String description = in.readUTF();
                            broadcast(user, fileName, description);
                            if (!REQUEST_ID.containsKey(fileName)) {
                                REQUEST_ID.put(fileName, requestID);
                                if (REQUEST_USER.get(requestID) == null) {
                                    REQUEST_USER.put(requestID, new ArrayList<>());
                                }
                                if (!REQUEST_USER.get(requestID).contains(clientName)) {
                                    REQUEST_USER.get(requestID).add(clientName);
                                }
                                requestID++;
                            }
                            else {
                                Long ID = REQUEST_ID.get(fileName);
                                if (!REQUEST_USER.get(ID).contains(clientName)) {
                                    REQUEST_USER.get(ID).add(clientName);
                                }
                            }
                        }

                        if (rd.equalsIgnoreCase("7")) {
                            for (String req : user.getRequestedFiles()) {
                                msg += req;
                                msg += "\n";
                            }
                            out.writeUTF(msg);
                        }

                        if (rd.equalsIgnoreCase("8")) {
                            out.writeUTF(user.getInbox());
                        }

                        if (rd.equalsIgnoreCase("9")) {
                            user.clearInbox();
                            out.writeUTF("Inbox cleared\n");
                        }
                        
                    }
                }
    
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                CLIENT_LIST.remove(USER_LIST.get(clientName));
            }
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Server server = new Server();
        server.start(5050, 6060);

        server.close();

    }
}



