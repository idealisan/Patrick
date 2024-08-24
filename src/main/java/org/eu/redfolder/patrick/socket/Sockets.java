package org.eu.redfolder.patrick.socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;

public class Sockets {
    public static void transferSocket(Socket a, Socket b) {
        new Thread(
                        () -> {
                            try {
                                a.getInputStream().transferTo(b.getOutputStream());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                .start();
        new Thread(
                        () -> {
                            try {
                                b.getInputStream().transferTo(a.getOutputStream());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                .start();
    }

    public static void transferSocket(Socket tcp, DatagramSocket udp) {
        new Thread(
                        () -> {
                            byte[] buffer = new byte[Short.MAX_VALUE * 2 - 1];
                            while (!tcp.isClosed()) {
                                try {
                                    int read = tcp.getInputStream().read(buffer);
                                    DatagramPacket datagramPacket =
                                            new DatagramPacket(buffer, read);
                                    udp.send(datagramPacket);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        })
                .start();
        new Thread(
                        () -> {
                            byte[] buffer = new byte[Short.MAX_VALUE * 2 - 1];
                            try {
                                DatagramPacket datagramPacket =
                                        new DatagramPacket(buffer, buffer.length);
                                udp.receive(datagramPacket);
                                tcp.getOutputStream()
                                        .write(
                                                datagramPacket.getData(),
                                                datagramPacket.getOffset(),
                                                datagramPacket.getLength());
                                tcp.getOutputStream().flush();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                .start();
    }
}
