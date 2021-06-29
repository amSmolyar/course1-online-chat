package ru.netology;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Logger {
    private static Logger logger;
    private int num = 1;

    private DateFormat dateFormat;
    private Date date;
    private File logFile;
    private BufferedWriter bufferedWriter;

    private Lock lock;
    private Condition condition;

    private Logger(String path, String dirName, String fileName) {
        this.num = 1;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy   HH:mm:ss");
        this.date = new Date();

        this.lock = new ReentrantLock(true);
        this.condition = lock.newCondition();

        String createLogFile = createLogFile(path, dirName, fileName);
        log(createLogFile);
    }

    public void log(Message message) {
        lock.lock();
        try {
            bufferedWriter.write(num++ + ".    " + message.toString());
            bufferedWriter.flush();
            condition.signalAll();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    public void log(String text) {
        lock.lock();
        try {
            bufferedWriter.write(num++ + ".    " + dateFormat.format(date) + "    :\n   " + text);
            bufferedWriter.flush();
            condition.signalAll();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            lock.lock();
        }
    }

    public static Logger getLogger(String path, String dirName, String fileName) {
        if (logger == null)
            logger = new Logger(path, dirName, fileName);
        return logger;
    }

    public void close() throws IOException {
            bufferedWriter.close();
    }

    private String createLogFile(String path, String dirName, String fileName) {
        StringBuilder temp = new StringBuilder();
        File srcDir = new File(path, dirName);
        if (srcDir.mkdir())
            temp.append("В каталоге " + path + " создана директория '" + dirName + "'.\n");
        else
            throw new RuntimeException("Ошибка при создании директории '" + dirName + "' в каталоге " + path);

        logFile = new File(path + "/" + dirName + "//" + fileName);
        try {
            if (logFile.createNewFile()) {
                temp.append("В каталоге " + path + "/" + dirName + " создан файл '" + fileName + "'.\n");
                bufferedWriter = new BufferedWriter(new FileWriter(logFile));
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.out.println("Ошибка при создании файла " + fileName);
        }


        return temp.toString();
    }


}
