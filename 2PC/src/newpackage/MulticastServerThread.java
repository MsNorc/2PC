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
import static java.lang.Thread.sleep;
import java.net.*;
import java.util.*;

public class MulticastServerThread extends QuoteServerThread {

    private long FIVE_SECONDS = 2000;
    private String melding = "melding nr:";
    private int nummer = 1;
    
    public MulticastServerThread() throws IOException {
        super("MulticastServerThread");
    }

    public void run() {
        while (nummer < 11) {
            try {
                byte[] buf = new byte[256];

                    // construct quote
               String message = melding + nummer;
               buf = message.getBytes();
		    // send it
                InetAddress group = InetAddress.getByName("230.0.0.1");
                DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 4446);
                socket.send(packet);

		    // sleep for a while
		try {
		    sleep((FIVE_SECONDS));
		} catch (InterruptedException e) { }
            } catch (IOException e) {
                e.printStackTrace();
		moreQuotes = false;
            }
            nummer++;
        }
	socket.close();
    }
}
