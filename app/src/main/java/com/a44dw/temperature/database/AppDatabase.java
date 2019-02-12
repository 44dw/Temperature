package com.a44dw.temperature.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.a44dw.temperature.dao.*;
import com.a44dw.temperature.entities.*;

//Создаём класс базы данных
@Database(entities = {SickPerson.class, Temperature.class, Symptom.class, Drug.class,
        PersonSymptom.class, PersonDrug.class, Note.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SickPersonDao sickPersonDao();
    public abstract TemperatureDao temperatureDao();
    public abstract SymptomDao symptomDao();
    public abstract DrugDao drugDao();
    public abstract PersonSymptomDao personSymptomDao();
    public abstract PersonDrugDao personDrugDao();
    public abstract NoteDao noteDao();
}