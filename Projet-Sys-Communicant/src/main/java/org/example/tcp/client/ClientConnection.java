package org.example.tcp.client;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.Objects;

public class ClientConnection {
    private final Socket socket;
    private final PrintWriter outToServer;
    private final BufferedReader inFromServer;
    private final MessageHandler messageHandler;

    public ClientConnection(String host, int port, String username, MessageHandler handler) throws IOException {
        this.socket = new Socket(host, port);
        this.outToServer = new PrintWriter(socket.getOutputStream(), true);
        this.inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.messageHandler = handler;
        outToServer.println(username);
        new Thread(this::listen).start();
    }

    private void listen() {
        try {
            String line;
            while (Objects.nonNull(line = inFromServer.readLine())) {
                messageHandler.handle(line);
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() ->
                    messageHandler.showError("❌ Disconnected from server.")
            );
        }
    }

    public void sendToServer(String message) {
        outToServer.println(message);
    }

    public void close() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
            if (outToServer != null) outToServer.close();
            if (inFromServer != null) inFromServer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
