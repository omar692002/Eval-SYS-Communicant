package org.example.udp.server;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * Handles parsing and executing incoming chat messages.
 */
public class MessageHandler {

    private final ClientManager clientManager;

    public MessageHandler(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    public void handle(DatagramSocket socket, byte[] data, InetAddress address, int port) {
        String msg = new String(data);
        if (msg.startsWith("JOIN:")) handleJoin(socket, msg, address, port);
        else if (msg.startsWith("LEAVE:")) handleLeave(socket, msg, address, port);
        else if (msg.startsWith("TO:")) handlePrivate(socket, msg);
        else if (msg.startsWith("ALL:")) handleBroadcast(socket, msg);
        else if (msg.startsWith("IMG:")) handleImage(socket, data);
        else if (msg.startsWith("FILE:")) handleFile(socket, data);

    }

    private void handleJoin(DatagramSocket socket, String msg, InetAddress address, int port)  {
        var pseudo = msg.substring(5).trim();
        var newClient = new ClientInfo(address, port, pseudo);
        clientManager.addClient(newClient);
        System.out.println("👤 " + pseudo + " joined (" + address + ":" + port + ")");
        clientManager.broadcastUserList(socket);
        clientManager.broadcast(socket, pseudo + " joined the chat 🟢", null);
    }

    private void handleLeave(DatagramSocket socket, String msg, InetAddress address, int port) {
        String pseudo = msg.substring(6).trim();
        clientManager.removeClient(address, port);
        clientManager.broadcast(socket, pseudo + " left the chat 🔴", null);
        clientManager.broadcastUserList(socket);
    }

    private void handlePrivate(DatagramSocket socket, String msg) {
        String[] parts = msg.split(":", 4);
        if (parts.length < 4) return;

        var target = parts[1];
        var sender = parts[2];
        var content = parts[3];

        clientManager.findByPseudo(target)
                .ifPresent(c -> clientManager.sendTo(socket, sender + " -> You: " + content, c));
    }

    private void handleBroadcast(DatagramSocket socket, String msg) {
        String[] parts = msg.split(":", 3);
        if (parts.length < 3) return;

        String sender = parts[1];
        String text = parts[2];

        ClientInfo senderClient = clientManager.findByPseudo(sender).orElse(null);
        clientManager.broadcast(socket, sender + " -> ALL: " + text, senderClient);
    }

    private void handleImage(DatagramSocket socket, byte[] data) {
        try {
            // Find positions of the first 4 colons (IMG:target:sender:fileName:)
            int[] colons = new int[4];
            int count = 0;
            for (int i = 0; i < data.length && count < 4; i++) {
                if (data[i] == ':') colons[count++] = i;
            }

            if (count < 4) return;

            String target = new String(data, 4, colons[1] - 4, StandardCharsets.UTF_8);
            String sender = new String(data, colons[1] + 1, colons[2] - colons[1] - 1, StandardCharsets.UTF_8);
            String fileName = new String(data, colons[2] + 1, colons[3] - colons[2] - 1, StandardCharsets.UTF_8);
            if (target.equals("ALL")) {
                clientManager.broadcastImage(socket, data);
            } else {
                clientManager.findByPseudo(target).ifPresent(c -> clientManager.sendPacket(socket, data, c));
            }

            System.out.println("📷 " + sender + " sent image '" + fileName + "' to " + target);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void handleFile(DatagramSocket socket, byte[] data) {
        try {
            int[] colons = new int[4];
            int count = 0;
            for (int i = 0; i < data.length && count < 4; i++) {
                if (data[i] == ':') colons[count++] = i;
            }

            if (count < 4) return;

            String target = new String(data, 5, colons[1] - 5, StandardCharsets.UTF_8);
            String sender = new String(data, colons[1] + 1, colons[2] - colons[1] - 1, StandardCharsets.UTF_8);
            String fileName = new String(data, colons[2] + 1, colons[3] - colons[2] - 1, StandardCharsets.UTF_8);

            if (target.equals("ALL")) {
                clientManager.broadcastImage(socket, data); // reuse for all binary packets
            } else {
                clientManager.findByPseudo(target).ifPresent(c -> clientManager.sendPacket(socket, data, c));
            }

            System.out.println("📁 " + sender + " sent file '" + fileName + "' to " + target);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
