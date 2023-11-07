import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    public static void main(String[] args) throws Exception {
        String IP = args[0];
        int port1 = Integer.parseInt(args[1]);
        int port2 = Integer.parseInt(args[2]);

        Socket socket = new Socket(IP, port1);
        Socket fileSocket = new Socket(IP, port2);

        Sender sendThread = new Sender(socket, fileSocket);
        Thread receiveThread = new Receiver(socket, fileSocket);

        sendThread.start();
        receiveThread.start();
    } // main
} // class

class Sender extends Thread {
    Socket socket;
    Socket fileSocket;
    Scanner scan = new Scanner(System.in);
    public Sender (Socket s, Socket fs) {
        this.socket = s;
        this.fileSocket = fs;
    }
    @Override
    public void run() {
        try {
            String sentence;

            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            FileInputStream fin;
            FileOutputStream fout;

            OutputStream fileOutput = fileSocket.getOutputStream();
            InputStream fileInput = fileSocket.getInputStream();

            DataOutputStream dataOutput = new DataOutputStream(fileOutput);
            DataInputStream dataInput = new DataInputStream(fileInput);

            while (true) {
                sentence = scan.nextLine();

                if (sentence.startsWith("#PUT")) { // file output
                    String[] token = sentence.split(" ");
                    String fileName = token[1];
                    fin = new FileInputStream(fileName);

                    byte[] content = fin.readAllBytes();
                    int dataCount = content.length / 65536;
                    dataCount++;
                    int lastDataLength = content.length % 65536;

                    writer.println("#PUT ");
                    dataOutput.writeUTF(fileName);
                    dataOutput.writeInt(dataCount);
                    dataOutput.writeInt(content.length);

                    System.out.print("[Server] Upload process: ");
                    int idx = 0;
                    while (dataCount > 0) {
                        System.out.print("#");
                        if (dataCount == 1) {
                            fileOutput.write(content, idx, lastDataLength);
                            break;
                        }
                        fileOutput.write(content, idx, 65536);
                        idx += 65536;
                        dataCount--;
                    }
                    System.out.println("\n[Server] Successfully uploaded the file.");
                }

                else if (sentence.startsWith("#GET")) { // file input
                    writer.println(sentence);

                    String fileName = dataInput.readUTF();
                    int dataCount = dataInput.readInt();
                    int totalLength = dataInput.readInt();
                    File file = new File(fileName);

                    fout = new FileOutputStream(file);

                    byte[] content = new byte[totalLength];

                    int lastDataCount = totalLength % 65536;

                    int idx = 0;
                    System.out.print("[Server] Download process: ");
                    while (dataCount > 0) {
                        System.out.print("#");
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

                    System.out.println("\n[Server] Successfully downloaded the file.");

                    fout.flush();
                    fout.close();
                }

                else if (sentence.startsWith("#QUIT")) {
                    dataInput.close();
                    dataOutput.close();
                    fileInput.close();
                    fileOutput.close();
                    socket.close();
                    fileSocket.close();
                    System.exit(0);
                }

                else { // message
                    writer.println(sentence);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class Receiver extends Thread {
    Socket socket;
    Socket fileSocket;
    public Receiver (Socket s, Socket fs) {
        this.socket = s;
        this.fileSocket = fs;
    }
    @Override
    public void run() {
        try {
            String sentence;
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            while (true) {
                sentence = reader.readLine();
                System.out.println(sentence);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}