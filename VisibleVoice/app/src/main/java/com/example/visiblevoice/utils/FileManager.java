package com.example.visiblevoice.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.example.visiblevoice.Client.HttpConnection;
import com.example.visiblevoice.Client.SFTPClient;
import com.example.visiblevoice.Client.ServerInfo;
import com.example.visiblevoice.Data.AppDataInfo;
import com.example.visiblevoice.models.Record;
import com.example.visiblevoice.db.AppDatabase;
import com.example.visiblevoice.db.RecordDAO;
import com.jcraft.jsch.IO;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class FileManager {
    private RecordDAO recordDAO;
    private SFTPClient sftpClient;
    private SharedPreferences userData;
    private SharedPreferences fileData;
    private final int BUFFER_SIZE = 1024;
    private byte[] buffer = new byte[BUFFER_SIZE];
    private String appRootPath;

    public FileManager(Context context, SharedPreferences userData, SharedPreferences fileData, String appRootPath) {
        recordDAO = Room.databaseBuilder(context, AppDatabase.class,"db-record" )
                .allowMainThreadQueries()   //Allows room to do operation on main thread
                .build()
                .getRecordDAO();
        sftpClient = new SFTPClient();
        this.userData = userData;
        this.fileData = fileData;
        this.appRootPath = appRootPath;
    }

    /*
        파일을 서버에 업로드하는 코드
        음성파일 변환을 요청하기 위해 업로드를 한다.
        근데 업로드를 하면 나중에 FCM 알람이 뜨기 때문에 SharedPreference에다 현재 파일 저장하지 않아도 된다??
     */

    public void connect() {
        sftpClient.init(ServerInfo.host,ServerInfo.username,ServerInfo.port,AppDataInfo.Path.VisibleVoiceFolder+"/"+ServerInfo.privatekey);
    }

    public boolean fileUpload(String filename) {
        String username = userData.getString(AppDataInfo.Login.userID, null);
        File file = new File(filename);

        String fname = file.getName().split("\\.")[0];

        if(!recordDAO.findRecordFromFilenameAndUsername(filename, username).isEmpty()) {
            Log.d("upload", "이미 존재하는 파일");
            return false;
        }

        connect();

        sftpClient.mkdir(ServerInfo.folderPath,username); // /home/vvuser
        //Log.d()
        sftpClient.upload(username,file);

        try {
            saveFile(new FileInputStream(file), file.getName());
        } catch (IOException ie) {
            ie.printStackTrace();
        }

        try {
            InputStream in = new FileInputStream(file);
            saveFile(in, filename);
            Record record = new Record();
            record.setFileName(fname);
            record.setUsername(username);
            recordDAO.insert(record);
        } catch (IOException ie) {
            ie.printStackTrace();
        }

        sftpClient.disconnection();
        return true;
    }

    public void fileDownloads() {
        connect();

        boolean[] check = new boolean[2];

        String pngFilename = fileData.getString(AppDataInfo.File.png, null);
        String jsonFilename = fileData.getString(AppDataInfo.File.json, null);
        String filename = jsonFilename.substring(0, jsonFilename.length()-5);
        String musicFilename = filename;

        check[0] = fileDownload(pngFilename);
        check[1] = fileDownload(jsonFilename);

        Record record = new Record();
        record.setAudioPath(musicFilename);
        record.setFileName(filename);
        record.setJsonPath(jsonFilename);
        record.setWordCloudPath(pngFilename);

        if(check[1]) recordDAO.updateWCPath(filename, pngFilename);
        if(check[2]) recordDAO.updateJSONPath(filename, jsonFilename);

        sftpClient.disconnection();
    }

    public boolean fileDownload(String filename) {
        File f = new File(AppDataInfo.Path.VisibleVoiceFolder + "/" + filename);
        if(f.exists()) return false;

        String username = userData.getString(AppDataInfo.Login.userID, null);
        ArrayList<Byte> byteArray = sftpClient.download(ServerInfo.folderPath+"/"+username,filename,AppDataInfo.Path.VisibleVoiceFolder);
        byte[] bytes = new byte[byteArray.size()];
        for(int i = 0; i < bytes.length; i++)
            bytes[i] = byteArray.get(i);

        saveFile(new ByteArrayInputStream(bytes), filename);

        return true;
    }

    public void saveFile(InputStream in, String filename) {
        File newFile = new File(appRootPath + "/" + userData.getString(AppDataInfo.Login.userID, null) + "/" + filename);
        if(!newFile.exists()) {
            try {
                newFile.createNewFile();
            } catch (IOException ie) {
                ie.printStackTrace();
            }
        }

        try {
            FileOutputStream os = new FileOutputStream(newFile);
            Log.d("download log","os : "+os);
            int readCount;
            while ((readCount = in.read(buffer)) > 0) {
                Log.d("download log","readCount : "+readCount);
                os.write(buffer, 0, readCount);
            }
            in.close();
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    TODO: 파일 업로드  다운로드는 AsyncTask로 구현
     */
}
