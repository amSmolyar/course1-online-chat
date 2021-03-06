# Проект "Сетевой чат"

По структуре проект делится на три модуля:
* server
* client
* library

Модуль server содержит классы, отвечающие за функционирование сервера.

Модуль client содержит классы, отвечающие за функционирование клиента.

Модуль library содержит классы, общие для сервера и клиента.

## Server

Запустить сервер можно, запустив класс Main модуля server. 

Номер порта для подключения клиентов считывается из файла настроек settings.txt, находящегося в каталоге проекта.

После инициализации сервера он переходит в режим ожидания подключения клиентов. Клиенты могут подключаться к серверу в любой момент времени. 

Сервер осуществляет ретрансляцию сообщений от одного клиента всем подключенным участникам.

Сервер обрабатывает и отправляет сообщения в соответствии с протоколом:

> \r\n  
 From: <имя отправителя>\r\n  
 Data-length: <длина текста сообщения в байтах>\r\n  
 Message: \n  
 <тело сообщения>\r\n  
 \r\n   

## Client

Запустить приложение "клиент" можно в любой момент, запустив класс Main модуля client. 

IP-адрес и номер порта для подключения к серверу считываются из файла настроек settings.txt, находящегося в каталоге проекта.

После инициализации и запуска клиента сервер обнаруживает новое подключение и предлагает клиенту выбрать имя для участия в чате. Имя должно быть уникальным.

Клиент осуществляет отправку сообщений серверу и прием сообщений других пользователей от сервера.

Клиент обрабатывает и отправляет сообщения, составленные в соответствии с протоколом:


> \r\n  
 From: <имя отправителя>\r\n  
 Data-length: <длина текста сообщения в байтах>\r\n  
 Message: \n  
 <тело сообщения>\r\n  
 \r\n  

