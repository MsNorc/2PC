/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package newpackage;

import java.io.*;
import java.net.*;

public class ClientNodes {

    private static String fileName = "text1.txt";
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
        
        ClientNodesGUI gui = new ClientNodesGUI();
        gui.setVisible(true);
        
        try {
            boolean run = true;
            while (run) {

                byte[] buf = new byte[256];
                packet = new DatagramPacket(buf, buf.length);
                ClientNodesGUI.textArea.append("Awaiting request..."+"\n");
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                ClientNodesGUI.textArea.append("Ready check received..."+"\n");
                if (received.equals("ready?")) {
                    ClientNodesGUI.textArea.append("Coordinator asks if client is ready for task execution..."+"\n");
                    ClientNodesGUI.textArea.append("Client ready, signaling coordinator..."+"\n");
                    Socket connection = new Socket("localhost", 1250);
                    PrintWriter writer = new PrintWriter(connection.getOutputStream(), true);
                    writer.println("yes");
                    ClientNodesGUI.textArea.append("Prepering execution..."+"\n");
                    fw = new FileWriter(fileName, true);
                    bw = new BufferedWriter(fw);

                } else if (received.contains("task")) {
                    textToBeWrittenToFile = received.substring(5);
                    ClientNodesGUI.textArea.append("Task received : " + textToBeWrittenToFile +"\n");
                } else if (received.equals("commit")) {
                    ClientNodesGUI.textArea.append("Coordinator signaling ready for task commit..."+"\n");
                    ClientNodesGUI.textArea.append("Executing...");
                    ClientNodesGUI.textArea.append("Write to file : " + textToBeWrittenToFile +"\n");
                    
                    bw.write(textToBeWrittenToFile);
                    bw.newLine();
                    bw.flush();
                    ClientNodesGUI.textArea.append("Task complete, signaling coordinator..." +"\n");
                    Socket connection = new Socket("localhost", 1250);
                    PrintWriter writer = new PrintWriter(connection.getOutputStream(), true);
                    writer.println("done");

                } else if (received.equals("abort")) {
                    ClientNodesGUI.textArea.append("Rollback command received..."+"\n");
                    ClientNodesGUI.textArea.append("Abort..."+"\n");
                    textToBeWrittenToFile = null;
                    fw = null;
                    bw = null;
                    ClientNodesGUI.textArea.append("Signaling coordinator, task aborted..."+"\n");
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
