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
import javax.swing.JOptionPane;

public class MulticastServerThread extends QuoteServerThread {

    private long TWO_SECONDS = 5000;
    private String message = "Melding nr. ";
    private int nr = 1;
    private ServerSocket responseSocket = new ServerSocket(1250);
    private int nodes = 0;

    public MulticastServerThread() throws IOException {
        super("MulticastServerThread");
    }

    public void run() {
        String nodesText =JOptionPane.showInputDialog("Antall noder:");
        nodes = Integer.parseInt(nodesText);
        for (int i = 0; i < 10; i++) {

            try {
                byte[] buf = new byte[256];

                // construct message
                String messageSend = message + nr;
                buf = messageSend.getBytes();

                // send it
                InetAddress group = InetAddress.getByName("230.0.0.1");
                DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 4446);
                if (i == 0) {
                    try {
                        sleep(5000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MulticastServerThread.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                System.out.println("Sender melding...");
                socket.send(packet);
                
                int teller = 0;
                while (teller < nodes) {
                    System.out.println("Vennter på kvitteringer..");
                    Socket connection = responseSocket.accept();// venter inntil noen tar kontakt
                    ThreadResponseHandler th = new ThreadResponseHandler(connection);
                    th.start();
                    teller++;
                }
                // sleep for a while
                try {
                    sleep(TWO_SECONDS);
                } catch (InterruptedException e) {
                }
            } catch (IOException e) {
                e.printStackTrace();

            }
            nr++;
        }
        socket.close();
    }
}
