package src.Server;

import java.util.ArrayList;
import java.util.List;

public class User {
    
    private String username;
    private List<String> requestedFiles;
    private String inbox;

    User(String username) {
        inbox = "";
        this.username = username;
        this.requestedFiles = new ArrayList<>();
    }

    public String getUserName() {
        return username;
    }

    public void addRequest(String name) {
        requestedFiles.add(name);
    }

    public List<String> getRequestedFiles() {
        return requestedFiles;
    }

    public void addInbox(String msg) {
        inbox += "\n";
        inbox += msg;
    }

    public String getInbox() {
        return inbox;
    }

    public void clearInbox() {
        inbox = "";
    }
}
