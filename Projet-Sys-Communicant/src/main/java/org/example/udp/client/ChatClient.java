package org.example.udp.client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Handles UDP chat client networking, message sending, and image handling.
 */
public class ChatClient {

    private DatagramSocket socket;
    private InetAddress serverAddress;
    private final String pseudo;
    private final ChatClientUI ui;

    /**
     * Creates a new chat client instance.
     *
     * @param pseudo Username of the client
     * @param ui     The ChatClientUI instance associated with this client
     */
    public ChatClient(String pseudo, ChatClientUI ui) {
        this.pseudo = pseudo;
        this.ui = ui;
    }

    public String getPseudo() {
        return pseudo;
    }

    /**
     * Connects the client to the server and starts listening for messages.
     */
    public void connect() {
        try {
            socket = new DatagramSocket();
            serverAddress = InetAddress.getByName("localhost");
            sendSystemMessage("JOIN:" + pseudo);
            startReceivingThread();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Disconnects the client from the server.
     */
    public void disconnect() {
        sendSystemMessage("LEAVE:" + pseudo);
        if (Objects.nonNull(socket)) socket.close();
    }

    /**
     * Sends a text message to one or more users.
     *
     * @param text    The message text
     * @param targets List of recipient usernames
     */
    public void sendMessage(String text, List<String> targets) {
        if (text.isBlank() || targets.isEmpty()) return;

        for (String target : targets) {
            String msg = formatTextMessage(target, text);
            sendPacket(msg.getBytes(StandardCharsets.UTF_8));
            // Affiche localement avec le format attendu par appendText(msg, sender)
            ui.appendText(text, "You -> " + target);
        }
    }

    /**
     * Sends an image to one or more users.
     *
     * @param targets List of recipient usernames
     */
    public void sendImage(List<String> targets) {
        if (targets.isEmpty()) return;

        File file = chooseImageFile();
        if (Objects.isNull(file)) return;

        try {
            byte[] imgBytes = Files.readAllBytes(file.toPath());
            String fileName = file.getName();

            for (String target : targets) {
                byte[] packetData = buildImagePacket(target, fileName, imgBytes);
                sendPacket(packetData);
                ui.appendText(" (Image): " + fileName, "You -> " + target);
                displayImageInChat(ImageIO.read(file), ui.getChatArea());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Formats a text message according to the protocol.
     */
    private String formatTextMessage(String target, String text) {
        return target.equals("ALL")
                ? "ALL:" + pseudo + ":" + text
                : "TO:" + target + ":" + pseudo + ":" + text;
    }

    /**
     * Prompts the user to select an image file.
     *
     * @return The selected File, or null if cancelled
     */
    private File chooseImageFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return null;
        return chooser.getSelectedFile();
    }

    /**
     * Builds the byte array for sending an image message.
     */
    private byte[] buildImagePacket(String target, String fileName, byte[] imgBytes) throws UnsupportedEncodingException {
        String header = "IMG:" + target + ":" + pseudo + ":" + fileName + ":";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);

        byte[] packetData = new byte[headerBytes.length + imgBytes.length];
        System.arraycopy(headerBytes, 0, packetData, 0, headerBytes.length);
        System.arraycopy(imgBytes, 0, packetData, headerBytes.length, imgBytes.length);

        return packetData;
    }

    /**
     * Starts a separate thread to receive messages from the server.
     */
    private void startReceivingThread() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[65535];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
                    SwingUtilities.invokeLater(() -> processIncomingMessage(data));
                }
            } catch (Exception e) {
                System.out.println("🔴 Connection closed: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Processes incoming messages (text or image) from the server.
     *
     * @param data Raw packet bytes
     */
    private void processIncomingMessage(byte[] data) {
        try {
            String peek = new String(data, 0, Math.min(10, data.length), StandardCharsets.UTF_8);

            if (peek.startsWith("USERLIST:")) {
                updateUserList(data);
                return;
            }

            if (peek.startsWith("IMG:")) {
                handleIncomingImage(data);
                return;
            }
            if (peek.startsWith("FILE:")) {
                handleIncomingFile(data);
                return;
            }

            appendTextIfNotOwnBroadcast(data);

        } catch (Exception e) {
            // fallback: afficher en tant que message serveur
            ui.appendText(new String(data, StandardCharsets.UTF_8), "Server");
        }
    }

    /**
     * Updates the connected users list in the UI.
     */
    private void updateUserList(byte[] data) {
        String payload = new String(data, StandardCharsets.UTF_8);
        List<String> users = Arrays.stream(payload.substring(9).split(","))
                .filter(u -> !u.isBlank() && !u.equals(pseudo))
                .collect(Collectors.toList());
        ui.updateUserList(users);
    }

    /**
     * Appends a text message if it is not a broadcast from self.
     */
    private void appendTextIfNotOwnBroadcast(byte[] data) {
        String msg = new String(data, StandardCharsets.UTF_8).trim();

        if (msg.contains("->")) {
            int arrowIndex = msg.indexOf("->");
            String sender = msg.substring(0, arrowIndex).trim();
            String content = msg.substring(arrowIndex + 2).trim();

            int colonIndex = content.indexOf(':');
            if (colonIndex != -1) {
                content = content.substring(colonIndex + 1).trim();
            }

            // Ne pas recolorer soi-même
            if (!sender.equals(pseudo)) {
                ui.appendText(content, sender);
            }
        } else {
            ui.appendText(msg, "Server");
        }
    }


    /**
     * Handles an incoming image packet.
     *
     * @param data Raw packet bytes
     * @throws IOException If image cannot be decoded
     */
    private void handleIncomingImage(byte[] data) throws IOException {
        int[] colons = new int[4];
        int count = 0;
        for (int i = 0; i < data.length && count < 4; i++) {
            if (data[i] == ':') colons[count++] = i;
        }
        if (count < 4) return;

        String target = new String(data, 4, colons[1] - 4, StandardCharsets.UTF_8);
        String sender = new String(data, colons[1] + 1, colons[2] - colons[1] - 1, StandardCharsets.UTF_8);
        String fileName = new String(data, colons[2] + 1, colons[3] - colons[2] - 1, StandardCharsets.UTF_8);

        if (sender.equals(pseudo)) return;

        byte[] imgBytes = Arrays.copyOfRange(data, colons[3] + 1, data.length);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgBytes));

        // afficher avec sender + contenu séparable
        ui.appendText(" (Image): " + fileName, sender + " -> " + target);
        displayImageInChat(img, ui.getChatArea());
    }

    /**
     * Displays an image in the chat area.
     *
     * @param img      Image to display
     * @param chatArea JTextPane where image will be displayed
     */
    public void displayImageInChat(BufferedImage img, JTextPane chatArea) {
        try {
            if (Objects.isNull(img)) return;
            ImageIcon icon = new ImageIcon(img.getScaledInstance(200, -1, java.awt.Image.SCALE_SMOOTH));
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
            chatArea.insertIcon(icon);
            chatArea.getDocument().insertString(chatArea.getDocument().getLength(), "\n", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a system message (JOIN/LEAVE) to the server.
     *
     * @param msg The message text
     */
    private void sendSystemMessage(String msg) {
        if (Objects.nonNull(socket)) {
            sendPacket(msg.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Sends a byte array packet to the server.
     *
     * @param data Packet data
     */
    private void sendPacket(byte[] data) {
        try {
            int serverPort = 9876;
            socket.send(new DatagramPacket(data, data.length, serverAddress, serverPort));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendFile(List<String> targets) {
        if (targets.isEmpty()) return;

        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();

        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            String fileName = file.getName();

            for (String target : targets) {
                String header = "FILE:" + target + ":" + pseudo + ":" + fileName + ":";
                byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
                byte[] packetData = new byte[headerBytes.length + fileBytes.length];

                System.arraycopy(headerBytes, 0, packetData, 0, headerBytes.length);
                System.arraycopy(fileBytes, 0, packetData, headerBytes.length, fileBytes.length);

                sendPacket(packetData);

                ui.appendClickableFile("You -> " + target, fileName, file);
            }

        } catch (IOException e) {
            e.printStackTrace();
            ui.appendText("❌ Failed to send file: " + file.getName(), "System");
        }
    }

    private void handleIncomingFile(byte[] data) throws IOException {
        int[] colons = new int[4];
        int count = 0;
        for (int i = 0; i < data.length && count < 4; i++) {
            if (data[i] == ':') colons[count++] = i;
        }
        if (count < 4) return;

        String target = new String(data, 5, colons[1] - 5, StandardCharsets.UTF_8);
        String sender = new String(data, colons[1] + 1, colons[2] - colons[1] - 1, StandardCharsets.UTF_8);
        String fileName = new String(data, colons[2] + 1, colons[3] - colons[2] - 1, StandardCharsets.UTF_8);

        if (sender.equals(pseudo)) return;

        byte[] fileBytes = Arrays.copyOfRange(data, colons[3] + 1, data.length);

        Path downloadsDir = Paths.get(System.getProperty("user.home"), "Downloads");
        if (!Files.exists(downloadsDir)) Files.createDirectories(downloadsDir);
        Path filePath = downloadsDir.resolve(fileName);
        Files.write(filePath, fileBytes);

        // 🆕 Add clickable link to chat
        ui.appendClickableFile(sender, fileName, filePath.toFile());
    }
}
