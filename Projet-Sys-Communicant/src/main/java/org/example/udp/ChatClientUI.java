package org.example.udp;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

public class ChatClientUI extends JFrame {
    private final JTextPane chatArea = new JTextPane();
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("Send");
    private final JButton sendImageButton = new JButton("📷");
    private final DefaultListModel<String> connectedModel = new DefaultListModel<>();
    private  JList<String> connectedList;

    private DatagramSocket socket;
    private InetAddress serverAddress;
    private final int serverPort = 9876;
    private final String pseudo;

    public ChatClientUI(String pseudo) throws Exception {
        this.pseudo = pseudo;
        socket = new DatagramSocket();
        serverAddress = InetAddress.getByName("localhost");

        setupUI();
        setupActions();

        sendSystemMessage("JOIN:" + pseudo);
        new Thread(this::receiveMessages).start();
    }

    private void setupUI() {
        setTitle("UDP Chat - " + pseudo);
        setSize(750, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        chatArea.setEditable(false);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JScrollPane chatScroll = new JScrollPane(chatArea);

        connectedList = new JList<>(connectedModel);
        connectedList.setBorder(BorderFactory.createTitledBorder("Connected Users"));
        connectedList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane userScroll = new JScrollPane(connectedList);
        userScroll.setPreferredSize(new Dimension(180, 0));

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        inputPanel.add(inputField, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        buttonsPanel.add(sendButton);
        buttonsPanel.add(sendImageButton);
        inputPanel.add(buttonsPanel, BorderLayout.EAST);

        add(chatScroll, BorderLayout.CENTER);
        add(userScroll, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);
    }

    private void setupActions() {
        inputField.addActionListener(e -> sendMessage());
        sendButton.addActionListener(e -> sendMessage());
        sendImageButton.addActionListener(e -> sendImage());

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
            String text = inputField.getText().trim();
            if (text.isEmpty()) return;

            List<String> targets = connectedList.getSelectedValuesList();
            if (targets.isEmpty()) return;

            for (String target : targets) {
                String msg;
                if (target.equals("ALL")) {
                    msg = "ALL:" + pseudo + ":" + text;
                    appendText("You -> ALL: " + text);
                } else {
                    msg = "TO:" + target + ":" + pseudo + ":" + text;
                    appendText("You -> " + target + ": " + text);
                }

                byte[] data = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
                socket.send(packet);
            }

            inputField.setText("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendImage() {
        try {
            List<String> targets = connectedList.getSelectedValuesList();
            if (targets.isEmpty()) return;

            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

            File file = chooser.getSelectedFile();
            byte[] imgBytes = Files.readAllBytes(file.toPath());
            String fileName = file.getName();

            for (String target : targets) {
                String header = "IMG:" + target + ":" + pseudo + ":" + fileName + ":";
                byte[] headerBytes = header.getBytes("UTF-8");

                byte[] packetData = new byte[headerBytes.length + imgBytes.length];
                System.arraycopy(headerBytes, 0, packetData, 0, headerBytes.length);
                System.arraycopy(imgBytes, 0, packetData, headerBytes.length, imgBytes.length);

                DatagramPacket packet = new DatagramPacket(packetData, packetData.length, serverAddress, serverPort);
                socket.send(packet);

                appendText("You -> " + target + " (Image): " + fileName);
                displayImage(file);
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
                updateUserList(new String(data).substring(9));
                return;
            }

            if (peek.startsWith("IMG:")) {
                handleIncomingImage(data);
                return;
            }

            String msg = new String(data).trim();

            // Prevent showing own broadcasted text messages again
            if (msg.startsWith(pseudo + " -> ALL:")) return;

            appendText(msg);
        } catch (Exception e) {
            appendText(new String(data));
        }
    }

    private void handleIncomingImage(byte[] data) throws Exception {
        int[] colons = new int[4];
        int count = 0;
        for (int i = 0; i < data.length && count < 4; i++) {
            if (data[i] == ':') colons[count++] = i;
        }

        if (count < 4) return;

        String target = new String(data, 4, colons[1] - 4, "UTF-8");
        String sender = new String(data, colons[1] + 1, colons[2] - colons[1] - 1, "UTF-8");
        String fileName = new String(data, colons[2] + 1, colons[3] - colons[2] - 1, "UTF-8");

        // Don't re-display own image broadcasts
        if (sender.equals(pseudo)) return;

        byte[] imgBytes = Arrays.copyOfRange(data, colons[3] + 1, data.length);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgBytes));

        appendText(sender + " -> " + target + " (Image): " + fileName);
        displayImage(img);
    }

    private void updateUserList(String list) {
        connectedModel.clear();
        connectedModel.addElement("ALL");
        Arrays.stream(list.split(","))
                .filter(u -> !u.isBlank() && !u.equals(pseudo))
                .forEach(connectedModel::addElement);
    }

    private void appendText(String msg) {
        try {
            StyledDocument doc = chatArea.getStyledDocument();
            doc.insertString(doc.getLength(), msg + "\n", null);
        } catch (Exception e) {
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
        try {
            if (img == null) return;
            ImageIcon icon = new ImageIcon(img.getScaledInstance(200, -1, Image.SCALE_SMOOTH));
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
            chatArea.insertIcon(icon);
            chatArea.getDocument().insertString(chatArea.getDocument().getLength(), "\n", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        String pseudo = JOptionPane.showInputDialog("Enter your username:");
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
