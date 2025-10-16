package org.example;


import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ServerGUI extends JFrame {
    private JTextArea logArea;
    private static final int PORT = 1234;

    private static Map<String, ClientHandler> clientMap = new HashMap<>();

    public ServerGUI() {
        setTitle("Server");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        setVisible(true);

        new Thread(this::startServer).start();
    }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            appendLog("✅ Server started on port " + PORT + "\n");

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket, this);
                new Thread(clientHandler).start();
            }

        } catch (IOException e) {
            appendLog("❌ Server error: " + e.getMessage() + "\n");
        }
    }

    public synchronized void broadcast(String message) {
        for (ClientHandler client : clientMap.values()) {
            client.sendMessage(message); // send to everyone including sender
        }
    }

    public synchronized void sendToUser(String recipient, String message) {
        ClientHandler client = clientMap.get(recipient);
        if (client != null) {
            client.sendMessage(message);
        }
    }

    public synchronized void addClient(String username, ClientHandler handler) {
        clientMap.put(username, handler);
        broadcastUserList();
    }

    public synchronized void removeClient(String username) {
        clientMap.remove(username);
        broadcastUserList();
    }

    public synchronized void broadcastUserList() {
        String users = String.join(",", clientMap.keySet());
        for (ClientHandler client : clientMap.values()) {
            client.sendMessage("USER_LIST:" + users);
        }
    }

    public void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerGUI::new);
    }

    // ------------------ ClientHandler -------------------
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

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // First message = username
                username = in.readLine();
                server.appendLog("🟢 " + username + " joined.\n");
                server.addClient(username, this);
                server.broadcast(username + " joined the chat!");

                String input;
                while ((input = in.readLine()) != null) {
                    if (input.startsWith("PRIVATE|")) {
                        String[] parts = input.split("\\|", 3);
                        if (parts.length == 3) {
                            String targetUser = parts[1];
                            String msg = parts[2];
                            server.sendToUser(targetUser, "(Private) " + username + ": " + msg);
                        }
                    } else if (input.startsWith("BROADCAST|")) {
                        String msg = input.substring("BROADCAST|".length());
                        server.appendLog("📩 " + username + ": " + msg + "\n");
                        server.broadcast(username + ": " + msg);
                    } else if (input.startsWith("IMAGE_PRIVATE|")) {
                        int first = input.indexOf('|');
                        int second = input.indexOf('|', first + 1);
                        int third = input.indexOf('|', second + 1);
                        if (first != -1 && second != -1 && third != -1) {
                            String recipient = input.substring(first + 1, second);
                            String filename = input.substring(second + 1, third);
                            String base64 = input.substring(third + 1);
                            server.sendToUser(recipient, "IMAGE_PRIVATE|" + username + "|" + filename + "|" + base64);
                        }
                    } else if (input.startsWith("IMAGE|")) {
                        int first = input.indexOf('|');
                        int second = input.indexOf('|', first + 1);
                        if (first != -1 && second != -1) {
                            String filename = input.substring(first + 1, second);
                            String base64 = input.substring(second + 1);

                            // 🔹 DEBUG: log what the server is broadcasting
                            server.appendLog("Broadcasting IMAGE from " + username +
                                    " filename=" + filename +
                                    " base64 length=" + base64.length() + "\n");

                            server.broadcast("IMAGE|" + username + "|" + filename + "|" + base64);
                        }
                    }

                }

            } catch (IOException e) {
                server.appendLog("🔴 " + username + " disconnected.\n");
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
                server.removeClient(username);
                server.broadcast(username + " left the chat.");
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
    }
}
