package com.a44dw.temperature;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface PersonSymptomDao {

    @Query("SELECT * FROM PersonSymptom")
    List<PersonSymptom> getAll();

    @Query("SELECT * FROM PersonSymptom WHERE temperatureId=:id AND symptomId=(SELECT symptId FROM Symptom WHERE name=:name)")
    PersonSymptom getByTempId(long id, String name);

    @Query("SELECT s.name AS sympt_name FROM Symptom as s, PersonSymptom as ps, Temperature as t WHERE ps.symptomId = s.symptId AND ps.temperatureId = t.tempId AND t.tempId = :id")
    List<String> getDatePersonSympt(long id);

    @Insert
    long insert(PersonSymptom personSymptom);

    @Update
    void update(PersonSymptom personSymptom);

    @Delete
    void delete(PersonSymptom personSymptom);
}
