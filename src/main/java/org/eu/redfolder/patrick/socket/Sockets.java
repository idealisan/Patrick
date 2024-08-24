package org.eu.redfolder.patrick.socket;

import static org.eu.redfolder.patrick.threadPool.ThreadPool.pool;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;

public class Sockets {
    public static void transferSocket(Socket a, Socket b) {
        pool.execute(
                new Thread(
                        () -> {
                            try {
                                a.getInputStream().transferTo(b.getOutputStream());
                            } catch (IOException e) {
                                System.out.println(e.getMessage());
                            }
                        }));
        pool.execute(
                new Thread(
                        () -> {
                            try {
                                b.getInputStream().transferTo(a.getOutputStream());
                            } catch (IOException e) {
                                System.out.println(e.getMessage());
                            }
                        }));
    }

    public static void transferSocket(Socket tcp, DatagramSocket udp) {
        Thread.ofVirtual()
                .start(
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
                        });
        Thread.ofVirtual()
                .start(
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
                        });
    }
}
