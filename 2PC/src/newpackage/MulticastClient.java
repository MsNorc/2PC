/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package newpackage;

import java.io.*;
import java.net.*;

public class MulticastClient {

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
                }
            }
        } finally {
            socket.leaveGroup(address);
            socket.close();
        }
    }

}
