import java.io.*;
import java.net.*;
import java.security.*; // for SHA-256

public class Peer {
    public static void main(String[] args) throws Exception
    {
        int port = Integer.parseInt(args[0]);

        BufferedReader scanFromUser = new BufferedReader(new InputStreamReader(System.in));
        String command = scanFromUser.readLine();
        String[] token = command.split(" "); // checking peer name and IP address

        if (!token[0].equals("#JOIN")) { // only #JOIN is accepted for first command
            System.out.println("Error: invalid command");
            System.exit(0);
        }

        // peer name
        String peerName = token[2];

        // converting room name (string) to IP address
        // room name -> SHA-256
        byte[] hashByte = new byte[32];
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(token[1].getBytes());
        hashByte = messageDigest.digest();
        // SHA-256 -> long
        // there is nothing like unsigned in JAVA, so use long instead
        long z = hashByte[31] & 0xFF;
        long y = hashByte[30] & 0xFF;
        long x = hashByte[29] & 0xFF;
        // long -> IPAddress
        InetAddress IPAddress = InetAddress.getByName("225." + x + "." + y + "." + z);

        // Client is only for sending message
        Thread Client = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MulticastSocket clientSocket = new MulticastSocket(port);

                    clientSocket.joinGroup(IPAddress);
                    clientSocket.setTimeToLive(10);

                    boolean exitFlag = false;

                    String joinMessage = peerName + "@" + "JOIN" + "@"; // send Join message first, then start loop
                    byte[] joinData = new byte[512];
                    joinData = joinMessage.getBytes();
                    DatagramPacket joinPacket = new DatagramPacket(joinData, joinData.length, IPAddress, port);
                    clientSocket.send(joinPacket);

                    // main function
                    while(!exitFlag)
                    {
                        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

                        String input = inFromUser.readLine();
                        String command; // words starting with "#"
                        String message; // words which peers actually read
                        if (input.substring(0, 1).equals("#")) { // any message starting with # is interpreted as command
                            command = input.substring(1);
                            message = peerName + "@" + command + "@";
                        }
                        else {
                            command = "MSG"; // messages' command is fixed as MSG
                            message = peerName + "@" + command + "@" + input + "@";
                        }

                        if (command.equals("EXIT")) {
                            exitFlag = true;
                        }

                        while (true) // to send messages by 512 byte chunks
                        {
                            if (message.length() < 512) { // no need to divided messages to chunks
                                break;
                            }

                            String messagePart = message.substring(0, 512);
                            byte[] sendPart = new byte[512];
                            sendPart = messagePart.getBytes();
                            DatagramPacket sendPartPacket = new DatagramPacket(sendPart, sendPart.length, IPAddress, port);
                            clientSocket.send(sendPartPacket); // send 512 bytes

                            String savedMessage = message.substring(513); // left messages saved
                            message = peerName + "@MSG@" + savedMessage + "@"; // every message needs peer name and command
                        }

                        // last message chunk sent here
                        byte[] sendData = new byte[512];
                        sendData = message.getBytes();

                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
                        clientSocket.send(sendPacket);
                    }

                    clientSocket.close();
                    System.exit(0);
                    return;
                }

                catch (Exception e) {
                    System.out.println("Error: client thread went down");
                    return;
                }
            }
        });

        // Server is used for checking commands and receiving messages
        Thread Server = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MulticastSocket serverSocket = new MulticastSocket(port);

                    serverSocket.joinGroup(IPAddress);
                    serverSocket.setTimeToLive(10);

                    byte[] receiveData = new byte[512];

                    while(true)
                    {
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        serverSocket.receive(receivePacket);

                        String reply = new String(receivePacket.getData());

                        String[] token = reply.split("@"); // now parse packet

                        if (token[1].equals("EXIT")) {
                            if (token[0].equals(peerName)) { // close server thread with client thread together
                                System.exit(0);
                            }
                            else { // if the one who left the room is another pper
                                System.out.println(token[0] + " has left the room");
                            }
                        }

                        if (token[1].equals("JOIN")) {
                            if (token[0].equals(peerName)) { // no message needed for itself joining the room
                                continue;
                            }
                            else { // if other peer joins the room, notice it
                                System.out.println(token[0] + " has joined the room");
                            }
                        }

                        // if packet is just message and it is not mine
                        if (!token[0].equals(peerName) && token[1].equals("MSG")) {
                            System.out.println(token[0] + ": " + token[2]);
                        }
                    }
                }

                catch (Exception e) {
                    System.out.println("Error: server thread went down");
                    return;
                }
            }
        });

        Client.start();
        Server.start();
        if (Client.isInterrupted()) {
            return;
        }
    }
}