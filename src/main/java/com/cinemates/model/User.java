package com.cinemates.model;

public class User {
    private int id;
    private String username;
    private String email;
    private String password;
    private boolean isOnline;

    // 1. Constructor mặc định
    public User() {}

    // 2. Constructor dùng cho danh sách bạn bè/người lạ (Rút gọn)
    public User(int id, String username, boolean isOnline) {
        this.id = id;
        this.username = username;
        this.isOnline = isOnline;
    }

    // 3. Constructor đầy đủ
    public User(int id, String username, String email, boolean isOnline) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.isOnline = isOnline;
    }

    // --- GETTERS & SETTERS (Để DatabaseHandler và Controller lấy dữ liệu) ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}