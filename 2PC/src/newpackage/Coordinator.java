/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package newpackage;

import java.io.*;
import java.util.Scanner;
import static java.lang.Thread.sleep;
import java.net.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;

public class Coordinator {

    private static Connection con;

    private static DatagramSocket socket = null;
    private static ServerSocket responseSocket;
    private static int nodes = 0;
    private static ArrayList<String> responses;

    private static byte[] buf = new byte[512];
    private static DatagramPacket packet;
    private static InetAddress group;

    private static boolean commit = false;
    private static boolean done = true;
    private static boolean abort = true;
    private static boolean nodesReady = true;
    private static boolean timeout = false;

    private static boolean hasResponse = true;

    private static int abortCounter = 0;
    private static final int MAX_ABORTS = 3;

    private static ArrayList<String> tasks;
    private static String taskToSend;
    private static final Scanner input = new Scanner(System.in);
    private static FileWriter fw;
    private static BufferedWriter bw;

    public static void main(String[] args) throws java.io.IOException {
        responseSocket = new ServerSocket(1250);
        socket = new DatagramSocket(4445);
        group = InetAddress.getByName("224.0.1.0");
        System.out.print("Input number of nodes: ");
        String nodesIn = input.next();
        nodes = Integer.parseInt(nodesIn);

        try {

            System.out.println("Initializing in 5 seconds...\n");
            sleep(5000);
            System.out.println("Starting two-phase commit protocol.");

            while (abortCounter < MAX_ABORTS && !commit) {
                System.out.println("Commencing preparation phase...");
                prepareNodes();
            }

            if (commit) {
                commit();
            } else {
                System.out.println("\nCommencing completion phase with failures...\n");
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
        responses = new ArrayList<>();
        try {
            int counter = 0;
            System.out.println("Waiting for response from nodes...\n");
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
                System.out.println("Recevied response from every node.\n");
            }
        } catch (InterruptedException | IOException e) {
            if (e.getMessage().contains("Accept timed out")) {
                System.out.println("Timeout has been reached.");
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
            System.out.println("Transmitting ready checks to nodes...");
            String ready = "ready?";

            buf = ready.getBytes();
            packet = new DatagramPacket(buf, buf.length, group, 4446);
            socket.send(packet);
            System.out.println("Ready checks sent to nodes.");
            getResponses();
            if (hasResponse) {
                System.out.println("Responses from nodes regarding ready state: ");
                for (int j = 0; j < nodes; j++) {
                    System.out.println("\t" + responses.get(j));
                    if (responses.get(j).equals("no")) {
                        nodesReady = false;
                    }
                }
                if (nodesReady) {
                    commit = true;
                    taskToSend = "task ";
                    tasks = getTasks();
                    for (String row : tasks) {
                        taskToSend += row + "\\";
                    }
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

    public static ArrayList<String> getTasks() {
        ArrayList<String> rows = new ArrayList<>();
        int id = -1;
        String taskString = "";
        try {
            
            Class.forName("com.mysql.jdbc.Driver");

            con = DriverManager.getConnection("jdbc:mysql://mysql.stud.aitel.hist.no:3306/haakojh","haakojh","zuxY.o9L");

            ResultSet res;
            String query = "SELECT * FROM tasks";
            Statement stm = con.createStatement();
            res = stm.executeQuery(query);
            while (res.next()) {
                id = res.getInt("id");
                taskString = res.getString("task");
                Task row = new Task(id, taskString);
                rows.add(row.toString());
            }
            res.close();
            stm.close();
            
        } catch (ClassNotFoundException | SQLException ex) {
            System.err.println(ex);
        }finally{
            try{
            con.close();
            } catch(SQLException e){
                System.err.println(e);
            }
        }
        return rows;

    }

    public static void commit() {
        try {

            System.out.println("\nCommencing completion phase with success...");
            System.out.println("Signaling nodes to commit to task.\n");
            String goForIt = "commit";
            buf = goForIt.getBytes();
            packet = new DatagramPacket(buf, buf.length, group, 4446);
            socket.send(packet);
            getResponses();
            if (hasResponse) {
                if (responses.size() == nodes) {
                    System.out.println("Responses from nodes regarding completion of task: ");
                    for (int j = 0; j < nodes; j++) {
                        System.out.println("Done - wrote this to file:\n" + responses.get(j).replace("\\", "\n"));
                        if (!responses.get(j).equals(taskToSend.substring(5))) {
                            System.out.println("The response recieved is not approved, will singal rollback...\n");
                            done = false;
                        }
                    }
                } else {
                    done = false;
                }
                if (done) {
                    System.out.println("Transaction completed successfully...\n");
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
        System.out.println("One or more nodes did not respond or said it wasn't ready...");

        if (abortCounter >= MAX_ABORTS) {

            System.out.println("Reached maximum number of timeouts and rejections by nodes...");
            logError();

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
                        System.out.println(" " + responses.get(j) + "\n");

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
            System.out.println("Logging that the current task will not be completed...");
            Date date = new Date();
            fw = new FileWriter("errorLog.txt", true);
            bw = new BufferedWriter(fw);
            ArrayList<String> tasks = getTasks();
            String error;
            for (String task : tasks) {
                error = (date.toString()) + "\t" +  task + ", could not be executed.";
                bw.write(error);
                bw.newLine();
                bw.flush();
            }
            System.out.println("Logging complete...\n");

        } catch (IOException ex) {
            System.err.println(ex);
        } finally {
            bw.close();
            fw.close();
        }
    }
}
