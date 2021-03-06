package com.a44dw.temperature.fragments;

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

import com.a44dw.temperature.App;
import com.a44dw.temperature.database.AppDatabase;
import com.a44dw.temperature.R;
import com.a44dw.temperature.activities.History;
import com.a44dw.temperature.entities.SickPerson;

import java.lang.ref.WeakReference;
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
        new SickPersonDaoGetNames(this).execute();
        return view;
    }

    private void fillHolderByNames(ArrayList<SickPerson> persons) {
        Random random = new Random();
        for(SickPerson person : persons) {
            LinearLayout line = (LinearLayout) getLayoutInflater()
                    .inflate(R.layout.history_names_line_inflater, namesHolder, false);
            line.setBackgroundColor(Color.argb(40, random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            line.setTag(person);
            TextView text = line.findViewById(R.id.sickName);
            text.setText(person.getName());
            namesHolder.addView(line);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        namesHolder.removeAllViews();
        namesHolder = null;
    }

    static private class SickPersonDaoGetNames extends AsyncTask<Void, Void, ArrayList<SickPerson>> {

        WeakReference<SickNamesHistoryFragment> wrFragment;

        public SickPersonDaoGetNames(SickNamesHistoryFragment fragment) {
            wrFragment = new WeakReference<>(fragment);
        }

        @Override
        protected ArrayList<SickPerson> doInBackground(Void... params) {
            return (ArrayList<SickPerson>)database.sickPersonDao().getAll();
        }

        @Override
        protected void onPostExecute(ArrayList<SickPerson> sickPeople) {
            wrFragment.get().fillHolderByNames(sickPeople);
        }
    }
}
