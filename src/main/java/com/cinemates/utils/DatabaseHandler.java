package com.cinemates.utils; // Nhớ dòng package này phải đúng nha

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import com.cinemates.model.Movie;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import com.cinemates.model.Episode;
public class DatabaseHandler {

    // Cấu hình cho MAMP (Port mặc định thường là 8889)
    // Nếu Ngân dùng XAMPP thì đổi 8889 thành 3306
    private static final String DB_URL = "jdbc:mysql://localhost:8889/cinemates_db";
    private static final String USER = "root";
    private static final String PASS = "root"; // Mật khẩu mặc định MAMP là root

    // Hàm lấy kết nối
    public static Connection getConnection() {
        Connection conn = null;
        try {
            // Load Driver (Bắt buộc với mấy bản Java mới)
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Mở kết nối
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            System.out.println("✅ Kết nối Database thành công!");

        } catch (ClassNotFoundException e) {
            System.out.println("❌ Lỗi: Không tìm thấy thư viện MySQL JDBC Driver.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: Không thể kết nối đến MySQL.");
            System.out.println("👉 Kiểm tra lại: MAMP đã bật chưa? Tên DB đúng chưa? Port 8889 hay 3306?");
            e.printStackTrace();
        }
        return conn;
    }
    public static List<Movie> getRecentlyAddedMovies() {
        List<Movie> list = new ArrayList<>();
        // Lấy id, title, poster_url của những phim mới nhất
        String sql = "SELECT id, title, poster_url FROM movies ORDER BY id DESC LIMIT 10";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Movie(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("poster_url")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
    public static Movie getMovieById(int movieId) {
        Movie movie = null;
        String sql = "SELECT * FROM movies WHERE id = ?";

        try (Connection conn = getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, movieId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                movie = new Movie(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("poster_url"),
                        rs.getString("description"),
                        rs.getString("genre"),
                        rs.getString("nation"),
                        rs.getString("duration")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return movie;
    }
    public static Episode getFirstEpisode(int movieId) {
        Episode episode = null;
        // Lấy tập có episode_no = 1 hoặc tập đầu tiên tìm thấy
        String sql = "SELECT * FROM episodes WHERE movie_id = ? ORDER BY episode_no ASC LIMIT 1";

        try (Connection conn = getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, movieId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                episode = new Episode(
                        rs.getInt("id"),
                        rs.getInt("movie_id"),
                        rs.getInt("episode_no"),
                        rs.getString("video_id"),
                        rs.getString("title")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return episode;
    }
    // Thêm hàm này vào class DatabaseHandler của Ngân
    public static boolean registerUser(String username, String email, String password) {
        String sql = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, password); // Ngân nhớ tạo bảng users trong DB trước nha

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean checkLogin(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            java.sql.ResultSet rs = pstmt.executeQuery();

            return rs.next(); // Nếu có kết quả trả về là đúng
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    // HÀM MAIN ĐỂ TEST NHANH (Chạy riêng file này thôi)
    public static void main(String[] args) {
        getConnection();
    }
}