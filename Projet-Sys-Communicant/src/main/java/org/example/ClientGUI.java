package org.example;// ClientGUI.java
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Arrays;

public class ClientGUI extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JList<String> userList; // NEW
    private DefaultListModel<String> userListModel; // NEW

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ClientGUI(String username) {
        this.username = username;

        // GUI setup
        setTitle("Chat - " + username);
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // User list
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setBorder(BorderFactory.createTitledBorder("Connected Users"));
        add(new JScrollPane(userList), BorderLayout.EAST);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        connectToServer();
        setVisible(true);
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 1234);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send username as first message
            out.println(username);
            chatArea.append("✅ Connected as " + username + "\n");

            // Thread to receive messages
            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        if (message.startsWith("USER_LIST:")) {
                            updateUserList(message.substring(10));
                        } else {
                            chatArea.append(message + "\n");
                        }
                    }
                } catch (IOException e) {
                    chatArea.append("❌ Disconnected from server\n");
                }
            }).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Cannot connect to server!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateUserList(String csvUsers) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            if (!csvUsers.isBlank()) {
                Arrays.stream(csvUsers.split(",")).forEach(userListModel::addElement);
            }
        });
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            out.println(username + ": " + message);
            inputField.setText("");
        }
    }

    public static void main(String[] args) {
        String username = JOptionPane.showInputDialog("Enter your username:");
        if (username != null && !username.isEmpty()) {
            SwingUtilities.invokeLater(() -> new ClientGUI(username));
        }
    }
}
