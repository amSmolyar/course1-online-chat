package ru.netology;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ConnectionParameters {
    private String ip;
    private int port;

    public ConnectionParameters(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }


    public static ConnectionParameters readSettingsFile(String path, String fileName) {
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
