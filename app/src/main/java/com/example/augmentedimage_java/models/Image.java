package com.example.augmentedimage_java.models;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "image")
public class Image {
    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "url")
    private String url;
    @ColumnInfo(name = "isVideo", defaultValue = "false")
    private boolean isVideo;

    public Image(String name, String url, @Nullable Boolean isVideo) {
        this.name = name;
        this.url = url;
        this.isVideo = isVideo;
    }

    @Ignore
    public Image(int id, String name, String url, @Nullable Boolean isVideo) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.isVideo = isVideo;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public String getUrl() {
        return url;
    }
    public boolean isVideo() {
        return isVideo;
    }
}
