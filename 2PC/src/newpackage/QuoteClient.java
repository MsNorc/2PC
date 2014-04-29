/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package newpackage;

/**
 *
 * @author Undis
 */
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.JOptionPane;
 
public class QuoteClient {
    public static void main(String[] args) throws IOException {
 
        String expression = JOptionPane.showInputDialog("input meth");
 
        
        
            // get a datagram socket
        DatagramSocket socket = new DatagramSocket();
 
            // send request
        
        
        
        byte[] buf = expression.getBytes();
        
        InetAddress address = InetAddress.getByName("localhost");
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 4445);
        socket.send(packet);
     
            // get response
        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
 
        // display response
        String received = new String(packet.getData(), 0, packet.getLength());
        System.out.println("calculated " + received);
     
        socket.close();
    }
}
