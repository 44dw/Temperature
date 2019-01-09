package com.a44dw.temperature;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;


public class SickNamesHistoryFragment extends Fragment {

    static AppDatabase database;
    LinearLayout namesHolder;
    TextView historyDescr;
    History history;

    public SickNamesHistoryFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = App.getInstance().getDatabase();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sick_names_history, container, false);
        namesHolder = view.findViewById(R.id.historyLayoutNames);
        historyDescr = view.findViewById(R.id.historyDescr);
        //получаем ссылку на текущую Activity
        history = (History) getActivity();
        //получаем список имён и "раскидываем" его в поле
        SickPersonDaoGetNames sickPersonDaoGetNames = new SickPersonDaoGetNames();
        sickPersonDaoGetNames.execute();
        Random random = new Random();
        try {
            ArrayList<String> names = sickPersonDaoGetNames.get();
            for(String name : names) {
                LinearLayout line = (LinearLayout) getLayoutInflater()
                        .inflate(R.layout.history_names_line_inflater, namesHolder, false);
                line.setBackgroundColor(Color.argb(40, random.nextInt(256), random.nextInt(256), random.nextInt(256)));
                line.setTag(name);
                TextView text = line.findViewById(R.id.sickName);
                text.setText(name);
                namesHolder.addView(line);
            }
            random = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        namesHolder.removeAllViews();
        namesHolder = null;
    }

    static private class SickPersonDaoGetNames extends AsyncTask<Void, Void, ArrayList<String>> {
        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            return (ArrayList<String>)database.sickPersonDao().getNames();
        }
    }
}
