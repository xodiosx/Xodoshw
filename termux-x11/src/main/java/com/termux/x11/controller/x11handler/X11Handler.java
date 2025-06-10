package com.termux.x11.controller.x11handler;

import android.util.Log;
import java.io.OutputStream;
import java.net.Socket;

public class X11Handler {
    private static final String SOCKET_PATH = "/tmp/x11input.sock";

    public static void sendKey(String key) {
        new Thread(() -> {
            try (Socket socket = new Socket()) {
                socket.connect(new java.net.InetSocketAddress("127.0.0.1", 7948)); // X11 listener
                OutputStream os = socket.getOutputStream();
                os.write((key + "\n").getBytes());
                os.flush();
                Log.d("X11Handler", "Sent key: " + key);
            } catch (Exception e) {
                Log.e("X11Handler", "Error: " + e.getMessage());
            }
        }).start();
    }
}