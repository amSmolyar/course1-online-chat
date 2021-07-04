package ru.netology;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerTest {
    @Test
    void server_no_null() {
        System.out.println("server_no_null");
        Server server = new Server();
        assertNotNull(server);
        server.close();
    }
}