package org.example;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.example.Server.*;

public class ClientRequest {
    
    private Path resourcePath;
    private final Client client;
    
    private String method;
    private final Map<String, List<String>> params = new HashMap<>();
    private String path;
    private byte[] requestLine; //исходный массив
    private List<String> headers; //список заголовков
    private int requestLenght; //длина полученного массива
    private int headersStart; //начальная позиция заголовков
    private int headersEnd; //конечная позиция заголовков
    private String body;
    private boolean endOfConnect = false;
    
    
    public void getQueryParams() {
        
        String valueDelimited = "\r\n\r\n";
        String paramName;
        String paramValue;
        
        final var contentType = extractHeader(this.headers, "Content-Type");
        if (contentType.isPresent()) {
            String paramHeader = parseContent(contentType.get(), "boundary");
            
            if (paramHeader != null) {
                String[] paramsBody = this.body.split(paramHeader);
                for (String param : paramsBody) {
                    if (param.startsWith("\r\nContent-Disposition")) {
                        String[] paramNameValue = param.split("=");
                        if (paramNameValue.length == 2) {
                            paramName = paramNameValue[1]
                                    .substring(1, paramNameValue[1].indexOf('\r') - 1)
                                    .trim();
                            paramValue = paramNameValue[1]
                                    .substring(paramNameValue[1].indexOf('\r') + valueDelimited.length());
                            paramValue = paramValue.substring(0, paramValue.indexOf('\n'))
                                    .trim();
                        } else {
                            paramName = paramNameValue[1]
                                    .substring(1, paramNameValue[1].indexOf(';') - 1)
                                    .trim();
                            paramValue = paramNameValue[2]
                                    .substring(1, paramNameValue[2].indexOf('\r') - 1)
                                    .trim();
                        }
                        if (!paramValue.equals("/end")) {
                            List<String> listParamValue;
                            if (params.isEmpty() || !params.containsKey(paramName)) {
                                listParamValue = new ArrayList<>();
                                listParamValue.add(paramValue);
                                this.params.put(paramName, listParamValue);
                            } else {
                                listParamValue = this.params.get(paramName);
                                listParamValue.add(paramValue);
                                this.params.put(paramName, listParamValue);
                            }
                        } else {
                            this.endOfConnect = true;
                            break;
                        }
                    }
                }
            }
        }
    }
    
    public List<String> getQueryParam(String name) {
        return this.params.get(name);
    }
    public boolean prepareRequest() throws IOException, URISyntaxException {
        
        // ищем request line
        
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        //конечная позиция запроса
        int requestLineEnd = indexOf(this.requestLine, requestLineDelimiter, 0, requestLenght);
        if (requestLineEnd == -1) {
            badRequest(this.client.getClientOut());
            return false;
        }
        
        // читаем request line
        final var request = new String(Arrays.copyOf(requestLine, requestLineEnd)).split(" ");
        if (request.length != 3) {
            badRequest(this.client.getClientOut());
            return false;
        }
        
        this.method = request[0];
        if (!allowedMethods.contains(this.method)) {
            badRequest(this.client.getClientOut());
            return false;
        }
        System.out.println(this.method);
        
        this.path = request[1];
        if (!this.path.startsWith("/")) {
            badRequest(this.client.getClientOut());
            return false;
        }
        // this.resourcePath = Path.of("./01_web/http-server", "public", this.path);
        System.out.println(this.path);
        
        // ищем заголовки
        byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        this.headersStart = requestLineEnd + requestLineDelimiter.length;
        this.headersEnd = indexOf(this.requestLine, headersDelimiter,
                this.headersStart, this.requestLenght);
        if (this.headersEnd == -1) {
            badRequest(this.client.getClientOut());
            return false;
        }
        return true;
    }
    
    public void parseHeaders(byte[] headersBytes) throws IOException {
    
        this.headers = Arrays.asList(new String(headersBytes).split("\r\n"));
        // для GET тела нет
        if (!method.equals(GET)) {
            // вычитываем Content-Length, чтобы прочитать body
            final var contentLength = extractHeader(this.headers, "Content-Length");
            
            if (contentLength.isPresent()) {
                final var length = Integer.parseInt(contentLength.get());
                final var bodyBytes = this.client.getClientIn().readNBytes(length);
                
                this.body = new String(bodyBytes);
                //System.out.println(body);
            }
        }
        
        this.client.getOut().write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        this.client.getOut().flush();
    }
    
    
    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }
    
    private String parseContent(String content, String keyWord) {
        String str[] = content.split(keyWord + "=");
        if (str.length != 2) return null;
        return str[1];
    }
    
    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }
    
     public boolean requestToDo() throws IOException {
        
        Handler handle = null;
        if (methodMap.containsKey(this.method)) {
            ConcurrentHashMap<Path, Handler> path = methodMap.get(this.method);
            if (path.containsKey(this.resourcePath)) handle = path.get(this.resourcePath);
        }
        if (handle != null) {
            handle.handle(this);
        }
        return handle != null;
    }
    
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
    
    public Path getResourcePath() {
        return this.resourcePath;
    }
    
    public String getMethod() {
        return this.method;
    }
    
    public Client getClient() {
        return this.client;
    }
    
    public String getPath() {
        return this.path;
    }
 
    public byte[] getRequestLine() {
        return this.requestLine;
    }
    
    public void setRequestLenght(int lenght) {
        this.requestLenght = lenght;
    }
    
    public int getHeaderStart() {
        return this.headersStart;
    }
    
    public int getHeaderEnd() {
        return this.headersEnd;
    }
    
    public void setRequestLine(byte[] requestLine) {
        this.requestLine = requestLine;
    }
    
    public ClientRequest(Client client) {
        this.client = client;
    }
    
    public boolean isEndOfConnect() {
        return this.endOfConnect;
    }
    
    public boolean isParams() {
        return this.params.isEmpty();
    }
}
