package org.academiadecodigo.org.GroupChat;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Client {

    private final static String DEFAULTSERVER = "localhost";
    private final static int DEFAULTPORT = 8080;

    private String userName;
    private Socket socket;

    /**
     * Bootstraps the server
     *
     * @param args optional Hostaame and Port command lime parameters
     */
    public static void main(String[] args) {

        String serverName = DEFAULTSERVER;
        int serverPort = DEFAULTPORT;

        if (args.length > 0) {
            serverName = args[0];
            try {
                serverPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Port" + args[1] + " must be an integer");
                System.exit(1);
            }
        }

        Client client = new Client();
        client.start(serverName, serverPort);
    }

    /**
     * Connects to the specified server/port
     *
     * @param serverName
     * @param port
     */
    private void start(String serverName, int port) {

        try {

            socket = new Socket(serverName, port);
            System.out.println("Connected: " + socket);

            new Thread(new InMessages()).start();   //reads messages from server
            new Thread(new OutMessages()).start();  //sends message to server

        } catch (UnknownHostException ex) {

            System.err.println("Unknown host: " + ex.getMessage());
            System.exit(1);
        } catch (IOException e) {

            System.err.println(e.getMessage());
            System.exit(1);


        }


    }

    /**
     * runnable to handle incoming messages from server
     */
    private class InMessages implements Runnable {

        @Override
        public void run() {

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                while (!socket.isClosed()) {

                    String response = reader.readLine();

                    if (response != null) {

                        System.out.println("\n" + response);
                        if (userName != null) {
                            System.out.print("[" + userName + "]: ");
                        }

                    } else {

                        try {
                            System.out.println("Connection closed. Leaving Chat Service...");
                            reader.close();
                            socket.close();
                        } catch (IOException e) {
                            System.out.println("Error closing connection " + e.getMessage());
                        }
                    }
                }

            } catch (SocketException e) {
                // Socket closed by other thread, no need for special handling
            } catch (IOException ex) {
                System.out.println("Error reading from server: " + ex.getMessage());

            }
            System.exit(1);
        }
    }

    /**
     * runnable to handle outgoing messages to server
     */
    private class OutMessages implements Runnable {

        @Override
        public void run() {

            try {
                BufferedReader consoleMsg = new BufferedReader(new InputStreamReader(System.in)); //gets input from terminal
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                String message = "";

                System.out.println("Please Enter your UserName: ");
                userName = consoleMsg.readLine();

                writer.println(userName);

                while (!socket.isClosed() && !message.equals("/Q")) {

                    message = consoleMsg.readLine();
                    writer.println(message);
                    System.out.println("[" + userName + "]: ");
                }

                System.out.println("Connection closed. Leaving Chat Service...");

                try {
                    consoleMsg.close();
                    writer.close();
                    socket.close();

                } catch (SocketException ex) {
                    System.out.println("Error closing socket " + ex.getMessage());
                }


            } catch (SocketException e) {
                System.out.println("Error sending message... " + e.getMessage());

            } catch (IOException ex) {
                System.out.println("Error getting output stream: " + ex.getMessage());

            }
            System.exit(0);
        }

    }

}


