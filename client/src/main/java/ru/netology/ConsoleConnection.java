package ru.netology;

import java.io.Closeable;
import java.util.Scanner;

public class ConsoleConnection implements Closeable {
    private Scanner scanner;

    public ConsoleConnection() {
        scanner = new Scanner(System.in);
    }

    public String nextLine() {
        return scanner.nextLine();
    }

    public void close() {
        scanner.close();
    }
}
