package src.Client;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Client {

    final String username;

    public Client(String username) {
        this.username = username;
    }

    private void start() {
        try {
            Socket socket1 = new Socket("localhost", 5050);
            Socket socket2 = new Socket("localhost", 6060);

            DataOutputStream out = new DataOutputStream(socket1.getOutputStream());
            DataInputStream in = new DataInputStream(socket1.getInputStream());


            out.writeUTF(username);
            Boolean confirm = in.readBoolean();
            String msg = in.readUTF();
            System.out.println(msg);

            if (confirm) {
                File dir = new File("./Desktop/" + username);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                ClientRead clientRead = new ClientRead(username, socket1, socket2);
                clientRead.start();
                ClientWrite clientWrite = new ClientWrite(username, socket1, socket2);
                clientWrite.start();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        

    }



    public static void main(String[] args) {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        try {
            System.out.print("Username: ");
            String username = br.readLine();
            Client client = new Client(username);

            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
}
