package org.example.udp.client;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ChatClient {

    private DatagramSocket socket;
    private InetAddress serverAddress;
    private final int serverPort = 9876;
    private final String pseudo;
    private final ChatClientUI ui;

    public ChatClient(String pseudo, ChatClientUI ui) {
        this.pseudo = pseudo;
        this.ui = ui;
    }

    public String getPseudo() {
        return pseudo;
    }

    public void connect() {
        try {
            socket = new DatagramSocket();
            serverAddress = InetAddress.getByName("localhost");
            sendSystemMessage("JOIN:" + pseudo);

            new Thread(this::receiveMessages).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        sendSystemMessage("LEAVE:" + pseudo);
        if (Objects.nonNull(socket)) socket.close();
    }

    public void sendMessage(String text, List<String> targets) {
        try {
            if (text.isBlank() || targets.isEmpty()) return;

            for (String target : targets) {
                String msg = target.equals("ALL")
                        ? "ALL:" + pseudo + ":" + text
                        : "TO:" + target + ":" + pseudo + ":" + text;

                if (Objects.nonNull(socket)) {
                    socket.send(new DatagramPacket(msg.getBytes(), msg.getBytes().length, serverAddress, serverPort));
                }

                ui.appendText("You -> " + target + (target.equals("ALL") ? "" : "") + ": " + text);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendImage(List<String> targets) {
        try {
            if (targets.isEmpty()) return;

            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;

            File file = chooser.getSelectedFile();
            byte[] imgBytes = Files.readAllBytes(file.toPath());
            String fileName = file.getName();

            for (String target : targets) {
                String header = "IMG:" + target + ":" + pseudo + ":" + fileName + ":";
                byte[] headerBytes = header.getBytes("UTF-8");

                byte[] packetData = new byte[headerBytes.length + imgBytes.length];
                System.arraycopy(headerBytes, 0, packetData, 0, headerBytes.length);
                System.arraycopy(imgBytes, 0, packetData, headerBytes.length, imgBytes.length);

                socket.send(new DatagramPacket(packetData, packetData.length, serverAddress, serverPort));
                ui.appendText("You -> " + target + " (Image): " + fileName);
                displayImageInChat(ImageIO.read(file), ui.getChatArea());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
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
    }

    private void processIncomingMessage(byte[] data) {
        try {
            String peek = new String(data, 0, Math.min(10, data.length));

            if (peek.startsWith("USERLIST:")) {
                List<String> users = Arrays.stream(new String(data).substring(9).split(","))
                        .filter(u -> !u.isBlank() && !u.equals(pseudo))
                        .collect(Collectors.toList());
                ui.updateUserList(users);
                return;
            }

            if (peek.startsWith("IMG:")) {
                handleIncomingImage(data);
                return;
            }

            String msg = new String(data).trim();
            if (msg.startsWith(pseudo + " -> ALL:")) return; // skip own broadcast

            ui.appendText(msg);

        } catch (Exception e) {
            ui.appendText(new String(data));
        }
    }

    private void handleIncomingImage(byte[] data) throws IOException {
        int[] colons = new int[4];
        int count = 0;
        for (int i = 0; i < data.length && count < 4; i++) {
            if (data[i] == ':') colons[count++] = i;
        }
        if (count < 4) return;

        String target = new String(data, 4, colons[1] - 4, "UTF-8");
        String sender = new String(data, colons[1] + 1, colons[2] - colons[1] - 1, "UTF-8");
        String fileName = new String(data, colons[2] + 1, colons[3] - colons[2] - 1, "UTF-8");

        if (sender.equals(pseudo)) return;

        byte[] imgBytes = Arrays.copyOfRange(data, colons[3] + 1, data.length);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgBytes));

        ui.appendText(sender + " -> " + target + " (Image): " + fileName);
        displayImageInChat(img, ui.getChatArea());
    }

    public void displayImageInChat(BufferedImage img, JTextPane chatArea) {
        try {
            if (Objects.isNull(img)) return;
            javax.swing.ImageIcon icon = new javax.swing.ImageIcon(img.getScaledInstance(200, -1, java.awt.Image.SCALE_SMOOTH));
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
            chatArea.insertIcon(icon);
            chatArea.getDocument().insertString(chatArea.getDocument().getLength(), "\n", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendSystemMessage(String msg) {
        try {
            if (Objects.nonNull(socket)) {
                socket.send(new DatagramPacket(msg.getBytes(), msg.getBytes().length, serverAddress, serverPort));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
