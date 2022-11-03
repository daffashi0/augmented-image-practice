package com.example.augmentedimage_java;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.augmentedimage_java.AppDatabase;
import com.example.augmentedimage_java.daos.ImageDao;
import com.example.augmentedimage_java.models.Image;

import java.util.List;

public class ImageRepository {
    private ImageDao mImageDao;
    private LiveData<List<Image>> mAllImages;

    // Note that in order to unit test the WordRepository, you have to remove the Application
    // dependency. This adds complexity and much more code, and this sample is not about testing.
    // See the BasicSample in the android-architecture-components repository at
    // https://github.com/googlesamples
    ImageRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        mImageDao = db.imageDao();
        mAllImages = mImageDao.getAll();
    }

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    LiveData<List<Image>> getAllWords() {
        return mAllImages;
    }

    // You must call this on a non-UI thread or your app will throw an exception. Room ensures
    // that you're not doing any long running operations on the main thread, blocking the UI.
    void insert(Image image) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mImageDao.insertAll(image);
        });
    }
}
