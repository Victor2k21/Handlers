package org.example;

import static org.example.Server.threadPool;

public class Main {
    
    public static void main(String[] args) {
        Server.initPoolThreads();
        new Handle().initHandler();
        Server.startServer();
        
        threadPool.shutdown();
        System.out.println("Сервер остановлен");
    }
}