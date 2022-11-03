package com.example.augmentedimage_java.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.augmentedimage_java.models.Image;

import java.util.List;

@Dao
public interface ImageDao{
    @Query("SELECT * FROM image")
    LiveData<List<Image>> getAll();

    @Query("SELECT * FROM image WHERE id IN (:imageIds)")
    LiveData<List<Image>> loadAllByIds(int[] imageIds);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(Image... Images);

    @Delete
    void delete(Image Image);

    @Query("DELETE FROM image")
    void deleteAll();
}
