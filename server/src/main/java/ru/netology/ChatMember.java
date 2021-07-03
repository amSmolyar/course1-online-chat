package ru.netology;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

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
        try {
            chooseUserName();

            while (true) {
                if (!parseMessage()) {
                    Server.logger.log("Чат покинул участник " + this.userName);
                    Server.sendAll(new Message("server", "Чат покинул участник " + this.userName));
                    Server.removeClient(this);
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                socketBuf.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private boolean parseMessage() throws IOException {
        String readLine;
        String writerName = "";
        Message message = null;
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
                    }
                } else {
                    send(new Message("server", "Неправильный формат сообщения!\n"));
                    return true;
                }
                cntHeader++;
            }
            break;
        }

        Server.sendAll(message);
        Server.logger.log(message);
        return true;
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

    public void chooseUserName() throws IOException {
        this.send(new Message("server", "Введите имя для участия в чате: " + "\n"));
        String clientMessage;
        while (true) {
            if (!(clientMessage = socketBuf.readLine().trim()).equals("")) {
                this.userName = clientMessage;
                if (!Server.listMember.contains(this)) {
                    Server.listMember.add(this);
                    Server.logger.log("К чату присоединился участник " + this.userName);
                    this.send(new Message("server", "Добро пожаловать!"));
                    Server.sendAll(new Message("server", "К чату присоединился участник " + this.userName));
                    break;
                } else {
                    this.send(new Message("server", "Выбранное вами имя занято. Повторите попытку: " + "\n"));
                }
            }
        }
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
