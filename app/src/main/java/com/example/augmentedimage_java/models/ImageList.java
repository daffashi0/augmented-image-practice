package com.example.augmentedimage_java.models;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Room;

import com.example.augmentedimage_java.daos.ImageDao;
import com.example.augmentedimage_java.models.Image;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ImageList {
    private String name;
    private String url;
    private boolean isVideo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isVideo() {
        return isVideo;
    }

    public void setVideo(boolean video) {
        isVideo = video;
    }

    @Override
    public String toString() {
        return "ImageList{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", isVideo=" + isVideo +
                '}';
    }
}
