package org.example;

import java.io.IOException;

@FunctionalInterface
public interface Handler {
    void handle(ClientRequest request) throws IOException;
}
