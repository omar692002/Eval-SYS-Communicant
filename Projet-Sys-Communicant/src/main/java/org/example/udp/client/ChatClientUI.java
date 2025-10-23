package org.example.udp.client;


import javax.swing.*;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.Objects;

public class ChatClientUI extends JFrame {

    public JTextPane getChatArea() {
        return chatArea;
    }

    private final JTextPane chatArea = new JTextPane();
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("Send");
    private final JButton sendImageButton = new JButton("📷");
    private final DefaultListModel<String> connectedModel = new DefaultListModel<>();
    private JList<String> connectedList;

    private final ChatClient chatClient;

    public ChatClientUI(String pseudo) throws Exception {
        this.chatClient = new ChatClient(pseudo, this);

        setupUI();
        setupActions();

        chatClient.connect();
    }

    private void setupUI() {
        setTitle("UDP Chat - " + chatClient.getPseudo());
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
        sendImageButton.addActionListener(e -> chatClient.sendImage(connectedList.getSelectedValuesList()));

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                chatClient.disconnect();
                dispose();
            }
        });
    }

    private void sendMessage() {
        chatClient.sendMessage(inputField.getText().trim(), connectedList.getSelectedValuesList());
        inputField.setText("");
    }

    public void appendText(String msg) {
        try {
            StyledDocument doc = chatArea.getStyledDocument();
            doc.insertString(doc.getLength(), msg + "\n", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void displayImage(java.awt.image.BufferedImage img) {
        chatClient.displayImageInChat(img, chatArea);
    }

    public void updateUserList(java.util.List<String> users) {
        connectedModel.clear();
        connectedModel.addElement("ALL");
        users.forEach(connectedModel::addElement);
    }
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
