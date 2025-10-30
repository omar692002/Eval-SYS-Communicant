package tcp.communication.multicast;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;

public class MulticastServerGUI extends JFrame {
    private JTextPane textPane;
    private JTextField inputField;
    private JButton sendTextButton, sendImageButton;
    private MulticastSocket socket;
    private InetAddress group;

    public MulticastServerGUI() {
        setTitle("Multicast Server");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        textPane = new JTextPane();
        textPane.setEditable(false);
        add(new JScrollPane(textPane), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendTextButton = new JButton("Send Text");
        sendImageButton = new JButton("Send Image");
        JPanel buttons = new JPanel();
        buttons.add(sendTextButton);
        buttons.add(sendImageButton);

        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        try {
            socket = new MulticastSocket();
            group = InetAddress.getByName("230.0.0.0");
        } catch (Exception e) {
            e.printStackTrace();
        }

        sendTextButton.addActionListener(e -> sendText());
        sendImageButton.addActionListener(e -> sendImage());
    }

    private void sendText() {
        try {
            String msg = inputField.getText().trim();
            if (!msg.isEmpty()) {
                byte[] data = ("TEXT:" + msg).getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, group, 4446);
                socket.send(packet);
                appendText("Server: " + msg);
                inputField.setText("");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendImage() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                byte[] imageBytes = Files.readAllBytes(file.toPath());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                baos.write("IMAGE:".getBytes());
                baos.write(imageBytes);
                byte[] data = baos.toByteArray();

                DatagramPacket packet = new DatagramPacket(data, data.length, group, 4446);
                socket.send(packet);
                appendImage(new ImageIcon(file.getAbsolutePath()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void appendText(String msg) {
        try {
            StyledDocument doc = textPane.getStyledDocument();
            doc.insertString(doc.getLength(), msg + "\n", null);
        } catch (Exception ignored) {}
    }

    private void appendImage(ImageIcon icon) {
        try {
            textPane.setCaretPosition(textPane.getDocument().getLength());
            textPane.insertIcon(icon);
            textPane.getDocument().insertString(textPane.getDocument().getLength(), "\n", null);
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MulticastServerGUI().setVisible(true));
    }
}
