package com.nightmare.internetsaver;

import android.content.Context;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ProxyServer {
    private static final int PROXY_PORT = 8888;
    private ServerSocket serverSocket;
    private Thread serverThread;
    private boolean isRunning = false;
    private Context context;
    private List<String> savedRequests;
    private OkHttpClient client;
    private boolean isReplayMode = false;
    private int replayIndex = 0;

    public ProxyServer(Context context) {
        this.context = context;
        this.savedRequests = new ArrayList<>();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void start() {
        if (isRunning) return;

        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PROXY_PORT);
                isRunning = true;

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        serverThread.setDaemon(true);
        serverThread.start();
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8)
            );
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8)
            );

            String requestLine = reader.readLine();
            if (requestLine == null) {
                clientSocket.close();
                return;
            }

            String[] parts = requestLine.split(" ");
            String method = parts[0];
            String path = parts[1];

            StringBuilder headers = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                headers.append(line).append("\n");
            }

            StringBuilder body = new StringBuilder();
            if (method.equals("POST") || method.equals("PUT")) {
                char[] buffer = new char[1024];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    body.append(buffer, 0, read);
                }
            }

            if (isReplayMode) {
                sendReplayResponse(writer, method, path);
            } else {
                forwardRequest(writer, method, path, headers.toString(), body.toString());
            }

            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void forwardRequest(BufferedWriter writer, String method, String path,
                               String headers, String body) {
        try {
            HttpRequest request = new HttpRequest(method, path, headers, body);
            Gson gson = new Gson();
            savedRequests.add(gson.toJson(request));

            String url = "http://httpbin.org" + path;
            Request.Builder requestBuilder = new Request.Builder().url(url);

            if (method.equals("POST") || method.equals("PUT")) {
                requestBuilder.method(method, okhttp3.RequestBody.create(body.getBytes()));
            } else {
                requestBuilder.method(method, null);
            }

            Request okRequest = requestBuilder.build();
            Response response = client.newCall(okRequest).execute();

            ResponseBody responseBody = response.body();
            String responseContent = responseBody != null ? responseBody.string() : "";

            writer.write("HTTP/1.1 " + response.code() + " " + response.message() + "\r\n");
            writer.write("Content-Type: application/json\r\n");
            writer.write("Content-Length: " + responseContent.length() + "\r\n");
            writer.write("\r\n");
            writer.write(responseContent);
            writer.flush();

            response.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendReplayResponse(BufferedWriter writer, String method, String path) {
        try {
            if (replayIndex < savedRequests.size()) {
                Gson gson = new Gson();
                String requestJson = savedRequests.get(replayIndex);
                HttpRequest request = gson.fromJson(requestJson, HttpRequest.class);

                String response = "{\"status\": \"ok\", \"method\": \"" + request.method + 
                                 "\", \"url\": \"" + request.url + "\"}";

                writer.write("HTTP/1.1 200 OK\r\n");
                writer.write("Content-Type: application/json\r\n");
                writer.write("Content-Length: " + response.length() + "\r\n");
                writer.write("\r\n");
                writer.write(response);
                writer.flush();

                replayIndex++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void enableReplayMode() {
        isReplayMode = true;
        replayIndex = 0;
    }

    public void disableReplayMode() {
        isReplayMode = false;
        replayIndex = 0;
    }

    public int getSavedRequestCount() {
        return savedRequests.size();
    }

    public void clearSavedRequests() {
        savedRequests.clear();
    }
}