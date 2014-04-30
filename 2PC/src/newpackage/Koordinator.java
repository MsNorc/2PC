/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package newpackage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import static java.lang.Thread.sleep;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

public class Koordinator {

    private final long FIVE_SECONDS = 5000;
    private final String message = "Melding nr. ";
    private int nr = 1;
    private static DatagramSocket socket = null;
    private static ServerSocket responseSocket;
    private static int nodes = 0;
    private static ArrayList<String> responses;

    private static byte[] buf = new byte[256];
    private static DatagramPacket packet;
    private static InetAddress group;

    private static boolean commit = false;
    private static boolean done = true;
    private static boolean abort = true;
    private static boolean nodesReady = true;
    private static boolean timeout = false;

    private static boolean hasResponse = true;

    private static int abortCounter = 0;
    private static int maxAborts = 3;
    
    private static String taskList = "taskList.txt";
    private static String task;
    private static FileReader fr;
    private static BufferedReader br;

    public static void main(String[] args) throws java.io.IOException {
        responseSocket = new ServerSocket(1250);
        socket = new DatagramSocket(4445);
        group = InetAddress.getByName("230.0.0.1");
        fr = new FileReader(taskList);
        br = new BufferedReader(fr);
        
        String nodesText = JOptionPane.showInputDialog("Antall noder:");
        nodes = Integer.parseInt(nodesText);

        try {
            System.out.println("Oppstart om 5 sekund...");
            sleep(5000);
            System.out.println("Starter to-fase-commit-protokoll...");

            while (abortCounter < maxAborts && !commit) {
                System.out.println("Starter forberedelsesfasen..");
                prepareNodes();
            }

            if (commit) {
                commit();
            } else {
                System.out.println("Starter utførelsesfasen med svikt...");
                abort();
            }
        } catch (InterruptedException e) {
            System.err.println(e);
        } finally {
            socket.close();
        }
    }

    public static synchronized void addResponse(String resp) {
        responses.add(resp);

    }

    public static void getResponses() {
        responses = new ArrayList<String>();
        try {
            int counter = 0;
            System.out.println("Venter på respons fra alle noder...");
            responseSocket.setSoTimeout(5000);
            while (counter < nodes) {

                Socket connection = responseSocket.accept();// venter inntil noen tar kontakt
                ThreadResponseHandler th = new ThreadResponseHandler(connection);
                th.start();
                counter++;
            }
            int waitTime = 0;
            while (responses.size() < nodes || waitTime >= 30) {
                Thread.sleep(1000);
                waitTime++;
            }
            if (responses.size() == nodes) {
                hasResponse = true;
            }
            if (hasResponse) {
                System.out.println("Alle responser er mottatt...");
            }
        } catch (InterruptedException | IOException e) {
            if (e.getMessage().contains("Accept timed out")) {
                System.out.println("Fikk timeout...");
                timeout = true;
                hasResponse = false;
                abort();
            } else {
                System.err.println(e);
            }
        }

    }

    public static void prepareNodes() {
        try {
            System.out.println("Er i forberedelsesfasen...");
            System.out.println("Sender ut forespørsel om klartilstand til noder...");
            String ready = "ready?";

            buf = ready.getBytes();
            packet = new DatagramPacket(buf, buf.length, group, 4446);
            socket.send(packet);
            System.out.println("Forspørsel om noder er klare, er sendt...");
            getResponses();
            if (hasResponse) {
                System.out.println("Responser om noder er klare: ");
                for (int j = 0; j < nodes; j++) {
                    System.out.println(" " + responses.get(j));
                    if (responses.get(j).equals("no")) {
                        nodesReady = false;
                    }
                }
                if (nodesReady) {
                    commit = true;
                    String taskToSend = "task ";
                    taskToSend += br.readLine();
                    buf = taskToSend.getBytes();
                    packet = new DatagramPacket(buf, buf.length, group, 4446);
                    socket.send(packet);
                } else {
                    commit = false;
                    abort();
                }
            }
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    public static void commit() {
        try {
            System.out.println("Alle noder er klare...");
            System.out.println("Starter utførelsesfasen med suksess...");
            System.out.println("Gir klarsignal for commit...");
            String goForIt = "commit";
            buf = goForIt.getBytes();
            packet = new DatagramPacket(buf, buf.length, group, 4446);
            socket.send(packet);
            getResponses();
            if (hasResponse) {
                System.out.println("Responser om utførelse: ");
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
        System.out.println("En eller flere noder sa at det ikke var klare eller svarte ikke...");
        
        if (abortCounter >= maxAborts) {
            System.out.println("Har nådd maks antall timeouts eller avslag fra noder...");
            System.out.println("Logger at oppgave ikke blir utført...");
        } else {
            try {
                
                abortCounter++;
                int tempNodes = nodes;
                System.out.println("Ber alle noder om å avbryte og rulle tilbake...");
                String dontDoIt = "abort";
                buf = dontDoIt.getBytes();
                packet = new DatagramPacket(buf, buf.length, group, 4446);
                socket.send(packet);
                if (timeout) {
                    nodes = responses.size();
                }
                getResponses();

                if (hasResponse) {
                    System.out.println("Responser om avbrytelse:");
                    for (int j = 0; j < nodes; j++) {
                        System.out.println(" " + responses.get(j));

                    }
                    if (responses.size() < nodes) {
                        done = false;
                    }
                }
                nodes = tempNodes;
                if (timeout) {
                    hasResponse = false;
                }
                timeout = false;
            } catch (IOException ex) {
                System.err.println(ex);
            }
        }
    }
}
