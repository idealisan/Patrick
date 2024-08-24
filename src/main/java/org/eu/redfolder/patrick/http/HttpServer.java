package org.eu.redfolder.patrick.http;

import org.eu.redfolder.patrick.socket.Sockets;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class HttpServer extends Thread {
    private final int firstByte;
    private final Socket clientSocket;

    public HttpServer(int firstByte, Socket clientSocket) {
        this.firstByte = firstByte;
        this.clientSocket = clientSocket;
    }

    public void run() {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = bufferedReader.readLine();
            String host = "";
            int port = 80;
            String method = line.split(" ")[0];
            if (firstByte == 'C' && method.equals("ONNECT")) {
                while (!line.trim().isEmpty()) {
                    line = bufferedReader.readLine();
                    if (line.startsWith("Host")) {
                        String[] split = line.split(":");
                        if (split.length >= 2) {
                            host = split[1].trim();
                        }
                        if (split.length == 3) {
                            port = Integer.parseInt(split[2]);
                        } else {
                            throw new RuntimeException("Host header is: " + line);
                        }
                    }
                }
                handleHttpsConnect(host, port, clientSocket);
            } else {
                line = String.valueOf((char) firstByte) + line;
                handleHttp(line, bufferedReader, host, port, clientSocket);
            }
        } catch (Throwable e) {
            System.out.println(e.getMessage());
        }
    }

    private void handleHttp(
            String line, BufferedReader bufferedReader, String host, int port, Socket clientSocket)
            throws IOException {
        ArrayList<String> headers = new ArrayList<>();
        while (!line.trim().isEmpty()) {
            headers.add(line);
            line = bufferedReader.readLine();
            if (line.startsWith("Host")) {
                String[] split = line.split(":");
                if (split.length == 2) {
                    host = split[1].trim();
                } else if (split.length == 3) {
                    port = Integer.parseInt(split[2]);
                } else {
                    throw new RuntimeException("Host header is: " + line);
                }
            }
        }
        System.out.printf("HTTP Requesting %s:%s%n", host, port);
        Socket remoteSocket = new Socket(host, port);
        OutputStream outputStream = remoteSocket.getOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(outputStream);
        headers.forEach(
                s -> {
                    try {
                        writer.write(s + "\r\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        writer.write("\r\n");
        writer.flush();
        Sockets.exchangeSocket(remoteSocket, clientSocket);
    }

    private void handleHttpsConnect(String host, int port, Socket clientSocket) throws IOException {
        System.out.println("HTTPS Connecting to https://" + host + ":" + port);
        clientSocket
                .getOutputStream()
                .write(
                        "HTTP/1.1 200 Connection Established\r\n\r\n"
                                .getBytes(StandardCharsets.UTF_8));
        clientSocket.getOutputStream().flush();
        Socket remoteSocket = new Socket(host, port);
        Sockets.exchangeSocket(remoteSocket, clientSocket);
    }
}
