package org.example.udp.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Handles all client-related operations such as add, remove, and broadcast.
 */
public class ClientManager {

    private final Set<ClientInfo> clients = ConcurrentHashMap.newKeySet();

    public void addClient(ClientInfo client) {
        clients.add(client);
    }

    public void removeClient(InetAddress address, int port) {
        clients.removeIf(c -> c.address().equals(address) && c.port() == port);
    }

    public Optional<ClientInfo> findByPseudo(String pseudo) {
        return clients.stream()
                .filter(c -> c.pseudo().equals(pseudo))
                .findFirst();
    }

    public String getUserList() {
        return clients.stream()
                .map(ClientInfo::pseudo)
                .collect(Collectors.joining(","));
    }

    public void broadcastUserList(DatagramSocket socket) {
        var msg = "USERLIST:" + getUserList();
        clients.forEach(c -> sendTo(socket, msg, c));
    }

    public void broadcast(DatagramSocket socket, String msg, ClientInfo exclude) {
        var data = msg.getBytes();
        clients.stream()
                .filter(c -> !Objects.equals(c, exclude))
                .forEach(c -> sendPacket(socket, data, c));
    }

    public void sendTo(DatagramSocket socket, String msg, ClientInfo client) {
        sendPacket(socket, msg.getBytes(), client);
    }

    void sendPacket(DatagramSocket socket, byte[] data, ClientInfo c) {
        try {
            socket.send(new DatagramPacket(data, data.length, c.address(), c.port()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void broadcastImage(DatagramSocket socket, byte[] data) {
        clients.forEach(c -> sendPacket(socket, data, c));
    }
}
