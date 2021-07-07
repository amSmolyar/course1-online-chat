package ru.netology;

public class ClientName {
    private static ThreadLocal<String> name = new ThreadLocal<>();

    public static String getName() {
        return name.get();
    }

    public static void setName(String newName) {
        name.set(newName);
    }
}
