package com.example.visiblevoice.Data;

import android.os.Environment;

public class AppDataInfo {
    public  static final class File {
        public static final String key = "downloadfile";
        public static final String json = "json";
        public static final String png = "png";
    }
    public static final class CurrentFile{
        public static final String key = "currentfile";
        public static final String filename = "filename";
        public static final String music = "musicfile";
        public static final String json = "jsonfile";
        public static final String png = "pngfile";
    }

    public  static final class Path{
        public static final String VisibleVoiceFolder = Environment.getExternalStorageDirectory().getAbsolutePath()+ "/"+"VisibleVoice";
    }
    public static final class Login{
        public static final String key = "logininfo";
        public static final String checkbox = "checkbox";
        public static final String userID = "userID";
        public static final String userPwd = "userPwd";
    }

}