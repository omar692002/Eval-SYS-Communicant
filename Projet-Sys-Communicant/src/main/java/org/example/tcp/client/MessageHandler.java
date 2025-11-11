package org.example.tcp.client;

import org.example.tcp.enums.MessageType;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

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
            case FILE -> handleFileMessage(message);
            case FILE_PRIVATE -> handlePrivateFile(message);
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


    private void handleFileMessage(String msg) {
        try {
            String[] parts = msg.split("\\|", 4);
            if (parts.length != 4) {
                System.out.println("[ERROR] Invalid FILE message format (" + parts.length + " parts): " + msg);
                return;
            }

            String sender = parts[1];
            String filename = parts[2];
            String base64 = parts[3];

            if (sender.equals(username)) {
                System.out.println("[INFO] Ignoring own file message for " + filename);
                return;
            }
            saveFile(sender, filename, base64, false);
        } catch (Exception e) {
            e.printStackTrace();
            appendToChat("⚠️ Failed to process file message.\n");
        }
    }

    private void handlePrivateFile(String msg) {
        try {
            System.out.println("[DEBUG] Received FILE_PRIVATE message: " + msg);

            String[] parts = msg.split("\\|", 4);
            if (parts.length != 4) {
                System.out.println("[ERROR] Invalid FILE_PRIVATE message format (" + parts.length + " parts): " + msg);
                return;
            }

            String sender = parts[1];
            String filename = parts[2];
            String base64 = parts[3];

            // 🚫 Skip if this client is the sender
            if (sender.equals(username)) {
                System.out.println("[INFO] Ignoring own private file message for " + filename);
                return;
            }

            System.out.println("[INFO] FILE_PRIVATE message parsed from " + sender + " → " + filename);
            saveFile(sender, filename, base64, true);
        } catch (Exception e) {
            e.printStackTrace();
            appendToChat("⚠️ Failed to process private file message.\n");
        }
    }

    private void saveFile(String sender, String filename, String base64, boolean isPrivate) {
        SwingUtilities.invokeLater(() -> {
            try {
                byte[] fileBytes = Base64.getDecoder().decode(base64);

                int choice = showFileOptionsDialog(sender, filename);
                switch (choice) {
                    case 0 -> previewFile(sender, filename, fileBytes);
                    case 1 -> downloadFile(sender, filename, fileBytes, isPrivate);
                    default ->
                            appendToChat("⚠️ You ignored the file \"" + filename + "\" from " + sender + "\n");
                }

            } catch (Exception e) {
                appendToChat("❌ Failed to handle \"" + filename + "\": " + e.getMessage() + "\n");
                e.printStackTrace();
            }
        });
    }

    private int showFileOptionsDialog(String sender, String filename) {
        Object[] options = {"Preview", "Download", "Cancel"};
        return JOptionPane.showOptionDialog(
                chatPane,
                sender + " sent you a file:\n" + filename + "\nWhat would you like to do?",
                "Incoming File",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1] // Default: Download
        );
    }

    private void previewFile(String sender, String filename, byte[] fileBytes) {
        if (!isTextFile(filename)) {
            appendToChat("⚠️ Preview not supported for non-text files (" + filename + ")\n");
            return;
        }

        try {
            String content = new String(fileBytes);
            JTextArea textArea = new JTextArea(content);
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(500, 400));

            JOptionPane.showMessageDialog(
                    chatPane,
                    scrollPane,
                    "Preview: " + filename,
                    JOptionPane.INFORMATION_MESSAGE
            );

            appendToChat("👀 You previewed \"" + filename + "\" from " + sender + "\n");
        } catch (Exception e) {
            appendToChat("❌ Failed to preview \"" + filename + "\": " + e.getMessage() + "\n");
        }
    }

    private void downloadFile(String sender, String filename, byte[] fileBytes, boolean isPrivate) {
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(filename));

            int result = chooser.showSaveDialog(chatPane);
            if (result != JFileChooser.APPROVE_OPTION) {
                appendToChat("❌ Download cancelled: " + filename + "\n");
                return;
            }

            File selectedFile = chooser.getSelectedFile();
            Files.write(selectedFile.toPath(), fileBytes);

            String prefix = isPrivate ? "🔒 (Private)" : "📥";
            appendToChat(prefix + " Downloaded \"" + filename + "\" → " + selectedFile.getAbsolutePath() + "\n");
        } catch (Exception e) {
            appendToChat("❌ Failed to download \"" + filename + "\": " + e.getMessage() + "\n");
        }
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

            // 🚫 Ignore own images
            if (sender.equals(username)) return;

            appendToChat("🖼️ " + sender + " sent image: " + filename + "\n");
            displayImage(sender + " (" + filename + ")", base64);
        } catch (Exception e) {
            e.printStackTrace();
            appendToChat("⚠️ Failed to display image from " + msg + "\n");
        }
    }

    public void displayImage(String sender, String base64) {
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

        // 🚫 Ignore own private images
        if (sender.equals(username)) return;

        appendToChat("🔒 (Private) " + sender + " sent an image\n");
        displayImage(sender, base64);
    }

    public void appendToChat(String text) {
        int sepIndex = text.indexOf(':');

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

        String sender = text.substring(0, sepIndex).trim();
        String message = text.substring(sepIndex + 1).trim();

        Color color = sender.equals(username)
                ? Color.BLUE
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
        int hash = Math.abs(name.hashCode());
        int r = 50 + (hash % 150);
        int g = 50 + ((hash / 150) % 150);
        int b = 50 + ((hash / 22500) % 150);
        return new Color(r, g, b);
    }

    private boolean isTextFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".txt") ||
                lower.endsWith(".log") ||
                lower.endsWith(".csv") ||
                lower.endsWith(".json") ||
                lower.endsWith(".xml") ||
                lower.endsWith(".html");
    }

}
