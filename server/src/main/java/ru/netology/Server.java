package ru.netology;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private static final String PATH_TO_SETTINGS_FILE = "D:/JAVA/online-chat";
    private static final String SETTINGS_FILE = "settings.txt";

    private static final String PATH_TO_LOG_DIR = "D:/JAVA/online-chat/server";
    private static final String LOG_DIR = "doc";
    private static final String LOG_NAME = "log.txt";

    private static final int N_THREAD_IN_POOL = 64;

    public static Logger logger;
    public static CopyOnWriteArrayList<ChatMember> listMember;

    private ServerSocket serverSocket;

    private static Lock lock;
    private static Condition condition;

    public Server() throws IOException {
        lock = new ReentrantLock(true);
        condition = lock.newCondition();
        // создание логгера:
        logger = Logger.getLogger(PATH_TO_LOG_DIR, LOG_DIR, LOG_NAME);
        listMember = new CopyOnWriteArrayList<>();

        // чтение файла с настройками settings.txt и формирование объекта, содержащего параметры подключения:
        ConnectionParameters connectionParameters = readSettingsFile(PATH_TO_SETTINGS_FILE, SETTINGS_FILE);
        logger.log("Чтение файла " + SETTINGS_FILE);
        // создание сервера, ожидание новых подключений и формирование новых потоков для работы с клиентами:
        try {
            serverSocket = new ServerSocket(connectionParameters.getPort());
            logger.log("Сервер запущен!");
        } catch (IOException e) {
            System.out.println(e.getMessage());
            logger.log("Не удалось запустить сервер!");
            logger.close();
        }
    }

    public void runServer() {
        ExecutorService executorService = Executors.newFixedThreadPool(N_THREAD_IN_POOL);
        // создание сервера, ожидание новых подключений и формирование новых потоков для работы с клиентами:
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                executorService.submit(() -> new ChatMember(socket));
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            executorService.shutdown();
            close();
        }
    }

    public static void sendAll(Message message) {
        lock.lock();
        for (ChatMember member : listMember) {
            member.send(message);
        }
        condition.signalAll();
        lock.unlock();
    }

    public static void removeClient(ChatMember client) {
        lock.lock();
        if (listMember.contains(client)) {
            listMember.remove(client);
            condition.signalAll();
        }
        lock.unlock();
    }

    public void close() {
        try {
            logger.log("Сервер остановлен!");
            serverSocket.close();
            logger.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }

    private static ConnectionParameters readSettingsFile(String path, String fileName) {
        File file = new File(path, fileName);
        String ip = "";
        int nPort = -1;
        ConnectionParameters connectionParameters;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length != 2)
                    throw new RuntimeException("Неправильный формат записи данных в файле" + fileName);

                if (parts[0].trim().toLowerCase().contains("ip")) {
                    if (parts[1].trim().matches("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})"))
                        ip = parts[1].trim();
                    else
                        throw new RuntimeException("Неправильный формат записи данных в файле" + fileName);
                } else if (parts[0].trim().toLowerCase().contains("port")) {
                    try {
                        nPort = Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException e) {
                        System.out.println(e.getMessage());
                        System.out.println("Неправильный формат записи данных в файле" + fileName);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        if (!ip.equals("") && (nPort != -1))
            connectionParameters = new ConnectionParameters(ip, nPort);
        else
            throw new RuntimeException("Неправильный формат записи данных в файле" + fileName);
        return connectionParameters;
    }
}
