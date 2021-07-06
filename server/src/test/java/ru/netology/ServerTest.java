package ru.netology;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class ServerTest {

    // ===========================================================================================
    // Проверяется, что при вызове констуктора сервера возвращается не null. А также не null
    // его приватные поля listMember и logger
    @Test
    void test_server_no_null() {
        System.out.println("test_server_no_null");
        Server server = new Server();
        assertNotNull(server);
        assertNotNull(server.getListMember());
        assertNotNull(server.getLogger());
        server.close();
    }

    // ===========================================================================================
    // Проверяются методы addClient(ChatMember client) и sendAll(Message message).
    // В список участников добавляются 5 участников, затем вызывается метод sendAll(Message message).
    // У каждого участника метод send(Message message) должен быть вызван только по одному разу.
    // Лист должен содержать 5 участников
    @Test
    void test_server_sendAll() {
        System.out.println("test_server_sendAll");
        Server server = new Server();
        Message message = new Message("server", "text from server");

        ChatMember chatMember1 = Mockito.mock(ChatMember.class);
        server.addClient(chatMember1);
        ChatMember chatMember2 = Mockito.mock(ChatMember.class);
        server.addClient(chatMember2);
        ChatMember chatMember3 = Mockito.mock(ChatMember.class);
        server.addClient(chatMember3);
        ChatMember chatMember4 = Mockito.mock(ChatMember.class);
        server.addClient(chatMember4);
        ChatMember chatMember5 = Mockito.mock(ChatMember.class);
        server.addClient(chatMember5);

        server.sendAll(message);
        Mockito.verify(chatMember1, Mockito.times(1)).send(message);
        Mockito.verify(chatMember2, Mockito.times(1)).send(message);
        Mockito.verify(chatMember3, Mockito.times(1)).send(message);
        Mockito.verify(chatMember4, Mockito.times(1)).send(message);
        Mockito.verify(chatMember5, Mockito.times(1)).send(message);

        assertEquals(5, server.getListMember().size());

        server.close();
    }

    // ===========================================================================================
    // Проверяются методы addClient(ChatMember client), removeClient(ChatMember client) и попутно sendAll(Message message).
    // В список участников добавляются по одному 5 участников (каждый раз контролируется размер списпка участников).
    // Затем по одному участники исключаются из списка (каждый раз контролируется размер списпка участников).
    // Во время добавления и исключения участника каждому участнику чата отправляется сообщение от сервера.
    // Контролируется количество вызовов метода send(Message message) для каждого из участников.
    @Test
    void test_server_add_remove_sendPush() {
        System.out.println("test_server_add_remove_sendPush");
        Server server = new Server();

        ChatMember chatMember1 = Mockito.mock(ChatMember.class);
        server.addClient(chatMember1);
        assertEquals(1, server.getListMember().size());

        ChatMember chatMember2 = Mockito.mock(ChatMember.class);
        server.addClient(chatMember2);
        assertEquals(2, server.getListMember().size());

        ChatMember chatMember3 = Mockito.mock(ChatMember.class);
        server.addClient(chatMember3);
        assertEquals(3, server.getListMember().size());

        ChatMember chatMember4 = Mockito.mock(ChatMember.class);
        server.addClient(chatMember4);
        assertEquals(4, server.getListMember().size());

        ChatMember chatMember5 = Mockito.mock(ChatMember.class);
        server.addClient(chatMember5);
        assertEquals(5, server.getListMember().size());

        Mockito.verify(chatMember1, Mockito.times(5)).send(Mockito.any(Message.class));
        Mockito.verify(chatMember2, Mockito.times(4)).send(Mockito.any(Message.class));
        Mockito.verify(chatMember3, Mockito.times(3)).send(Mockito.any(Message.class));
        Mockito.verify(chatMember4, Mockito.times(2)).send(Mockito.any(Message.class));
        Mockito.verify(chatMember5, Mockito.times(1)).send(Mockito.any(Message.class));

        // remove 1 member
        server.removeClient(chatMember1);
        Mockito.verify(chatMember1, Mockito.times(6)).send(Mockito.any(Message.class));
        assertEquals(4, server.getListMember().size());

        // remove 2 member
        server.removeClient(chatMember2);
        Mockito.verify(chatMember2, Mockito.times(6)).send(Mockito.any(Message.class));
        assertEquals(3, server.getListMember().size());

        // remove 3 member
        server.removeClient(chatMember3);
        Mockito.verify(chatMember3, Mockito.times(6)).send(Mockito.any(Message.class));
        assertEquals(2, server.getListMember().size());

        // remove 4 member
        server.removeClient(chatMember4);
        Mockito.verify(chatMember4, Mockito.times(6)).send(Mockito.any(Message.class));
        assertEquals(1, server.getListMember().size());

        // remove 5 member
        server.removeClient(chatMember5);
        Mockito.verify(chatMember5, Mockito.times(6)).send(Mockito.any(Message.class));
        assertEquals(0, server.getListMember().size());

        server.close();
    }

    // ===========================================================================================
    // Проверяется метод clientContains(ChatMember client), а также повторно addClient(ChatMember client).
    // В чат добавляются 3 участника. 4ый создается, но не добавляется. Проверяем наличие в списке участников всех
    // 4-х. Убеждаемся, что первые 3 есть в списке, а 4го нет. Добавляем в чат 4-го. Проверяем, что он появился.
    @Test
    void test_server_contains() {
        System.out.println("test_server_contains");
        Server server = new Server();

        ChatMember chatMember1 = Mockito.mock(ChatMember.class);
        server.addClient(chatMember1);

        ChatMember chatMember2 = Mockito.mock(ChatMember.class);
        server.addClient(chatMember2);

        ChatMember chatMember3 = Mockito.mock(ChatMember.class);
        server.addClient(chatMember3);

        ChatMember chatMember4 = Mockito.mock(ChatMember.class);

        assertTrue(server.clientContains(chatMember1));
        assertTrue(server.clientContains(chatMember2));
        assertTrue(server.clientContains(chatMember3));
        assertFalse(server.clientContains(chatMember4));

        server.addClient(chatMember4);
        assertTrue(server.clientContains(chatMember4));

        server.close();
    }

}