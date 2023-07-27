package org.example;

import java.io.BufferedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Request implements Handler {
    
    
    @Override
    public void handle(Request request, BufferedOutputStream responseStream) {
        
        final var filePath = Path.of(".", "public", resourcePath);
        final var mimeType = Files.probeContentType(filePath);
        BufferedOutputStream out = this.client.getClientOut();
        
        Handler getClassic = (rqst, response) -> {
        
        };
    
    }
}
