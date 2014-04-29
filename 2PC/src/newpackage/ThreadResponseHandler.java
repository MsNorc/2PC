package newpackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ThreadResponseHandler extends Thread {
    /* Åpner strømmer for kommunikasjon med klientprogrammet */

    Socket connection;
    
    public ThreadResponseHandler(Socket connection) {
        this.connection = connection;
        
    }
    
    public void run(){
        try{
            
        InputStreamReader readConn
                = new InputStreamReader(connection.getInputStream());
        BufferedReader reader = new BufferedReader(readConn);
        PrintWriter writer = new PrintWriter(connection.getOutputStream(), true);
        String aLine = reader.readLine();  // mottar en linje med tekst
        System.out.println("En klient kvitterte melding: " + aLine);
           
        

        /* Lukker koblinger */
        reader.close();
        writer.close();
        connection.close();
        } catch(IOException e){
            System.out.println(e);   
        }
    }
    
            
}
