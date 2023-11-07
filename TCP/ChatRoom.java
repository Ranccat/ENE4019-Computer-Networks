import java.io.File;
import java.util.ArrayList;
import java.net.*;

public class ChatRoom {
    String roomName;
    public ArrayList<User> UserList = new ArrayList<>();
    public ArrayList<File> FileList = new ArrayList<>();

    public ChatRoom (String name) {
        this.roomName = name;
    }

    public String GetRoomName () {
        return roomName;
    }
}