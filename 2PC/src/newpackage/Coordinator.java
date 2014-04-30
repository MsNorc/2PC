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

public class Coordinator {

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
        
        String nodesText = JOptionPane.showInputDialog("Input number of nodes:");
        nodes = Integer.parseInt(nodesText);

        try {
            System.out.println("Initializing in 5 seconds...");
            sleep(5000);
            System.out.println("Startup two-phase commit protocol...");

            while (abortCounter < maxAborts && !commit) {
                System.out.println("Initializing..");
                prepareNodes();
            }

            if (commit) {
                commit();
            } else {
                System.out.println("Initializing with errors...");
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
            System.out.println("Waiting for response from nodes...");
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
                System.out.println("Every node responded...");
            }
        } catch (InterruptedException | IOException e) {
            if (e.getMessage().contains("Accept timed out")) {
                System.out.println("Timeout...");
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
            System.out.println("Startup phase...");
            System.out.println("Transmitting ready checks...");
            String ready = "ready?";

            buf = ready.getBytes();
            packet = new DatagramPacket(buf, buf.length, group, 4446);
            socket.send(packet);
            System.out.println("Ready checks sent...");
            getResponses();
            if (hasResponse) {
                System.out.println("Responses from nodes: ");
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
            System.out.println("Every node responded...");
            System.out.println("Successfully starting execution phase...");
            System.out.println("Signaling for commit...");
            String goForIt = "commit";
            buf = goForIt.getBytes();
            packet = new DatagramPacket(buf, buf.length, group, 4446);
            socket.send(packet);
            getResponses();
            if (hasResponse) {
                System.out.println("Execution responses: ");
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
        System.out.println("One or more nodes did not respond...");
        
        if (abortCounter >= maxAborts) {
            System.out.println("Reached maximum number of timeouts, or rejected by a node...");
            System.out.println("Logging errors...");
        } else {
            try {
                
                abortCounter++;
                int tempNodes = nodes;
                System.out.println("Rollback command to each node...");
                String dontDoIt = "abort";
                buf = dontDoIt.getBytes();
                packet = new DatagramPacket(buf, buf.length, group, 4446);
                socket.send(packet);
                if (timeout) {
                    nodes = responses.size();
                }
                getResponses();

                if (hasResponse) {
                    System.out.println("Abort responses:");
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
