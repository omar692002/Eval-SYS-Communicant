package tcp.communication.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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

    private Point initialClick;

    public ClientGUI(String username) {

        // Remove OS frame
        setUndecorated(true);
        // Rounded border
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(60,60,65), 2, true));
        // Transparent background to simulate shadow later
        setBackground(new Color(0,0,0,0));

        this.username = username;

        setupUI();
        setupTitleBar();
        setupListeners();
        connect();

        setVisible(true);
    }

    // ======== MODERN CUSTOM TITLE BAR =========
    private void setupTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(20, 86, 197));
        titleBar.setPreferredSize(new Dimension(0, 32));

        JLabel title = new JLabel("  💬 Modern Chat");
        title.setForeground(Color.black);
        title.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Close button
        JButton closeBtn = new JButton("✕");
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setBackground(new Color(200,60,60));
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setPreferredSize(new Dimension(45, 32));
        closeBtn.addActionListener(e -> System.exit(0));

        // Dragging behavior
        titleBar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                initialClick = e.getPoint();
            }
        });

        titleBar.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                int X = getLocation().x + e.getX() - initialClick.x;
                int Y = getLocation().y + e.getY() - initialClick.y;
                setLocation(X, Y);
            }
        });

        titleBar.add(title, BorderLayout.WEST);
        titleBar.add(closeBtn, BorderLayout.EAST);

        add(titleBar, BorderLayout.NORTH);
    }


    private void setupUI() {
        setTitle("💬 Chat - " + username);
        setSize(750, 520);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        Color bg = new Color(245, 247, 250);

        // ========= CHAT AREA =========
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatPane.setBackground(Color.WHITE);

        JScrollPane chatScroll = new JScrollPane(chatPane);
        chatScroll.setBorder(new EmptyBorder(5,5,5,5));

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userList.setBackground(Color.WHITE);

        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(160, 0));
        userScroll.setBorder(BorderFactory.createTitledBorder("Online"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatScroll, userScroll);
        splitPane.setResizeWeight(0.78);
        splitPane.setDividerLocation(550);
        add(splitPane, BorderLayout.CENTER);

        // ========= INPUT =========
        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBorder(new EmptyBorder(5,5,5,5));

        sendButton = createModernButton("Send", new Color(54, 140, 255));
        sendImageButton = createModernButton("Send Image", new Color(109, 190, 90));
        sendFileButton = createModernButton("Send File", new Color(255, 165, 60));

        // === NEW: Disconnect with icon bottom-left ===
        disconnectButton = new JButton("Disconnect");
        ImageIcon icon = new ImageIcon("C:\\Users\\LENOVO\\IdeaProjects\\Eval-SYS-Communicant\\Projet-Sys-Communicant\\src\\main\\java\\icon.png");
        Image img = icon.getImage();
        Image newImg = img.getScaledInstance(24, 24, Image.SCALE_SMOOTH); // width, height
        disconnectButton.setIcon(new ImageIcon(newImg)); // put icon in /icons/
        disconnectButton.setForeground(Color.WHITE);
        disconnectButton.setBackground(new Color(255, 67, 67));
        disconnectButton.setBorder(new EmptyBorder(8, 14, 8, 14));
        disconnectButton.setBorderPainted(false);
        disconnectButton.setFocusPainted(false);
        disconnectButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        disconnectButton.setPreferredSize(new Dimension(140, 40));

        JPanel bottomLeftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomLeftPanel.setBackground(bg);
        bottomLeftPanel.add(disconnectButton);

        JPanel buttons = new JPanel(new GridLayout(1, 3, 4, 4));
        buttons.setBackground(bg);
        buttons.setBorder(new EmptyBorder(3,3,3,3));
        buttons.add(sendButton);
        buttons.add(sendImageButton);
        buttons.add(sendFileButton);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(bg);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(buttons, BorderLayout.EAST);

        JPanel southWrapper = new JPanel(new BorderLayout());
        southWrapper.setBackground(bg);
        southWrapper.add(bottomLeftPanel, BorderLayout.WEST);
        southWrapper.add(inputPanel, BorderLayout.CENTER);

        add(southWrapper, BorderLayout.SOUTH);
    }


    // ========= Modern button styling =========
    private JButton createModernButton(String txt, Color color) {
        JButton b = new JButton(txt);
        b.setForeground(Color.WHITE);
        b.setBackground(color);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return b;
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
                this, "Are you sure you want to disconnect?",
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
