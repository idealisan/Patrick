package org.eu.redfolder.patrick.socket;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class Sockets {
    public static void transferDatagram(
            DatagramSocket clientSocket,
            DatagramPacket clientPacket,
            DatagramSocket remoteSocket,
            DatagramPacket remotePacket)
            throws IOException {
        Thread.ofVirtual().start(() -> transfer(clientSocket, clientPacket, remoteSocket));
        Thread.ofVirtual().start(() -> transfer(remoteSocket, remotePacket, clientSocket));
    }

    private static void transfer(
            DatagramSocket clientSocket, DatagramPacket clientPacket, DatagramSocket remoteSocket) {
        DatagramChannel clientSocketChannel = clientSocket.getChannel();
        DatagramChannel remoteSocketChannel = remoteSocket.getChannel();
        while (clientSocket.isConnected() && remoteSocket.isConnected()) {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(clientPacket.getData());
                clientSocketChannel.receive(buffer);
                buffer.flip();
                buffer.position(3);
                byte ATYP = buffer.get();
                byte[] ip = null;
                short port = 0;
                if (ATYP == 0x01) {
                    ip = new byte[4];
                    buffer.get(ip);
                    port = buffer.getShort();
                } else if (ATYP == 0x04) {
                    ip = new byte[16];
                    buffer.get(ip);
                    port = buffer.getShort();
                } else {
                    System.out.println("Not supported ATYP: " + ATYP);
                    Thread.currentThread().interrupt();
                }
                assert ip != null;
                remoteSocketChannel.send(
                        buffer, new InetSocketAddress(InetAddress.getByAddress(ip), port));
                buffer.flip();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public static void exchangeSocket(Socket a, Socket b) {
        Thread.ofVirtual().start(() -> transfer(a, b));
        Thread.ofVirtual().start(() -> transfer(b, a));
    }

    private static void transfer(Socket a, Socket b) {
        try {
            a.getInputStream().transferTo(b.getOutputStream());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
