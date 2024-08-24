package org.eu.redfolder.patrick;

import org.eu.redfolder.patrick.http.HttpServer;
import org.eu.redfolder.patrick.socks5.Socks5Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) throws IOException {
        ServerSocket proxyServer = new ServerSocket(1080);
        while (!proxyServer.isClosed()) {
            Socket clientSocket = proxyServer.accept();
            InputStream inputStream = clientSocket.getInputStream();
            int firstByte = inputStream.read();
            if (firstByte == 0x05) {
                Thread.ofVirtual().start(new Socks5Server(clientSocket));
            } else {
                Thread.ofVirtual().start(new HttpServer(firstByte, clientSocket));
            }
        }
    }
}
