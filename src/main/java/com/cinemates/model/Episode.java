package com.cinemates.model;

public class Episode {
    private int id;
    private int movieId;
    private int episodeNo;
    private String videoId; // ID Google Drive
    private String title;

    public Episode(int id, int movieId, int episodeNo, String videoId, String title) {
        this.id = id;
        this.movieId = movieId;
        this.episodeNo = episodeNo;
        this.videoId = videoId;
        this.title = title;
    }

    public String getVideoId() { return videoId; }
    public String getTitle() { return title; }
    public int getEpisodeNo() { return episodeNo; }
}