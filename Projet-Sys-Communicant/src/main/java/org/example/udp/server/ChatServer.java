package org.example.udp.server;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.util.Arrays;

/**
 * Core UDP chat server logic.
 * Listens for packets and delegates handling.
 */
public class ChatServer implements Runnable {

    private static final int PORT = 9876;
    private final ClientManager clientManager = new ClientManager();
    private final MessageHandler messageHandler = new MessageHandler(clientManager);

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.println("🟢 UDP Server running on port " + PORT);

            var buffer = new byte[65535];

            while (true) {
                var packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                var data = Arrays.copyOf(packet.getData(), packet.getLength());
                messageHandler.handle(socket, data, packet.getAddress(), packet.getPort());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
