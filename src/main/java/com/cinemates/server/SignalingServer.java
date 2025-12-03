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

    // Map lưu phòng
    private static final Map<String, String> activeRooms = new HashMap<>();

    public static void main(String[] args) {
        // IN CÁI HEADER BẢNG BÁO CÁO CHO NÓ NGẦU
        System.out.println("\n>>> HỆ THỐNG GIÁM SÁT KẾT NỐI P2P (SERVER MONITOR) <<<");
        System.out.println("==================================================================================");
        System.out.println(String.format("| %-15s | %-18s | %-18s | %-15s |", "USERNAME", "PUBLIC IP (WAN)", "LOCAL IP (LAN)", "STATUS"));
        System.out.println("==================================================================================");

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

                // Lấy IP thật (Do Pinggy forward tới thì thường là 127.0.0.1 hoặc IP gateway)
                // Nhưng mình sẽ dùng cái IP giả lập từ Client gửi lên để hiển thị
                String realIp = socket.getInetAddress().getHostAddress();

                String request;
                while ((request = in.readLine()) != null) {
                    String[] parts = request.split(" ");
                    String command = parts[0];

                    // --- XỬ LÝ LỆNH BÁO CÁO (REPORT) ---
                    if (command.equals("REPORT")) {
                        // Cấu trúc tin nhắn: REPORT <Tên> <IP_Giả_Lập>
                        String username = (parts.length > 1) ? parts[1] : "Unknown";
                        String simulatedIp = (parts.length > 2) ? parts[2] : "Unknown";

                        // In ra bảng theo định dạng cột cho đẹp
                        System.out.println(String.format("| %-15s | %-18s | %-18s | %-15s |",
                                username, realIp, simulatedIp, "✅ ONLINE"));

                        System.out.println("----------------------------------------------------------------------------------");
                    }

                    // --- CÁC LỆNH CŨ (GIỮ NGUYÊN) ---
                    else if (command.equals("CREATE")) {
                        String roomId = String.valueOf(1000 + new Random().nextInt(9000));
                        activeRooms.put(roomId, "127.0.0.1"); // Demo local thì cứ trả về localhost là đc
                        out.println("CREATED " + roomId);
                    }
                    else if (command.equals("JOIN")) {
                        String roomId = parts[1];
                        if (activeRooms.containsKey(roomId)) {
                            out.println("FOUND " + activeRooms.get(roomId));
                        } else {
                            out.println("NOT_FOUND");
                        }
                    }
                }
            } catch (Exception e) { }
        }
    }
}