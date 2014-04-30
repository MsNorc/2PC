package newpackage;

import java.io.*;
import static java.lang.Thread.sleep;
import java.net.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

public class MulticastServerThread {

    private final long FIVE_SECONDS = 5000;
    private final String message = "Melding nr. ";
    private int nr = 1;
    private static DatagramSocket socket = null;
    private static ServerSocket responseSocket;
    private static int nodes = 0;
    private static ArrayList<String> responses = new ArrayList<String>();

    private static byte[] buf = new byte[256];
    private static DatagramPacket packet;
    private static InetAddress group;

    private static boolean commit = true;
    private static boolean done = true;
    private static boolean abort = true;

    private static boolean isResponse = true;

    public MulticastServerThread() throws IOException {
        responseSocket = new ServerSocket(1250);
        socket = new DatagramSocket(4445);
        group = InetAddress.getByName("230.0.0.1");
    }

    public static synchronized void addResponse(String resp) {

        responses.add(resp);

    }

    public void run() {
        String nodesText = JOptionPane.showInputDialog("Antall noder:");
        nodes = Integer.parseInt(nodesText);

        try {
            // construct message
            String messageSend = message + nr;
            buf = messageSend.getBytes();

            // send it
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
            packet = new DatagramPacket(buf, buf.length, group, 4446);
            socket.send(packet);
            System.out.println("Forspørsel er sendt...");
            getResponses();
            System.out.print("check1.1 for isResp : ");
            System.out.println(isResponse);
            if (isResponse) {
                System.out.println("Alle responser er mottatt...");
                System.out.println("Responser: ");
                for (int j = 0; j < nodes; j++) {
                    System.out.println(" " + responses.get(j));
                    if (responses.get(j).equals("no")) {
                        commit = false;
                    }
                }
            }
            responses = new ArrayList<String>();
            if (commit) {
                commit();
            } else {
                abort();

            }

        } catch (IOException e) {
            System.err.println(e);
        } finally {
            socket.close();
        }
    }

    public static void getResponses() {
        try {
            int teller = 0;
            System.out.println("Venter på respons fra alle noder...");
            responseSocket.setSoTimeout(5000);

            System.out.println("check1 for isResp : " + isResponse);

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
        } catch (InterruptedException | IOException e) {
            if (e.getMessage().contains("Accept timed out")) {
                System.out.println("Fikk timeout...");
                isResponse = false;
                abort();
            } else {
                System.err.println(e);
            }
        }

    }

    public static void commit() {
        try {
            System.out.println("Gir klarsignal for commit...");
            String goForIt = "commit";
            buf = goForIt.getBytes();
            packet = new DatagramPacket(buf, buf.length, group, 4446);
            socket.send(packet);
            getResponses();
            System.out.print("check2 for isResp : ");
            System.out.println(isResponse);
            if (isResponse) {
                System.out.println("Alle responser er mottatt...");
                System.out.println("Responser: ");
                for (int j = 0; j < nodes; j++) {
                    System.out.println(" " + responses.get(j));

                }
                if (responses.size() < nodes) {
                    done = false;
                }
            }
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    public static void abort() {
        try {
            System.out.println("Ber alle noder om å avbryte og rulle tilbake...");
            String dontDoIt = "abort";
            buf = dontDoIt.getBytes();
            packet = new DatagramPacket(buf, buf.length, group, 4446);
            socket.send(packet);
            getResponses();
            System.out.print("check3 for isResp : ");
            System.out.println(isResponse);
            System.out.println("Alle responser er mottatt...");
            System.out.println("Responser: ");
            for (int j = 0; j < nodes; j++) {
                System.out.println(" " + responses.get(j));
                if (responses.get(j).equals("no")) {
                    commit = false;
                }
            }
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }
}
