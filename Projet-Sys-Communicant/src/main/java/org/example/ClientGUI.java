package org.example;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Base64;

public class ClientGUI extends JFrame {
    private JTextPane chatPane;
    private JTextField inputField;
    private JButton sendButton;
    private JButton sendImageButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ClientGUI(String username) {
        this.username = username;

        setTitle("Chat - " + username);
        setSize(700, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        chatPane = new JTextPane();
        chatPane.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatPane);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setBorder(BorderFactory.createTitledBorder("Connected Users"));
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(150, 0));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatScroll, userScroll);
        splitPane.setDividerLocation(500);
        splitPane.setResizeWeight(1.0);
        add(splitPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");
        sendImageButton = new JButton("Send Image");

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(sendButton);
        buttonPanel.add(sendImageButton);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        sendImageButton.addActionListener(e -> sendImage());

        connectToServer();
        setVisible(true);
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 1234);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send username
            out.println(username);
            appendToChat("✅ Connected as " + username + "\n", Color.BLUE);

            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        if (message.startsWith("USER_LIST:")) {
                            updateUserList(message.substring(10));
                        } else if (message.startsWith("IMAGE|")) {
                            handleImageMessage(message);
                        } else if (message.startsWith("IMAGE_PRIVATE|")) {
                            handlePrivateImage(message);
                        } else {
                            appendToChat(message + "\n", Color.BLACK);
                        }
                    }
                } catch (IOException e) {
                    appendToChat("❌ Disconnected from server\n", Color.RED);
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
        if (message.isEmpty()) return;

        String selectedUser = userList.getSelectedValue();

        if (selectedUser != null && !selectedUser.equals(username)) {
            out.println("PRIVATE|" + selectedUser + "|" + message);
            appendToChat("➡️ (to " + selectedUser + "): " + message + "\n", Color.MAGENTA);
        } else {
            out.println("BROADCAST|" + message);
            appendToChat("(Me): " + message + "\n", Color.BLUE);
        }

        inputField.setText("");
    }

    private void sendImage() {
        String selectedUser = userList.getSelectedValue();

        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                byte[] imageBytes = java.nio.file.Files.readAllBytes(file.toPath());
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                if (selectedUser != null && !selectedUser.equals(username)) {
                    out.println("IMAGE_PRIVATE|" + selectedUser + "|" + file.getName() + "|" + base64Image);
                    appendToChat("➡️ (to " + selectedUser + ") sent image: " + file.getName() + "\n", Color.MAGENTA);
                } else {
                    out.println("IMAGE|" + file.getName() + "|" + base64Image);
                    appendToChat("📤 Sent image: " + file.getName() + "\n", Color.BLUE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to send image: " + ex.getMessage());
            }
        }
    }

    private void handleImageMessage(String msg) {
        // IMAGE|sender|filename|base64
        String[] parts = msg.split("\\|", 3);
        if (parts.length != 3) return;
        String sender = parts[1];
        String base64 = parts[2];

        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            ImageIcon icon = new ImageIcon(bytes);
            appendIcon(sender, icon);
        } catch (Exception e) {
            appendToChat("⚠️ Failed to display image from " + sender + "\n", Color.RED);
        }
    }

    private void handlePrivateImage(String msg) {
        // IMAGE_PRIVATE|sender|filename|base64
        String[] parts = msg.split("\\|", 4);
        if (parts.length != 4) return;
        String sender = parts[1];
        String base64 = parts[3];

        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            ImageIcon icon = new ImageIcon(bytes);
            appendIcon("(Private) " + sender, icon);
        } catch (Exception e) {
            appendToChat("⚠️ Failed to display private image from " + sender + "\n", Color.RED);
        }
    }

    private void appendIcon(String sender, ImageIcon icon) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatPane.getStyledDocument();
            try {
                doc.insertString(doc.getLength(), sender + ":\n", null);
                chatPane.setCaretPosition(doc.getLength());
                chatPane.insertIcon(icon);
                doc.insertString(doc.getLength(), "\n", null);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private void appendToChat(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatPane.getStyledDocument();
            Style style = chatPane.addStyle("Style", null);
            StyleConstants.setForeground(style, color);
            try {
                doc.insertString(doc.getLength(), text, style);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        String username = JOptionPane.showInputDialog("Enter your username:");
        if (username != null && !username.isEmpty()) {
            SwingUtilities.invokeLater(() -> new ClientGUI(username));
        }
    }
}
