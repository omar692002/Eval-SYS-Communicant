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
    private final Map<String, Color> userColors = new HashMap<>();

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
            default -> appendToChat(message + "\n");
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
            appendToChat("🖼️ " + sender + " sent image: " + filename + "\n");
            displayImage(sender + " (" + filename + ")", base64);
        } catch (Exception e) {
            e.printStackTrace();
            appendToChat("⚠️ Failed to display image from " + msg + "\n");
        }
    }


    private void displayImage(String sender, String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            ImageIcon icon = new ImageIcon(bytes);
            appendIcon(sender, icon);
        } catch (Exception e) {
            appendToChat("⚠️ Failed to display image from " + sender + "\n");
        }
    }

    private void handlePrivateImage(String msg) {
        String[] parts = msg.split("\\|", 4);
        if (parts.length != 4) return;
        String sender = parts[1];
        String base64 = parts[3];
        appendToChat("🔒 (Private) " + sender + " sent an image\n");
        displayImage(sender, base64);
    }


    public void appendToChat(String text) {

        int sepIndex = text.indexOf(':');

        // 🧱 If no ':' found, treat as system or general message
        if (sepIndex == -1) {
            SwingUtilities.invokeLater(() -> {
                StyledDocument doc = chatPane.getStyledDocument();
                Style style = chatPane.addStyle("SystemStyle", null);
                StyleConstants.setForeground(style, Color.GRAY);
                StyleConstants.setItalic(style, true);
                try {
                    doc.insertString(doc.getLength(), text + "\n", style);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            });
            return;
        }

        // 🧩 Otherwise, parse sender + message
        String sender = text.substring(0, sepIndex).trim();
        String message = text.substring(sepIndex + 1).trim();

        Color color = sender.equals(username)
                ? Color.BLUE // your own messages
                : getColorForUser(sender);

        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatPane.getStyledDocument();
            Style style = chatPane.addStyle("UserStyle", null);
            StyleConstants.setForeground(style, color);
            try {
                doc.insertString(doc.getLength(), sender + ": " + message + "\n", style);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

//    public void appendToChat(String text) {
//        System.out.println(text);
//        int sepIndex = text.indexOf(':');
//        String sender = text.substring(0, sepIndex).trim();
//        var  message=text .substring(sepIndex + 1).trim();
//
//        Color color = sender.equals(username)
//                ? Color.BLUE // your own messages
//                : getColorForUser(sender);
//
//        SwingUtilities.invokeLater(() -> {
//            StyledDocument doc = chatPane.getStyledDocument();
//            Style style = chatPane.addStyle("Style", null);
//            StyleConstants.setForeground(style, color);
//            try {
//                doc.insertString(doc.getLength(), message, style);
//            } catch (BadLocationException e) {
//                e.printStackTrace();
//            }
//        });
//    }

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
        appendToChat(message + "\n");
    }
    private Color getColorForUser(String sender) {
        if (sender == null || sender.isBlank()) return Color.BLACK;
        return userColors.computeIfAbsent(sender, this::generateColorFromName);
    }

    private Color generateColorFromName(String name) {
        // deterministic color based on hash (so same user always same color)
        int hash = Math.abs(name.hashCode());
        int r = 50 + (hash % 150);
        int g = 50 + ((hash / 150) % 150);
        int b = 50 + ((hash / 22500) % 150);
        return new Color(r, g, b);
    }
}