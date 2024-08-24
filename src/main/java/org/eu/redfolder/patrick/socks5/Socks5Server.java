package org.eu.redfolder.patrick.socks5;

import org.eu.redfolder.patrick.socket.Sockets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Socks5Server extends Thread {
    private final Socket clientSocket;

    public Socks5Server(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            int nMethods = inputStream.read();
            byte[] methods = new byte[nMethods];
            inputStream.read(methods);
            boolean supportNoAuth = false;
            for (byte b : methods) {
                if (b == 0) {
                    supportNoAuth = true;
                    break;
                }
            }
            if (!supportNoAuth) {
                throw new RuntimeException("No supported authentication method found");
            }
            clientSocket.getOutputStream().write(new byte[] {0x05, 0x00});
            clientSocket.getOutputStream().flush();
            handleTunnel(clientSocket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleTunnel(Socket clientSocket) throws IOException {
        InputStream clientInputStream = clientSocket.getInputStream();
        while (clientInputStream.available() <= 0) {
            Thread.yield();
        }
        byte[] handshake = new byte[clientInputStream.available()];
        clientInputStream.read(handshake);
        byte[] address = Arrays.copyOfRange(handshake, 4, handshake.length - 2);
        InetAddress inetAddress;
        if (handshake[3] == 0x01 || handshake[3] == 0x04) {
            // IPv4
            inetAddress = InetAddress.getByAddress(address);
        } else if (handshake[3] == 0x02) {
            // domain name
            inetAddress = InetAddress.getByName(new String(address, StandardCharsets.ISO_8859_1));
        } else {
            throw new RuntimeException("Invalid handshake received for ATYP.");
        }
        int port = handshake[handshake.length - 2] << 8 | (handshake[handshake.length - 1] & 0xff);
        byte cmd = handshake[1];
        handshake[1] = 0;

        System.out.printf("SOCKS5 Connection %s:%s%n", inetAddress, port);
        if (cmd == 0x01) {
            clientSocket.getOutputStream().write(handshake);
            clientSocket.getOutputStream().flush();
            handleTCP(clientSocket, inetAddress, port);
        } else if (cmd == 0x03) {
            InetAddress localAddress = clientSocket.getLocalAddress();
            int udpPort = handleUDP(localAddress);
            byte[] bytes = localAddress.getAddress();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.write(
                    new byte[] {0x05, 0x00, 0x00, (byte) (bytes.length == 4 ? 0x01 : 0x04)});
            byteArrayOutputStream.write(bytes);
            byteArrayOutputStream.write(udpPort >>> 8);
            byteArrayOutputStream.write(port);
            byteArrayOutputStream.writeTo(clientSocket.getOutputStream());
            clientSocket.getOutputStream().flush();
        } else {
            System.out.println("Unsupported CMD");
        }
    }

    private int handleUDP(InetAddress address) throws IOException {
        DatagramSocket clientDatagramSocket = new DatagramSocket();
        clientDatagramSocket.bind(new InetSocketAddress(address, 0));
        Thread.ofVirtual()
                .start(
                        () -> {
                            try {
                                DatagramPacket clientPacket =
                                        new DatagramPacket(
                                                new byte[Short.MAX_VALUE], Short.MAX_VALUE);
                                DatagramSocket remoteSocket = new DatagramSocket();
                                DatagramPacket remotePacket =
                                        new DatagramPacket(
                                                new byte[Short.MAX_VALUE], Short.MAX_VALUE);
                                Sockets.transferDatagram(
                                        clientDatagramSocket,
                                        clientPacket,
                                        remoteSocket,
                                        remotePacket);
                            } catch (IOException e) {
                                System.out.println(e.getMessage());
                            }
                        });
        return clientDatagramSocket.getPort();
    }

    private void handleTCP(Socket clientSocket, InetAddress inetAddress, int port)
            throws IOException {
        Socket remoteSocket = new Socket(inetAddress, port);
        Sockets.exchangeSocket(clientSocket, remoteSocket);
    }
}
