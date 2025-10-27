package tcp.communication.client;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

public class ClientGUI extends JFrame {
    private JTextPane chatPane;
    private JTextField inputField;
    private JButton sendButton, sendImageButton, sendFileButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JButton disconnectButton;
    private ClientConnection connection;
    private MessageHandler handler;
    private final String username;

    public ClientGUI(String username) {
        this.username = username;
        setupUI();
        setupListeners();
        connect();
        setVisible(true);
    }

    private void setupUI() {
        setTitle("Chat - " + username);
        setSize(700, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // ===== TOP PANEL =====
        var topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        disconnectButton = new JButton("Disconnect");
        topPanel.add(disconnectButton);
        add(topPanel, BorderLayout.NORTH);

        // ===== CHAT AREA =====
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatPane);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(150, 0));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatScroll, userScroll);
        splitPane.setResizeWeight(0.8); // 80% chat, 20% user list
        splitPane.setDividerLocation(520);
        splitPane.setOneTouchExpandable(true); // adds small arrows to resize
        userScroll.setPreferredSize(new Dimension(180, 0));
        add(splitPane, BorderLayout.CENTER);

        // ===== INPUT PANEL =====
        inputField = new JTextField();
        sendButton = new JButton("Send");
        sendImageButton = new JButton("Send Image");
        sendFileButton = new JButton("Send File");

        JPanel buttons = new JPanel(new GridLayout(1, 3));
        buttons.add(sendButton);
        buttons.add(sendImageButton);
        buttons.add(sendFileButton);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(buttons, BorderLayout.EAST);

        add(inputPanel, BorderLayout.SOUTH);
    }

    private void setupListeners() {
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        sendImageButton.addActionListener(e -> sendImage());
        sendFileButton.addActionListener(e -> sendFile());
        disconnectButton.addActionListener(e -> disconnect());
    }

    private void connect() {
        try {
            handler = new MessageHandler(chatPane, userListModel, username);
            connection = new ClientConnection("localhost", 1234, username, handler);
            handler.appendToChat("✅ Connected as " + username + "\n");
        } catch (IOException e) {
            showError("Cannot connect to server!");
        }
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty()) return;

        String target = userList.getSelectedValue();
        boolean broadcast = (target == null || target.equals("All"));

        if (broadcast) {
            connection.sendToServer("BROADCAST|" + message);
        } else {
            connection.sendToServer("PRIVATE|" + target + "|" + message);
            handler.appendToChat("➡️ (to " + target + "): " + message + "\n");
        }
        inputField.setText("");
    }

    private void disconnect() {
        var confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to disconnect?",
                "Confirm Disconnect",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            if (connection != null) {
                connection.close();
            }
            handler.appendToChat("🔌 You disconnected from the server.\n");
        } catch (Exception e) {
            showError("Error during disconnection: " + e.getMessage());
        } finally {
            dispose();
            System.out.println("[INFO] Client " + username + " disconnected manually.");
        }
    }


    private void sendImage() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        String target = userList.getSelectedValue();
        boolean broadcast = (target == null || target.equals("All"));

        try {
            byte[] imageBytes = java.nio.file.Files.readAllBytes(file.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            if (broadcast) {
                connection.sendToServer("IMAGE|" + username + "|" + file.getName() + "|" + base64Image);
                handler.appendToChat("📤 Sent image: " + file.getName() + "\n");
            } else {
                connection.sendToServer("IMAGE_PRIVATE|" + target + "|" + file.getName() + "|" + base64Image);
                handler.appendToChat("➡️ (to " + target + ") sent image: " + file.getName() + "\n");
            }
        } catch (IOException e) {
            showError("Failed to send image: " + e.getMessage());
        }
    }

    private void sendFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        String target = userList.getSelectedValue();
        boolean broadcast = (target == null || target.equals("All"));

        try {
            byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());
            String base64File = Base64.getEncoder().encodeToString(fileBytes);

            if (broadcast) {
                connection.sendToServer("FILE|" + username + "|" + file.getName() + "|" + base64File);
                handler.appendToChat("📤 Sent file: " + file.getName() + "\n");
            } else {
                connection.sendToServer("FILE_PRIVATE|" + target + "|" + file.getName() + "|" + base64File);
                handler.appendToChat("➡️ (to " + target + ") sent file: " + file.getName() + "\n");
            }
        } catch (IOException e) {
            showError("Failed to send file: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        String username = JOptionPane.showInputDialog("Enter your username:");
        if (username != null && !username.isBlank()) {
            SwingUtilities.invokeLater(() -> new ClientGUI(username));
        }
    }
}
