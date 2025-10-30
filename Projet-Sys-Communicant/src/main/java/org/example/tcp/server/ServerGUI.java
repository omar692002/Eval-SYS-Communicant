package org.example.tcp.server;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ServerGUI extends JFrame {
    private JTextArea logArea;
    private static final int PORT = 1234;
    private static final Map<String, ClientHandler> clientMap = new HashMap<>();

    public ServerGUI() {
        CreateGUI();
        new Thread(this::startServer).start();
    }

    private void CreateGUI() {
        setTitle("Server");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);
        setVisible(true);
    }

    /**
     * This method starts the server and wait for clients to join , each client run in
     * a separate thread to allow multiple clients to communicate in parallel
     */
    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            appendLog("Server started on port " + PORT + "\n");
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket, this);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            appendLog("Server error: " + e.getMessage() + "\n");
        }
    }

    public synchronized void broadcastToClients(String message) {
        for (ClientHandler client : clientMap.values()) {
            client.sendMessage(message);
        }
    }

    public synchronized void sendToClient(String recipient, String message) {
        ClientHandler client = clientMap.get(recipient);
        if (client != null) {
            client.sendMessage(message);
        }
    }

    public synchronized void addClient(String username, ClientHandler handler) {
        clientMap.put(username, handler);
        broadcastUserList();
    }

    public synchronized void broadcastUserList() {
        String users = String.join(",", clientMap.keySet());
        for (ClientHandler client : clientMap.values()) {
            client.sendMessage("USER_LIST:" + users);
        }
    }

    public synchronized void removeClient(String username) {
        clientMap.remove(username);
        broadcastUserList();
    }

    public void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerGUI::new);
    }

}
