package com.nightmare.internetsaver;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private ProxyService proxyService;
    private boolean isBound = false;
    private ProxyServer proxyServer;
    private TextView statusText;
    private TextView infoText;
    private Button saveButton;
    private Button playButton;
    private Button stopButton;
    private boolean isSaving = false;
    private boolean isPlaying = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ProxyService.LocalBinder binder = (ProxyService.LocalBinder) service;
            proxyService = binder.getService();
            proxyServer = proxyService.getProxyServer();
            isBound = true;
            updateUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            proxyService = null;
            proxyServer = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        infoText = findViewById(R.id.infoText);
        saveButton = findViewById(R.id.saveButton);
        playButton = findViewById(R.id.playButton);
        stopButton = findViewById(R.id.stopButton);

        Intent serviceIntent = new Intent(this, ProxyService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        saveButton.setOnClickListener(v -> startSavingInternet());
        playButton.setOnClickListener(v -> startPlayingInternet());
        stopButton.setOnClickListener(v -> stopAll());
    }

    private void startSavingInternet() {
        if (proxyServer == null) {
            Toast.makeText(this, "خدمت آماده نیست", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isSaving) {
            Toast.makeText(this, "قبلاً در حال ذخیره است", Toast.LENGTH_SHORT).show();
            return;
        }

        isSaving = true;
        isPlaying = false;
        proxyServer.clearSavedRequests();
        proxyServer.disableReplayMode();
        proxyServer.start();

        statusText.setText("🟢 در حال ذخیره اینترنت...");
        infoText.setText("اینترنت شبانه را استفاده کنید");
        Toast.makeText(this, "ذخیره اینترنت شروع شد", Toast.LENGTH_SHORT).show();
        updateUI();
    }

    private void startPlayingInternet() {
        if (proxyServer == null) {
            Toast.makeText(this, "خدمت آماده نیست", Toast.LENGTH_SHORT).show();
            return;
        }

        if (proxyServer.getSavedRequestCount() == 0) {
            Toast.makeText(this, "هیچ داده ذخیره شده نیست", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isPlaying) {
            Toast.makeText(this, "قبلاً در حال پخش است", Toast.LENGTH_SHORT).show();
            return;
        }

        isPlaying = true;
        isSaving = false;
        proxyServer.enableReplayMode();
        proxyServer.start();

        statusText.setText("🔵 در حال پخش اینترنت ذخیره شده...");
        infoText.setText("اینترنت اصلی را خاموش کنید\nVPN را روشن کنید\nاکنون می‌توانید بدون اینترنت استفاده کنید");
        Toast.makeText(this, "پخش اینترنت ذخیره شده شروع شد", Toast.LENGTH_SHORT).show();
        updateUI();
    }

    private void stopAll() {
        if (proxyServer != null) {
            proxyServer.stop();
        }

        isSaving = false;
        isPlaying = false;

        statusText.setText("⚫ متوقف شده");
        infoText.setText("دستور را دوباره انتخاب کنید");
        Toast.makeText(this, "تمام فعالیت‌ها متوقف شدند", Toast.LENGTH_SHORT).show();
        updateUI();
    }

    private void updateUI() {
        if (proxyServer != null) {
            int requestCount = proxyServer.getSavedRequestCount();
            infoText.setText("درخواست‌های ذخیره شده: " + requestCount);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }

        if (proxyServer != null) {
            proxyServer.stop();
        }
    }
}