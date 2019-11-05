package com.example.visiblevoice.db;

import com.example.visiblevoice.models.Record;
import java.util.List;
import androidx.room.*;

//query
@Dao
public interface RecordDAO {
    @Insert
    public void insert(Record... records);
    @Update
    public void update(Record... records);
    @Delete
    public void delete(Record... record);
    @Query("SELECT * FROM record")
    public List<Record> getRecords();
    @Query("DELETE FROM record")
    public void clearRecordTable();
    @Query("DELETE FROM record WHERE fileName=:fileName")
    public void deleteRecord(String fileName);
    @Query("UPDATE record SET wordCloudPath=:wordCloudPath WHERE fileName=:fileName")
    public void updateWCPath(String fileName,String wordCloudPath);
    @Query("UPDATE record SET jsonPath=:jsonPath WHERE fileName=:fileName")
    public void updateJSONPath(String fileName,String jsonPath);
    @Query("SELECT * FROM record WHERE filename=:filename AND username=:username")
    public List<Record> findRecordFromFilenameAndUsername(String filename, String username);
}
