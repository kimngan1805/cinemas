package com.cinemates;

import java.io.PrintWriter;
import java.net.Socket;

public class ClientSession {
    // Biến tĩnh (Static) để truy cập mọi nơi
    private static Socket socket;
    private static PrintWriter out;
    private static String username;
    public static String myUsername = "Anonymous";
    public static boolean connect(String ip, int port, String name) {
        try {
            socket = new Socket(ip, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            username = name;

            // Gửi lệnh đăng ký ngay lập tức
            out.println("REGISTER " + username);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getUsername() { return username; }
    public static void send(String msg) { if (out != null) out.println(msg); }
}