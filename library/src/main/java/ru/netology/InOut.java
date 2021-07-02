package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class InOut implements Closeable {
    private final Socket socket;
    private final BufferedOutputStream writer;
    private final BufferedReader reader;

    public InOut(ServerSocket server) {
        try {
            this.socket = server.accept();
            this.writer = createWriter();
            this.reader = createReader();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public InOut(String ip, int port) {
        try {
            this.socket = new Socket(ip, port);
            this.writer = createWriter();
            this.reader = createReader();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String readLine() throws IOException  {
        while (!reader.ready()) {}

        return reader.readLine();
    }

    public String readByteArrayAndConvertToString(int cntByte) throws IOException {
        ByteArrayOutputStream bodyBAOStream = new ByteArrayOutputStream();
        while (bodyBAOStream.size() < cntByte) {
            while (!reader.ready()) {}
            bodyBAOStream.write(reader.read());
        }
        byte[] byteArray = bodyBAOStream.toByteArray();
        return new String(byteArray, StandardCharsets.UTF_8);
    }

    public void write(String message) throws IOException {
        writer.write(message.getBytes());
        writer.flush();
    }

    private BufferedOutputStream createWriter() throws IOException {
        return new BufferedOutputStream(socket.getOutputStream());
    }

    private BufferedReader createReader() throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void close() throws IOException {
        writer.close();
        reader.close();
        socket.close();
    }
}
