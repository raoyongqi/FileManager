package com.example.easytutofilemanager;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Looper;
import android.os.Handler;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder>{

    Context context;
    File[] filesAndFolders;

    public MyAdapter(Context context, File[] filesAndFolders){
        this.context = context;
        this.filesAndFolders = filesAndFolders;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.recycler_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyAdapter.ViewHolder holder, int position) {

        File selectedFile = filesAndFolders[position];
        holder.textView.setText(selectedFile.getName());

        if(selectedFile.isDirectory()){
            holder.imageView.setImageResource(R.drawable.ic_baseline_folder_24);
        }else{
            holder.imageView.setImageResource(R.drawable.ic_baseline_insert_drive_file_24);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedFile.isDirectory()) {
                    // 如果是目录，跳转到文件列表页面
                    Intent intent = new Intent(context, FileListActivity.class);
                    String path = selectedFile.getAbsolutePath();
                    intent.putExtra("path", path);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } else {
                    // 如果是文件
                    try {
                        if (selectedFile.exists() && selectedFile.isFile()) {
                            // 获取文件名并判断扩展名
                            String fileName = selectedFile.getName().toLowerCase();
                            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") || fileName.endsWith(".bmp")) {
                                // 如果是图片文件，跳转到图片查看页面
                                Intent intent = new Intent(context, ImageViewerActivity.class);
                                intent.putExtra("image_path", selectedFile.getAbsolutePath());
                                if (!(context instanceof Activity)) {
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                }
                                context.startActivity(intent);
                            } else if (fileName.endsWith(".mp3")) {
                                // 如果是音频文件，首先检查文件是否已经存在
                                String fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));
                                String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1);
                                String destinationFile = Environment.getExternalStorageDirectory().getPath() + "/" + fileNameWithoutExtension + "." + fileExtension;

                                File destFile = new File(destinationFile);

                                if (destFile.exists()) {
                                    // 文件已存在，直接播放
                                    playAudio(destinationFile);
                                } else {
                                    // 文件不存在，执行复制并播放
                                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                                    executorService.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                // 执行文件复制操作
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    Files.copy(selectedFile.toPath(), Paths.get(destinationFile), StandardCopyOption.REPLACE_EXISTING);
                                                }

                                                // 文件复制完成后，切回主线程播放音频
                                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        playAudio(destinationFile);
                                                    }
                                                });
                                            } catch (Exception e) {
                                                // 复制失败，处理错误
                                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(context, "无法复制音频！", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                                e.printStackTrace();
                                            } finally {
                                                // 确保线程池在任务完成后关闭
                                                executorService.shutdown();
                                            }
                                        }
                                    });
                                }
                            } else {
                                Toast.makeText(context, "文件格式不支持！", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // 如果文件不存在或无效
                            Toast.makeText(context, "文件不存在或无效！", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("FileSelection", "Error opening file: " + selectedFile.getAbsolutePath(), e);
                        Toast.makeText(context.getApplicationContext(), "无法打开文件", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            // 播放音频的辅助方法
            private void playAudio(String destinationFile) {
                try {
                    MediaPlayer mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(destinationFile);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    Toast.makeText(context, "音频文件播放成功！", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(context, "播放音频失败！", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });




        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                PopupMenu popupMenu = new PopupMenu(context,v);
                popupMenu.getMenu().add("DELETE");
                popupMenu.getMenu().add("MOVE");
                popupMenu.getMenu().add("RENAME");

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if(item.getTitle().equals("DELETE")){
                            boolean deleted = selectedFile.delete();
                            if(deleted){
                                Toast.makeText(context.getApplicationContext(),"DELETED ",Toast.LENGTH_SHORT).show();
                                v.setVisibility(View.GONE);
                            }
                        }
                        if(item.getTitle().equals("MOVE")){
                            Toast.makeText(context.getApplicationContext(),"MOVED ",Toast.LENGTH_SHORT).show();

                        }
                        if(item.getTitle().equals("RENAME")){
                            Toast.makeText(context.getApplicationContext(),"RENAME ",Toast.LENGTH_SHORT).show();

                        }
                        return true;
                    }
                });

                popupMenu.show();
                return true;
            }
        });


    }

    @Override
    public int getItemCount() {
        return filesAndFolders.length;
    }


    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView textView;
        ImageView imageView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.file_name_text_view);
            imageView = itemView.findViewById(R.id.icon_view);
        }
    }
}
