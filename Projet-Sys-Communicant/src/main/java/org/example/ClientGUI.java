package org.example;// ClientGUI.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ClientGUI extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ClientGUI(String username) {
        this.username = username;

        // GUI setup
        setTitle("Chat Client - " + username);
        setSize(400, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");

        panel.add(inputField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);
        add(panel, BorderLayout.SOUTH);

        // Button action
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

            chatArea.append("✅ Connected to server\n");

            // Thread to receive messages
            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        chatArea.append(message + "\n");
                    }
                } catch (IOException e) {
                    chatArea.append("❌ Disconnected from server\n");
                }
            }).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Cannot connect to server!", "Error", JOptionPane.ERROR_MESSAGE);
        }
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
