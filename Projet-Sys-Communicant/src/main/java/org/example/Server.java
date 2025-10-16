package org.example;// Server.java
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 1234;
    private static Set<ClientHandler> clients = new HashSet<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ Server started on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("👤 New client connected!");

                ClientHandler clientHandler = new ClientHandler(socket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("📩 " + message);
                    Server.broadcast(message, this);
                }

            } catch (IOException e) {
                System.out.println("❌ Client disconnected.");
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
                clients.remove(this);
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
    }
}
