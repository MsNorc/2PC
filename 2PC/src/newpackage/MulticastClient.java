/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package newpackage;

import java.io.*;
import static java.lang.Thread.sleep;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MulticastClient {

    public static void main(String[] args) throws IOException {

        MulticastSocket socket = new MulticastSocket(4446);
        InetAddress address = InetAddress.getByName("230.0.0.1");
        socket.joinGroup(address);

        DatagramPacket packet;
        double id = Math.random();

        // get a few quotes
        for (int i = 0; i < 10; i++) {

            byte[] buf = new byte[256];
            packet = new DatagramPacket(buf, buf.length);
            System.out.println("Venter på pakke");
            socket.receive(packet);

            String received = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Melding mottat:" + received);
            System.out.println("Kviterer..");
            Socket connection = new Socket("Sindre-PC", 1250);
            PrintWriter writer = new PrintWriter(connection.getOutputStream(), true);
            writer.println(id+" "+received);

        }

        socket.leaveGroup(address);
        socket.close();
    }

}
