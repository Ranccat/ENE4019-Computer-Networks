import javax.net.ssl.SSLServerSocket;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;

public class Server {
    static ArrayList<ChatRoom> RoomList = new ArrayList<>(); // contains a room name and all sockets in the room
    static ArrayList<User> ServerUserList = new ArrayList<>(); // only for checking username

    public static void main(String[] args) throws Exception{
        int port1 = Integer.parseInt(args[0]);
        int port2 = Integer.parseInt(args[1]);
        ServerSocket welcomeSocket = new ServerSocket(port1);
        ServerSocket welcomeFileSocket = new ServerSocket(port2);

        while (true) {
            Socket chatSocket = welcomeSocket.accept();
            Socket fileSocket = welcomeFileSocket.accept();

            Thread Connection = new ServerThread(chatSocket, fileSocket);
            Connection.start();
        }
    } // main
} // class

class ServerThread extends Thread {

    Socket socket;
    Socket fileSocket;
    boolean joinFlag = false;
    String myRoomName;
    String myUserName;
    ChatRoom myRoom;
    User Me;
    public ServerThread (Socket s, Socket fs) {
        this.socket = s;
        this.fileSocket = fs;
    }
    @Override
    public void run() {
        try {
            InputStream input = socket.getInputStream();
            InputStream fileInput = fileSocket.getInputStream();
            OutputStream output = socket.getOutputStream();
            OutputStream fileOutput = fileSocket.getOutputStream();

            DataInputStream dataInput = new DataInputStream(fileInput);
            DataOutputStream dataOutput = new DataOutputStream(fileOutput);

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            PrintWriter writer = new PrintWriter(output, true);

            writer.println("[Server] Welcome to chatting room service! If you need help, type '#HELP'.");

            while (true) {
                String sentence = reader.readLine();

                if (sentence.startsWith("#")) { // only command
                    String[] token = sentence.split(" ");
                    String serverMessage;

                    ///// CREATE /////
                    if (token[0].equals("#CREATE")) {
                        if (joinFlag) {
                            writer.println("[Server] You are already in room " + myRoomName + ".");
                            continue;
                        }

                        int createError = 0;
                        for (int i = 0; i < Server.RoomList.size(); i++) {
                            if (token[1].equals(Server.RoomList.get(i).roomName)) createError = 1;
                        }
                        for (int i = 0; i < Server.ServerUserList.size(); i++) {
                            if (token[2].equals(Server.ServerUserList.get(i).userName)) createError = 2;
                        }

                        if (createError > 0) {
                            if (createError == 1) {
                                serverMessage = "[Server] The room name already exists. Try another one.";
                            }
                            else { // createError == 2
                                serverMessage = "[Server] The user name already exists. Try another one.";
                            }
                            writer.println(serverMessage);
                            continue;
                        }

                        // can add a room now
                        ChatRoom newRoom = new ChatRoom(token[1]);
                        User newUser = new User(socket, token[2]);

                        newRoom.UserList.add(newUser);
                        Server.RoomList.add(newRoom);

                        Me = newUser;
                        myRoomName = token[1];
                        myUserName = token[2];
                        Server.ServerUserList.add(newUser);
                        myRoom = newRoom;

                        joinFlag = true;

                        writer.println("[Server] You successfully made room <" + myRoomName + ">.");
                    }

                    ///// JOIN /////
                    else if (token[0].equals("#JOIN")) {
                        if (joinFlag) {
                            writer.println("[Server] You are already in room <" + myRoomName + ">.");
                            continue;
                        }

                        int joinError = 1;
                        for (int i = 0; i < Server.RoomList.size(); i++) {
                            if (token[1].equals(Server.RoomList.get(i).roomName)) joinError = 0;
                        }
                        for (int i = 0; i < Server.ServerUserList.size(); i++) {
                            if (token[2].equals(Server.ServerUserList.get(i).userName)) joinError = 2;
                        }

                        if (joinError > 0) {
                            if (joinError == 1) {
                                serverMessage = "[Server] Cannot find room <" + token[1] + ">. Try again.";
                            }
                            else { // joinError == 2
                                serverMessage = "[Server] The user name already exists. Try another one.";
                            }
                            writer.println(serverMessage);
                            continue;
                        }

                        // can join a room now
                        User newUser = new User(socket, token[2]);
                        Me = newUser;
                        for (int i = 0; i < Server.RoomList.size(); i++) {
                            if (Server.RoomList.get(i).roomName.equals(token[1])) {
                                myRoom = Server.RoomList.get(i);
                                myRoomName = myRoom.GetRoomName();
                            }
                        }
                        myRoom.UserList.add(newUser);
                        myUserName = token[2];
                        Server.ServerUserList.add(Me);

                        joinFlag = true;
                        writer.println("[Server] You successfully joined room <" + myRoomName + ">.");

                        for (int i = 0; i < myRoom.UserList.size(); i++) {
                            if (myRoom.UserList.get(i).equals(Me)) {
                                continue;
                            }
                            OutputStream sendOutput = myRoom.UserList.get(i).socket.getOutputStream();
                            PrintWriter sendWriter = new PrintWriter(sendOutput, true);
                            sendWriter.println("[Server] " + myUserName + " has joined the room.");
                        }
                    }

                    ///// STATUS /////
                    else if (token[0].equals("#STATUS")) {
                        writer.println("--------------- STATUS ---------------");
                        writer.println("Room name: " + myRoomName);

                        for (int i = 0; i < myRoom.UserList.size(); i++) {
                            serverMessage = "User " + (i+1) + ": " + myRoom.UserList.get(i).userName;
                            if (myRoom.UserList.get(i).userName.equals(myUserName)) {
                                serverMessage += " (You)";
                            }
                            writer.println(serverMessage);
                        }
                        writer.println("--------------------------------------");
                    }

                    ///// EXIT /////
                    else if (token[0].equals("#EXIT")) {
                        if (!joinFlag) {
                            writer.println("[Server] You are not in a room to exit or already have left a room.");
                            continue;
                        }

                        for (int i = 0; i < myRoom.UserList.size(); i++) {
                            if (myRoom.UserList.get(i).equals(Me)) {
                                continue;
                            }
                            OutputStream sendOutput = myRoom.UserList.get(i).socket.getOutputStream();
                            PrintWriter sendWriter = new PrintWriter(sendOutput, true);
                            sendWriter.println("[Server] " + myUserName + " has left the room. ");
                        }

                        myRoom.UserList.remove(Me);
                        Server.ServerUserList.remove(Me);

                        if (myRoom.UserList.isEmpty()) {
                            Server.RoomList.remove(myRoom);
                        }
                        myRoom = null;
                        myUserName = null;

                        joinFlag = false;

                        writer.println("[Server] You have left the room <" + myRoomName + ">.");
                        myRoomName = null;
                    }

                    ///// PUT /////
                    else if (token[0].equals("#PUT")) {
                        String fileName = dataInput.readUTF();
                        int dataCount = dataInput.readInt();
                        int totalLength = dataInput.readInt();

                        File file = new File("files/" + fileName);

                        FileOutputStream fout = new FileOutputStream(file);

                        byte[] content = new byte[totalLength];

                        int lastDataCount = totalLength % 65536;

                        int idx = 0;
                        while (dataCount > 0) {
                            if (dataCount == 1) {
                                fileInput.read(content, idx, lastDataCount);
                                fout.write(content, idx, lastDataCount);
                                break;
                            }
                            fileInput.read(content, idx, 65536);
                            fout.write(content, idx, 65536);
                            idx += 65536;
                            dataCount--;
                        }

                        myRoom.FileList.add(file);

                        fout.flush();
                        fout.close();
                    }

                    ///// GET /////
                    else if (token[0].equals("#GET")) {
                        String fileName = token[1];

                        int fileIndex = -1;
                        for (int i = 0; i < myRoom.FileList.size(); i++) {
                            if (myRoom.FileList.get(i).getName().equals(fileName)) {
                                fileIndex = i;
                                break;
                            }
                        }

                        if (fileIndex == -1) {
                            writer.println("Cannot find the file " + fileName);
                        }

                        File file = myRoom.FileList.get(fileIndex);
                        byte[] content = Files.readAllBytes(file.toPath());

                        int dataCount = content.length / 65536;
                        dataCount++;
                        int lastDataLength = content.length % 65536;

                        dataOutput.writeUTF(fileName);
                        dataOutput.writeInt(dataCount);
                        dataOutput.writeInt(content.length);

                        int idx = 0;
                        while (dataCount > 0) {
                            if (dataCount == 1) {
                                fileOutput.write(content, idx, lastDataLength);
                                break;
                            }
                            fileOutput.write(content, idx, 65536);
                            idx += 65536;
                            dataCount--;
                        }
                    }

                    ///// QUIT /////
                    else if (token[0].equals("#QUIT")) {
                        input.close();
                        dataInput.close();
                        fileInput.close();
                        output.close();
                        dataOutput.close();
                        fileOutput.close();
                        socket.close();
                        fileSocket.close();
                    }

                    ///// HELP /////
                    else if (token[0].equals("#HELP")) {
                        writer.println("--------------------------------------");
                        writer.println("#CREATE: create a room and automatically join");
                        writer.println("#JOIN: join an existing room");
                        writer.println("#STATUS: show the status of the room you are in");
                        writer.println("#EXIT: exit the current room");
                        writer.println("#PUT: upload a file");
                        writer.println("#GET: download a file");
                        writer.println("#QUIT: exit the program");
                        writer.println("--------------------------------------");
                    }

                    ///// Invalid command /////
                    else {
                        writer.println("[Server] Invalid command. To show commands, type '#HELP'");
                    }
                }

                ///// MESSAGE /////
                else {
                    if (!joinFlag) {
                        sentence = "[Server] You are not in a room. Join a room or create one to send message.";
                        writer.println(sentence);
                        continue;
                    }

                    for (int i = 0; i < myRoom.UserList.size(); i++) {
                        if (myRoom.UserList.get(i).equals(Me)) {
                            continue;
                        }
                        OutputStream sendOutput = myRoom.UserList.get(i).socket.getOutputStream();
                        PrintWriter sendWriter = new PrintWriter(sendOutput, true);
                        sendWriter.println(myUserName + ": " + sentence);
                    }
                }
            } // while
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}