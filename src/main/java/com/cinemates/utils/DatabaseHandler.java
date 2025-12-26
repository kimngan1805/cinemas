package com.cinemates.utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.cinemates.model.Movie;
import com.cinemates.model.Episode;
import com.cinemates.model.User;

public class DatabaseHandler {

    private static final String DB_URL = "jdbc:mysql://localhost:8889/cinemates_db";
    private static final String USER = "root";
    private static final String PASS = "root";

    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            System.out.println("✅ Kết nối Database thành công!");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }

    // --- LOGIC ĐĂNG NHẬP & TRẠNG THÁI ---

    public static int checkLogin(String username, String password) {
        String sql = "SELECT id FROM users WHERE username = ? AND password = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int userId = rs.getInt("id");
                updateOnlineStatus(userId, true);
                return userId;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public static void updateOnlineStatus(int userId, boolean status) {
        String sql = "UPDATE users SET is_online = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, status);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- LOGIC KẾT BẠN (FRIENDSHIP) ---

    // 1. SỬA TẠI ĐÂY: Lấy trạng thái online thật của bạn bè
    public static List<User> getFriendsList(int userId) {
        List<User> friends = new ArrayList<>();
        // Đã thêm u.is_online vào câu lệnh SELECT
        String sql = "SELECT u.id, u.username, u.is_online FROM users u " +
                "JOIN friendships f ON (u.id = f.sender_id OR u.id = f.receiver_id) " +
                "WHERE (f.sender_id = ? OR f.receiver_id = ?) " +
                "AND f.status = 'ACCEPTED' AND u.id != ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                // SỬA: Thay 'true' bằng rs.getBoolean("is_online")
                friends.add(new User(rs.getInt("id"), rs.getString("username"), rs.getBoolean("is_online")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return friends;
    }

    // 2. SỬA TẠI ĐÂY: Lấy trạng thái online của người lạ
    public static List<User> getOnlineStrangers(int userId) {
        List<User> strangers = new ArrayList<>();
        // Đã thêm is_online vào câu lệnh SELECT
        String sql = "SELECT id, username, is_online FROM users WHERE is_online = TRUE AND id != ? " +
                "AND id NOT IN (SELECT sender_id FROM friendships WHERE receiver_id = ?) " +
                "AND id NOT IN (SELECT receiver_id FROM friendships WHERE sender_id = ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                // SỬA: Lấy đúng trạng thái từ DB
                strangers.add(new User(rs.getInt("id"), rs.getString("username"), rs.getBoolean("is_online")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return strangers;
    }

    public static boolean sendFriendRequest(int senderId, int receiverId) {
        String sql = "INSERT INTO friendships (sender_id, receiver_id, status) VALUES (?, ?, 'PENDING')";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, senderId);
            pstmt.setInt(2, receiverId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public static boolean handleFriendRequest(int senderId, int receiverId, String status) {
        String sql = "UPDATE friendships SET status = ? WHERE sender_id = ? AND receiver_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, senderId);
            pstmt.setInt(3, receiverId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    // --- CÁC HÀM CŨ (GIỮ NGUYÊN) ---
    public static List<Movie> getRecentlyAddedMovies() {
        List<Movie> list = new ArrayList<>();
        String sql = "SELECT id, title, poster_url FROM movies ORDER BY id DESC LIMIT 10";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) { list.add(new Movie(rs.getInt("id"), rs.getString("title"), rs.getString("poster_url"))); }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public static Movie getMovieById(int movieId) {
        Movie movie = null;
        String sql = "SELECT * FROM movies WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, movieId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) { movie = new Movie(rs.getInt("id"), rs.getString("title"), rs.getString("poster_url"), rs.getString("description"), rs.getString("genre"), rs.getString("nation"), rs.getString("duration")); }
        } catch (Exception e) { e.printStackTrace(); }
        return movie;
    }

    public static Episode getFirstEpisode(int movieId) {
        Episode episode = null;
        String sql = "SELECT * FROM episodes WHERE movie_id = ? ORDER BY episode_no ASC LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, movieId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) { episode = new Episode(rs.getInt("id"), rs.getInt("movie_id"), rs.getInt("episode_no"), rs.getString("video_id"), rs.getString("title")); }
        } catch (Exception e) { e.printStackTrace(); }
        return episode;
    }

    // 3. SỬA TẠI ĐÂY: Lấy trạng thái online của người gửi yêu cầu kết bạn
    public static List<User> getPendingRequests(int receiverId) {
        List<User> requests = new ArrayList<>();
        // Đã thêm u.is_online vào câu lệnh SELECT
        String sql = "SELECT u.id, u.username, u.is_online FROM users u " +
                "JOIN friendships f ON u.id = f.sender_id " +
                "WHERE f.receiver_id = ? AND f.status = 'PENDING'";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, receiverId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                // SỬA: Lấy đúng trạng thái từ DB
                requests.add(new User(rs.getInt("id"), rs.getString("username"), rs.getBoolean("is_online")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return requests;
    }

    public static boolean registerUser(String username, String email, String password) {
        String sql = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username); pstmt.setString(2, email); pstmt.setString(3, password);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static void resetAllUsersToOffline() {
        String sql = "UPDATE users SET is_online = FALSE";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("🧹 Đã dọn dẹp: Tất cả người dùng về Offline.");
        } catch (SQLException e) { e.printStackTrace(); }
    }
}