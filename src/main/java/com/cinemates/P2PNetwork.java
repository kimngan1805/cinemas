package com.cinemates;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

public class P2PNetwork {
    private static final int PORT = 6666; // Cổng để 2 máy chat với nhau
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Cái này để báo tin nhắn về cho giao diện
    private Consumer<String> onMessageReceived;

    public void setOnMessageReceived(Consumer<String> listener) {
        this.onMessageReceived = listener;
    }

    // 1. CHỨC NĂNG HOST (Tạo phòng)
    public void startHost() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Đang chờ kết nối tại port " + PORT);
                Socket client = serverSocket.accept(); // Chờ người khác vào
                setupConnection(client);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // 2. CHỨC NĂNG GUEST (Vào phòng)
    public void connect(String ip) {
        new Thread(() -> {
            try {
                System.out.println("Đang kết nối tới IP: " + ip);
                Socket socket = new Socket(ip, PORT);
                setupConnection(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // 3. THIẾT LẬP KẾT NỐI
    private void setupConnection(Socket socket) {
        this.socket = socket;
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // --- SỬA ĐOẠN NÀY ---
            // Thay vì chào, mình gửi lệnh ngầm xin dữ liệu ngay lập tức
            out.println("CMD:REQUEST_INFO");
            // ---------------------

            String msg;
            while ((msg = in.readLine()) != null) {
                if (onMessageReceived != null) {
                    onMessageReceived.accept(msg);
                }
            }
        } catch (IOException e) {
            System.out.println("Mất kết nối P2P");
        }
    }

    // 4. GỬI TIN NHẮN
    public void send(String msg) {
        if (out != null) out.println(msg);
    }
}