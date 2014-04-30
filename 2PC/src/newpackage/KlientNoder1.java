/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package newpackage;

import java.io.*;
import java.net.*;

public class KlientNoder1 {

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
                System.out.println("Venter på forespørsel fra koordinator...");
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Forespørsel mottatt...");
                if (received.equals("ready?")) {
                    System.out.println("Koordinator spør om jeg er klar til å utføre instruks...");
                    System.out.println("Jeg er klar, og gir respons til koordinator...");
                    Socket connection = new Socket("localhost", 1250);
                    PrintWriter writer = new PrintWriter(connection.getOutputStream(), true);
                    writer.println("yes");
                    System.out.println("Jeg forbereder meg til utførelse...");
                    fw = new FileWriter(fileName, true);
                    bw = new BufferedWriter(fw);

                } else if (received.contains("task")) {
                    textToBeWrittenToFile = received.substring(5);
                    System.out.println("Jeg mottok oppgaven: " + textToBeWrittenToFile);
                } else if (received.equals("commit")) {
                    System.out.println("Koordinator sier det er klart for å utføre oppgaven...");
                    System.out.println("Utfører oppgaven...");
                    System.out.println("Jeg skriver dette til fil: " + textToBeWrittenToFile);
                   
                    bw.write(textToBeWrittenToFile);
                    bw.newLine();
                    bw.flush();

                    System.out.println("Varsler koordinator om at jeg er ferdig...");
                    Socket connection = new Socket("localhost", 1250);
                    PrintWriter writer = new PrintWriter(connection.getOutputStream(), true);
                    writer.println("done");

                } else if (received.equals("abort")) {
                    System.out.println("Koordinator sier at oppgaven skal avbrytes og rullestilbake...");
                    System.out.println("Avbryter...");
                    textToBeWrittenToFile = null;
                    fw = null;
                    bw = null;
                    System.out.println("Varsler koordinator om at har avbrutt oppgaven...");
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
