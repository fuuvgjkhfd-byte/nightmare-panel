package com.nightmare.internetsaver;

public class HttpResponse {
    public int statusCode;
    public String headers;
    public String body;
    public long timestamp;

    public HttpResponse(int statusCode, String headers, String body) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
        this.timestamp = System.currentTimeMillis();
    }
}