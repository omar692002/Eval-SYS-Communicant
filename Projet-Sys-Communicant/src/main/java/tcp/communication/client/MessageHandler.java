package tcp.communication.client;

import tcp.communication.enums.MessageType;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.*;
import java.util.Base64;

public class MessageHandler {
    private final JTextPane chatPane;
    private final DefaultListModel<String> userListModel;
    private final String username;

    public MessageHandler(JTextPane chatPane, DefaultListModel<String> userListModel, String username) {
        this.chatPane = chatPane;
        this.userListModel = userListModel;
        this.username = username;
    }

    public void handle(String message) {
        MessageType type = MessageType.from(message);
        switch (type) {
            case USER_LIST -> handleUserList(message.substring(10));
            case IMAGE -> handleImageMessage(message);
            case IMAGE_PRIVATE -> handlePrivateImage(message);
            case UNKNOWN -> appendToChat(message + "\n", Color.BLACK);
            default -> appendToChat(message + "\n", Color.GRAY);
        }
    }

    private void handleUserList(String csvUsers) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            userListModel.addElement("All");
            if (!csvUsers.isBlank()) {
                Arrays.stream(csvUsers.split(","))
                        .filter(u -> !u.equals(username))
                        .forEach(userListModel::addElement);
            }
        });
    }

    private void handleImageMessage(String msg) {
        try {
            String[] parts = msg.split("\\|", 4);
            if (parts.length != 4) {
                System.out.println("[ERROR] Invalid IMAGE message format: " + msg);
                return;
            }
            String sender = parts[1];
            String filename = parts[2];
            String base64 = parts[3];
            displayImage(sender + " (" + filename + ")", base64);
        } catch (Exception e) {
            e.printStackTrace();
            appendToChat("⚠️ Failed to display image from " + msg + "\n", Color.RED);
        }
    }


    private void displayImage(String sender, String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            ImageIcon icon = new ImageIcon(bytes);
            appendIcon(sender, icon);
        } catch (Exception e) {
            appendToChat("⚠️ Failed to display image from " + sender + "\n", Color.RED);
        }
    }

    private void handlePrivateImage(String msg) {
        String[] parts = msg.split("\\|", 4);
        if (parts.length != 4) return;
        String sender = parts[1];
        String base64 = parts[3];
        displayImage("(Private) " + sender, base64);
    }



    public void appendToChat(String text, Color color) {
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

    public void appendIcon(String sender, ImageIcon icon) {
        SwingUtilities.invokeLater(() -> {
            try {
                StyledDocument doc = chatPane.getStyledDocument();
                doc.insertString(doc.getLength(), sender + ":\n", null);
                chatPane.insertIcon(icon);
                doc.insertString(doc.getLength(), "\n", null);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    public void showError(String message) {
        appendToChat(message + "\n", Color.RED);
    }
}