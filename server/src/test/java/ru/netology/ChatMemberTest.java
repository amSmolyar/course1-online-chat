package ru.netology;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

class ChatMemberTest {
    public static Stream<Message> getMessageValues() {
        Message[] messageArray = new Message[5];
        for (int ii = 0; ii < messageArray.length; ii++) {
            messageArray[ii] = new Message(String.valueOf(ii), "some text");
        }
        return Arrays.stream(messageArray);
    }

    // ===========================================================================================

    // Проверяем, что метод send(Message message) не выбрасывает исключения
    // при стабильном подключении. Проверяем 5 раз с разными сообщениями.
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

            System.out.println("send_does_not_throw");

            try {
                serverConnection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();
        InOut newConnection = new InOut("127.0.0.1", 58005);

        thread.interrupt();
        newConnection.close();
        serverSocket.close();
    }

    // ===========================================================================================

    // Проверяем, что метод send(Message message) отправляет сообщения корректным образом, и
    // они могут быть считаны приемным буффером. Проверяем 5 раз с разными сообщениями.
    @ParameterizedTest
    @MethodSource("getMessageValues")
    void test_send(Message message) throws IOException {
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

        System.out.println("test_send");

        thread.interrupt();
        newConnection.close();
        serverSocket.close();
    }

    // ===========================================================================================

    // Проверяется работа методов chooseUserName(), addUser() и Server.clientContains(ChatMember client)
    // Создаем сервер, чтобы иметь доступ к его методам и коллекции участников чата.
    // имитация подключения производится только с одного InOut,
    // поскольку работаем с одним вспомогательным потоком.
    //
    // В первое подключение прописываем имя участнику "client1". Список участников пуст,
    // поэтому без проблем должна производиться запись в коллекцию участников.
    //
    // Во второе подключение прописываем имя участнику "client2". Список участников состоит
    // только из client1, поэтому без проблем должна производиться запись в коллекцию участников.
    // из-за того, что фактически пишем мы из одного потока, имитируя нескольких клиентов, сообщения
    // сервера, адресованные всем участникам чата, должны приходить нам в количестве участников чата
    // (во второе подключение 2 раза, в третье -3).
    //
    // В третье подключение прописываем сначала участнику имя "client1" и убеждаемся, что оно занято.
    // Получаем соответствующее уведомление от сервера. Затем прописываем участнику имя "client2"
    // и убеждаемся, что оно тоже занято. Получаем соответствующее уведомление от сервера. После
    // этого прописываем имя участнику "client3". Данное имя не занято, потому участник без проблем
    // добавляется в коллекцию участников.

    @Test
    void test_chooseUserName() throws IOException {
        Thread thread;
        List<ChatMember> list = new ArrayList<>();

        ServerSocket serverSocket = new ServerSocket(58005);
        thread = new Thread(() -> {
            Server server = new Server();
            InOut serverConnection = new InOut(serverSocket);
            ChatMember chatMember = new ChatMember(serverConnection);
            String name1 = chatMember.chooseUserName();
            assertEquals("client1", name1);
            chatMember.addUser();
            System.out.println("test_chooseUserName1");

            ChatMember chatMember2 = new ChatMember(serverConnection);
            String name2 = chatMember2.chooseUserName();
            assertEquals("client2", name2);
            chatMember2.addUser();
            System.out.println("test_chooseUserName2");

            ChatMember chatMember3 = new ChatMember(serverConnection);
            String name3 = chatMember3.chooseUserName();
            assertEquals("client3", name3);
            chatMember3.addUser();
            System.out.println("test_chooseUserName3");

            try {
                serverConnection.close();
                server.close();
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();

        // first connection:
        InOut newConnection = new InOut("127.0.0.1", 58005);
        String readLine;

        checkEnterLogin(newConnection);
        // free login:
        newConnection.write("client1\r\n");
        checkWelcome(newConnection);
        checkNewClient(newConnection, "client1");

        // second connection:
        checkEnterLogin(newConnection);
        // free login:
        newConnection.write("client2\r\n");
        checkWelcome(newConnection);
        for (int ii = 0; ii < 2; ii++) {
            checkNewClient(newConnection, "client2");
        }

        // third connection:
        checkEnterLogin(newConnection);
        // busy login:
        newConnection.write("client1\r\n");
        checkBusyName(newConnection);
        // busy login:
        newConnection.write("client2\r\n");
        checkBusyName(newConnection);
        // free login:
        newConnection.write("client3\r\n");
        checkWelcome(newConnection);
        for (int ii = 0; ii < 3; ii++) {
            checkNewClient(newConnection, "client3");
        }


        thread.interrupt();
        newConnection.close();
        serverSocket.close();
    }

    private void checkHeaders(InOut newConnection) {
        String readLine;

        readLine = newConnection.readLine();
        assertEquals("", readLine);

        readLine = newConnection.readLine();
        assertEquals("From: server", readLine);

        readLine = newConnection.readLine();
        assertTrue(readLine.startsWith("Data-length: "));

        readLine = newConnection.readLine();
        assertEquals("Message: ", readLine);
    }

    private void checkWelcome(InOut newConnection) {
        String readLine;
        checkHeaders(newConnection);

        readLine = newConnection.readLine();
        assertEquals("Welcome!", readLine);

        readLine = newConnection.readLine();
        assertEquals("", readLine);
    }

    private void checkNewClient(InOut newConnection, String name) {
        String readLine;
        checkHeaders(newConnection);

        readLine = newConnection.readLine();
        assertEquals(name + " has joined the chat", readLine);

        readLine = newConnection.readLine();
        assertEquals("", readLine);
    }

    private void checkBusyName(InOut newConnection) {
        String readLine;
        checkHeaders(newConnection);

        readLine = newConnection.readLine();
        assertEquals("Login busy. Try again: ", readLine);

        readLine = newConnection.readLine();
        assertEquals("", readLine);
    }

    private void checkEnterLogin(InOut newConnection) {
        String readLine;
        checkHeaders(newConnection);

        readLine = newConnection.readLine();
        assertEquals("Enter login: ", readLine);

        readLine = newConnection.readLine();
        assertEquals("", readLine);
    }

    // ===========================================================================================

    // Проверяется метод parseMessage(). В чат добавляются 2 участника (client1, client2).
    //
    // Создаем сервер, чтобы иметь доступ к его методам и коллекции участников чата.
    // имитация подключения производится только с одного InOut,
    // поскольку работаем с одним вспомогательным потоком.
    //
    // из-за того, что фактически пишем мы из одного потока, имитируя нескольких клиентов, сообщения
    // сервера, адресованные всем участникам чата, должны приходить нам в количестве участников чата
    //
    // client2 отправляет сообщения с ошибками в разных частях.
    @Test
    void test_parseMessage_with_errors() throws IOException {
        Thread thread;

        ServerSocket serverSocket = new ServerSocket(58005);
        thread = new Thread(() -> {
            Server server = new Server();
            InOut serverConnection = new InOut(serverSocket);
            ChatMember chatMember = new ChatMember(serverConnection);
            String name1 = chatMember.chooseUserName();
            assertEquals("client1", name1);
            chatMember.addUser();

            ChatMember chatMember2 = new ChatMember(serverConnection);
            String name2 = chatMember2.chooseUserName();
            assertEquals("client2", name2);
            chatMember2.addUser();

            while (chatMember2.parseMessage()) {
            }


            try {
                serverConnection.close();
                server.close();
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();

        // first connection:
        InOut newConnection = new InOut("127.0.0.1", 58005);
        String readLine;

        checkEnterLogin(newConnection);
        // free login:
        newConnection.write("client1\r\n");
        checkWelcome(newConnection);
        checkNewClient(newConnection, "client1");

        // second connection:
        checkEnterLogin(newConnection);
        // free login:
        newConnection.write("client2\r\n");
        checkWelcome(newConnection);
        for (int ii = 0; ii < 2; ii++) {
            checkNewClient(newConnection, "client2");
        }

        // error in "From: " (wrong writer):
        newConnection.write("\r\nFrom: " + "" +
                "\r\nData-length: " + "text".getBytes().length +
                "\r\nMessage: \n" +
                "text" + "\r\n" +
                "\r\n");
        checkFormatError(newConnection);
        checkFormatError(newConnection);

        // error in "Data-length: " (Datalength):
        newConnection.write("\r\nFrom: " + "client2" +
                "\r\nDatalength: " + "text".getBytes().length +
                "\r\nMessage: \n" +
                "text" + "\r\n" +
                "\r\n");
        checkFormatError(newConnection);
        checkFormatError(newConnection);

        // error in "Message: " (without message):
        newConnection.write("\r\nFrom: " + "client2" +
                "\r\nDatalength: " + "text".getBytes().length +
                "text" + "\r\n" +
                "\r\n");
        checkFormatError(newConnection);
        checkFormatError(newConnection);

        // error in the middle: " (without "\r\n" between 2 messages):
        newConnection.write("\r\nFrom: " + "client2" +
                "\r\nData-length: " + "text".getBytes().length +
                "\r\nMessage: \n" +
                "text" + "\r\n" +
                "\r\nFrom: " + "client2" +
                "\r\nData-length: " + "text".getBytes().length +
                "\r\nMessage: \n" +
                "text" + "\r\n" +
                "\r\n");
        checkFormatError(newConnection);
        checkFormatError(newConnection);

        thread.interrupt();
        newConnection.close();
        serverSocket.close();
    }

    private void checkFormatError(InOut newConnection) {
        String readLine;

        readLine = newConnection.readLine();
        assertEquals("", readLine);

        readLine = newConnection.readLine();
        assertEquals("From: server", readLine);

        readLine = newConnection.readLine();
        assertTrue(readLine.startsWith("Data-length: "));

        readLine = newConnection.readLine();
        assertEquals("Message: ", readLine);

        readLine = newConnection.readLine();
        assertEquals("Incorrect message format!", readLine);

        readLine = newConnection.readLine();
        assertEquals("", readLine);
    }

    // ===========================================================================================

    // Проверяется метод parseMessage(). В чат добавляются 2 участника (client1, client2).
    //
    // Создаем сервер, чтобы иметь доступ к его методам и коллекции участников чата.
    // имитация подключения производится только с одного InOut,
    // поскольку работаем с одним вспомогательным потоком.
    //
    // из-за того, что фактически пишем мы из одного потока, имитируя нескольких клиентов, сообщения
    // сервера, адресованные всем участникам чата, должны приходить нам в количестве участников чата
    //
    // client2 отправляет сообщения без ошибок.
    @ParameterizedTest
    @ValueSource(strings = {"text1", "1", "another text"})
    void test_parseMessage_without_errors(String text) throws IOException {
        Thread thread;

        ServerSocket serverSocket = new ServerSocket(58005);
        thread = new Thread(() -> {
            Server server = new Server();
            InOut serverConnection = new InOut(serverSocket);
            ChatMember chatMember = new ChatMember(serverConnection);
            String name1 = chatMember.chooseUserName();
            assertEquals("client1", name1);
            chatMember.addUser();

            ChatMember chatMember2 = new ChatMember(serverConnection);
            String name2 = chatMember2.chooseUserName();
            assertEquals("client2", name2);
            chatMember2.addUser();

            while (chatMember2.parseMessage()) {
            }


            try {
                serverConnection.close();
                server.close();
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();

        // first connection:
        InOut newConnection = new InOut("127.0.0.1", 58005);
        String readLine;

        checkEnterLogin(newConnection);
        // free login:
        newConnection.write("client1\r\n");
        checkWelcome(newConnection);
        checkNewClient(newConnection, "client1");

        // second connection:
        checkEnterLogin(newConnection);
        // free login:
        newConnection.write("client2\r\n");
        checkWelcome(newConnection);
        for (int ii = 0; ii < 2; ii++) {
            checkNewClient(newConnection, "client2");
        }

        // message from client2:
        newConnection.write("\r\nFrom: " + "client2" +
                "\r\nData-length: " + text.getBytes().length +
                "\r\nMessage: \n" +
                text + "\r\n" +
                "\r\n");
        checkNewMessage(newConnection, text);
        checkNewMessage(newConnection, text);

        newConnection.write("/exit");


        thread.interrupt();
        newConnection.close();
        serverSocket.close();
    }

    private void checkNewMessage(InOut newConnection, String body) {
        String readLine;

        readLine = newConnection.readLine();
        assertEquals("", readLine);

        readLine = newConnection.readLine();
        assertTrue(readLine.startsWith("From: "));

        readLine = newConnection.readLine();
        assertTrue(readLine.startsWith("Data-length: "));

        readLine = newConnection.readLine();
        assertEquals("Message: ", readLine);

        readLine = newConnection.readLine();
        assertEquals(body, readLine);

        readLine = newConnection.readLine();
        assertEquals("", readLine);
    }


    private void checkChatLeft(InOut newConnection, String name) {
        String readLine;

        readLine = newConnection.readLine();
        assertEquals("", readLine);

        readLine = newConnection.readLine();
        assertEquals("From: server", readLine);

        readLine = newConnection.readLine();
        assertTrue(readLine.startsWith("Data-length: "));

        readLine = newConnection.readLine();
        assertEquals("Message: ", readLine);

        readLine = newConnection.readLine();
        assertEquals(name + " left the chat", readLine);

        readLine = newConnection.readLine();
        assertEquals("", readLine);
    }

}