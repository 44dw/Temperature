package com.a44dw.temperature.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.a44dw.temperature.App;
import com.a44dw.temperature.database.AppDatabase;
import com.a44dw.temperature.MainActivity;
import com.a44dw.temperature.entities.SickPerson;
import com.a44dw.temperature.pojo.PersonDateNote;
import com.a44dw.temperature.pojo.PersonDrugHistory;
import com.a44dw.temperature.pojo.PersonSymptHistory;
import com.a44dw.temperature.R;
import com.a44dw.temperature.entities.Temperature;
import com.a44dw.temperature.pojo.Point;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TableHistoryFragment extends Fragment {

    private static final String TAG_NOTE = "note";
    private OnHistoryTableActionListener listener;
    private Map<String, List<Point>> pointMap;
    private ArrayList<CheckBox> boxesList;
    public ConstraintLayout view;
    static AppDatabase database;
    private LinearLayout holder;

    public TableHistoryFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = App.getInstance().getDatabase();
        pointMap = new HashMap<>();
        boxesList = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = (ConstraintLayout) inflater.inflate(R.layout.fragment_history, container, false);
        holder = view.findViewById(R.id.tableHolder);
        //Рисуем таблицу
        drawTable(listener.getChosenPerson());
        return view;
    }

    public void setHistoryTableActionListener(OnHistoryTableActionListener listener) {
        this.listener = listener;
    }

    public List<Point> getSelectedRecords() {
        List<Point> result = new ArrayList<>();
        for(CheckBox cb : boxesList) {
            if(!cb.isChecked()) continue;
            String checkedDate =
                ((TextView)((ConstraintLayout)cb.getParent())
                    .findViewById(R.id.historyTableDateHolder)).getText().toString();
            result.addAll(pointMap.get(checkedDate));
        }
        return result;
    }

    public void drawTable(SickPerson person) {
        new GetPointsFromDB(this).execute(person.getSickId());
    }

    private void fillTable(HashMap<String, List<Point>> pointMap) {
        this.pointMap = pointMap;
        ArrayList<String> datesList = new ArrayList<>(pointMap.keySet());
        Collections.sort(datesList, new Comparator<String>() {
            DateFormat f = new SimpleDateFormat("dd.MM.yy", Locale.getDefault());
            @Override
            public int compare(String o1, String o2) {
                try {
                    return f.parse(o1).compareTo(f.parse(o2));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });

        for(String date : datesList) {
            ArrayList<Point> datePointsList = (ArrayList<Point>) pointMap.get(date);

            //для каждой даты создаём таблицу
            ConstraintLayout oneDateHistory = (ConstraintLayout) getLayoutInflater()
                    .inflate(R.layout.history_table_onedate_inflater, holder, false);
            TableLayout table = oneDateHistory.findViewById(R.id.historyTable);
            table.setVisibility(View.GONE);
            TextView dateHolder = oneDateHistory.findViewById(R.id.historyTableDateHolder);
            dateHolder.setText(date);
            CheckBox checkBox = oneDateHistory.findViewById(R.id.historyShowOnMain);
            boxesList.add(checkBox);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    listener.onDateSelectCheckedChanged(isChecked);
                }
            });

            for(Point p : datePointsList) {
                drawRow(p, table);
            }

            holder.addView(oneDateHistory);
        }

        //делаем видимым последнюю таблицу
        if(holder.getChildCount() > 0) {
            ConstraintLayout lastDateHistory = (ConstraintLayout) holder.getChildAt(holder.getChildCount() - 1);
            ConstraintLayout subHolder = lastDateHistory.findViewById(R.id.historyConstraint);
            TableLayout lastTable = lastDateHistory.findViewById(R.id.historyTable);
            lastTable.setVisibility(View.VISIBLE);
            subHolder.setBackgroundColor(getResources().getColor(R.color.dateBlueDark));
        }

        //прячем прогрессбар
        view.findViewById(R.id.progressBar).setVisibility(View.GONE);

        //проверяем, в допустимых ли значениях ширина экрана
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        ConstraintLayout warning = view.findViewById(R.id.warningLayout);
        if(metrics.widthPixels < 700) {
            warning.setVisibility(View.VISIBLE);
            holder.setVisibility(View.GONE);
            listener.onHideShowOnMainButton(true);
        } else {
            //Показываем таблицу
            holder.setVisibility(View.VISIBLE);
            listener.onHideShowOnMainButton(false);
        }

    }

    private void drawRow(Point p, TableLayout table) {
        TableRow row = (TableRow) getLayoutInflater().inflate(R.layout.history_table_row_inflater, table, false);
        TextView timeField = row.findViewById(R.id.historyTime);
        TextView tempField = row.findViewById(R.id.historyTemp);
        TextView symptField = row.findViewById(R.id.historySympt);
        TextView drugField = row.findViewById(R.id.historyDrug);
        ImageView noteField = row.findViewById(R.id.historyNote);
        TableRow noteRow = null;

        timeField.setText(p.getTime());
        if(p.getTemperature() > 0) tempField.setText(String.valueOf(p.getTemperature()));

        for(String[] drug : p.getDrugArray()) {
            String text;
            if(drugField.getText().toString().length() > 0) {
                text = drugField.getText().toString() + "\n"
                        + drug[0] + " " + drug[1] + " " + drug[2];
            } else {
                text = drug[0] + " " + drug[1] + " " + drug[2];
            }
            drugField.setText(text);
        }

        for(String sympt : p.getSymptArray()) {
            if(symptField.getText().toString().length() > 0) {
                String text = symptField.getText().toString() + "\n" + sympt;
                symptField.setText(text);
            } else symptField.setText(sympt);
        }

        if(p.getNote().length() > 0) {
            //добавляем ImageView картинку
            noteField.setImageResource(R.drawable.ic_writing_on);
            //добавляем тэг, чтобы onClickListener мог определить сегмент с заметкой
            noteField.setTag(TAG_NOTE);
            //надуваем ряд для заметки
            noteRow = (TableRow)getLayoutInflater()
                    .inflate(R.layout.history_table_note_inflater, table, false);
            TextView note = noteRow.findViewById(R.id.historyNoteShow);
            note.setText(p.getNote());
        }

        table.addView(row);
        //добавляем ряд с заметкой и делаем его невидимым
        if(noteRow != null) {
            table.addView(noteRow);
            noteRow.setVisibility(View.GONE);
        }
    }

    static public class GetPointsFromDB extends AsyncTask<Long, Void, HashMap<String, List<Point>>> {

        WeakReference<TableHistoryFragment> wrFragment;

        public GetPointsFromDB(TableHistoryFragment fragment) {
            this.wrFragment = new WeakReference<>(fragment);
        }

        @Override
        protected HashMap<String, List<Point>> doInBackground(Long... id) {
            HashMap<String, List<Point>> pointMap = new HashMap<>();
            ArrayList<String> datesList = (ArrayList<String>) database.temperatureDao().getUnicDatesWithPerson(id[0]);

            for (String date : datesList) {
                List<Point> datePoints = new ArrayList<>();

                ArrayList<PersonDrugHistory> personDrugHistoryList = (ArrayList<PersonDrugHistory>) database.sickPersonDao().getPersonDrugHistory(id[0], date);
                ArrayList<PersonSymptHistory> personSymptHistoryList = (ArrayList<PersonSymptHistory>) database.sickPersonDao().getPersonSymptHistory(id[0], date);
                ArrayList<PersonDateNote> personDateNoteList = (ArrayList<PersonDateNote>) database.temperatureDao().getNotesWithPersonAndDate(id[0], date);
                ArrayList<Temperature> temperatureList = (ArrayList<Temperature>) database.temperatureDao().getAllWithPersonAndDate(id[0], date);

                Collections.sort(temperatureList, new Comparator<Temperature>() {
                    @Override
                    public int compare(Temperature o1, Temperature o2) {
                        try {
                            return MainActivity.timeFormat.parse(o1.time).compareTo(MainActivity.timeFormat.parse(o2.time));
                        } catch (ParseException e) {
                            e.printStackTrace();
                            return 0;
                        }
                    }
                });

                for(Temperature t : temperatureList) {
                    Point point = new Point(t.getTime(), t.getDate(), t.getTemperature(), t.getTempId());

                    point.setDrugArray(fillDrugList(personDrugHistoryList, t.getTempId()));
                    point.setSymptArray(fillSymptList(personSymptHistoryList, t.getTempId()));
                    point.setNote(getNote(personDateNoteList, t.getTempId()));

                    datePoints.add(point);
                }

                pointMap.put(date, datePoints);
            }
            return pointMap;
        }

        @Override
        protected void onPostExecute(HashMap<String, List<Point>> pointMap) {
            wrFragment.get().fillTable(pointMap);
        }

        private String getNote(ArrayList<PersonDateNote> personDateNoteList, long tempId) {

            for(int i=0; i<personDateNoteList.size(); i++) {
                PersonDateNote pdn = personDateNoteList.get(i);
                if((pdn.tempId == tempId)&&(pdn.note.length() > 0)) {
                    return pdn.getNote();
                }
            }
            return "";
        }

        private ArrayList<String> fillSymptList(ArrayList<PersonSymptHistory> personSymptHistoryList, long tempId) {
            ArrayList<String> symptArray = new ArrayList<>();

            for(int i=0; i<personSymptHistoryList.size(); i++) {
                PersonSymptHistory psh = personSymptHistoryList.get(i);
                if(psh.temperatureId == tempId) {
                    symptArray.add(psh.getName());
                    personSymptHistoryList.remove(i);
                    i--;
                }
            }

            return symptArray;
        }

        private ArrayList<String[]> fillDrugList(ArrayList<PersonDrugHistory> personDrugHistoryList, long tempId) {
            ArrayList<String[]> drugArray = new ArrayList<>();

            for(int i=0; i<personDrugHistoryList.size(); i++) {
                PersonDrugHistory pdh = personDrugHistoryList.get(i);
                if(pdh.temperatureId == tempId) {
                    drugArray.add(new String[]{pdh.getName(), String.valueOf(pdh.getAmount()), pdh.getDrugUnit()});
                    personDrugHistoryList.remove(i);
                    i--;
                }
            }
            return drugArray;
        }
    }

    public interface OnHistoryTableActionListener {
        void onDateSelectCheckedChanged(boolean checked);
        void onHideShowOnMainButton(boolean isNeed);
        SickPerson getChosenPerson();
    }
}