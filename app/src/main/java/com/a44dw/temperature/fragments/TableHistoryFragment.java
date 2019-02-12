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
import com.a44dw.temperature.pojo.PersonDateNote;
import com.a44dw.temperature.pojo.PersonDrugHistory;
import com.a44dw.temperature.pojo.PersonSymptHistory;
import com.a44dw.temperature.R;
import com.a44dw.temperature.entities.Temperature;
import com.a44dw.temperature.activities.History;
import com.a44dw.temperature.pojo.Point;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class TableHistoryFragment extends Fragment {

    private OnHistoryTableActionListener listener;
    private Map<String, List<Point>> pointMap;
    private ArrayList<CheckBox> boxesList;
    public ConstraintLayout view;
    static AppDatabase database;
    ArrayList<String> selectedDates;
    LinearLayout holder;

    public TableHistoryFragment() {}

    //TODO выяснить, для чего это
    public void setSelectedDatesArray(ArrayList<String> selectedDates) {
        this.selectedDates = selectedDates;
    }

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
        //Рисуем таблицу
        drawTable(History.chosenName.getName());
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

    public void drawTable(String name) {
        SickPersonDaoGetByName sickPersonDaoGetByName = new SickPersonDaoGetByName();
        sickPersonDaoGetByName.execute(name);
        try {
            long personId = sickPersonDaoGetByName.get();
            //получаем список дат, когда болел Person
            TemperatureDaoGetUnicDatesWithPerson getUnicDatesWithPerson
                    = new TemperatureDaoGetUnicDatesWithPerson();
            getUnicDatesWithPerson.execute(personId);

            ArrayList<String> dates = getUnicDatesWithPerson.get();

            Collections.sort(dates, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    try {
                        return MainActivity.dateFormat.parse(o1).compareTo(MainActivity.dateFormat.parse(o2));
                    } catch (ParseException e) {
                        e.printStackTrace();
                        return 0;
                    }
                }
            });

            holder = view.findViewById(R.id.tableHolder);

            for(String date : dates) {
                List<Point> datePoints = new ArrayList();

                //и для каждой создаём таблицу
                ConstraintLayout oneDateHistory = (ConstraintLayout) getLayoutInflater()
                        .inflate(R.layout.history_table_onedate_inflater, holder, false);
                TableLayout table = oneDateHistory.findViewById(R.id.historyTable);
                table.setVisibility(View.GONE);
                TextView dateHolder = oneDateHistory.findViewById(R.id.historyTableDateHolder);
                dateHolder.setText(date);

                //готовим лист аргументов для передачи в запросы к БД
                ArrayList<String> args = new ArrayList<>();
                args.add(String.valueOf(personId));
                args.add(date);

                //Получаем список лекарств, к-е принимал человек в эту дату
                GetPersonDrugHistory getPersonDrugHistory = new GetPersonDrugHistory();
                getPersonDrugHistory.execute(args);

                //Получаем список симптомов, к-е были у человека в эту дату
                GetPersonSymptHistory getPersonSymptHistory = new GetPersonSymptHistory();
                getPersonSymptHistory.execute(args);

                //Получаем список заметок за текущую дату и человека
                GetNotesWithPersonAndDate getNotesWithPersonAndDate = new GetNotesWithPersonAndDate();
                getNotesWithPersonAndDate.execute(args);

                //получаем список температур по человеку и дате
                GetAllWithPersonAndDate getAllWithPersonAndDate = new GetAllWithPersonAndDate();
                getAllWithPersonAndDate.execute(args);

                //получаем все листы в переменные
                ArrayList<PersonDrugHistory> personDrugHistory = getPersonDrugHistory.get();
                ArrayList<PersonSymptHistory> personSymptHistory = getPersonSymptHistory.get();
                ArrayList<PersonDateNote> noteHistory = getNotesWithPersonAndDate.get();
                ArrayList<Temperature> temperatures = getAllWithPersonAndDate.get();

                Collections.sort(temperatures, new Comparator<Temperature>() {
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

                CheckBox checkBox = oneDateHistory.findViewById(R.id.historyShowOnMain);
                boxesList.add(checkBox);
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        listener.onDateSelectCheckedChanged(isChecked);
                    }
                });

                for(Temperature t : temperatures) {
                    Point point = new Point(t.getTime(), t.getDate(), t.getTemperature(), t.getTempId());
                    ArrayList<String[]> drugArray = new ArrayList<>();
                    ArrayList<String> symptArray = new ArrayList<>();
                    String pointNote = "";

                    TableRow row = (TableRow) getLayoutInflater().inflate(R.layout.history_table_row_inflater, table, false);
                    TextView timeField = row.findViewById(R.id.historyTime);
                    TextView tempField = row.findViewById(R.id.historyTemp);
                    TextView symptField = row.findViewById(R.id.historySympt);
                    TextView drugField = row.findViewById(R.id.historyDrug);
                    ImageView noteField = row.findViewById(R.id.historyNote);
                    TableRow noteRow = null;
                    timeField.setText(t.time);
                    if(t.temperature > 0) tempField.setText(String.valueOf(t.temperature));

                    for(int i=0; i<personDrugHistory.size(); i++) {
                        PersonDrugHistory pdh = personDrugHistory.get(i);
                        if(pdh.temperatureId == t.tempId) {
                            drugArray.add(new String[]{pdh.getName(), String.valueOf(pdh.getAmount()), pdh.getDrugUnit()});
                            if(drugField.getText().toString().length() > 0) {
                                String text = drugField.getText().toString() + "\n"
                                        + pdh.name + " " + pdh.amount + " " + pdh.drugUnit;
                                drugField.setText(text);
                            } else {
                                String text = pdh.name + " " + pdh.amount + " " + pdh.drugUnit;
                                drugField.setText(text);
                            }
                            personDrugHistory.remove(i);
                            i--;
                        }
                    }

                    for(int i=0; i<personSymptHistory.size(); i++) {
                        PersonSymptHistory psh = personSymptHistory.get(i);
                        if(psh.temperatureId == t.tempId) {
                            symptArray.add(psh.getName());
                            if(symptField.getText().toString().length() > 0) {
                                String text = symptField.getText().toString() + "\n" + psh.name;
                                symptField.setText(text);
                            } else symptField.setText(psh.name);
                            personSymptHistory.remove(i);
                            i--;
                        }
                    }

                    for(int i=0; i<noteHistory.size(); i++) {
                        PersonDateNote pdn = noteHistory.get(i);
                        if((pdn.tempId == t.tempId)&&(pdn.note.length() > 0)) {
                            //добавляем ImageView картинку
                            noteField.setImageResource(R.drawable.ic_writing_on);
                            //добавляем тэг, чтобы onClickListener мог определить сегмент с заметкой
                            noteField.setTag("note");
                            //надуваем ряд для заметки
                            noteRow = (TableRow)getLayoutInflater()
                                    .inflate(R.layout.history_table_note_inflater, table, false);
                            TextView note = noteRow.findViewById(R.id.historyNoteShow);
                            note.setText(pdn.getNote());
                            pointNote = pdn.getNote();
                        }
                    }
                    //формируем Point и добавляем его в лист
                    point.setDrugArray(drugArray);
                    point.setSymptArray(symptArray);
                    point.setNote(pointNote);
                    datePoints.add(point);

                    table.addView(row);
                    //добавляем ряд с заметкой и делаем его невидимым
                    if(noteRow != null) {
                        table.addView(noteRow);
                        noteRow.setVisibility(View.GONE);
                    }
                }
                holder.addView(oneDateHistory);
                //добавляем points list в map
                pointMap.put(date, datePoints);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        //делаем видимым последнюю таблицу
        if(holder.getChildCount() > 0) {
            ConstraintLayout lastDateHistory = (ConstraintLayout) holder.getChildAt(holder.getChildCount() - 1);
            ConstraintLayout subHolder = lastDateHistory.findViewById(R.id.historyConstraint);
            //TextView date = lastDateHistory.findViewById(R.id.historyTableDateHolder);
            TableLayout lastTable = lastDateHistory.findViewById(R.id.historyTable);
            lastTable.setVisibility(View.VISIBLE);
            subHolder.setBackgroundColor(getResources().getColor(R.color.dateBlueDark));
        }
        //проверяем, в допустимых ли значениях ширина экрана
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        ConstraintLayout warning = view.findViewById(R.id.warningLayout);
        if(metrics.widthPixels < 700) {
            warning.setVisibility(View.VISIBLE);
            holder.setVisibility(View.GONE);
            listener.onNeedToChangeOrientation(true);
        } else listener.onNeedToChangeOrientation(false);
    }

    //получаем id больного
    static public class SickPersonDaoGetByName extends AsyncTask<String, Void, Long> {
        @Override
        protected Long doInBackground(String... name) {
            return database.sickPersonDao().getByName(name[0]).sickId;
        }
    }

    //получаем список дат, когда болел Person
    static private class TemperatureDaoGetUnicDatesWithPerson extends AsyncTask<Long, Void, ArrayList<String>> {
        @Override
        protected ArrayList<String> doInBackground(Long... id) {
            return (ArrayList<String>) database.temperatureDao().getUnicDatesWithPerson(id[0]);
        }
    }

    //Получаем список лекарств, к-е принимал человек в эту дату
    static private class GetPersonDrugHistory extends AsyncTask<ArrayList, Void, ArrayList<PersonDrugHistory>> {
        protected ArrayList<PersonDrugHistory> doInBackground(ArrayList... args) {
            return (ArrayList<PersonDrugHistory>) database.sickPersonDao()
                    .getPersonDrugHistory(Long.parseLong((String) args[0].get(0)), (String) args[0].get(1));
        }
    }

    //Получаем список симптомов, к-е были у человека в эту дату
    static private class GetPersonSymptHistory extends AsyncTask<ArrayList, Void, ArrayList<PersonSymptHistory>> {
        protected ArrayList<PersonSymptHistory> doInBackground(ArrayList... args) {
            return (ArrayList<PersonSymptHistory>) database.sickPersonDao()
                    .getPersonSymptHistory(Long.parseLong((String) args[0].get(0)), (String) args[0].get(1));
        }
    }

    //Получаем список заметок за текущую дату и человека
    static private class GetNotesWithPersonAndDate extends AsyncTask<ArrayList, Void, ArrayList<PersonDateNote>> {
        protected ArrayList<PersonDateNote> doInBackground(ArrayList... args) {
            return (ArrayList<PersonDateNote>) database.temperatureDao().getNotesWithPersonAndDate(Long.parseLong((String) args[0].get(0)), (String) args[0].get(1));
        }
    }

    //получаем список температур по человеку и дате
    static private class GetAllWithPersonAndDate extends AsyncTask<ArrayList, Void, ArrayList<Temperature>> {
        protected ArrayList<Temperature> doInBackground(ArrayList... args) {
            return (ArrayList<Temperature>) database.temperatureDao()
                    .getAllWithPersonAndDate(Long.parseLong((String) args[0].get(0)), (String) args[0].get(1));
        }
    }
    public interface OnHistoryTableActionListener {
        void onDateSelectCheckedChanged(boolean checked);
        void onNeedToChangeOrientation(boolean isNeed);
    }
}