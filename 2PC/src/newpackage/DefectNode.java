/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package newpackage;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

public class DefectNode {

    private static String fileName = "clientText.txt";
    private static final Scanner input = new Scanner(System.in);
    private static FileWriter fw;
    private static BufferedWriter bw;
    private static FileReader fr;
    private static BufferedReader br;
    private static String textToBeWrittenToFile;
    private static String host;
    private static ArrayList<String> backup;

    public static void main(String[] args) throws IOException {
        System.out.print("Type in a host to connect with: ");
        host = input.next();
        if (host.equals("localhost")) {
            System.out.print("Type in a uniqe name for the text-file this client shall write to (exluding file-ending): ");
            fileName = input.next();
            fileName = fileName + ".txt";
        }

        MulticastSocket socket = new MulticastSocket(4446);
        InetAddress address = InetAddress.getByName("224.0.1.0");
        socket.joinGroup(address);

        DatagramPacket packet;

        try {
            boolean run = true;
            while (run) {

                byte[] buf = new byte[512];
                packet = new DatagramPacket(buf, buf.length);
                System.out.println("Awaiting request...");
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Request received...\n");
                if (received.equals("ready?")) {
                    System.out.println("Coordinator asks if client is ready for task execution...");
                    System.out.println("Client ready, signaling coordinator...");
                    Socket connection = new Socket(host, 1250);
                    PrintWriter writer = new PrintWriter(connection.getOutputStream(), true);
                    if (!new File(fileName).isFile()) {
                        writer.println("no");
                    } else {
                        writer.println("yes");
                        System.out.println("Preparing execution...\n");
                        backup = new ArrayList<>();
                        fr = new FileReader(fileName);
                        br = new BufferedReader(fr);
                        String line = br.readLine();
                        while (line != null) {
                            backup.add(line);
                            line = br.readLine();
                        }
                        fw = new FileWriter(fileName, true);
                        bw = new BufferedWriter(fw);
                    }

                } else if (received.contains("task")) {
                    textToBeWrittenToFile = received.substring(5);
                    textToBeWrittenToFile = textToBeWrittenToFile.replace("I", "@");
                    String text = textToBeWrittenToFile.replace("\\", "\n");
                    System.out.println("Task received:\n" + text);
                } else if (received.equals("commit")) {
                    String text = textToBeWrittenToFile.replace("\\", "\n");
                    System.out.println("Coordinator signaling ready for task commit...");
                    System.out.println("Executing given task...\n");
                    System.out.println("Writing the following to file:\n" + text);

                    bw.write(text);
                    bw.newLine();
                    bw.flush();

                    System.out.println("Task complete, signaling coordinator...\n");
                    Socket connection = new Socket(host, 1250);
                    PrintWriter writer = new PrintWriter(connection.getOutputStream(), true);
                    writer.println(textToBeWrittenToFile);

                } else if (received.equals("abort")) {
                    System.out.println("Command to abort and roll back recived...");
                    System.out.println("Aborting and rolling back...");
                    textToBeWrittenToFile = null;
                    fw = null;
                    bw = null;
                    System.out.println("Signaling coordinator, task aborted and rolled back...\n");
                    Socket connection = new Socket(host, 1250);
                    PrintWriter writer = new PrintWriter(connection.getOutputStream(), true);
                    writer.println("aborted");
                } else if (received.equals("rollback")) {
                    System.out.println("Command to rollback last update recived...");
                    System.out.println("Rolling back...");
                    fr = new FileReader(fileName);
                    br = new BufferedReader(fr);
                    fw = new FileWriter(fileName, false);
                    bw = new BufferedWriter(fw);

                    for (String s : backup) {
                        bw.write(s);
                        bw.newLine();
                    }
                    bw.flush();
                }
            }
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            socket.leaveGroup(address);
            socket.close();
            bw.close();
            fw.close();
        }
    }

}
