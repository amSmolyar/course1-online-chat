package ru.netology;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ClientTest {

    public static Stream<Message> getMessageValues() {
        Message[] messageArray = new Message[5];
        for (int ii = 0; ii < messageArray.length; ii++) {
            messageArray[ii] = new Message(String.valueOf(ii), "some text");
        }
        return Arrays.stream(messageArray);
    }


    // ===========================================================================================
    // Проверяется, что при вызове констуктора клиента возвращается не null. А также не null
    // его приватные поля listMember и logger
    @Test
    void test_client_no_null() throws IOException {
        System.out.println("test_client_no_null");

        Thread thread;

        ServerSocket serverSocket = new ServerSocket(58005);
        thread = new Thread(() -> {
            InOut serverConnection = new InOut(serverSocket);

            if (Thread.currentThread().isInterrupted()) {
                try {
                    serverConnection.close();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        });
        thread.start();

        Client client = new Client();
        assertNotNull(client);

        thread.interrupt();
        client.getConnection().close();
        serverSocket.close();
    }


    // ===========================================================================================

    // Проверяем, что метод send(Message message) отправляет сообщения корректным образом, и
    // они могут быть считаны приемным буффером. Проверяем 5 раз с разными сообщениями.
    @ParameterizedTest
    @MethodSource("getMessageValues")
    void test_client_send(Message message) throws IOException {
        System.out.println("test_client_send");

        Thread thread;

        ServerSocket serverSocket = new ServerSocket(58005);
        thread = new Thread(() -> {
            InOut serverConnection = new InOut(serverSocket);

            String readLine;

            readLine = serverConnection.readLine();
            assertEquals("", readLine);

            readLine = serverConnection.readLine();
            assertEquals("From: " + message.getWriter(), readLine);

            readLine = serverConnection.readLine();
            assertEquals("Data-length: " + message.getBodyLength(), readLine);

            readLine = serverConnection.readLine();
            assertEquals("Message: ", readLine);

            readLine = serverConnection.readLine();
            assertEquals(message.getBody(), readLine);

            readLine = serverConnection.readLine();
            assertEquals("", readLine);

            try {
                serverConnection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();

        Client client = new Client("127.0.0.1", 58005);
        client.send(message);


        thread.interrupt();
        client.getConnection().close();
        serverSocket.close();
    }

    // ===========================================================================================

    // Проверяется метод writeToChat(ConsoleConnection consoleConnection).
    // имитируется ввод текста сообщения из консоли, затем этот текст отравляется "серверу",
    // который парсит полученное сообщение и проверяет его правильность
    // Проверка на основании 3 вариантов сообщений от консоли.
    @ParameterizedTest
    @ValueSource(strings = {"text from scanner", "1", "any"})
    void test_writeToChat_message(String text) throws IOException {
        System.out.println("test_writeToChat_message");

        Thread thread;

        ServerSocket serverSocket = new ServerSocket(58005);
        thread = new Thread(() -> {
            InOut serverConnection = new InOut(serverSocket);

            String readLine;

            readLine = serverConnection.readLine();
            assertEquals("", readLine);

            readLine = serverConnection.readLine();
            assertEquals("From: ", readLine);

            readLine = serverConnection.readLine();
            assertEquals("Data-length: " + text.getBytes().length, readLine);

            readLine = serverConnection.readLine();
            assertEquals("Message: ", readLine);

            readLine = serverConnection.readLine();
            assertEquals(text, readLine);

            readLine = serverConnection.readLine();
            assertEquals("", readLine);

            try {
                serverConnection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();

        ConsoleConnection scanner = Mockito.mock(ConsoleConnection.class);
        Mockito.when(scanner.nextLine()).thenReturn(text);

        Client client = new Client("127.0.0.1", 58005);
        assertTrue(client.writeToChat(scanner));


        thread.interrupt();
        client.getConnection().close();
        serverSocket.close();
    }

    // ===========================================================================================

    // Проверяется метод writeToChat(ConsoleConnection consoleConnection).
    // имитируется ввод текста сообщения из консоли, затем этот текст отравляется "серверу",
    // который парсит полученное сообщение и проверяет его правильность
    // Проверка, что сообщение о выходе из чата формируется правильно.
    @Test
    void test_writeToChat_exit() throws IOException {
        System.out.println("test_writeToChat_exit");

        Thread thread;

        ServerSocket serverSocket = new ServerSocket(58005);
        thread = new Thread(() -> {
            InOut serverConnection = new InOut(serverSocket);

            String readLine;

            readLine = serverConnection.readLine();
            assertEquals("", readLine);

            readLine = serverConnection.readLine();
            assertEquals("/exit", readLine);

            try {
                serverConnection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();

        ConsoleConnection scanner = Mockito.mock(ConsoleConnection.class);
        Mockito.when(scanner.nextLine()).thenReturn("/exit");

        Client client = new Client("127.0.0.1", 58005);
        assertFalse(client.writeToChat(scanner));


        thread.interrupt();
        client.getConnection().close();
        scanner.close();
        serverSocket.close();
    }

    // ===========================================================================================

    // Проверка метода readFromChat().
    // Метод возвращает принятое сообщение, только если оно пришло в правильном формате.
    // В данном тесте формируется несколько вариантов ошибочных сообщений (они должны отсеяться) и следом
    // за ними - одно правильное. На выходе метода должно сформироваться последнее сообщение.
    // Его и проверяем на правильность
    @Test
    void test_readFromChat() throws IOException {
        System.out.println("test_registration");

        Thread thread;

        ServerSocket serverSocket = new ServerSocket(58005);
        thread = new Thread(() -> {
            InOut serverConnection = new InOut(serverSocket);

            try {
                // error in "From: " ("Frm: "):
                serverConnection.write("\r\nFrm: " + "" +
                        "\r\nData-length: " + "text".getBytes().length +
                        "\r\nMessage: \n" +
                        "text" + "\r\n" +
                        "\r\n");

                // error in "From: " (wrong writer):
                serverConnection.write("\r\nFrom: " + "" +
                        "\r\nData-length: " + "text".getBytes().length +
                        "\r\nMessage: \n" +
                        "text" + "\r\n" +
                        "\r\n");

                // error in "Data-length: " (Datalength):
                serverConnection.write("\r\nFrom: " + "server" +
                        "\r\nDatalength: " + "text".getBytes().length +
                        "\r\nMessage: \n" +
                        "text" + "\r\n" +
                        "\r\n");

                // error in "Message: " (without message):
                serverConnection.write("\r\nFrom: " + "server" +
                        "\r\nData-length: " + "text".getBytes().length +
                        "text" + "\r\n" +
                        "\r\n");

                // right message:
                serverConnection.write("\r\nFrom: " + "server" +
                        "\r\nData-length: " + "text".getBytes().length +
                        "\r\nMessage: \n" +
                        "text" + "\r\n" +
                        "\r\n");


                serverConnection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();

        Client client = new Client("127.0.0.1", 58005);
        Message readMessage = client.readFromChat();
        assertTrue(readMessage.equals(new Message("server", "text")));

        thread.interrupt();
        client.getConnection().close();
        serverSocket.close();
    }

    // ===========================================================================================

    // Проверка метода registration(ConsoleConnection consoleConnection, Message message).
    // На вход метода подаются возможные варианты сообщений, связанных с регистрацией клиента в чате.
    // Первое сообщение - просьба ввести логин. Второе - логин занят. Третье - добро пожаловать.
    // После каждого сообщения проверя.тся значения переменных имени клиента и введенного логина.
    // Путем мокирования имитируется ввод текста пользователем через консоль
    @Test
    void test_registration() throws IOException {
        System.out.println("test_registration");

        Thread thread;

        ServerSocket serverSocket = new ServerSocket(58005);
        thread = new Thread(() -> {
            InOut serverConnection = new InOut(serverSocket);

            if (Thread.currentThread().isInterrupted()) {
                try {
                    serverConnection.close();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        });
        thread.start();

        Client client = new Client("127.0.0.1", 58005);
        ConsoleConnection scanner = Mockito.mock(ConsoleConnection.class);

        Message message1 = new Message("server", "Enter login: ");

        Mockito.when(scanner.nextLine()).thenReturn("client1");
        client.registration(scanner, message1);
        assertEquals("", client.getClientName());
        assertEquals("client1", client.getWrittenName());

        Message message2 = new Message("server", "Login busy. Try again: ");

        Mockito.when(scanner.nextLine()).thenReturn("client2");
        client.registration(scanner, message2);
        assertEquals("", client.getClientName());
        assertEquals("client2", client.getWrittenName());

        Message message3 = new Message("server", "Welcome!");

        Mockito.when(scanner.nextLine()).thenReturn("client2");
        client.registration(scanner, message3);
        assertEquals("client2", client.getClientName());
        assertEquals("client2", client.getWrittenName());
        assertNotNull(client.getLogger());

        try {
            thread.join();
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
        thread.interrupt();
        client.getConnection().close();
        scanner.close();
        serverSocket.close();
    }

    // ===========================================================================================

    // Проверка метода messageAnalise(Message message).
    // После создания объекта клиента его логин равен "" (до регистрации в чате).
    // Формируем несколько сообщений для клиента, автором у который указан "" и убеждаемся,
    // что автор данных сообщений изменился на "you"
    @ParameterizedTest
    @ValueSource(strings = {"1", "text", "body"})
    void test_messageAnalise(String body) throws IOException {
        System.out.println("test_messageAnalise");

        Thread thread;

        ServerSocket serverSocket = new ServerSocket(58005);
        thread = new Thread(() -> {
            InOut serverConnection = new InOut(serverSocket);

            try {
                serverConnection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();

        Client client = new Client("127.0.0.1", 58005);
        client.setLogger("1");

        Message message1 = new Message("", body);
        client.messageAnalise(message1);

        assertEquals("you", message1.getWriter());

        Message message2 = new Message("x", body);
        client.messageAnalise(message1);

        assertEquals("x", message2.getWriter());

        thread.interrupt();
        client.getConnection().close();
        serverSocket.close();
    }

    // ===========================================================================================

    // Проверка метода writeName(ConsoleConnection consoleConnection).
    // Мокированием имитируется ввод текста через консоль.
    // Убеждаемся, что сообщение, считанное из консоли,
    // отправляется и безошибочно считывается и парсится адресатом
    @Test
    void test_writeName() throws IOException {
        System.out.println("test_writeName");

        Thread thread;

        ServerSocket serverSocket = new ServerSocket(58005);
        thread = new Thread(() -> {
            InOut serverConnection = new InOut(serverSocket);

            String readLine;

            readLine = serverConnection.readLine();
            assertEquals("clientName", readLine);

            try {
                serverConnection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();

        ConsoleConnection scanner = Mockito.mock(ConsoleConnection.class);
        Mockito.when(scanner.nextLine()).thenReturn("clientName");

        Client client = new Client("127.0.0.1", 58005);
        String clientName = client.writeName(scanner);
        assertEquals("clientName", clientName);


        thread.interrupt();
        client.getConnection().close();
        scanner.close();
        serverSocket.close();
    }
}