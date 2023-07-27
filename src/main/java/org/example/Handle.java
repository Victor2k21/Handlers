package org.example;

import java.io.BufferedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.example.Server.methodMap;
import static org.example.Server.pathMap;

public class Handle implements Handler {
    
    
    Handler getClassic = (rqst) -> {
        final var template = Files.readString(rqst.getResourcePath());
        final var mimeType = Files.probeContentType(rqst.getResourcePath());
        final var out = rqst.getClient().getClientOut();
        
        final var content = template.replace(
                "{time}",
                LocalDateTime.now().toString()
        ).getBytes();
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + content.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.write(content);
        out.flush();
    };
    
    Handler getOther = (rqst) -> {
        final var length = Files.size(rqst.getResourcePath());
        final var mimeType = Files.probeContentType(rqst.getResourcePath());
        BufferedOutputStream out = rqst.getClient().getClientOut();
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(rqst.getResourcePath(), out);
        out.flush();
    };
    
    
    @Override
    public void handle(ClientRequest request) {
    
    }
    
    public void initHandler() {
        initGetHandler();
        initPutHandle();
        initPostHandle();
    }
    
    public void initGetHandler() {
       
            pathMap.put(Path.of(".", "public", "/index.html"), getOther);
            pathMap.put(Path.of(".", "public", "/spring.svg"), getOther);
            pathMap.put(Path.of(".", "public", "/resources.html"), getOther);
            pathMap.put(Path.of(".", "public", "/spring.png"), getOther);
            pathMap.put(Path.of(".", "public", "/styles.css"), getOther);
            pathMap.put(Path.of(".", "public", "/app.js"), getOther);
            pathMap.put(Path.of(".", "public", "/links.html"), getOther);
            pathMap.put(Path.of(".", "public", "/forms.html"), getOther);
            pathMap.put(Path.of(".", "public", "/classic.html"), getClassic);
            pathMap.put(Path.of(".", "public", "/events.html"), getOther);
            pathMap.put(Path.of(".", "public", "/events.js"), getOther);
       
            methodMap.put("GET", pathMap);
        }
        
        private void initPutHandle () {
        
        }
        
        private void initPostHandle () {
            pathMap.put(Path.of(".", "public", "/"), getOther);
    
            methodMap.put("POST", pathMap);
        }
    }
