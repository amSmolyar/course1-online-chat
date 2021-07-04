package ru.netology;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ChatMemberTest {
    public static Stream<Message> getMessageValues() {
        Message[] messageArray = new Message[5];
        for (int ii = 0; ii < messageArray.length; ii++) {
            messageArray[ii] = new Message(String.valueOf(ii), "some text");
        }
        return Arrays.stream(messageArray);
    }

    @ParameterizedTest
    @MethodSource("getMessageValues")
    void send_does_not_throw(Message message) throws IOException {
        Thread thread;

        ServerSocket serverSocket = new ServerSocket(58005);
        thread = new Thread(() -> {
            InOut serverConnection = new InOut(serverSocket);
            ChatMember chatMember = new ChatMember(serverConnection);
            assertDoesNotThrow(() ->
                    chatMember.send(message));

            try {
                serverConnection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();
        new InOut("127.0.0.1", 58005);

        serverSocket.close();
    }

    @ParameterizedTest
    @MethodSource("getMessageValues")
    void send(Message message) throws IOException {
        Thread thread;

        ServerSocket serverSocket = new ServerSocket(58005);
        thread = new Thread(() -> {
            InOut serverConnection = new InOut(serverSocket);
            ChatMember chatMember = new ChatMember(serverConnection);
            chatMember.send(message);

            try {
                serverConnection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();

        InOut newConnection = new InOut("127.0.0.1", 58005);
        String readLine;

        readLine = newConnection.readLine();
        assertEquals("", readLine);

        readLine = newConnection.readLine();
        assertEquals("From: " + message.getWriter(), readLine);

        readLine = newConnection.readLine();
        assertEquals("Data-length: " + message.getBodyLength(), readLine);

        readLine = newConnection.readLine();
        assertEquals("Message: ", readLine);

        readLine = newConnection.readLine();
        assertEquals(message.getBody(), readLine);

        readLine = newConnection.readLine();
        assertEquals("", readLine);

        serverSocket.close();
    }

    @Test
    void chooseUserName() {
    }
}