package org.example.udp.client;

import javax.swing.*;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * ChatClientUI represents the main chat window for the UDP chat application.
 * It handles UI rendering, user input, and interactions with the ChatClient.
 */
public class ChatClientUI extends JFrame {

    private final JTextPane chatArea = new JTextPane();
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("Send");
    private final JButton sendImageButton = new JButton("📷");
    private final DefaultListModel<String> connectedModel = new DefaultListModel<>();
    private JList<String> connectedList;
    private final JButton signOutButton = new JButton("Sign Out");
    private final JButton sendFileButton = new JButton("📎"); // 🆕

    private final ChatClient chatClient;

    /**
     * Constructs the chat UI for a given username.
     *
     * @param pseudo The username of the client.
     */
    public ChatClientUI(String pseudo) {
        this.chatClient = new ChatClient(pseudo, this);

        initUI();
        initActions();

        chatClient.connect();
    }

    /**
     * Initializes and lays out the UI components.
     */
    private void initUI() {
        setTitle("UDP Chat - " + chatClient.getPseudo());
        setSize(750, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        addChatArea();
        addUserList();
        addInputPanel();

        setLocationRelativeTo(null); // center window
    }

    /**
     * Initializes action listeners for buttons and window events.
     */
    private void initActions() {
        inputField.addActionListener(e -> sendMessage());
        sendButton.addActionListener(e -> sendMessage());
        sendImageButton.addActionListener(e -> chatClient.sendImage(getSelectedUsers()));
        sendFileButton.addActionListener(e -> chatClient.sendFile(getSelectedUsers())); // 🆕
        signOutButton.addActionListener(e -> signOut());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                chatClient.disconnect();
                dispose();
            }
        });
    }
    private void signOut() {
        chatClient.disconnect();
        appendText("You have signed out. 👋");
        new Timer(300, e -> {
            dispose();
            System.exit(0);
        }).start();
    }

    /**
     * Adds the chat text pane to the UI.
     */
    private void addChatArea() {
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JScrollPane chatScroll = new JScrollPane(chatArea);
        add(chatScroll, BorderLayout.CENTER);
    }

    /**
     * Adds the connected users list to the UI.
     */
    private void addUserList() {
        connectedList = new JList<>(connectedModel);
        connectedList.setBorder(BorderFactory.createTitledBorder("Connected Users"));
        connectedList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane userScroll = new JScrollPane(connectedList);
        userScroll.setPreferredSize(new Dimension(180, 0));
        add(userScroll, BorderLayout.EAST);
    }

    /**
     * Adds the input field and buttons panel to the UI.
     */
    private void addInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        inputPanel.add(inputField, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel(new GridLayout(1, 4, 5, 0)); // was 3, now 4
        buttonsPanel.add(sendButton);
        buttonsPanel.add(sendImageButton);
        buttonsPanel.add(sendFileButton);  // 🆕
        buttonsPanel.add(signOutButton);
        inputPanel.add(buttonsPanel, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);
    }

    /**
     * Sends a message typed in the input field to selected users.
     */
    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || getSelectedUsers().isEmpty()) return;

        chatClient.sendMessage(text, getSelectedUsers());
        inputField.setText("");
    }

    /**
     * Appends a message to the chat area.
     *
     * @param msg The message to append.
     */
    public void appendText(String msg) {
        try {
            StyledDocument doc = chatArea.getStyledDocument();
            doc.insertString(doc.getLength(), msg + "\n", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void appendClickableFile(String sender, String fileName, File file) {
        try {
            StyledDocument doc = chatArea.getStyledDocument();

            // Append sender info
            doc.insertString(doc.getLength(), sender + " sent a file: ", null);

            // Create clickable style
            Style style = chatArea.addStyle("link", null);
            StyleConstants.setForeground(style, Color.BLUE);
            StyleConstants.setUnderline(style, true);

            int start = doc.getLength();
            doc.insertString(start, fileName + "\n", style);

            // Add mouse listener for click
            chatArea.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        int offset = chatArea.viewToModel2D(e.getPoint());
                        if (offset >= start && offset < start + fileName.length()) {
                            Desktop.getDesktop().open(file);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(chatArea, "⚠️ Cannot open file: " + ex.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Updates the connected users list.
     *
     * @param users The list of connected usernames.
     */
    public void updateUserList(List<String> users) {
        connectedModel.clear();
        connectedModel.addElement("ALL");
        users.forEach(connectedModel::addElement);
    }

    /**
     * Returns the selected users in the connected list.
     *
     * @return List of selected usernames.
     */
    private List<String> getSelectedUsers() {
        return connectedList.getSelectedValuesList();
    }

    /**
     * Returns the chat text pane.
     *
     * @return The JTextPane used for chat display.
     */
    public JTextPane getChatArea() {
        return chatArea;
    }

    /**
     * Entry point of the chat client application.
     *
     * @param args Command line arguments (unused).
     */
    public static void main(String[] args) {
        String pseudo = JOptionPane.showInputDialog("Enter your username:");

        if (Objects.nonNull(pseudo) && !pseudo.isBlank()) {
            SwingUtilities.invokeLater(() -> {
                try {
                    ChatClientUI ui = new ChatClientUI(pseudo);
                    ui.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            System.out.println("❌ Username cannot be empty. Exiting.");
        }
    }
}
