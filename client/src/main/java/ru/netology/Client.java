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

    private Scanner scanner;
    private InOut connection;
    private Logger logger;
    private String clientName = "";

    private String writtenName;

    private final DateFormat dateFormat;
    private final Date date;

    private static Lock lock;
    private static Condition condition;

    public Client() {
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy   HH:mm:ss");
        this.date = new Date();
        this.lock = new ReentrantLock(true);
        this.condition = lock.newCondition();
    }

    public void runClient() {
        scanner = new Scanner(System.in);
        // подключение к серверу:
        ConnectionParameters connectionParameters = ConnectionParameters.readSettingsFile(PATH_TO_SETTINGS_FILE, SETTINGS_FILE);
        connection = new InOut(connectionParameters.getIp(), connectionParameters.getPort());

        try {
            while (this.clientName.equals(""))
                readFromChat();

            Thread reader = new Thread(() -> readFromChat());
            reader.start();

            while (true) {
                if (!writeToChat())
                    break;
            }
            
            reader.interrupt();
            logger.log("Выход из чата..");
            logger.close();
            connection.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void send(Message message) {
        try {
            connection.write("\r\nFrom: " + message.getWriter() +
                    "\r\nData-length: " + message.getBodyLength() +
                    "\r\nMessage: \n" +
                    message.getBody() + "\r\n" +
                    "\r\n");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public String writeName () throws IOException {
        lock.lock();
        System.out.println("Введите имя:");
        String scannerData;
        while (true) {
            scannerData = scanner.nextLine();
            if (!scannerData.equals(""))
                break;
        }
        condition.signalAll();
        lock.unlock();

        connection.write(scannerData);
        return scannerData;

    }

    public boolean writeToChat () {
        lock.lock();
        System.out.println("Введите текст:");
        String scannerData;
        scannerData = scanner.nextLine();
        condition.signalAll();
        lock.unlock();

        send(new Message(this.clientName, scannerData));

        if (scannerData.equals(STOP_WORD))
            return false;
        else
            return true;
    }

    public void readFromChat() {
        String readLine;
        String writerName = "";
        int bodyLength;
        int cntHeader;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                bodyLength = -1;

                cntHeader = 0;
                while (!(readLine = connection.readLine().trim()).equals("")) {
                    if (cntHeader == 0) {
                        if (readLine.startsWith("From:"))
                            writerName = readLine.substring(readLine.indexOf(":") + 1).trim();
                        else
                            continue;
                    } else if ((cntHeader == 1) && (readLine.startsWith("Data-length:"))) {
                        try {
                            bodyLength = Integer.parseInt(readLine.substring(readLine.indexOf(":") + 1).trim());
                        } catch (NumberFormatException e) {
                            System.out.println(e.getMessage());
                            continue;
                        }
                    } else if ((cntHeader == 2) && (readLine.startsWith("Message:"))) {
                        if (bodyLength > 0) {
                            String textFromBuf = connection.readByteArrayAndConvertToString(bodyLength);
                            Message message = new Message(writerName, bodyLength, textFromBuf, dateFormat.format(date));

                            String body = message.getBody().toLowerCase();
                            if (writerName.equals("server")) {
                                if (body.contains("введите имя") || body.contains("имя занято")) {
                                    writtenName = writeName();
                                } else if (body.contains("добро пожаловать")) {
                                    logger = Logger.getLogger(PATH_TO_LOG_DIR, LOG_DIR, this.clientName + ".log");
                                    this.clientName = writtenName;
                                    logger.log(message);
                                } else
                                    logger.log(message);
                            } else if (writerName.equals(this.clientName)) {
                                message.setWriter("You");
                                logger.log(message);
                            } else
                                logger.log(message);
                        }
                    } else {
                        continue;
                    }
                    cntHeader++;
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
