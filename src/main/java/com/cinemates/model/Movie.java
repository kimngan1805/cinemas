package com.cinemates.model;

public class Movie {
    private int id;
    private String title;
    private String posterUrl;
    private String description; // Mới thêm
    private String genre;       // Mới thêm
    private String nation;      // Mới thêm
    private String duration;    // Mới thêm

    // Constructor đầy đủ
    public Movie(int id, String title, String posterUrl, String description, String genre, String nation, String duration) {
        this.id = id;
        this.title = title;
        this.posterUrl = posterUrl;
        this.description = description;
        this.genre = genre;
        this.nation = nation;
        this.duration = duration;
    }

    // Constructor rút gọn (dùng cho Gallery - chỉ cần ID, Tên, Ảnh)
    public Movie(int id, String title, String posterUrl) {
        this.id = id;
        this.title = title;
        this.posterUrl = posterUrl;
    }

    // Getter
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getPosterUrl() { return posterUrl; }
    public String getDescription() { return description; }
    public String getGenre() { return genre; }
    public String getNation() { return nation; }
    public String getDuration() { return duration; }
}