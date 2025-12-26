package com.cinemates.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CentralDiscoveryServer {
    private static final int PORT = 6789;
    private static Map<String, String> onlineClients = new ConcurrentHashMap<>();
    private static Map<String, ObjectOutputStream> clientOutputs = new ConcurrentHashMap<>();
    private static Map<String, RoomInfo> roomRegistry = new ConcurrentHashMap<>();

    // Class lưu thông tin phòng
    static class RoomInfo {
        String hostUsername;
        String hostIP;
        String movieName;
        boolean isPublic;
        int currentUsers;

        RoomInfo(String host, String ip, String movie, boolean pub) {
            this.hostUsername = host;
            this.hostIP = ip;
            this.movieName = movie;
            this.isPublic = pub;
            this.currentUsers = 1;
        }
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            String serverIp = InetAddress.getLocalHost().getHostAddress();
            System.out.println("===========================================");
            System.out.println("🚀 CINEMATES CENTRAL SERVER - ONLINE");
            System.out.println("📍 IP: " + serverIp + " | Port: " + PORT);
            System.out.println("===========================================");

            // Thread broadcast Public Rooms mỗi 5s
            new Timer().schedule(new TimerTask() {
                public void run() {
                    broadcastPublicRooms();
                }
            }, 0, 5000);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("❌ Lỗi Server: " + e.getMessage());
        }
    }

    private static void broadcastPublicRooms() {
        StringBuilder list = new StringBuilder("PUBLIC_ROOMS|");

        for (Map.Entry<String, RoomInfo> entry : roomRegistry.entrySet()) {
            RoomInfo room = entry.getValue();
            if (room.isPublic) {
                list.append(entry.getKey()).append(":")
                        .append(room.movieName).append(":")
                        .append(room.currentUsers).append("/10:")
                        .append(room.hostIP).append(",");
            }
        }

        String message = list.toString();
        for (ObjectOutputStream out : clientOutputs.values()) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                // Client offline
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private String currentUsername;
        private ObjectOutputStream out;
        private ObjectInputStream in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                Object data = in.readObject();
                if (data != null) {
                    String[] info = data.toString().split("\\|");
                    this.currentUsername = info[0].trim();
                    String clientInfo = info[1] + ":" + info[2];

                    onlineClients.put(currentUsername, clientInfo);
                    clientOutputs.put(currentUsername, out);

                    System.out.println("\n[MỚI ONLINE] " + currentUsername + " @ " + clientInfo);
                }

                while (true) {
                    Object msg = in.readObject();
                    if (msg != null) handleCommand(msg.toString());
                }
            } catch (Exception e) {
                // Client ngắt kết nối
            } finally {
                cleanUp();
            }
        }

        private void handleCommand(String cmd) {
            String[] parts = cmd.split("\\|");

            if (cmd.startsWith("REGISTER_ROOM|")) {
                String roomCode = parts[1].trim();
                String hostUsername = parts[2].trim();
                String movieName = parts.length > 3 ? parts[3].trim() : "Unknown";
                boolean isPublic = parts.length > 4 && parts[4].equals("true");

                // ⭐ LẤY IP TỪ onlineClients
                String hostIPWithPort = onlineClients.get(hostUsername);

                if (hostIPWithPort != null) {
                    // Tách IP ra, bỏ port
                    String hostIP = hostIPWithPort.split(":")[0];

                    RoomInfo room = new RoomInfo(hostUsername, hostIP, movieName, isPublic);
                    roomRegistry.put(roomCode, room);

                    System.out.println("🏠 Phòng " + roomCode + " (" + (isPublic ? "PUBLIC" : "PRIVATE") +
                            ") - Host: " + hostUsername + " @ " + hostIP);
                } else {
                    System.err.println("⚠️ Không tìm thấy IP của host: " + hostUsername);
                }
            }

            else if (cmd.startsWith("FIND_ROOM|")) {
                String roomCode = parts[1].trim();
                String guestName = parts[2].trim();
                RoomInfo room = roomRegistry.get(roomCode);

                if (room != null && onlineClients.containsKey(room.hostUsername)) {
                    relayMessage(guestName, "ROOM_FOUND|" + roomCode + "|" + room.hostIP);
                    room.currentUsers++;
                    System.out.println("✅ " + guestName + " vào phòng " + roomCode);
                } else {
                    relayMessage(guestName, "ROOM_NOT_FOUND|" + roomCode);
                }
            }

            else if (cmd.startsWith("CLOSE_ROOM|")) {
                String roomCode = parts[1].trim();
                roomRegistry.remove(roomCode);
                System.out.println("🧹 Phòng " + roomCode + " đã đóng");
            }

            else if (cmd.startsWith("INVITE|")) {
                String targetUser = parts[2].trim();
                relayMessage(targetUser, cmd);
            }

            else if (cmd.equals("REQUEST_PUBLIC_ROOMS")) {
                broadcastPublicRooms();
            }
        }

        private void relayMessage(String targetUser, String message) {
            ObjectOutputStream targetOut = clientOutputs.get(targetUser);
            if (targetOut != null) {
                try {
                    targetOut.writeObject(message);
                    targetOut.flush();
                } catch (IOException e) {
                    System.err.println("⚠️ Lỗi gửi tin cho " + targetUser);
                }
            }
        }

        private void cleanUp() {
            if (currentUsername != null) {
                onlineClients.remove(currentUsername);
                clientOutputs.remove(currentUsername);
                roomRegistry.entrySet().removeIf(entry -> entry.getValue().hostUsername.equals(currentUsername));
                System.err.println("❌ " + currentUsername + " đã offline");
            }
            try { socket.close(); } catch (IOException e) { }
        }
    }
}