/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package newpackage;

import java.io.*;
import java.net.*;

public class ClientNodes1 {

    private static String fileName = "text2.txt";
    private static FileWriter fw;
    private static BufferedWriter bw;
    private static FileReader fr;
    private static BufferedReader br;
    private static String textToBeWrittenToFile;

    public static void main(String[] args) throws IOException {

        MulticastSocket socket = new MulticastSocket(4446);
        InetAddress address = InetAddress.getByName("230.0.0.1");
        socket.joinGroup(address);

        DatagramPacket packet;
        double id = Math.random();

        // get a few quotes
        try {
            boolean run = true;
            while (run) {

                byte[] buf = new byte[256];
                packet = new DatagramPacket(buf, buf.length);
                System.out.println("Standy, awaits ready check from coordinator...");
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("ready check received...");
                if (received.equals("ready?")) {
                    System.out.println("Coordinator asks if client is ready for task execution...");
                    System.out.println("Client ready, sending response to coordinator...");
                    Socket connection = new Socket("localhost", 1250);
                    PrintWriter writer = new PrintWriter(connection.getOutputStream(), true);
                    writer.println("yes");
                    System.out.println("Preparing execution...");
                    fw = new FileWriter(fileName, true);
                    bw = new BufferedWriter(fw);

                } else if (received.contains("task")) {
                    textToBeWrittenToFile = received.substring(5);
                    System.out.println("Task received : " + textToBeWrittenToFile);
                } else if (received.equals("commit")) {
                    System.out.println("Coordinator is signaling ready for task commit...");
                    System.out.println("Executing task...");
                    System.out.println("Writing to file : " + textToBeWrittenToFile);
                   
                    bw.write(textToBeWrittenToFile);
                    bw.newLine();
                    bw.flush();

                    System.out.println("Task complete, signaling coordinator...");
                    Socket connection = new Socket("localhost", 1250);
                    PrintWriter writer = new PrintWriter(connection.getOutputStream(), true);
                    writer.println("done");

                } else if (received.equals("abort")) {
                    System.out.println("Rollback command received...");
                    System.out.println("Abort...");
                    textToBeWrittenToFile = null;
                    fw = null;
                    bw = null;
                    System.out.println("Signaling coordinator, task aborted...");
                    Socket connection = new Socket("localhost", 1250);
                    PrintWriter writer = new PrintWriter(connection.getOutputStream(), true);
                    writer.println("aborted");
                }
            }
        } catch(IOException e){
            System.err.println(e);
        }finally {
            socket.leaveGroup(address);
            socket.close();
            bw.close();
            fw.close();
        }
    }

}
