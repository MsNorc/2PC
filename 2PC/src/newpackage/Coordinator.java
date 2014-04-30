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
        CoordinatorGUI gui = new CoordinatorGUI();
        gui.setVisible(true);
        String nodesText = JOptionPane.showInputDialog("Input number of nodes:");
        nodes = Integer.parseInt(nodesText);

        try {
            
            CoordinatorGUI.textArea.append("Initializing in 5 seconds..."+"\n");
            sleep(5000);
            CoordinatorGUI.textArea.append("Startup two-phase commit protocol..."+"\n");
            

            while (abortCounter < maxAborts && !commit) {
                CoordinatorGUI.textArea.append("Commencing preparation phase..."+"\n");
                prepareNodes();
            }

            if (commit) {
                commit();
            } else {
                CoordinatorGUI.textArea.append("Commenizing completion phase with failures..."+"\n");
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
            CoordinatorGUI.textArea.append("Waiting for response from nodes..."+"\n");
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
                CoordinatorGUI.textArea.append("Every node responded..."+"\n");
            }
        } catch (InterruptedException | IOException e) {
            if (e.getMessage().contains("Accept timed out")) {
                CoordinatorGUI.textArea.append("Timeout..."+"\n");
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
            CoordinatorGUI.textArea.append("Startup phase..."+"\n");
            CoordinatorGUI.textArea.append("Transmitting ready checks..."+"\n");
            String ready = "ready?";

            buf = ready.getBytes();
            packet = new DatagramPacket(buf, buf.length, group, 4446);
            socket.send(packet);
            CoordinatorGUI.textArea.append("Ready checks sent..."+"\n");
            getResponses();
            if (hasResponse) {
                CoordinatorGUI.textArea.append("Responses from nodes: "+"\n");
                for (int j = 0; j < nodes; j++) {
                    CoordinatorGUI.textArea.append(" " + responses.get(j)+"\n");
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
            CoordinatorGUI.textArea.append("Every node responded..."+"\n");
            CoordinatorGUI.textArea.append("Commenizing completion phase with succses..."+"\n");
            CoordinatorGUI.textArea.append("Signaling for commit..."+"\n");
            String goForIt = "commit";
            buf = goForIt.getBytes();
            packet = new DatagramPacket(buf, buf.length, group, 4446);
            socket.send(packet);
            getResponses();
            if (hasResponse) {
                CoordinatorGUI.textArea.append("Execution responses: "+"\n");
                for (int j = 0; j < nodes; j++) {
                    CoordinatorGUI.textArea.append(" " + responses.get(j)+"\n");

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
        CoordinatorGUI.textArea.append("One or more nodes did not respond..."+"\n");
        
        if (abortCounter >= maxAborts) {
            CoordinatorGUI.textArea.append("Reached maximum number of timeouts, or rejected by a node..."+"\n");
            CoordinatorGUI.textArea.append("Logging errors..."+"\n");
        } else {
            try {
                
                abortCounter++;
                int tempNodes = nodes;
                CoordinatorGUI.textArea.append("Rollback command to each node..."+"\n");
                String dontDoIt = "abort";
                buf = dontDoIt.getBytes();
                packet = new DatagramPacket(buf, buf.length, group, 4446);
                socket.send(packet);
                if (timeout) {
                    nodes = responses.size();
                }
                getResponses();

                if (hasResponse) {
                    CoordinatorGUI.textArea.append("Abort responses:"+"\n");
                    for (int j = 0; j < nodes; j++) {
                        CoordinatorGUI.textArea.append(" " + responses.get(j)+"\n");

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
