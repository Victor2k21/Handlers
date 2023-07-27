package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

public class Server {
    
    private static final int PORT = 8089;
    private static final int THREADS_QUANTITY = 6;
    
    public static final ConcurrentHashMap<Path, Handler> pathMap = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, ConcurrentHashMap<Path, Handler>> methodMap =
            new ConcurrentHashMap<>();
    
    public static ServerSocket serverSocket = null;
    public static final ConcurrentHashMap<Socket, Client> clientList = new ConcurrentHashMap<>();
    public static final ArrayBlockingQueue<Future<ClientRequest>> threads = new ArrayBlockingQueue<>(64);
    
    public static ExecutorService threadPool;
    
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final int LIMIT = 4096;
    public static final List<String> allowedMethods = List.of(GET, POST);
    
    
    // инициируем пул потоков
    public static void initPoolThreads() {
        threadPool = Executors.newFixedThreadPool(THREADS_QUANTITY);
    }
    
    private Server() {
        
        try (final ServerSocket serverSckt = new ServerSocket(PORT)) {
            
            serverSocket = serverSckt;
            System.out.println("Сервер запущен");
            
            Client client;
            while (true) {
                Socket socket;
                BufferedInputStream input;
                BufferedOutputStream output;
                try {
                    final Socket sckt = serverSocket.accept();
                    final var in = new BufferedInputStream(sckt.getInputStream());
                    final var out = new BufferedOutputStream(sckt.getOutputStream());
                    
                    // лимит на request line + заголовки
                    socket = sckt;
                    input = in;
                    output = out;
                    
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                
                if (clientList.containsKey(socket)) {
                    client = clientList.get(socket);
                    client.getClientIn().mark(LIMIT);
                } else {
                    synchronized (clientList) {
                        client = new Client(socket, input, output);
                        clientList.put(socket, client);
                        clientList.notifyAll();
                    }
                    try {
                        if (client.readClient()) break;
                    } catch (IOException | ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void startServer() {
        if (serverSocket == null) new Server();
    }
}