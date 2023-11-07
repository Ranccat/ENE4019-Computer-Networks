import java.net.*;

public class User {
    Socket socket;
    String userName;
    public User (Socket s, String name) {
        this.socket = s;
        this.userName = name;
    }

    public String GetUserName () {
        return userName;
    }

    public Socket GetSocket () {
        return socket;
    }
}
