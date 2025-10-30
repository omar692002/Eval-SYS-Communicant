package tcp.communication.multicast;

import javax.swing.*;
import java.awt.*;
import java.net.*;

public class MulticastClientGUI extends JFrame {
    private JTextArea textArea;
    private MulticastSocket socket;
    private InetAddress group;
    private final int PORT = 4446;

    public MulticastClientGUI() {
        setTitle("Multicast Client");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        textArea = new JTextArea();
        textArea.setEditable(false);
        add(new JScrollPane(textArea), BorderLayout.CENTER);

        try {
            socket = new MulticastSocket(PORT);
            group = InetAddress.getByName("230.0.0.0");
            socket.joinGroup(group);

            Thread receiverThread = new Thread(() -> {
                while (true) {
                    try {
                        byte[] buffer = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        String message = new String(packet.getData(), 0, packet.getLength());
                        SwingUtilities.invokeLater(() -> textArea.append(message + "\n"));
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            });
            receiverThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MulticastClientGUI().setVisible(true));
    }
}
