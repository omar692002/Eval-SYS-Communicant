package org.example.udp;

import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 9876;
    private static final Set<ClientInfo> clients = new HashSet<>();

    private record ClientInfo(InetAddress address, int port, String pseudo) {}

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.println("🟢 UDP Server running on port " + PORT);
            byte[] buffer = new byte[65535];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
                String message = new String(data, 0, Math.min(10, data.length));

                InetAddress address = packet.getAddress();
                int port = packet.getPort();

                if (message.startsWith("JOIN:")) handleJoin(socket, new String(data), address, port);
                else if (message.startsWith("LEAVE:")) handleLeave(socket, new String(data), address, port);
                else if (message.startsWith("TO:")) handlePrivate(socket, new String(data));
                else if (message.startsWith("ALL:")) handleBroadcast(socket, new String(data));
                else if (message.startsWith("IMG:")) handleImage(socket, data);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleJoin(DatagramSocket socket, String msg, InetAddress address, int port) throws Exception {
        String pseudo = msg.substring(5);
        ClientInfo newClient = new ClientInfo(address, port, pseudo);
        clients.add(newClient);
        System.out.println("👤 " + pseudo + " joined (" + address + ":" + port + ")");
        broadcastUserList(socket);
        broadcast(socket, pseudo + " joined the chat 🟢", null);
    }

    private static void handleLeave(DatagramSocket socket, String msg, InetAddress address, int port) throws Exception {
        String pseudo = msg.substring(6);
        clients.removeIf(c -> c.address.equals(address) && c.port == port);
        broadcast(socket, pseudo + " left the chat 🔴", null);
        broadcastUserList(socket);
    }

    private static void handlePrivate(DatagramSocket socket, String msg) throws Exception {
        String[] parts = msg.split(":", 4);
        if (parts.length < 4) return;

        String target = parts[1];
        String sender = parts[2];
        String content = parts[3];

        clients.stream()
                .filter(c -> c.pseudo.equals(target))
                .findFirst()
                .ifPresent(c -> sendTo(socket, sender + " -> You: " + content, c));
    }

    private static void handleBroadcast(DatagramSocket socket, String msg) throws Exception {
        String[] parts = msg.split(":", 3);
        if (parts.length < 3) return;

        String sender = parts[1];
        String text = parts[2];
        ClientInfo senderClient = clients.stream().filter(c -> c.pseudo.equals(sender)).findFirst().orElse(null);

        broadcast(socket, sender + " -> ALL: " + text, senderClient);
    }

    private static void handleImage(DatagramSocket socket, byte[] data) throws Exception {
        for (ClientInfo c : clients) {
            DatagramPacket p = new DatagramPacket(data, data.length, c.address, c.port);
            socket.send(p);
        }
    }

    private static void broadcastUserList(DatagramSocket socket) throws Exception {
        String userList = String.join(",", clients.stream().map(ClientInfo::pseudo).toList());
        for (ClientInfo c : clients) sendTo(socket, "USERLIST:" + userList, c);
    }

    private static void broadcast(DatagramSocket socket, String msg, ClientInfo exclude) throws Exception {
        byte[] data = msg.getBytes();
        for (ClientInfo c : clients) {
            if (exclude != null && c.equals(exclude)) continue;
            DatagramPacket p = new DatagramPacket(data, data.length, c.address, c.port);
            socket.send(p);
        }
    }

    private static void sendTo(DatagramSocket socket, String msg, ClientInfo client) {
        try {
            byte[] data = msg.getBytes();
            DatagramPacket p = new DatagramPacket(data, data.length, client.address, client.port);
            socket.send(p);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
