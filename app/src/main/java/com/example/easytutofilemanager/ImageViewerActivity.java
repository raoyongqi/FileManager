package com.example.easytutofilemanager;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ImageViewerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        ImageView imageView = findViewById(R.id.imageView);

        // 获取传递过来的文件路径
        Intent intent = getIntent();
        String filePath = intent.getStringExtra("image_path");

        if (filePath != null) {
            File selectedFile = new File(filePath);
            if (selectedFile.exists() && selectedFile.isFile()) {
                try (FileInputStream fis = new FileInputStream(selectedFile)) {
                    Bitmap bitmap = BitmapFactory.decodeStream(fis);
                    imageView.setImageBitmap(bitmap); // 设置图片到 ImageView
                } catch (IOException e) {
                    e.printStackTrace(); // 处理异常
                }
            }
        }
    }
}
