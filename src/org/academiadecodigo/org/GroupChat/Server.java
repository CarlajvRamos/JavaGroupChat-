package org.academiadecodigo.org.GroupChat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

public class Server {

    private static final int DEFAULTPORT = 8080;
    private List<ClientThreats> clientThreats = Collections.synchronizedList(new ArrayList<ClientThreats>());


    /**
     * Bootstraps the server
     *
     * @param args optional PORT command line parameter
     */
    public static void main(String[] args) {

        int serverPort = DEFAULTPORT;

        if (args.length > 0) {
            try {
                serverPort = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Port" + args[0] + " must be an integer");
                System.exit(1);
            }
        }

        Server server = new Server();
        server.startServer(serverPort);
    }

    /**
     * Listens to the specified port
     *
     * @param port
     */
    private void startServer(int port) {

        try {
            ServerSocket server = new ServerSocket(port);
            System.out.println("ChatServer is listening on port " + port);

            while (true) {
                try {
                    Socket socket = server.accept();
                    System.out.println("New user connected");

                    ClientThreats newUser = new ClientThreats(socket, this);
                    new Thread(newUser).start();

                } catch (IOException e) {
                    System.out.println("Something wrong in server");
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            System.out.println("Something wrong in server " + e.getMessage());
        }
    }

    /**
     * Broadcast message to all other chat uers
     *
     * @param msg    the message sent by the user
     * @param sender the sender of the message
     */
    private void broadcast(String msg, ClientThreats sender) {

        synchronized (clientThreats) {
            for (ClientThreats aUser : clientThreats) {
                if (aUser != sender) {
                    aUser.sendMsg(msg);
                }
            }
        }
    }

    /**
     * Sends message to a specific user
     *
     * @param userTo        the user to send the message
     * @param serverMessage the message to be set
     */
    private void directMsg(String userTo, String serverMessage) {

        synchronized (clientThreats) {
            for (ClientThreats aUser : clientThreats) {
                if (aUser.getuName().equals(userTo)) {
                    aUser.sendMsg(serverMessage);
                    return;
                }
            }
        }


    }

    /**
     * Checks if the user is already using the chat service
     *
     * @param user the name of the user
     * @return true if user is using the service
     */
    public boolean userExists(String user) {

        System.out.println(user);
        for (ClientThreats aUser : clientThreats) {
            System.out.println(aUser.getuName());
            if (aUser.getuName().equals(user)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds a string with all users using the chat service
     *
     * @return
     */
    private String printAllUsers() {

        StringBuilder allUsers = new StringBuilder();
        allUsers.append("List of connected users; ");

        synchronized (clientThreats) {
            for (ClientThreats user : clientThreats) {
                allUsers.append(user.getuName() + "; ");
            }
        }
        return allUsers.toString();
    }

    /**
     * runnable to handle client connections
     */
    class ClientThreats implements Runnable {

        private Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private Server server;
        private String uName;

        public ClientThreats(Socket socket, Server server) throws IOException {
            this.socket = socket;
            this.server = server;
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
        }

        public void sendMsg(String msg) {
            writer.println(msg);
            System.out.println(msg);
        }

        public void close() throws IOException {

            System.out.println("Client " + uName + " closed, exiting...");
            broadcast("(" + uName + "): left the Chat...", this);

            reader.close();
            socket.close();
            clientThreats.remove(this);
        }

        public String getuName() {
            return uName;
        }

        @Override
        public void run() {

            String clientMessage = "";

            //check if user already logged in
            try {
                uName = reader.readLine();
                if (userExists(uName)) {
                    sendMsg("Message from Server ::: User already connected...");
                    socket.close();
                    reader.close();
                    return;
                }

                Thread.currentThread().setName(uName);
                clientThreats.add(this);

                directMsg(uName, printAllUsers());
                broadcast("New user connected: " + uName, this);

            } catch (SocketException e) {
                System.out.println("Connection lost...");
            } catch (IOException ioException) {
                System.out.println(ioException.getMessage());
            }

            while (!socket.isClosed() && !clientMessage.equals("/Q")) {

                try {

                    clientMessage = reader.readLine();

                    if (clientMessage == null) {
                        close();
                        return;
                    }

                    if (!clientMessage.isEmpty()) {

                        if (clientMessage.startsWith("@")) {

                            String[] directMsg = clientMessage.split(" ", 2);
                            String userTo = directMsg[0].substring(1);

                            if (userExists(userTo)) {
                                clientMessage = directMsg[1];
                                directMsg(userTo, "(" + uName + "): " + clientMessage);
                            }
                        } else {
                            broadcast("(" + uName + "): " + clientMessage, this);
                        }
                    }

                } catch (IOException e) {
                    System.out.println("Receiving error on " + uName + " : " + e.getMessage());
                }
            }
            try {
                close();
            } catch (IOException e) {
                System.out.println("Receiving error closing... " + uName + " : " + e.getMessage());
            }
        }
    }

}