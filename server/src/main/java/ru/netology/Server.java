package ru.netology;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
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

    private static Logger logger;
    private static List<ChatMember> listMember;

    private ServerSocket serverSocket;

    private static Lock lock;
    private static Condition condition;

    public Server() {
        lock = new ReentrantLock(true);
        condition = lock.newCondition();
        // создание логгера:
        logger = Logger.getLogger(PATH_TO_LOG_DIR, LOG_DIR, LOG_NAME);
        listMember = new ArrayList<>();

        // чтение файла с настройками settings.txt и формирование объекта, содержащего параметры подключения:
        ConnectionParameters connectionParameters = ConnectionParameters.readSettingsFile(PATH_TO_SETTINGS_FILE, SETTINGS_FILE);
        logger.log("Чтение файла " + SETTINGS_FILE + ".");
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
        while (!Thread.currentThread().isInterrupted()) {
            InOut newConnection = new InOut(serverSocket);
            FutureTask futureTask = (FutureTask) executorService.submit(new ChatMember(newConnection));
            if (futureTask.isDone())
                futureTask.cancel(false);
        }
        executorService.shutdown();
        close();
    }

    public static void sendAll(Message message) {
        lock.lock();
        for (ChatMember member : listMember) {
            member.send(message);
        }
        logger.log(message);
        condition.signalAll();
        lock.unlock();
    }

    public static boolean clientContains(ChatMember client) {
        lock.lock();
        boolean isHere = listMember.contains(client);
        condition.signalAll();
        lock.unlock();

        return isHere;
    }

    public static void addClient(ChatMember client) {
        lock.lock();
        listMember.add(client);
        condition.signalAll();
        lock.unlock();

        logger.log("К чату присоединился участник " + client.getUserName());
        sendAll(new Message("server", client.getUserName() + " has joined the chat"));
    }

    public static void removeClient(ChatMember client) {
        lock.lock();
        if (listMember.contains(client)) {
            logger.log("Чат покинул участник " + client.getUserName());
            sendAll(new Message("server", client.getUserName() + " left the chat"));
            listMember.remove(client);
        }
        condition.signalAll();
        lock.unlock();
    }

    public void close() {
        try {
            logger.log("Сервер остановлен!");
            if (serverSocket != null)
                serverSocket.close();
            logger.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static List<ChatMember> getListMember() {
        return listMember;
    }

    public static Logger getLogger() {
        return logger;
    }
}
