package org.example.udp.multicast;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.awt.image.BufferedImage;

public class ChatServerMulticast extends JFrame {

    private static final String GROUP_ADDR = "230.0.0.1";
    private static final int PORT = 9876;

    private JTextPane chatArea;
    private JTextField inputField;
    private JButton sendButton, sendImageButton, quitButton;

    private MulticastSocket socket;
    private InetAddress group;
    private final String username = "SERVER";

    public ChatServerMulticast() {
        super("Multicast Chat - Server");
        setupUI();
        setupNetwork();
        startListening();
    }

    private void setupUI() {
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        inputField = new JTextField();
        sendButton = new JButton("Send");
        sendImageButton = new JButton("📷");
        quitButton = new JButton("Quit");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(sendButton);
        buttons.add(sendImageButton);
        buttons.add(quitButton);

        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendTextMessage());
        sendImageButton.addActionListener(e -> sendImage());
        inputField.addActionListener(e -> sendTextMessage());
        quitButton.addActionListener(e -> quitChat());

        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void setupNetwork() {
        try {
            group = InetAddress.getByName(GROUP_ADDR);
            socket = new MulticastSocket(PORT);
            socket.joinGroup(group);

            sendSystemMessage("🟢 Server started (multicast " + GROUP_ADDR + ":" + PORT + ")");
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

                    // Ignore own messages
                    if (packet.getAddress().isLoopbackAddress()) continue;

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

    private void sendTextMessage() {
        try {
            String text = inputField.getText().trim();
            if (text.isEmpty()) return;

            String msg = "TEXT:" + username + ": " + text;
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);

            DatagramPacket packet = new DatagramPacket(data, data.length, group, PORT);
            socket.send(packet);

            appendText(username + ": " + text);
            inputField.setText("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendImage() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        try {
            byte[] imgBytes = java.nio.file.Files.readAllBytes(file.toPath());
            String header = "IMG:" + username + ":" + file.getName() + ":";
            byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);

            byte[] packetData = new byte[headerBytes.length + imgBytes.length];
            System.arraycopy(headerBytes, 0, packetData, 0, headerBytes.length);
            System.arraycopy(imgBytes, 0, packetData, headerBytes.length, imgBytes.length);

            DatagramPacket packet = new DatagramPacket(packetData, packetData.length, group, PORT);
            socket.send(packet);

            appendText(username + " sent image: " + file.getName());
            displayImage(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private void displayImage(File file) {
        try {
            BufferedImage img = ImageIO.read(file);
            displayImage(img);
        } catch (IOException e) {
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

    private void sendSystemMessage(String msg) {
        try {
            String sysMsg = "TEXT:" + msg;
            byte[] data = sysMsg.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, group, PORT);
            socket.send(packet);
            appendText(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void quitChat() {
        try {
            sendSystemMessage("🔴 Server stopped.");
            socket.leaveGroup(group);
            socket.close();
            dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatServerMulticast().setVisible(true));
    }
}
