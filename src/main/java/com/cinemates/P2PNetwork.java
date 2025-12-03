package com.cinemates;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class P2PNetwork {
    private static final int PORT = 6666;

    private List<ConnectionHandler> clients = new ArrayList<>();
    private ConnectionHandler hostConnection;
    private ServerSocket serverSocket;
    private boolean isRunning = true;
    private Consumer<String> onMessageReceived;

    public void setOnMessageReceived(Consumer<String> listener) {
        this.onMessageReceived = listener;
    }

    // --- 1. HOST ---
    public void startHost() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                System.out.println("Host đang chờ tại port " + PORT);

                while (isRunning) {
                    Socket socket = serverSocket.accept();
                    // Host không cần gửi tên mình khi nhận khách, chỉ cần quản lý
                    ConnectionHandler client = new ConnectionHandler(socket, null);
                    clients.add(client);
                    new Thread(client).start();

                    broadcast("CMD:COUNT:" + (clients.size() + 1));
                }
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    // --- 2. GUEST (FIX LỖI KẸT MẠNG TẠI ĐÂY) ---
    public void connect(String ip, String myName) {
        new Thread(() -> {
            try {
                Socket socket = new Socket(ip, PORT);
                // Truyền tên vào Constructor để nó tự gửi khi luồng sẵn sàng
                hostConnection = new ConnectionHandler(socket, myName);
                new Thread(hostConnection).start();
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    public void send(String msg) {
        if (!clients.isEmpty()) broadcast(msg);
        else if (hostConnection != null) hostConnection.send(msg);
    }

    private void broadcast(String msg) {
        for (ConnectionHandler client : clients) {
            client.send(msg);
        }
    }

    public void close() {
        isRunning = false;
        try {
            if (serverSocket != null) serverSocket.close();
            for (ConnectionHandler client : clients) client.close();
            if (hostConnection != null) hostConnection.close();
        } catch (Exception e) {}
    }

    // --- CLASS CON ---
    private class ConnectionHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName = "Người lạ";
        private String nameToSend; // Biến tạm để giữ tên cần gửi

        // Constructor nhận thêm nameToSend
        public ConnectionHandler(Socket socket, String nameToSend) {
            this.socket = socket;
            this.nameToSend = nameToSend;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // --- FIX: GỬI TÊN TẠI ĐÂY (Khi luồng đã chạy) ---
                if (nameToSend != null) {
                    out.println("CMD:NAME:" + nameToSend);
                }

                if (onMessageReceived != null) onMessageReceived.accept("SYSTEM: Connected");

                String msg;
                while (isRunning && (msg = in.readLine()) != null) {

                    if (msg.startsWith("CMD:NAME:")) {
                        this.clientName = msg.split(":")[1];
                        if (!clients.isEmpty()) {
                            broadcast("CHAT:System:" + clientName + " đã tham gia phòng.");
                        }
                        continue;
                    }

                    if (!clients.isEmpty()) {
                        broadcast(msg);
                    }
                    if (onMessageReceived != null) onMessageReceived.accept(msg);
                }
            } catch (IOException e) {
                if (clients.contains(this)) {
                    clients.remove(this);
                    broadcast("CMD:COUNT:" + (clients.size() + 1));
                    broadcast("CHAT:System:" + clientName + " đã rời phòng.");
                }
            }
        }

        public void send(String msg) {
            if (out != null) out.println(msg);
        }

        public void close() {
            try { socket.close(); } catch (IOException e) {}
        }
    }
}