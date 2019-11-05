package com.example.visiblevoice.models;


import androidx.annotation.NonNull;
import androidx.room.*;

//table
@Entity(tableName = "record")
public class Record {
    @PrimaryKey
    @NonNull
    public String fileName;
    private String audioPath;
    private String wordCloudPath;
    private String jsonPath;
    @NonNull
    private String username;

    @NonNull
    public String getFileName() {  return fileName;  }

    public void setFileName(@NonNull String fileName) {  this.fileName = fileName;  }

    public String getAudioPath() {  return audioPath; }

    public void setAudioPath(String audioPath) {   this.audioPath = audioPath;  }

    public String getWordCloudPath() {   return wordCloudPath;  }

    public void setWordCloudPath(String wordCloudPath) {  this.wordCloudPath = wordCloudPath; }

    public String getJsonPath() { return jsonPath; }

    public void setJsonPath(String jsonPath) {this.jsonPath = jsonPath; }

    @NonNull
    public String getUsername() { return username; }

    public void setUsername(@NonNull String username) {this.username = username; }

}
