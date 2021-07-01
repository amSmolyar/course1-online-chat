package ru.netology;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Message {
    private String writer;
    private String time;
    private int bodyLength;
    private String body;

    private DateFormat dateFormat;
    private Date date;

    public Message(String writer, int bodyLength, String body, String time) {
        this.writer = writer;
        this.bodyLength = bodyLength;
        this.body = body;
        this.time = time;
    }

    public Message(String writer, int bodyLength, String body) {
        this.writer = writer;
        this.bodyLength = bodyLength;
        this.body = body;

        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy   HH:mm:ss");
        this.date = new Date();

        this.time = dateFormat.format(date);
    }

    public Message(String writer, String body) {
        this.writer = writer;
        this.bodyLength = body.getBytes().length;
        this.body = body;

        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy   HH:mm:ss");
        this.date = new Date();

        this.time = dateFormat.format(date);
    }

    public String getWriter() {
        return writer;
    }

    public void setWriter(String writer) {
        this.writer = writer;
    }

    public int getBodyLength() {
        return bodyLength;
    }

    public void setBodyLength(int bodyLength) {
        this.bodyLength = bodyLength;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return  time +
                "\n     from: " + writer + ":" +
                "\n         " + body + "\r\n" +
                "\r\n";
    }
}
