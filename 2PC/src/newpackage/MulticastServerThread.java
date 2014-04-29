package newpackage;

import java.io.*;
import static java.lang.Thread.sleep;
import java.net.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

public class MulticastServerThread extends QuoteServerThread {

    private final long FIVE_SECONDS = 5000;
    private final String message = "Melding nr. ";
    private int nr = 1;
    private ServerSocket responseSocket = new ServerSocket(1250);
    private int nodes = 0;
    private static ArrayList<String> responses = new ArrayList<String>();
    private boolean commit = true;

    public MulticastServerThread() throws IOException {
        super("MulticastServerThread");
    }

    public static synchronized void addResponse(String resp) {

        responses.add(resp);

    }

    @Override
    public void run() {
        String nodesText = JOptionPane.showInputDialog("Antall noder:");
        nodes = Integer.parseInt(nodesText);

        try {

            byte[] buf = new byte[256];

            // construct message
            String messageSend = message + nr;
            buf = messageSend.getBytes();

            // send it
            InetAddress group = InetAddress.getByName("230.0.0.1");
            // DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 4446);
            System.out.println("Oppstart om 5 sekund...");
            try {
                sleep(5000);
            } catch (InterruptedException ex) {
                Logger.getLogger(MulticastServerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("Starter to-fase-commit-protokoll...");
            System.out.println("Er i forberedelsesfasen...");
            System.out.println("Sender ut forespørsel om klartilstand til noder...");
            String ready = "ready?";
            buf = ready.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 4446);
            socket.send(packet);
            System.out.println("Forspørsel er sendt...");
            int teller = 0;
            System.out.println("Venter på response fra alle noder...");
            while (teller < nodes) {
                Socket connection = responseSocket.accept();// venter inntil noen tar kontakt
                ThreadResponseHandler th = new ThreadResponseHandler(connection);
                th.start();
                teller++;
            }
            int waitTime = 0;
            while (responses.size() < nodes || waitTime >= 30) {

                Thread.sleep(1000);

                waitTime++;
            }
            System.out.println("Alle responser er mottatt...");
            System.out.println("Responser: ");
            for (int j = 0; j < nodes; j++) {
                System.out.println(" " + responses.get(j));
                if (responses.get(j).equals("no")) {
                    commit = false;
                }
            }
            if (commit) {
                System.out.println("Klarsignal for commit...");
            }else{
                System.out.println("Avbryter...");
            }

        } catch (InterruptedException | IOException e) {
            System.err.println(e);
        } finally {
            socket.close();
        }
    }
}
