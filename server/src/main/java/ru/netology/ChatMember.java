package ru.netology;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import static ru.netology.Server.*;

public class ChatMember implements Runnable {
    private static final String STOP_WORD = "/exit";

    private final InOut socketBuf;
    private String userName;

    private final DateFormat dateFormat;
    private final Date date;

    public ChatMember(InOut inOut) {
        this.socketBuf = inOut;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy   HH:mm:ss");
        this.date = new Date();
    }

    @Override
    public void run() {
        this.userName = chooseUserName();
        addUser();

        while (true) {
            if (!parseMessage()) {
                removeClient(this);
                break;
            }
        }

        try {
            socketBuf.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private boolean parseMessage() {
        String readLine;
        String writerName = "";
        Message message;
        int cntHeader;
        while (true) {
            int bodyLength = -1;

            cntHeader = 0;
            while (!(readLine = socketBuf.readLine().trim()).equals("")) {
                if (cntHeader == 0) {
                    if (readLine.equalsIgnoreCase(STOP_WORD))
                        return false;
                    else if (readLine.startsWith("From:"))
                        writerName = readLine.substring(readLine.indexOf(":") + 1).trim();
                    else {
                        send(new Message("server", "Неправильный формат сообщения!\n"));
                        return true;
                    }
                } else if ((cntHeader == 1) && (readLine.startsWith("Data-length:"))) {
                    try {
                        bodyLength = Integer.parseInt(readLine.substring(readLine.indexOf(":") + 1).trim());
                    } catch (NumberFormatException e) {
                        System.out.println(e.getMessage());
                        send(new Message("server", "Неправильный формат сообщения!\n"));
                        return true;
                    }
                } else if ((cntHeader == 2) && (readLine.startsWith("Message:"))) {
                    if (bodyLength > 0) {
                        String textFromBuf = socketBuf.readByteArrayAndConvertToString(bodyLength);
                        message = new Message(writerName, bodyLength, textFromBuf, dateFormat.format(date));
                        sendAll(message);
                        return true;
                    }
                } else {
                    send(new Message("server", "Неправильный формат сообщения!\n"));
                    return true;
                }
                cntHeader++;
            }
            return true;
        }
    }

    public void send(Message message) {
        try {
            socketBuf.write("\r\nFrom: " + message.getWriter() +
                    "\r\nData-length: " + message.getBodyLength() +
                    "\r\nMessage: \n" +
                    message.getBody() + "\r\n" +
                    "\r\n");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void addUser() {
        this.send(new Message("server", "Welcome!"));
        addClient(this);
    }

    public String chooseUserName() {
        this.send(new Message("server", "Enter login: " + "\n"));
        String clientMessage;

        while (true) {
            if (!(clientMessage = socketBuf.readLine().trim()).equals("")) {
                if (!clientContains(this)) {
                    break;
                } else {
                    this.send(new Message("server", "Login busy. Try again: " + "\n"));
                }
            }
        }
        return clientMessage;
    }

    public String getUserName() {
        return userName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMember member = (ChatMember) o;
        return Objects.equals(userName, member.getUserName());
    }

}
