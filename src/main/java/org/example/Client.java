package org.example;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.example.Server.*;

public class Client {
    
    private final Socket clientSocket;
    private final BufferedInputStream in;
    private final BufferedOutputStream out;
    //private final byte[] request = new byte[LIMIT];
    
    public Client(Socket clientSocket, BufferedInputStream in, BufferedOutputStream out) {
        this.clientSocket = clientSocket;
        this.in = in;
        this.out = out;
        this.in.mark(LIMIT);
    }
    
    public boolean readClient() throws IOException, ExecutionException, InterruptedException {
        Callable<ClientRequest> read = () -> {
            
            ClientRequest clientRequest = new ClientRequest(this);
            
            try {
                byte[] buffer = new byte[LIMIT];
                clientRequest.setRequestLenght(this.in.read(buffer));
                clientRequest.setRequestLine(buffer);
                if (clientRequest.prepareRequest()) {
                    this.in.reset();
                    this.in.skip(clientRequest.getHeaderStart());
                    buffer = this.in.readNBytes(clientRequest.getHeaderEnd() - clientRequest.getHeaderStart());
                    clientRequest.parseHeaders(buffer);
                    clientRequest.getQueryParams();
                }
                
                if (clientRequest.isEndOfConnect()) {
                    clientRequest.getClient().in.close();
                    clientRequest.getClient().out.close();
                    clientRequest.getClient().clientSocket.close();
                    return clientRequest;
                }
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
            
            // if (!clientRequest.requestToDo()) {
            if (!allowedMethods.contains(clientRequest.getMethod())) {
                System.out.println("Некорректный запрос метода");
            }
              return clientRequest;
        };
        
        ClientRequest clientRequest = null;
        Future<ClientRequest> task = threadPool.submit(read);
        threads.add(task);
        
        for (Future<ClientRequest> t : threads) {
            if (!t.isDone()) {
                clientRequest = t.get();
                //
                if (!clientRequest.isParams()) {
                    System.out.println("name = title -> " + clientRequest.getQueryParam("title"));
                    System.out.println("name = value -> " + clientRequest.getQueryParam("value"));
                    System.out.println("name = image -> " + clientRequest.getQueryParam("image"));
                }
            }
        }
        if (clientRequest == null) return false;
        return clientRequest.isEndOfConnect();
    }
    
    public BufferedInputStream getClientIn() {
        return this.in;
    }
    
    public BufferedOutputStream getClientOut() {
        return this.out;
    }
    
    public BufferedInputStream getInSocket() {
        return this.in;
    }
    
    public BufferedOutputStream getOut() {
        return this.out;
    }
}
