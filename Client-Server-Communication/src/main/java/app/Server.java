package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{

    private ServerSocket server;

    // Connected clinents
    private ArrayList<ConnectionHandler> connections;

    // Working server
    private boolean working;

    // Thread pool of connections
    private ExecutorService pool;

    public Server(){
        connections = new ArrayList<>();
        working = true;
    }

    @Override
    public void run() {
        try {

            // Setting server on port
            server = new ServerSocket(12345);
            pool = Executors.newCachedThreadPool();
            
            while (working) {
                // Server accpet connections
                Socket clientSocket = server.accept();
        
                // Adding connection
                ConnectionHandler handler = new ConnectionHandler(clientSocket);
                connections.add(handler);
                pool.execute(handler);
            }
                
            
        } catch (IOException e) {
            shutdown();
        }
    }
    
    
    // Shutdown Server
    public void shutdown(){
        try {
            working = false;
            if(!server.isClosed()){
                server.close();
            }

            // If server stop working all connections will shutdown
            for (ConnectionHandler connection : connections){
                connection.shutdown();
            }

        } catch (IOException e) {
            // Ingmoring exception
        }
    }

    // Inner Class that hanle connections
    class ConnectionHandler implements Runnable{

        private Socket clientSocket;
        // Getting information
        private BufferedReader in;
        // Sending information
        private PrintWriter out;


        private String nickName;

        public ConnectionHandler(Socket clientSocket){
            this.clientSocket = clientSocket;
        }

        @Override
        public void run(){
            try {
                // Initializing in and out
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                

                //out.println("Hello"); -> Sending information
                //in.readLine(); -> Reciving information

                out.println("Enetr your nickname: ");
                nickName = in.readLine();

                System.out.println(nickName + " connected!");
                broadcast(nickName + "join to server!");

                // Waiting for a new message
                String message;
                // Proccesing when message is not null
                while ((message = in.readLine()) != null){
                    // Checking commands
                    if (message.startsWith("/nick")){
                        changeNickName(message);
                    }else if(message.startsWith("/quit")){
                        broadcast(nickName + " left a server!");
                        out.print(nickName + " left a server!");
                        shutdown();
                    }else if(message.startsWith("/help")){
                        // TODO: help
                    }else{
                        // Server sending message to all users from nickName user
                        broadcast(nickName + ": " + message);
                    }
                }

            } catch (IOException e) {
                shutdown();
            }
        }

        public void shutdown(){
            try {
                in.close();
                out.close();
                if(!clientSocket.isClosed()){
                    clientSocket.close();
                }
            } catch (IOException e) {
                // Ingmoring exception
            }
        }

        // Sending message to all connections
        public void broadcast(String message){
            for(ConnectionHandler connection : connections){
                if(connection != null){
                    connection.sendMessage(message);
                }
            }
        }

        // Sending message
        public void sendMessage(String message){
            out.println(message);
        }

        // Changing nickName
        public void changeNickName(String message){
            String[] messageSplit = message.split(" ", 2);
            if (messageSplit.length == 2){
                System.out.println(nickName + " change his nick to " + messageSplit[1]);
                broadcast(nickName + " change his nick to " + messageSplit[1]);
                out.print( "You succesfully change your nickName " + messageSplit[1]);
                nickName = messageSplit[1];
            }else{
                System.out.println(nickName + " his attempt to change his nickName ended in failure!");
                out.print( "Your input was bad. NickName stayed!");
            }
        }
    }


    // Running server
    public static void main(String[] args) {

        Server server = new Server();
        server.run();
        
    }
}
