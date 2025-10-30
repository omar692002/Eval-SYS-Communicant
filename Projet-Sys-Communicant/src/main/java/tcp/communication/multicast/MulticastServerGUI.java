package tcp.communication.multicast;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;

public class MulticastServerGUI extends JFrame {
    private JTextArea textArea;
    private JTextField inputField;
    private JButton sendButton;
    private MulticastSocket socket;
    private InetAddress group;
    private final int PORT = 4446;

    public MulticastServerGUI() {
        setTitle("Multicast Server");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        textArea = new JTextArea();
        textArea.setEditable(false);
        add(new JScrollPane(textArea), BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");

        panel.add(inputField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);
        add(panel, BorderLayout.SOUTH);

        try {
            socket = new MulticastSocket();
            group = InetAddress.getByName("230.0.0.0");
        } catch (Exception e) {
            e.printStackTrace();
        }

        sendButton.addActionListener(e -> sendMessage());
    }

    private void sendMessage() {
        try {
            String message = inputField.getText();
            if (!message.isEmpty()) {
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
                socket.send(packet);
                textArea.append("Server: " + message + "\n");
                inputField.setText("");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MulticastServerGUI().setVisible(true));
    }
}
