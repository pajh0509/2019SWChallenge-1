package com.example.visiblevoice.Activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.SharedPreferences;
import android.os.Bundle;

import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.room.Room;

import com.example.visiblevoice.Controller.MusicListController;
import com.example.visiblevoice.Data.AppDataInfo;
import com.example.visiblevoice.R;
import com.example.visiblevoice.db.AppDatabase;
import com.example.visiblevoice.db.RecordDAO;
import com.example.visiblevoice.models.Record;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class FileListActivity extends AppCompatActivity implements View.OnClickListener{
    private Intent intent;
    private ImageButton fileUploadBtn;
    private ListView musicListListView;

    private ArrayAdapter<String> listAdapter;
    private MusicListController musicListController=MusicListController.getInstance();
    private ArrayList<String> nameList;
    private SharedPreferences currentfile;
    private RecordDAO recordDAO;
    public static Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        fileUploadBtn = findViewById(R.id.fileUploadBtn);
        fileUploadBtn.setOnClickListener(this);
        musicListListView=findViewById(R.id.musicListListView);

        mContext = this;

        updateMusicList();

        Log.d("filepath", "저장완료");
        nameList=new ArrayList<String>();
        for(com.example.visiblevoice.Data.Record record:musicListController.musicList)
            nameList.add(record.file_name);

        listAdapter = new ArrayAdapter<String>(FileListActivity.this, android.R.layout.simple_list_item_1, nameList);
        musicListListView.setAdapter(listAdapter);
        recordDAO = Room.databaseBuilder(getApplicationContext(), AppDatabase.class,"db-record" )
                .allowMainThreadQueries()   //Allows room to do operation on main thread
                .build()
                .getRecordDAO();

        Record record = new Record();
        /*String jsonFileName = jsontextView.getText().toString();
        String fileName = jsonFileName.substring(0,jsonFileName.length()-5);*/


        /*
            현재 재생목록 저장.
         */
        musicListListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                musicListController.setCurrent(position);

                SharedPreferences.Editor setCurrentmusic = currentfile.edit();
                Log.d("file저장","파일이름 : "+musicListController.getCurrentFilename());
                Log.d("file저장","음성파일이름 : "+musicListController.getCurrentMusicPath());
                Log.d("file저장","json 파일이름 : "+musicListController.getCurrentJsonPath());
                Log.d("file저장","png 파일이름 : "+musicListController.getCurrentPngPath());
                try{
                    if(musicListController.getCurrentPngPath()!=null)
                        setCurrentmusic.putString(AppDataInfo.CurrentFile.png, musicListController.getCurrentPngPath());
                    if(musicListController.getCurrentJsonPath()!=null)
                        setCurrentmusic.putString(AppDataInfo.CurrentFile.json , musicListController.getCurrentJsonPath());

                    if(musicListController.getCurrentFilename()!=null)
                        setCurrentmusic.putString(AppDataInfo.CurrentFile.filename, musicListController.getCurrentFilename());

                    if(musicListController.getCurrentMusicPath()!=null)
                        setCurrentmusic.putString(AppDataInfo.CurrentFile.music, musicListController.getCurrentMusicPath());

                    setCurrentmusic.commit();

                }
                catch (NullPointerException ne){
                    ne.printStackTrace();
                }
                setCurrentmusic.commit();

                ((MainActivity)MainActivity.mContext).refreshMediaPlayer();
                finish();
            }
        });

        musicListListView.setLongClickable(true);

        /*
            목록 삭제 리스너
         */
        musicListListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){

            private String fileName;
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                Log.d("long",musicListController.getFilename(position)+"클릭");
                fileName = musicListController.getFilename(position);

                //musicListListView.setAdapter(listAdapter);
                AlertDialog.Builder alert = new AlertDialog.Builder(
                        FileListActivity.this);
                alert.setTitle("파일 삭제");
                alert.setMessage("선택하신 파일 "+fileName+"을 삭제하시겠 습니까?");
                alert.setPositiveButton("YES", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do your work here
                        Toast.makeText(getApplicationContext(), "OK Click", Toast.LENGTH_SHORT).show();
                        recordDAO.deleteRecord(fileName);
                        musicListController.removeRecord(position);
                        File jsonfile = new File(AppDataInfo.Path.VisibleVoiceFolder+"/"+fileName+".json");
                        File pngfile = new File(AppDataInfo.Path.VisibleVoiceFolder+"/"+fileName+".png");
                        if(jsonfile.exists())
                            jsonfile.delete();
                        if(pngfile.exists())
                            pngfile.delete();
                        //updateMusicList();

                        dialog.dismiss();
                        finish();
                    }
                });
                alert.setNegativeButton("NO", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                    }
                });

                alert.show();
                listAdapter.notifyDataSetChanged();

                return true;
            }
        });
    }

    public void updateMusicList(){


        currentfile = getApplicationContext().getSharedPreferences(AppDataInfo.CurrentFile.key,AppCompatActivity.MODE_PRIVATE);

        recordDAO = Room.databaseBuilder(getApplicationContext(), AppDatabase.class,"db-record" )
                .allowMainThreadQueries()   //Allows room to do operation on main thread
                .build()
                .getRecordDAO();
        //recordDAO.clearRecordTable();
        List<com.example.visiblevoice.models.Record> recordList= recordDAO.getRecords();
        musicListController = new MusicListController();


        for(com.example.visiblevoice.models.Record record_model : recordList){
            int check =1;
            for(com.example.visiblevoice.Data.Record record_data:musicListController.musicList){
                Log.d("filepath", "record_model.getFileName : "+record_model.getFileName());
                Log.d("filepath", "record_data : "+record_data.file_name);
                if(record_model.getFileName() == record_data.file_name){
                    File json_file;
                    File png_file;
                    File audio_file;
                    if(record_model.getWordCloudPath()==null){
                        png_file = null;
                    }
                    else{
                        png_file = new File(record_model.getWordCloudPath());
                    }
                    if(record_model.getJsonPath()==null){
                        json_file = null;
                    }
                    else{
                        json_file = new File(record_model.getJsonPath());
                    }

                    if(record_model.getAudioPath() ==null){
                        audio_file = null;
                    }
                    else{
                        audio_file = new File(record_model.getAudioPath());
                    }

                    record_data.setMusic_file(audio_file);
                    record_data.setJson_file(json_file);
                    record_data.setPng_file(png_file);
                    check =0;
                    break;
                }
            }
            if(check==1){
                File json_file;
                File png_file;
                File audio_file;
                if(record_model.getWordCloudPath()==null){
                    png_file = null;
                }
                else{
                    png_file = new File(record_model.getWordCloudPath());
                }
                if(record_model.getJsonPath()==null){
                    json_file = null;
                }
                else{
                    json_file = new File(record_model.getJsonPath());
                }

                if(record_model.getAudioPath() ==null){
                    audio_file = null;
                }
                else{
                    audio_file = new File(record_model.getAudioPath());
                }

                com.example.visiblevoice.Data.Record record = new com.example.visiblevoice.Data.Record(record_model.getFileName(),audio_file,json_file,png_file);
                Log.d("filepath", "외않되 : "+record.getPng_file());
                //record.setPng_file(png_file);
                musicListController.addRecord(record);

            }
        }


    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.fileUploadBtn :
                intent = new Intent(FileListActivity.this, FileUploadActivity.class);
                startActivity(intent);
                finish();
                break;
        }
    }

}
