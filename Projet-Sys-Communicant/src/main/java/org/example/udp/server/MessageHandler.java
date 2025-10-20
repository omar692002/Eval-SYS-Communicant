package org.example.udp.server;

import java.net.DatagramSocket;
import java.net.InetAddress;

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
        clientManager.broadcastImage(socket, data);
    }
}
