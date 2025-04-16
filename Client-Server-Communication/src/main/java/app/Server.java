package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{

    private ServerSocket server;

    // Connected clinents
    private List<ConnectionHandler> connections;

    // Working server
    private boolean working;

    // Thread pool of connections
    private ExecutorService pool;

    public Server(){
        connections = new CopyOnWriteArrayList<>();
        working = true;
    }

    @Override
    public void run() {
        try {

            // Setting server on port
            server = new ServerSocket(12345);
            log("Server started on port 12345");
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
            pool.shutdown();
            if(!server.isClosed()){
                server.close();
            }

            // If server stop working all connections will shutdown
            for (ConnectionHandler connection : connections){
                log("Shutting down connection for: " + connection.nickName);
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

        // User nickName
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

                log(nickName + " connected!");
                broadcast(nickName + " join to server!");

                // Waiting for a new message
                String message;
                // Proccesing when message is not null
                while ((message = in.readLine()) != null){
                    // Checking commands
                    if (message.startsWith("/nick")){
                        log(nickName + " use commend /nick");
                        changeNickName(message);
                    }else if(message.startsWith("/quit")){
                        log(nickName + " use commend /quit");
                        broadcast(nickName + " left the server!");
                        log(nickName + " left the server!");
                        shutdown();
                        break;
                    }else if(message.startsWith("/help")){
                        log(nickName + " use commend /help");
                        // TODO: help  
                    }else if (message.startsWith("/list")) {
                        log(nickName + " use commend /list");
                        listUsers();
                    }else if (message.startsWith("/msg")) {
                        privateMessage(message);
                    }else{
                        // Server sending message to all users from nickName user
                        broadcast(nickName + ": " + message);
                        log(nickName + " send message: " + message);
                    }
                }

            } catch (IOException e) {
                shutdown();
            }
        }

        public void shutdown(){
            try {
                connections.remove(this);
                in.close();
                out.close();
                if(!clientSocket.isClosed()){
                    log("Shutting down connection for: " + nickName);
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
                broadcast(nickName + " change his nick to " + messageSplit[1]);
                out.print( "You succesfully change your nickName " + messageSplit[1]);
                log(nickName + " change his nick to " + messageSplit[1]);
                nickName = messageSplit[1];
            }else{
                log(nickName + " failed to change nickname!");
                out.print( "Your input was bad. NickName stayed!");
            }
        }

        public void listUsers(){
            // Start building message
            StringBuilder message = new StringBuilder();

            message.append("Online users: ");
            for (ConnectionHandler connection : connections) {
                message.append("\n").append(connection.nickName);
            }

            // Send message
            sendMessage(message.toString());
            
        }

        public void privateMessage(String message){
            // split message
            String[] messageSplit = message.split(" ", 3);

            // take information from message
            String targetNick = messageSplit[1];
            String privateMsg = messageSplit[2];

            boolean found = false;

            log(nickName + " send private message to " + targetNick);
            // Searching for user
            for (ConnectionHandler connection : connections) {
                if (connection.nickName.equals(targetNick)) {
                    connection.sendMessage("[PM from " + nickName + "] " + privateMsg);
                    this.sendMessage("[PM to " + targetNick + "] " + privateMsg);
                    log(targetNick + " get private message frome " + nickName);
                    found = true;
                    break;
                }
            }

            // if not found
            if (!found) {
                log("Server not found user: " + targetNick + ". Message send by " + nickName + " was not recived!");
                sendMessage("User " + targetNick + " not found.");
            }
        }
        
    }

    public void log(String message) {
        System.out.printf("[%s] %s%n", java.time.LocalTime.now(), message);
    }


    // Running server
    public static void main(String[] args) {

        Server server = new Server();
        server.run();
        
    }
}
