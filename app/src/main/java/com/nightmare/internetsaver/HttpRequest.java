package com.nightmare.internetsaver;

public class HttpRequest {
    public String method;
    public String url;
    public String headers;
    public String body;
    public long timestamp;

    public HttpRequest(String method, String url, String headers, String body) {
        this.method = method;
        this.url = url;
        this.headers = headers;
        this.body = body;
        this.timestamp = System.currentTimeMillis();
    }
}