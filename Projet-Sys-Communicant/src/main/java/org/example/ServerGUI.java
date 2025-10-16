package org.example;// ServerGUI.java
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ServerGUI extends JFrame {
    private JTextArea logArea;
    private static final int PORT = 1234;
    private static Set<ClientHandler> clients = new HashSet<>();

    public ServerGUI() {
        setTitle("Server");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        setVisible(true);

        new Thread(this::startServer).start(); // Start the server logic
    }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            appendLog("✅ Server started on port " + PORT + "\n");

            while (true) {
                Socket socket = serverSocket.accept();
                appendLog("👤 New client connected!\n");

                ClientHandler clientHandler = new ClientHandler(socket, this);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }

        } catch (IOException e) {
            appendLog("❌ Server error: " + e.getMessage() + "\n");
        }
    }

    public synchronized void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public synchronized void broadcastUserList() {
        String userList = "USER_LIST:" + String.join(",", getUsernames());
        for (ClientHandler client : clients) {
            client.sendMessage(userList);
        }
    }

    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
        broadcastUserList();
    }

    private Set<String> getUsernames() {
        Set<String> names = new HashSet<>();
        for (ClientHandler c : clients) names.add(c.getUsername());
        return names;
    }

    public void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerGUI::new);
    }

    // Inner class to handle each client
    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private ServerGUI server;

        public ClientHandler(Socket socket, ServerGUI server) {
            this.socket = socket;
            this.server = server;
        }

        public String getUsername() {
            return username;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // First message from client is their username
                username = in.readLine();
                server.appendLog("🟢 " + username + " joined.\n");

                server.broadcast(username + " joined the chat!", this);
                server.broadcastUserList();

                String message;
                while ((message = in.readLine()) != null) {
                    server.appendLog("📩 " + message + "\n");
                    server.broadcast(message, this);
                }

            } catch (IOException e) {
                server.appendLog("🔴 " + username + " disconnected.\n");
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
                server.removeClient(this);
                server.broadcast(username + " left the chat.", this);
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
    }
}
