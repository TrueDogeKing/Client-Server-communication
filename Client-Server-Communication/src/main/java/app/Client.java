package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client implements Runnable{
    
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean working;

    public Client(){
        working = true;
    }

    @Override
    public void run(){
        
        try {
            // Connect to the server
            clientSocket = new Socket("127.0.0.1", 12345);
            System.out.println("Connected to the server!");

            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Added inputHandler to the client
            InputHandler handler = new InputHandler();
            Thread t = new Thread(handler);
            t.start();

            // Reciving messages from server
            String serverMessages;
            while ((serverMessages = in.readLine()) != null && working) {
                System.out.println(serverMessages);
            }
            shutdown(); 

        } catch (IOException e) {
            System.out.println("Connection failed!");
        }

    }

    public void shutdown(){
        try {
            working = false;
            in.close();
            out.close();
            if(!clientSocket.isClosed()){
                clientSocket.close();
            }
        } catch (IOException e) {
            // Ingmoring exception
        }
    }

    public class InputHandler implements Runnable {

        @Override
        public void run() {

            try {
                // User Input
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));

                while(working){
                    String message = inputReader.readLine();
                    // what to do
                    if (message == "/quit"){
                        out.println(message);
                        inputReader.close();
                        shutdown();
                        break;
                    }else{
                        // Broadcast message
                        out.println(message);
                    }
                }

            } catch (IOException e) {
                shutdown();
            }
        }
    
        
    }

    public static void main(String[] args) {
        Client client = new Client();

        client.run();
    }
}
