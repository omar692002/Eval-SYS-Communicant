package org.example.udp.server;

import java.net.InetAddress;

/**
 * Represents a connected UDP chat client.
 */
public record ClientInfo(InetAddress address, int port, String pseudo) {}
