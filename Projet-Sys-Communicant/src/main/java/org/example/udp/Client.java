package org.example.udp;

import java.net.*;
import java.util.Scanner;

public class Client {
    private static final int SERVER_PORT = 9876 ;

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        DatagramSocket socket = new DatagramSocket();
        InetAddress serverAddress = InetAddress.getByName("localhost");

        // Thread pour écouter les messages entrants
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("\n📩 " + message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        System.out.print("Entrez votre pseudo : ");
        String pseudo = scanner.nextLine();

        while (true) {
            String msg = scanner.nextLine();
            String fullMessage = pseudo + ": " + msg;
            byte[] data = fullMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
            socket.send(packet);
        }
    }
}
