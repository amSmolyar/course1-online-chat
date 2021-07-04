package ru.netology;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ServerTest {
    //private CopyOnWriteArrayList<ChatMember> listMember = getClientList();
    //private static Server server;

    public static CopyOnWriteArrayList<ChatMember> getClientList() {
        CopyOnWriteArrayList<ChatMember> clientList = new CopyOnWriteArrayList<>();
        for (int ii = 0; ii < 5; ii++) {
            clientList.add(new ChatMember(new InOut("127.0.0.1", 10000)));
        }
        return clientList;
    }

/*
    @BeforeAll
    public static void server_no_null() {
        System.out.println("server_no_null");
        Server server = new Server();
        assertNotNull(server);
        server.close();
    }

    @Test
    void test_server_sendAll() {
        InOut inOut;

        Server server = new Server();
        Thread serverThread = new Thread(() -> server.runServer());
        serverThread.start();
        for (int ii = 0; ii < 5; ii++) {
            inOut = new InOut("127.0.0.1", 58005);

        }

        serverThread.interrupt();
        assertEquals(Server.listMember.size(), 5);
        serverThread.interrupt();

        //for (int ii = 0; ii < Server.listMember.size(); ii++) {

        //}
    }

 */

    @Test
    void test_server_removeClient() {

    }
}