package com.a44dw.temperature.pojo;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Point implements Serializable {

    public String getTime() {
        return time;
    }

    public String getDate() {
        return date;
    }

    public float getTemperature() {
        return temperature;
    }

    public ArrayList<String[]> getDrugArray() {
        return drugArray;
    }

    public ArrayList<String> getSymptArray() {
        return symptArray;
    }

    public String getNote() {
        return note;
    }

    public long getTempId() {
        return tempId;
    }

    private String time;
    private String date;
    private float temperature;
    private ArrayList<String[]> drugArray;
    private ArrayList<String> symptArray;
    private String note;
    private long tempId;

    public static final String KEY_TIME = "time";
    public static final String KEY_DATE = "date";
    public static final String KEY_TEMP = "temp";
    public static final String KEY_DARR = "darr";
    public static final String KEY_SARR = "sarr";
    public static final String KEY_NOTE = "note";

    public Point(String t, String d, float tm, ArrayList<String[]> da, ArrayList<String> sa, long tid, String n) {
        time = t;
        date = d;
        temperature = tm;
        drugArray = da;
        symptArray = sa;
        tempId = tid;
        note = n;
    }

    public Point(String time, String date, float temperature, long tempId) {
        this.time = time;
        this.date = date;
        this.temperature = temperature;
        this.tempId = tempId;
    }

    public HashMap<String, Object> getData() {
        HashMap<String, Object> data = new HashMap<>();

        data.put(KEY_TIME, time);
        data.put(KEY_DATE, date);
        data.put(KEY_TEMP, temperature);
        data.put(KEY_DARR, drugArray);
        data.put(KEY_SARR, symptArray);
        data.put(KEY_NOTE, note);
        return data;
    }

    public void setDrugArray(ArrayList<String[]> drugArray) {
        this.drugArray = drugArray;
    }

    public void setSymptArray(ArrayList<String> symptArray) {
        this.symptArray = symptArray;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
