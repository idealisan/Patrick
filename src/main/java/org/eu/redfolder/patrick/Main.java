package org.eu.redfolder.patrick;

import org.eu.redfolder.patrick.http.HttpServer;
import org.eu.redfolder.patrick.socks5.Socks5Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        ServerSocket proxyServer = new ServerSocket(7080);
        while (!proxyServer.isClosed()) {
            Socket clientSocket = proxyServer.accept();
            InputStream inputStream = clientSocket.getInputStream();
            int firstByte = inputStream.read();
            if (firstByte == 0x05) {
                new Socks5Server(clientSocket).start();
            } else {
                new HttpServer(firstByte, clientSocket).start();
            }
        }
    }
}
