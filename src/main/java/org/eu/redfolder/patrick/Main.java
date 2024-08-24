package org.eu.redfolder.patrick;

import org.eu.redfolder.patrick.http.HttpServer;
import org.eu.redfolder.patrick.socks5.Socks5Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import static org.eu.redfolder.patrick.threadPool.ThreadPool.pool;

public class Main {
    public static void main(String[] args) throws IOException {
        ServerSocket proxyServer = new ServerSocket(1080);
        while (!proxyServer.isClosed()) {
            Socket clientSocket = proxyServer.accept();
            InputStream inputStream = clientSocket.getInputStream();
            int firstByte = inputStream.read();
            if (firstByte == 0x05) {
                pool.execute(new Socks5Server(clientSocket));
            } else {
                pool.execute(new HttpServer(firstByte, clientSocket));
            }
        }
    }
}
