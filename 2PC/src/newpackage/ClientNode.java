/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package newpackage;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import javax.swing.JOptionPane;

public class ClientNode {

    private static String fileName = "clientText.txt";
    private static FileWriter fw;
    private static BufferedWriter bw;
    private static FileReader fr;
    private static BufferedReader br;
    private static String textToBeWrittenToFile;
    private static String host;
    private static ArrayList<String> backup;

    public static void main(String[] args) throws IOException {
        host = JOptionPane.showInputDialog("Type in host to connect with:");
        if(host.equals("localhost")){
            fileName = JOptionPane.showInputDialog("Type in a uniqe name for the text-file the client shall write to");
            fileName = fileName + ".txt";
        }
        if(!new File(fileName).isFile()){
                fw = new FileWriter(fileName);
                fw.write("");
                fw.close();
            }
        MulticastSocket socket = new MulticastSocket(4446);
        InetAddress address = InetAddress.getByName("224.0.1.0");
        socket.joinGroup(address);

        DatagramPacket packet;
        
        ClientNodesGUI gui = new ClientNodesGUI();
        gui.setVisible(true);
        
        try {
            boolean run = true;
            while (run) {

                byte[] buf = new byte[512];
                packet = new DatagramPacket(buf, buf.length);
                System.out.println("Awaiting request..."+ "\n"+ "\n");
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Request received..."+ "\n"+ "\n");
                if (received.equals("ready?")) {
                    System.out.println("Coordinator asks if client is ready for task execution..."+ "\n"+ "\n");
                    System.out.println("Client ready, signaling coordinator..."+ "\n"+ "\n");
                    Socket connection = new Socket(host, 1250);
                    PrintWriter writer = new PrintWriter(connection.getOutputStream(), true);
                    writer.println("yes");
                    System.out.println("Preparing execution..."+ "\n"+ "\n");
                    backup = new ArrayList<>();
                    fr = new FileReader(fileName);
                    br = new BufferedReader(fr);
                    String line = br.readLine();
                    while(line != null){
                        backup.add(line);
                        line = br.readLine();
                    }
                    fw = new FileWriter(fileName, true);
                    bw = new BufferedWriter(fw);

                } else if (received.contains("task")) {
                    textToBeWrittenToFile = received.substring(5);
                    String text = textToBeWrittenToFile.replace("\\", "\n");
                    System.out.println("Task received:\n" + text + "\n");
                } else if (received.equals("commit")) {
                    String text = textToBeWrittenToFile.replace("\\", "\n");
                    System.out.println("Coordinator signaling ready for task commit..."+ "\n");
                    System.out.println("Executing given task...\n");
                    System.out.println("Writing the following to file:\n" + text + "\n");
                    
                    bw.write(text);
                    bw.newLine();
                    bw.flush();
                    
                    System.out.println("Task complete, signaling coordinator..." + "\n"+ "\n");
                    Socket connection = new Socket(host, 1250);
                    PrintWriter writer = new PrintWriter(connection.getOutputStream(), true);
                    writer.println(textToBeWrittenToFile);

                } else if (received.equals("abort")) {
                    System.out.println("Command to abort and roll back recived..."+ "\n");
                    System.out.println("Aborting and rolling back..."+ "\n");
                    textToBeWrittenToFile = null;
                    fw = null;
                    bw = null;
                    System.out.println("Signaling coordinator, task aborted and rolled back...\n\n");
                    Socket connection = new Socket(host, 1250);
                    PrintWriter writer = new PrintWriter(connection.getOutputStream(), true);
                    writer.println("aborted");
                } else if (received.equals("rollback")){
                    System.out.println("Command to rollback last update recived...\n");
                    System.out.println("Rolling back...\n\n");
                    fr = new FileReader(fileName);
                    br = new BufferedReader(fr);
                    String check = br.readLine();
                    if (check.equals(textToBeWrittenToFile)) {
                        fw = new FileWriter(fileName, false);
                        bw = new BufferedWriter(fw);
                        
                        for(String s : backup){
                            bw.write(s);
                            bw.newLine();
                        }
                        bw.flush();
                    }
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
