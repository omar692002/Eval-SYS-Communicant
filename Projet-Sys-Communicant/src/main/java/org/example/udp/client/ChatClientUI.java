package org.example.udp.client;

import javax.swing.*;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
        sendImageButton.addActionListener(e ->
                chatClient.sendImage(getSelectedUsers()));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                chatClient.disconnect();
                dispose();
            }
        });
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

        JPanel buttonsPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        buttonsPanel.add(sendButton);
        buttonsPanel.add(sendImageButton);

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
