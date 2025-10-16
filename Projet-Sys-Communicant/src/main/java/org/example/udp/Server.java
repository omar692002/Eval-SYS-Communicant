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
            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength()).trim();

                InetAddress address = packet.getAddress();
                int port = packet.getPort();

                if (message.startsWith("JOIN:")) {
                    String pseudo = message.substring(5);
                    ClientInfo newClient = new ClientInfo(address, port, pseudo);
                    clients.add(newClient);
                    System.out.println("👤 " + pseudo + " joined (" + address + ":" + port + ")");

                    // Notifier tout le monde avec la liste mise à jour
                    String userList = String.join(",", clients.stream().map(ClientInfo::pseudo).toList());
                    for (ClientInfo c : clients) {
                        sendToOne(socket, "USERLIST:" + userList, c);
                    }

                    broadcast(socket, pseudo + " joined the chat 🟢", newClient);

                } else if (message.startsWith("LEAVE:")) {
                    String pseudo = message.substring(6);
                    clients.removeIf(c -> c.address.equals(address) && c.port == port);
                    System.out.println("❌ " + pseudo + " left the chat");
                    broadcast(socket, pseudo + " left the chat 🔴", null);

                } else if (message.startsWith("TO:")) {
                    // Format: TO:<targetPseudo>:<senderPseudo>:<message>
                    String[] parts = message.split(":", 4);
                    if (parts.length < 4) continue;
                    String targetPseudo = parts[1];
                    String senderPseudo = parts[2];
                    String msgText = parts[3];

                    Optional<ClientInfo> target = clients.stream()
                            .filter(c -> c.pseudo.equals(targetPseudo))
                            .findFirst();

                    if (target.isPresent()) {
                        sendToOne(socket, senderPseudo + " -> You: " + msgText, target.get());
                    }

                } else if (message.startsWith("ALL:")) {
                    // Format: ALL:<senderPseudo>:<message>
                    String[] parts = message.split(":", 3);
                    if (parts.length < 3) continue;
                    String senderPseudo = parts[1];
                    String msgText = parts[2];
                    broadcast(socket, senderPseudo + " -> ALL: " + msgText, null);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void broadcast(DatagramSocket socket, String message, ClientInfo exclude) throws Exception {
        byte[] data = message.getBytes();
        for (ClientInfo c : clients) {
            if (exclude != null && c.equals(exclude)) continue;
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, c.address, c.port);
            socket.send(sendPacket);
        }
    }

    private static void sendToOne(DatagramSocket socket, String message, ClientInfo client) throws Exception {
        byte[] data = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, client.address, client.port);
        socket.send(sendPacket);
    }
}
