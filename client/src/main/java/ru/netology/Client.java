package ru.netology;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Client {
    private static final String PATH_TO_SETTINGS_FILE = "D:/JAVA/online-chat";
    private static final String SETTINGS_FILE = "settings.txt";

    private static final String PATH_TO_LOG_DIR = "D:/JAVA/online-chat/client";
    private static final String LOG_DIR = "doc";

    private static final String STOP_WORD = "/exit";

    private ConsoleConnection consoleConnection;
    private InOut connection;
    private Logger logger;

    private final DateFormat dateFormat;
    private final Date date;

    private static Lock lock;
    private static Condition condition;

    public Client() {
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy   HH:mm:ss");
        this.date = new Date();
        this.lock = new ReentrantLock(true);
        this.condition = lock.newCondition();

        ClientName.setName("");

        consoleConnection = new ConsoleConnection();
        // подключение к серверу:
        ConnectionParameters connectionParameters = ConnectionParameters.readSettingsFile(PATH_TO_SETTINGS_FILE, SETTINGS_FILE);
        connection = new InOut(connectionParameters.getIp(), connectionParameters.getPort());

        logger = Logger.getLogger(PATH_TO_LOG_DIR, LOG_DIR, "log.txt");
    }

    public Client(String ip, int port) {
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy   HH:mm:ss");
        this.date = new Date();
        this.lock = new ReentrantLock(true);
        this.condition = lock.newCondition();

        ClientName.setName("");

        consoleConnection = new ConsoleConnection();
        // подключение к серверу:
        connection = new InOut(ip, port);
        logger = Logger.getLogger(PATH_TO_LOG_DIR, LOG_DIR, "log.txt");
    }

    public void runClient() {
        Message inMessage;

        try {
            while (true) {
                inMessage = readFromChat();
                if (registration(consoleConnection, inMessage))
                    break;
            }

            Thread reader = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    Message message = readFromChat();
                    messageAnalise(message);
                }
            });
            reader.start();

            while (true) {
                if (!writeToChat(consoleConnection))
                    break;
            }

            reader.interrupt();
            this.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void send(Message message) {
        try {
            connection.write("\r\nFrom: " + ClientName.getName() +
                    "\r\nData-length: " + message.getBodyLength() +
                    "\r\nMessage: \n" +
                    message.getBody() + "\r\n" +
                    "\r\n");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public boolean writeToChat(ConsoleConnection consoleConnection) throws IOException {
        lock.lock();
        System.out.println("Введите текст:");
        String scannerData;
        scannerData = consoleConnection.nextLine();
        condition.signalAll();
        lock.unlock();

        if (!scannerData.equals(STOP_WORD)) {
            send(new Message(ClientName.getName(), scannerData));
            return true;
        } else {
            connection.write("\r\n" + STOP_WORD + "\r\n");
            return false;
        }
    }

    public Message readFromChat() {
        String readLine;
        String writerName = "";
        int bodyLength = 0;
        String textFromBuf = "";
        int cntHeader;

        while (writerName.equals("") || (bodyLength <= 0) || textFromBuf.equals("")) {
            bodyLength = -1;

            cntHeader = 0;
            while (!(readLine = connection.readLine().trim()).equals("")) {
                if (cntHeader == 0) {
                    if (readLine.startsWith("From:")) {
                        writerName = readLine.substring(readLine.indexOf(":") + 1).trim();
                        if (writerName.equals(""))
                            break;
                    } else
                        break;
                } else if (cntHeader == 1) {
                    try {
                        if (readLine.startsWith("Data-length:")) {
                            bodyLength = Integer.parseInt(readLine.substring(readLine.indexOf(":") + 1).trim());
                            if (bodyLength <= 0)
                                break;
                        } else
                            break;
                    } catch (NumberFormatException e) {
                        System.out.println(e.getMessage());
                        break;
                    }
                } else if (cntHeader == 2) {
                    if (readLine.startsWith("Message:")) {
                        if (bodyLength > 0) {
                            textFromBuf = connection.readByteArrayAndConvertToString(bodyLength);
                        } else
                            break;
                    }
                }

                cntHeader++;
            }
        }
        return new Message(writerName, bodyLength, textFromBuf, dateFormat.format(date));
    }

    public boolean registration(ConsoleConnection consoleConnection, Message message) throws IOException {
        String body = message.getBody().toLowerCase();
        if (message.getWriter().equals("server")) {
            if (body.contains("enter login") || body.contains("busy")) {
                ClientName.setName(writeName(consoleConnection));
            } else if (body.contains("welcome")) {
                logger.log(message);
                return true;
            }
        }
        return false;
    }

    public void messageAnalise(Message message) {
        if (message.getWriter().equals(ClientName.getName()))
            message.setWriter("you");

        logger.log(message);
    }

    public String writeName(ConsoleConnection consoleConnection) throws IOException {
        lock.lock();
        System.out.println("Введите имя:");
        String scannerData;
        while (true) {
            scannerData = consoleConnection.nextLine();
            if (!scannerData.equals(""))
                break;
        }
        condition.signalAll();
        lock.unlock();

        connection.write(scannerData + "\r\n");
        return scannerData;
    }

    public void close() throws IOException {
        logger.log("Выход из чата..");
        consoleConnection.close();
        logger.close();
        connection.close();
    }

    public InOut getConnection() {
        return connection;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(String fileName) {
        this.logger = Logger.getLogger(PATH_TO_LOG_DIR, LOG_DIR, fileName + ".log");
    }

    public String getClientName() {
        return ClientName.getName();
    }

    public ConsoleConnection getConsoleConnection() {
        return consoleConnection;
    }
}
