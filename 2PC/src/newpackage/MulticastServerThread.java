package newpackage;

import java.io.*;
import static java.lang.Thread.sleep;
import java.net.*;
import javax.swing.JOptionPane;

public class MulticastServerThread extends QuoteServerThread {

    private final long FIVE_SECONDS = 5000;
    private final String message = "Melding nr. ";
    private int nr = 1;
    private ServerSocket responseSocket = new ServerSocket(1250);
    private int nodes = 0;

    public MulticastServerThread() throws IOException {
        super("MulticastServerThread");
    }

    @Override
    public void run() {
        String nodesText = JOptionPane.showInputDialog("Antall noder:");
        nodes = Integer.parseInt(nodesText);

        try {

            for (int i = 0; i < 10; i++) {

                byte[] buf = new byte[256];

                // construct message
                String messageSend = message + nr;
                buf = messageSend.getBytes();

                // send it
                InetAddress group = InetAddress.getByName("230.0.0.1");
                DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 4446);
                if (i == 0) {
                    sleep(5000);
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
                sleep(FIVE_SECONDS);

            }
            nr++;
        } catch (InterruptedException | IOException e) {
            System.err.println(e);
        } finally {
            socket.close();
        }
    }
}
