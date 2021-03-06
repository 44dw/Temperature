package com.a44dw.temperature.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.a44dw.temperature.entities.Note;

@Dao
public interface NoteDao {
    @Query("SELECT * FROM note WHERE tempId = :id")
    Note getById(long id);

    @Insert
    long insert(Note note);

    @Update
    void update(Note note);

    @Delete
    void delete(Note note);
}
