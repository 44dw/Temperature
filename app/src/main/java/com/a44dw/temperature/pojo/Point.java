package com.a44dw.temperature.pojo;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Point implements Serializable {

    public String time;
    public String date;
    public float temperature;
    public ArrayList<String[]> drugArray;
    public ArrayList<String> symptArray;
    public String note;
    public long tempId;

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

    public ArrayList getData() {
        ArrayList<Object> data = new ArrayList<>();
        data.add(time);
        data.add(date);
        data.add(temperature);
        data.add(drugArray);
        data.add(symptArray);
        data.add(note);
        return data;
    }

    public int describeContents() {
        return 0;
    }


    // упаковываем объект в Parcel
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(time);
        parcel.writeFloat(temperature);
        parcel.writeString(note);
        parcel.writeList(drugArray);
        parcel.writeStringList(symptArray);
        parcel.writeLong(tempId);
    }

    public static final Parcelable.Creator<Point> CREATOR = new Parcelable.Creator<Point>() {
        // распаковываем объект из Parcel
        public Point createFromParcel(Parcel in) {
            return new Point(in);
        }

        public Point[] newArray(int size) {
            return new Point[size];
        }
    };

    // конструктор, считывающий данные из Parcel
    private Point(Parcel parcel) {
        time = parcel.readString();
        temperature = parcel.readFloat();
        note = parcel.readString();
        parcel.readList(drugArray, List.class.getClassLoader());
        parcel.readStringList(symptArray);
        tempId = parcel.readLong();
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
