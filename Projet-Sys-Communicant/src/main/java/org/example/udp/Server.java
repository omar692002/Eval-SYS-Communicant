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

                    // Send the current user list to the newly joined client
                    String userList = String.join(",",
                            clients.stream().map(ClientInfo::pseudo).toList());
                    sendToOne(socket, "USERLIST:" + userList, newClient);

                    // Notify everyone else
                    broadcast(socket, pseudo + " joined the chat 🟢", address, port);
                }

                else if (message.startsWith("LEAVE:")) {
                    String pseudo = message.substring(6);
                    clients.removeIf(c -> c.address.equals(address) && c.port == port);
                    System.out.println("❌ " + pseudo + " left the chat");
                    broadcast(socket, pseudo + " left the chat 🔴", address, port);
                }

                else {
                    // Normal chat message — broadcast to others only
                    broadcast(socket, message, address, port);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void broadcast(DatagramSocket socket, String message,
                                  InetAddress senderAddr, int senderPort) throws Exception {
        byte[] data = message.getBytes();
        for (ClientInfo c : clients) {
            if (c.address.equals(senderAddr) && c.port == senderPort)
                continue; // Skip sender
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
