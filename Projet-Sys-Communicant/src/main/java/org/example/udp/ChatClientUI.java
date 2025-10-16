package org.example.udp;

import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

public class ChatClientUI extends JFrame {
    private JTextArea chatArea = new JTextArea();
    private JTextField inputField = new JTextField();
    private JButton sendButton = new JButton("Send");
    private DefaultListModel<String> connectedModel = new DefaultListModel<>();
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort = 9876;
    private String pseudo;
    private Set<String> connectedUsers = new HashSet<>();

    public ChatClientUI(String pseudo) throws Exception {
        this.pseudo = pseudo;
        socket = new DatagramSocket(0);
        serverAddress = InetAddress.getByName("localhost");

        setTitle("Chat UDP - " + pseudo);
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Chat area
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);

        // Connected users list
        JList<String> connectedList = new JList<>(connectedModel);
        connectedList.setBorder(BorderFactory.createTitledBorder("🟢 Connected Users"));

        JScrollPane userScroll = new JScrollPane(connectedList);
        userScroll.setPreferredSize(new Dimension(200, 0)); // width=200px

        JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.add(userScroll, BorderLayout.CENTER);

        // Bottom panel with input and send button
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(chatScroll, BorderLayout.CENTER);
        add(userPanel, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // Action listeners
        inputField.addActionListener(e -> sendMessage());
        sendButton.addActionListener(e -> sendMessage());

        // Notify server you joined
        sendSystemMessage("JOIN:" + pseudo);

        // Start listening thread
        new Thread(this::receiveMessages).start();

        // Handle closing
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                sendSystemMessage("LEAVE:" + pseudo);
                socket.close();
                dispose();
            }
        });
    }

    private void sendSystemMessage(String msg) {
        try {
            byte[] data = msg.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        try {
            String msgText = inputField.getText().trim();
            if (msgText.isEmpty()) return;

            String msg = pseudo + ": " + msgText;
            byte[] data = msg.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
            socket.send(packet);

            // Show only your version locally
            chatArea.append("You: " + msgText + "\n");
            inputField.setText("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        try {
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength()).trim();

                SwingUtilities.invokeLater(() -> processIncomingMessage(message));
            }
        } catch (Exception e) {
            System.out.println("Client closed: " + e.getMessage());
        }
    }

    private void processIncomingMessage(String message) {
        if (message.startsWith("USERLIST:")) {
            String list = message.substring(9);
            connectedModel.clear();
            connectedUsers.clear();
            Arrays.stream(list.split(","))
                    .filter(u -> !u.isBlank())
                    .forEach(u -> {
                        connectedUsers.add(u);
                        connectedModel.addElement(u);
                    });
        } else if (message.endsWith("🟢")) {
            String user = message.split(" ")[0];
            connectedUsers.add(user);
            if (!connectedModel.contains(user)) connectedModel.addElement(user);
            chatArea.append(message + "\n");
        } else {
            chatArea.append(message + "\n");
        }
    }

    public static void main(String[] args) throws Exception {
        String pseudo = JOptionPane.showInputDialog("Enter your pseudo:");
        if (pseudo != null && !pseudo.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                try {
                    new ChatClientUI(pseudo).setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
