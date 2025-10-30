package org.example.udp.multicast;

import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ChatServerMulticast extends JFrame {

    private static final String GROUP_ADDR = "230.0.0.1";
    private static final int PORT = 9876;

    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton quitButton;

    private MulticastSocket socket;
    private InetAddress group;

    public ChatServerMulticast() {
        super("Multicast Chat - [All / Server]");
        setupUI();
        setupNetwork();
        startListening();
    }

    private void setupUI() {
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        inputField = new JTextField();
        sendButton = new JButton("Send (All)");
        quitButton = new JButton("Quit");

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(inputField, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(sendButton);
        buttons.add(quitButton);
        bottomPanel.add(buttons, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        quitButton.addActionListener(e -> quitChat());

        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void setupNetwork() {
        try {
            group = InetAddress.getByName(GROUP_ADDR);
            socket = new MulticastSocket(PORT);
            socket.joinGroup(group);

            sendSystemMessage("🟢 [Server] All connected!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startListening() {
        Thread receiver = new Thread(() -> {
            byte[] buffer = new byte[65535];
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    SwingUtilities.invokeLater(() -> chatArea.append(msg + "\n"));
                } catch (Exception e) {
                    break;
                }
            }
        });
        receiver.start();
    }

    private void sendMessage() {
        try {
            String text = inputField.getText().trim();
            if (text.isEmpty()) return;

            String msg = "[ALL]: " + text;
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);

            DatagramPacket packet = new DatagramPacket(data, data.length, group, PORT);
            socket.send(packet);

            chatArea.append(msg + "\n");
            inputField.setText("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendSystemMessage(String msg) {
        try {
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, group, PORT);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void quitChat() {
        try {
            sendSystemMessage("🔴 [Server] is shutting down.");
            socket.leaveGroup(group);
            socket.close();
            dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChatServerMulticast server = new ChatServerMulticast();
            server.setVisible(true);
        });
    }
}

