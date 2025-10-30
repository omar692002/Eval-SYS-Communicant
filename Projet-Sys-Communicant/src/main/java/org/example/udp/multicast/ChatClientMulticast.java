package org.example.udp.multicast;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ChatClientMulticast extends JFrame {

    private static final String GROUP_ADDR = "230.0.0.1";
    private static final int PORT = 9876;

    private JTextPane chatArea;
    private MulticastSocket socket;
    private InetAddress group;

    public ChatClientMulticast() {
        super("Multicast Chat - Client (Passive)");
        setupUI();
        setupNetwork();
        startListening();
    }

    private void setupUI() {
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void setupNetwork() {
        try {
            group = InetAddress.getByName(GROUP_ADDR);
            socket = new MulticastSocket(PORT);
            socket.joinGroup(group);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startListening() {
        new Thread(() -> {
            byte[] buffer = new byte[65535];
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                    SwingUtilities.invokeLater(() -> {
                        if (msg.startsWith("TEXT:")) {
                            appendText(msg.substring(5));
                        } else if (msg.startsWith("IMG:")) {
                            handleIncomingImage(packet.getData(), packet.getLength());
                        }
                    });
                } catch (IOException e) {
                    break;
                }
            }
        }).start();
    }

    private void handleIncomingImage(byte[] data, int length) {
        try {
            int[] colons = new int[3];
            int count = 0;
            for (int i = 0; i < length && count < 3; i++) {
                if (data[i] == ':') colons[count++] = i;
            }
            if (count < 3) return;

            String sender = new String(data, 4, colons[1] - 4, StandardCharsets.UTF_8);
            String fileName = new String(data, colons[1] + 1, colons[2] - colons[1] - 1, StandardCharsets.UTF_8);
            byte[] imgBytes = java.util.Arrays.copyOfRange(data, colons[2] + 1, length);

            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgBytes));
            appendText(sender + " sent image: " + fileName);
            displayImage(img);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void appendText(String msg) {
        try {
            chatArea.getDocument().insertString(chatArea.getDocument().getLength(), msg + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void displayImage(BufferedImage img) {
        if (img == null) return;
        ImageIcon icon = new ImageIcon(img.getScaledInstance(200, -1, Image.SCALE_SMOOTH));
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
        chatArea.insertIcon(icon);
        try {
            chatArea.getDocument().insertString(chatArea.getDocument().getLength(), "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClientMulticast().setVisible(true));
    }
}
