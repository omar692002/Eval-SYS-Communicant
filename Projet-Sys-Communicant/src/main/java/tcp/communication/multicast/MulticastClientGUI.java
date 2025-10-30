package tcp.communication.multicast;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Arrays;

public class MulticastClientGUI extends JFrame {
    private JTextPane textPane;
    private MulticastSocket socket;
    private InetAddress group;

    public MulticastClientGUI() {
        setTitle("Multicast Client");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        textPane = new JTextPane();
        textPane.setEditable(false);
        add(new JScrollPane(textPane), BorderLayout.CENTER);

        try {
            socket = new MulticastSocket(4446);
            group = InetAddress.getByName("230.0.0.0");
            socket.joinGroup(group);

            new Thread(this::receiveMessages).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        try {
            while (true) {
                byte[] buffer = new byte[65507]; // max UDP packet size
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());

                // 🔍 Debugging output
                String rawData = new String(data);
                System.out.println("---- RECEIVED PACKET ----");
                System.out.println("Raw data: " + rawData);
                System.out.println("Length: " + data.length);

                // Check for headers
                if (new String(data, 0, 5).equals("TEXT:")) {
                    String message = new String(data, 5, data.length - 5);
                    System.out.println("Parsed as TEXT: " + message);
                    SwingUtilities.invokeLater(() -> appendText(message));

                } else if (new String(data, 0, 6).equals("IMAGE:")) {
                    System.out.println("Parsed as IMAGE (" + data.length + " bytes)");
                    byte[] imgData = Arrays.copyOfRange(data, 6, data.length);
                    ImageIcon icon = new ImageIcon(imgData);
                    SwingUtilities.invokeLater(() -> appendImage(icon));

                } else {
                    System.out.println("⚠️ Unknown packet format!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        SwingUtilities.invokeLater(() -> new MulticastClientGUI().setVisible(true));
    }
}
