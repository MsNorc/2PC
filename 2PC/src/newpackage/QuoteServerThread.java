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
 
public class QuoteServerThread extends Thread {
 
    
    protected DatagramSocket socket = null;
    protected BufferedReader in = null;
    protected boolean moreQuotes = true;
    
 
    public QuoteServerThread() throws IOException {
    this("QuoteServerThread");
    }
 
    public QuoteServerThread(String name) throws IOException {
        super(name);
        socket = new DatagramSocket(4445);
 
     /*   try {
            in = new BufferedReader(new FileReader("one-liners.txt"));
        } catch (FileNotFoundException e) {
            System.err.println("Could not open quote file. Serving time instead.");
        }   */
    }
 
    public void run() {
 
        while (moreQuotes) {
            try {
                byte[] buf = new byte[256];
 
                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
 
                // figure out response
                String text = "";
                buf = packet.getData();
                for (int i = 0; i < buf.length; i++){
                    text += (char) buf[i];
                }
                System.out.println("text " + text);
                String reply ="";
                Calculator calc = new Calculator(text);
                int num = calc.calculate();
                System.out.println("nummer " + num);
                reply = num+"";
                System.out.println(reply);
                buf = reply.getBytes();
                
        // send the response to the client at "address" and "port"
                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                packet = new DatagramPacket(buf, buf.length, address, port);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
        moreQuotes = false;
            }
        }
        socket.close();
    }
 
    protected String getNextQuote() {
        String returnValue = null;
        try {
            if ((returnValue = in.readLine()) == null) {
                in.close();
        moreQuotes = false;
                returnValue = "No more quotes. Goodbye.";
            }
        } catch (IOException e) {
            returnValue = "IOException occurred in server.";
        }
        return returnValue;
    }
}
