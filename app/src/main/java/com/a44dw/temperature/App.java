package com.a44dw.temperature;

import android.app.Application;
import android.arch.persistence.room.Room;

public class App extends Application {

    //единственный экземпляр Application
    public static App instance;

    private static SickPerson appPerson;

    //в оригинале было AppDatabase. Это экземпляр нашей БД
    private AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        database = Room.databaseBuilder(this, AppDatabase.class, "AppDatabase").build();
    }
    public static App getInstance() {
        return instance;
    }

    //возвращает нашу БД
    public AppDatabase getDatabase() {
        return database;
    }

    public SickPerson getPerson() {
        return appPerson;
    }

    //установить текущего больного
    public void setPerson(SickPerson person) {
        appPerson = person;
    }

    public void delPerson() {
        appPerson = null;
    }
}
