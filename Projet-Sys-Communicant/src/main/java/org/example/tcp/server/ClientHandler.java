package org.example.tcp.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;

public class ClientHandler implements Runnable {
    public static final String PRIVATE = "PRIVATE";
    public static final String BROADCAST = "BROADCAST";
    public static final String IMAGE_PRIVATE = "IMAGE_PRIVATE";
    public static final String IMAGE_PUBLIC = "IMAGE";
    public static final String FILE = "FILE"; // ✅ fixed typo
    public static final String FILE_PRIVATE = "FILE_PRIVATE"; // ✅ added

    private final Socket socket;
    private PrintWriter outToClient;
    private BufferedReader inFromClient;
    private String clientUsername;
    private final ServerGUI server;

    public ClientHandler(Socket socket, ServerGUI server) {
        this.socket = socket;
        this.server = server;
    }

    public void run() {
        try {
            inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outToClient = new PrintWriter(socket.getOutputStream(), true);
            handShakeConnexion();

            String inputMessage;
            while ((inputMessage = inFromClient.readLine()) != null) {
                handleIncomingMessage(inputMessage);
            }
        } catch (IOException e) {
            server.appendLog("🔴 " + clientUsername + " disconnected.\n");
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            server.removeClient(clientUsername);
            server.broadcastToClients(clientUsername + " left the chat.");
        }
    }

    private void handShakeConnexion() throws IOException {
        clientUsername = inFromClient.readLine();
        server.appendLog("🟢 " + clientUsername + " joined.\n");
        server.addClient(clientUsername, this);
        server.broadcastToClients(clientUsername + ": joined the chat!");
    }

    private void handleIncomingMessage(String input) {
        if (Objects.isNull(input) || input.isEmpty()) return;

        var command = input.contains("|") ? input.substring(0, input.indexOf('|')) : input;

        switch (command) {
            case PRIVATE -> handlePrivateMessage(input);
            case BROADCAST -> handleBroadcastMessage(input);
            case IMAGE_PRIVATE -> handlePrivateImage(input);
            case IMAGE_PUBLIC -> handleBroadcastImage(input);
            case FILE -> handleBroadcastFile(input);          // ✅ added
            case FILE_PRIVATE -> handlePrivateFile(input);    // ✅ added
            default -> {
                server.appendLog("⚠️ Unknown command from " + clientUsername + ": " + input + "\n");
                System.out.println("[DEBUG] Unknown command received: " + input);
            }
        }
    }

    private void handlePrivateMessage(String input) {
        String[] parts = input.split("\\|", 3);
        if (parts.length == 3) {
            String targetUser = parts[1];
            String msg = parts[2];
            server.sendToClient(targetUser, "(Private) " + clientUsername + ": " + msg);
        }
    }

    private void handleBroadcastMessage(String input) {
        String msg = input.substring("BROADCAST|".length());
        server.appendLog("📩 " + clientUsername + ": " + msg + "\n");
        server.broadcastToClients(clientUsername + ": " + msg);
    }

    private void handlePrivateImage(String input) {
        int first = input.indexOf('|');
        int second = input.indexOf('|', first + 1);
        int third = input.indexOf('|', second + 1);
        if (first != -1 && second != -1 && third != -1) {
            String recipient = input.substring(first + 1, second);
            String filename = input.substring(second + 1, third);
            String base64 = input.substring(third + 1);
            server.sendToClient(recipient, "IMAGE_PRIVATE|" + clientUsername + "|" + filename + "|" + base64);
        }
    }

    private void handleBroadcastImage(String input) {
        try {
            int first = input.indexOf('|');
            int second = input.indexOf('|', first + 1);
            int third = input.indexOf('|', second + 1);

            if (first != -1 && second != -1 && third != -1) {
                String sender = input.substring(first + 1, second);
                String filename = input.substring(second + 1, third);
                String base64 = input.substring(third + 1);
                server.broadcastToClients("IMAGE|" + sender + "|" + filename + "|" + base64);
            } else {
                System.out.println("[ERROR] Invalid broadcast image input: " + input);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== 🆕 FILE HANDLING ====================

    private void handlePrivateFile(String input) {
        try {
            int first = input.indexOf('|');
            int second = input.indexOf('|', first + 1);
            int third = input.indexOf('|', second + 1);

            if (first == -1 || second == -1 || third == -1) {
                System.out.println("[ERROR] Invalid FILE_PRIVATE format: " + input);
                return;
            }

            String recipient = input.substring(first + 1, second);
            String filename = input.substring(second + 1, third);
            String base64 = input.substring(third + 1);

            server.sendToClient(recipient, "FILE_PRIVATE|" + clientUsername + "|" + filename + "|" + base64);
            server.appendLog("📤 (Private File) " + clientUsername + " → " + recipient + ": " + filename + "\n");
        } catch (Exception e) {
            System.out.println("[EXCEPTION] handlePrivateFile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleBroadcastFile(String input) {
        try {
            int first = input.indexOf('|');
            int second = input.indexOf('|', first + 1);
            int third = input.indexOf('|', second + 1);

            if (first == -1 || second == -1 || third == -1) {
                System.out.println("[ERROR] Invalid FILE format: " + input);
                return;
            }

            String sender = input.substring(first + 1, second);
            String filename = input.substring(second + 1, third);
            String base64 = input.substring(third + 1);

            server.broadcastToClients("FILE|" + sender + "|" + filename + "|" + base64);
            server.appendLog("📦 " + sender + " sent file: " + filename + "\n");
        } catch (Exception e) {
            System.out.println("[EXCEPTION] handleBroadcastFile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==========================================================

    public void sendMessage(String message) {
        outToClient.println(message);
    }
}
