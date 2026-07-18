package com.nightmare.internetsaver;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class ProxyService extends Service {
    private ProxyServer proxyServer;
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        ProxyService getService() {
            return ProxyService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        proxyServer = new ProxyServer(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("START_PROXY".equals(action)) {
                proxyServer.start();
            } else if ("STOP_PROXY".equals(action)) {
                proxyServer.stop();
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }
}