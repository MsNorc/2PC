/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package newpackage;

import java.io.*;
import static java.lang.Thread.sleep;
import java.net.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

public class Coordinator {

    private static Connection con;

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
    private static FileWriter fw;
    private static BufferedWriter bw;

    public static void main(String[] args) throws java.io.IOException {
        responseSocket = new ServerSocket(1250);
        socket = new DatagramSocket(4445);
        group = InetAddress.getByName("224.0.1.0");
        fr = new FileReader(taskList);
        br = new BufferedReader(fr);
        CoordinatorGUI gui = new CoordinatorGUI();
        gui.setVisible(true);
        String nodesText = JOptionPane.showInputDialog("Input number of nodes:");
        nodes = Integer.parseInt(nodesText);

        try {

            CoordinatorGUI.textArea.append("Initializing in 5 seconds..." + "\n" + "\n");
            sleep(5000);
            CoordinatorGUI.textArea.append("Starting two-phase commit protocol..." + "\n" + "\n");

            while (abortCounter < maxAborts && !commit) {
                CoordinatorGUI.textArea.append("Commencing preparation phase..." + "\n" + "\n");
                prepareNodes();
            }

            if (commit) {
                commit();
            } else {
                CoordinatorGUI.textArea.append("Commenizing completion phase with failures..." + "\n" + "\n");
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

    public static void getResponses() throws IOException {
        responses = new ArrayList<String>();
        try {
            int counter = 0;
            CoordinatorGUI.textArea.append("Waiting for response from nodes..." + "\n" + "\n");
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
                CoordinatorGUI.textArea.append("Recevied response from every node..." + "\n" + "\n");
            }
        } catch (InterruptedException | IOException e) {
            if (e.getMessage().contains("Accept timed out")) {
                CoordinatorGUI.textArea.append("Timeout has been reached..." + "\n" + "\n");
                timeout = true;
                hasResponse = false;
                if (commit) {
                    String rollback = "rollback";
                    buf = rollback.getBytes();
                    packet = new DatagramPacket(buf, buf.length, group, 4446);
                    socket.send(packet);
                    logError();
                } else {
                    abort();
                }
            } else {
                System.err.println(e);
            }
        }

    }

    public static void prepareNodes() {
        try {
            CoordinatorGUI.textArea.append("Transmitting ready checks to nodes..." + "\n" + "\n");
            String ready = "ready?";

            buf = ready.getBytes();
            packet = new DatagramPacket(buf, buf.length, group, 4446);
            socket.send(packet);
            CoordinatorGUI.textArea.append("Ready checks sent to nodes..." + "\n" + "\n");
            getResponses();
            if (hasResponse) {
                CoordinatorGUI.textArea.append("Responses from nodes regarding ready state: " + "\n" + "\n");
                for (int j = 0; j < nodes; j++) {
                    CoordinatorGUI.textArea.append(" " + responses.get(j) + "\n" + "\n");
                    if (responses.get(j).equals("no")) {
                        nodesReady = false;
                    }
                }
                if (nodesReady) {
                    commit = true;
                    String taskToSend = "task ";
                    task = getTask(1);
                    taskToSend += task;
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

    public static String getTask(int id) {
        String taskString = "";
        try {
            
            Class.forName("org.apache.derby.jdbc.ClientDriver");

            con = DriverManager.getConnection("jdbc:derby://localhost:1527/Nettverksprog;user=root;password=root");

            ResultSet res;
            String query = "Select task from tasks where id =" + id;
            Statement stm = con.createStatement();
            res = stm.executeQuery(query);
            if (res.next()) {
                taskString = res.getString("task");
            }

            con.close();

        } catch (Exception ex) {
            System.err.println(ex);
        }

        return taskString;

    }

    public static void commit() {
        try {

            CoordinatorGUI.textArea.append("Commenizing completion phase with succses..." + "\n" + "\n");
            CoordinatorGUI.textArea.append("Signaling nodes to commit to task..." + "\n" + "\n");
            String goForIt = "commit";
            buf = goForIt.getBytes();
            packet = new DatagramPacket(buf, buf.length, group, 4446);
            socket.send(packet);
            getResponses();
            if (hasResponse) {
                if (responses.size() == nodes) {
                    CoordinatorGUI.textArea.append("Responses from nodes regarding completion of task: " + "\n" + "\n");
                    for (int j = 0; j < nodes; j++) {
                        CoordinatorGUI.textArea.append("done - wrote this to file: " + responses.get(j) + "\n" + "\n");
                        System.out.println(responses.get(j) + ", " + task);
                        if (!responses.get(j).equals(task)) {
                            CoordinatorGUI.textArea.append("this response is not approved will singal rollback..." + "\n" + "\n");
                            done = false;
                        }

                    }
                } else {
                    done = false;
                }
                if (done) {
                    CoordinatorGUI.textArea.append("Transaction completed succsesfully..." + "\n" + "\n");
                } else {
                    String rollback = "rollback";
                    buf = rollback.getBytes();
                    packet = new DatagramPacket(buf, buf.length, group, 4446);
                    socket.send(packet);
                    logError();
                }
            }
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    public static void abort() throws IOException {
        CoordinatorGUI.textArea.append("One or more nodes did not respond or said it wasn't ready..." + "\n" + "\n");

        if (abortCounter >= maxAborts) {

            CoordinatorGUI.textArea.append("Reached maximum number of timeouts and rejections by nodes..." + "\n" + "\n");
            logError();

        } else {
            try {

                abortCounter++;
                int tempNodes = nodes;
                CoordinatorGUI.textArea.append("Rollback command to each node..." + "\n" + "\n");
                String dontDoIt = "abort";
                buf = dontDoIt.getBytes();
                packet = new DatagramPacket(buf, buf.length, group, 4446);
                socket.send(packet);
                if (timeout) {
                    nodes = responses.size();
                }
                getResponses();

                if (hasResponse) {
                    CoordinatorGUI.textArea.append("Abort responses:" + "\n" + "\n");
                    for (int j = 0; j < nodes; j++) {
                        CoordinatorGUI.textArea.append(" " + responses.get(j) + "\n" + "\n");

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

    private static void logError() throws IOException {
        try {
            CoordinatorGUI.textArea.append("Logging that the current task will not be completed..." + "\n" + "\n");
            Date date = new Date();
            String currentTask = br.readLine();
            String error = ("The task: " + "'" + currentTask + "'" + " was not completed at the date and time " + date.toString());
            fw = new FileWriter("errorLog.txt", true);
            bw = new BufferedWriter(fw);
            bw.write(error);
            bw.newLine();
            bw.flush();
            CoordinatorGUI.textArea.append("Logging complete..." + "\n" + "\n");

        } catch (IOException ex) {
            System.err.println(ex);
        } finally {
            bw.close();
            fw.close();
            br.close();
            fw.close();
        }
    }
}
