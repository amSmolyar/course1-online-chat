package ru.netology;

import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class ChatMember implements Runnable {
    private final Socket socket;
    private String userName;

    private DateFormat dateFormat;
    private Date date;

    private BufferedOutputStream outBuf;
    private BufferedReader inBuf;

    public ChatMember(Socket socket) {
        this.socket = socket;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy   HH:mm:ss");
        this.date = new Date();
    }

    @Override
    public void run() {
        try {
            outBuf = new BufferedOutputStream(socket.getOutputStream());
            inBuf = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            getUserName(outBuf, inBuf);

            Message message;
            while (true) {
                if (!parseMessage(inBuf)) {
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
                inBuf.close();
                outBuf.close();
                socket.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private boolean parseMessage(BufferedReader in) throws IOException {
        String readLine;
        String writerName = "";
        String body;
        Message message;
        int cntHeader;
        byte[] bodyByteArray;
        while (true) {
            int bodyLength = -1;

            while (!inBuf.ready()) {}

            cntHeader = 0;
            while (!(readLine = in.readLine().trim()).equals("")) {
                if (cntHeader == 0) {
                    if (readLine.equalsIgnoreCase("/exit"))
                        return false;
                    else if (readLine.startsWith("From: "))
                        writerName = readLine.substring(readLine.indexOf(":")).trim();
                    else {
                        send(new Message("server", "Неправильный формат сообщения!\n"));
                        return true;
                    }
                } else if ((cntHeader == 1) && (readLine.startsWith("Data-length: "))) {
                    bodyLength = Integer.parseInt(readLine.substring(readLine.indexOf(":")).trim());
                } else if ((cntHeader == 2) && (readLine.startsWith("Message: "))) {
                    if (bodyLength > 0) {
                        bodyByteArray = readBody(inBuf, bodyLength);
                        message = new Message(writerName, bodyLength, bodyByteArray.toString(), dateFormat.format(date));
                        send(message);
                        Server.logger.log(message);
                    }
                } else {
                    send(new Message("server", "Неправильный формат сообщения!\n"));
                    return true;
                }

                cntHeader++;
                while (!inBuf.ready()) {}
            }
            return true;
        }
    }

    private byte[] readBody(BufferedReader in, int bodyLength) throws IOException {
        // =============  body  =============
        ByteArrayOutputStream bodyBAOStream = new ByteArrayOutputStream();
        while (bodyBAOStream.size() < bodyLength) {
            while (!in.ready()) {}
            bodyBAOStream.write(in.read());
        }
        return bodyBAOStream.toByteArray();
    }

    public void send(Message message) {
        try {
            outBuf.write(("From: " + message.getWriter() +
                        "\nData-length: " + message.getBodyLength() +
                        "\nMessage: \n" +
                        message.getBody() + "\r\n" +
                        "\r\n").getBytes());
            outBuf.flush();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void getUserName(BufferedOutputStream outBuf, BufferedReader inBuf) throws IOException {
        outBuf.write(("Введите имя для участия в чате: " + "\n").getBytes());
        outBuf.flush();

        String clientMessage;
        while (true) {
            if ((clientMessage = inBuf.readLine().trim()).equals("")) {
                this.userName = clientMessage;
                if (!Server.listMember.contains(this)) {
                    Server.listMember.add(this);
                    Server.logger.log("К чату присоединился участник " + this.userName);
                    Server.sendAll(new Message("server", "К чату присоединился участник " + this.userName));
                    break;
                } else {
                    outBuf.write(("Выбранное вами имя занято. Повторите попытку: " + "\n").getBytes());
                    outBuf.flush();
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
        return socket.equals(member.socket) &&
                Objects.equals(userName, member.getUserName());
    }

}
