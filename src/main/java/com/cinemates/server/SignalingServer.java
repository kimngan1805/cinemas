package com.cinemates.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SignalingServer {
    private static final int PORT = 5000;

    // Map: Mã phòng -> Thông tin Host (IP Public, IP Local)
    private static final Map<String, HostInfo> activeRooms = new HashMap<>();

    // Class lưu thông tin Host
    static class HostInfo {
        String publicIp;
        String localIp; // Cái này để mở rộng sau này nếu cần

        public HostInfo(String publicIp) {
            this.publicIp = publicIp;
        }
    }

    public static void main(String[] args) {
        System.out.println(">>> SERVER ĐÁM MÂY (GIẢ LẬP) ĐANG CHẠY PORT " + PORT + " <<<");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new Thread(new ClientHandler(serverSocket.accept())).start();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        public ClientHandler(Socket socket) { this.socket = socket; }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // Lấy IP Public của người đang kết nối tới Server
                String clientIp = socket.getInetAddress().getHostAddress();
                System.out.println("📩 Tin nhắn từ: " + clientIp);

                String request;
                while ((request = in.readLine()) != null) {
                    String[] parts = request.split(" ");
                    String command = parts[0];

                    if (command.equals("CREATE")) {
                        String roomId = String.valueOf(1000 + new Random().nextInt(9000));

                        // Lưu IP của Host lại
                        activeRooms.put(roomId, new HostInfo(clientIp));

                        out.println("CREATED " + roomId);
                        System.out.println("✅ Phòng " + roomId + " tạo bởi " + clientIp);
                    }
                    else if (command.equals("JOIN")) {
                        if (parts.length < 2) { out.println("ERROR"); continue; }
                        String roomId = parts[1];

                        if (activeRooms.containsKey(roomId)) {
                            HostInfo host = activeRooms.get(roomId);

                            // --- LOGIC THÔNG MINH Ở ĐÂY ---
                            // So sánh IP của người xin vào (Guest) và IP chủ phòng (Host)
                            String targetIp;

                            if (host.publicIp.equals(clientIp)) {
                                // Nếu IP giống hệt nhau -> Tức là đang test trên cùng 1 máy hoặc cùng Wifi
                                System.out.println("⚠️ Phát hiện cùng mạng/máy. Trả về localhost.");
                                targetIp = "127.0.0.1";
                            } else {
                                // Nếu khác IP -> Trả về IP Public để kết nối qua Internet
                                targetIp = host.publicIp;
                            }

                            out.println("FOUND " + targetIp);
                            System.out.println("🔗 Chỉ đường cho Guest tới: " + targetIp);
                        } else {
                            out.println("NOT_FOUND");
                        }
                    }
                }
            } catch (Exception e) { }
        }
    }
}