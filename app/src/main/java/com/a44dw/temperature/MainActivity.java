package com.a44dw.temperature;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.pm.PackageInfoCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity implements PickSickDialogFragment.PickSickDialogListener,
                                                               ClearScreenDialog.ClearScreenDialogListener {

    private static PointGeneratorDialog generatorDialog;
    private static DisplayMetrics metrics;
    public ArrayList<Point> points;

    private static final String VERSION_KEY = "version_number";

    final int REQUEST_CODE_ADDPOINT = 1;
    final int REQUEST_CODE_FOCUSPOINT = 2;
    final int REQUEST_CODE_HISTORY = 3;
    final int REQUEST_CODE_PREFERENCES = 4;
    public static final String EXTRA_ALL = "data";
    public static final String EXTRA_FIRST = "firstPoint";
    public static final String EXTRA_ID = "tempId";
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy", Locale.US);
    public static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);

    private static AppDatabase database;
    private LinearLayout mainLinesHolder;
    private ConstraintLayout greeter;
    private Menu menu;
    public static int numOfLines;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (generatorDialog == null) generatorDialog = new PointGeneratorDialog();
        if (points == null) points = new ArrayList<>();
        if (mainLinesHolder == null) mainLinesHolder = findViewById(R.id.mainTempLinesHolder);
        if (metrics == null) {
            metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        //получаем ссылку на БД
        database = App.getInstance().getDatabase();
        greeter = findViewById(R.id.mainGreetHolder);

        SharedPreferences preferences = getPreferences(Activity.MODE_PRIVATE);
        if(preferences.contains("sickName")) {
            String name = preferences.getString("sickName", null);
            if(name != null) {
                SickPersonDaoGetByName sickPersonDaoGetByName = new SickPersonDaoGetByName();
                sickPersonDaoGetByName.execute(name);
                try {
                    SickPerson person = sickPersonDaoGetByName.get();
                    App.getInstance().setPerson(person);
                    //метод не вызывался при загрузке SharedPrefs, когда меню ещё не создано
                    if(menu != null)changeTitle();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }

        numOfLines = Integer.valueOf(PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString(Preferences.NUM_LINES, "15"));

        //разворачиваем сохранённые точки, только если есть имя пользователя
        if(preferences.contains("onScreenPoints")) {
            if(App.getInstance().getPerson() != null) {
                greeter.setVisibility(GONE);
                final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.US);
                Set<String> pointsSet = preferences.getStringSet("onScreenPoints", null);
                if(pointsSet != null) {
                    //извлекаем points из json
                    for(String s : pointsSet) {
                        Gson gson = new Gson();
                        Point point = gson.fromJson(s, Point.class);
                        points.add(point);
                    }
                    //Сортируем массив
                    Collections.sort(points, new Comparator<Point>() {
                        @Override
                        public int compare(Point point1, Point point2) {
                            try {
                                Date date1 = format.parse(point1.date + " " + point1.time);
                                Date date2 = format.parse(point2.date + " " + point2.time);
                                return (date1.compareTo(date2));
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            return 1;
                        }
                    });
                    //рисуем view
                    for(Point p : points) {
                        ConstraintLayout point = generatorDialog.createPoint(p, true);
                        mainLinesHolder.addView(point);
                    }
                    //прокручивает ScrollView вниз
                    scrollDown();
                }
            }
        }
        if(points.size() == 0) greeter.setVisibility(VISIBLE);
        checkIfVersionIsNew();
    }

    private void checkIfVersionIsNew() {
        long savedVersion = getPreferences(Activity.MODE_PRIVATE).getLong(VERSION_KEY, 0);
        long currentVersion = 0;
        try {
            currentVersion = PackageInfoCompat.getLongVersionCode(getPackageManager().getPackageInfo(getPackageName(), 0));
        } catch (PackageManager.NameNotFoundException e) {e.printStackTrace();}
        if(currentVersion > savedVersion) {
            //showWhatsNewDialog();

            getPreferences(Activity.MODE_PRIVATE).edit().putLong(VERSION_KEY, currentVersion).apply();
        }
    }

    private void showWhatsNewDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_whatsnew, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setView(view).setTitle(R.string.dialog_whatsnew)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.create().show();
    }

    private void scrollDown() {
        final ScrollView mainView = findViewById(R.id.ScrollView);
        mainView.post(new Runnable() {
            @Override
            public void run() {
                mainView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private class PointGeneratorDialog {

        //Создание записи после PointGenerator
        private Object[] createPoint(float t,
                                 ArrayList<String[]> drugArray,
                                 ArrayList<String> symptArray,
                                 String time,
                                 String date,
                                 long tempId,
                                 String note,
                                 boolean restorePoint,
                                 boolean backdating) {
            ConstraintLayout line;
            Point currentPoint = null;
            if(!restorePoint) currentPoint = new Point(time, date, t, drugArray, symptArray, tempId, note);
            if(t > 0.0f) {
                line = (ConstraintLayout) getLayoutInflater()
                            .inflate(R.layout.main_add_point_inflater_alt_t, mainLinesHolder, false);
                ImageView redline = line.findViewById(R.id.addPointRedline);
                //вычисляем и устанавливаем длину красной линии
                double scalePointLength = (double) metrics.widthPixels/74;
                double tCut = (t - 34.8)*10;
                redline.getLayoutParams().width = (int)(scalePointLength * tCut);
           } else {
                //если температура не указана - надуваем и заполняем другой шаблон
               line = (ConstraintLayout) getLayoutInflater()
                    .inflate(R.layout.main_add_point_inflater_large, mainLinesHolder, false);
            }
            if(drugArray.size() > 0) fillDrugField(line, currentPoint, drugArray, t);
            if(symptArray.size() > 0) fillSymptField(line, currentPoint, symptArray, t);
            //если есть заметка, делаем значок цветным
            if(note.length() > 0) fillNoteField(line, currentPoint, note, t);
            //ищем текстовое поле времени и заполняем его
            TextView timeField = line.findViewById(R.id.addPointTime);
            TextView dateField = line.findViewById(R.id.addPointDate);
            timeField.setText(time);
            dateField.setText(date);
            int position = -1;
            //создаём новую точку
            if(currentPoint != null) {
                //если массив переполнен, удаляем первую точку
                if((numOfLines > 0)&&(points.size()+1 > numOfLines)) {
                    points.remove(0);
                    mainLinesHolder.removeView(mainLinesHolder.getChildAt(0));
                }
                try {
                    if(backdating) {
                        Date currentPointDate = dateFormat.parse(date);
                        Date currentPointTime = timeFormat.parse(time);
                        Date oldPointDte;
                        Date oldPointTime;
                        Cycle1:
                        for(int i=0; i<=points.size(); i++) {
                            //если дата из будущего
                            if(i == points.size()) {
                                points.add(currentPoint);
                                break;
                            }
                            oldPointDte = dateFormat.parse(points.get(i).date);
                            int compareResult = currentPointDate.compareTo(oldPointDte);
                            switch (compareResult) {
                                //раньше
                                case -1: {
                                    points.add(i, currentPoint);
                                    position = i;
                                    break Cycle1;
                                }
                                //та же дата
                                case 0: {
                                    oldPointTime = timeFormat.parse(points.get(i).time);
                                    if(currentPointTime.compareTo(oldPointTime) < 0) {
                                        points.add(i, currentPoint);
                                        position = i;
                                        break Cycle1;
                                    }
                                    break;
                                }
                            }
                        }
                    } else points.add(currentPoint);
                } catch (ParseException e) {
                    e.printStackTrace();
                    points.add(currentPoint);
                }
            }
            //прикрепляем к хольдеру тэг
            line.setTag(tempId);
            return new Object[]{line, position};
        }

        private ConstraintLayout createPoint(Point p, boolean restorePoint) {
            return (ConstraintLayout) createPoint(p.temperature, p.drugArray, p.symptArray, p.time, p.date, p.tempId, p.note, restorePoint, false)[0];
        }

        void fillSymptField(ConstraintLayout line, Point p, ArrayList<String> symptArray, float t) {
            //если шаблон верный, заполняем поле, если нет - только symptArray в Point
            if(t == 0.0f) {
                TextView symptField = line.findViewById(R.id.addPointSympt);
                //обнуляем поле
                symptField.setText("");
                if(symptArray.size()>0) {
                    for(int i=0; i<symptArray.size(); i++) {
                        if(i < 2) {
                            String textInSymptField = symptField.getText() + symptArray.get(i) + "\n";
                            symptField.setText(textInSymptField);
                        } else {
                            String textInSymptField = symptField.getText() + "...";
                            symptField.setText(textInSymptField);
                            break;
                        }
                    }
                }
            }
            ImageView symptIcon = line.findViewById(R.id.addPointSPic);
            if(symptArray.size()>0) symptIcon.setImageResource(R.drawable.ic_s_red);
            else symptIcon.setImageResource(R.drawable.ic_s_grey);

            if(p != null) p.symptArray = symptArray;
        }

        void fillDrugField(ConstraintLayout line, Point p, ArrayList<String[]> drugArray, float t) {
            //определяем тип шаблона
            if(t == 0.0f) {
                TextView drugField = line.findViewById(R.id.addPointDrug);
                //обнуляем поле
                drugField.setText("");
                if(drugArray.size()>0) {
                    for(int i=0; i<drugArray.size(); i++) {
                        String textInDrugField;
                        if(i < 2) {
                            textInDrugField = drugField.getText() + drugArray.get(i)[0] + " " + drugArray.get(i)[1] + " " + drugArray.get(i)[2] + "\n";
                            drugField.setText(textInDrugField);
                        }
                        else {
                            textInDrugField = drugField.getText() + "...";
                            drugField.setText(textInDrugField);
                            break;
                        }
                    }
                }
            }
            ImageView drugIcon = line.findViewById(R.id.addPointDrugPic);
            if(drugArray.size()>0) drugIcon.setImageResource(R.drawable.ic_medicine_blue);
            else drugIcon.setImageResource(R.drawable.ic_medicine_grey);

            if(p != null) p.drugArray = drugArray;
        }

        void fillNoteField(ConstraintLayout line, Point p, String note, float t) {
            //определяем тип шаблона
            if(t == 0.0f) {
                TextView noteField = line.findViewById(R.id.addPointNote);
                noteField.setText(note);
            }
            ImageView noteIcon = line.findViewById(R.id.addPointNotePic);
            if(note.length() > 0) noteIcon.setImageResource(R.drawable.ic_writing_on);
            else noteIcon.setImageResource(R.drawable.ic_writing);

            if(p != null) p.note = note;
        }

        @SuppressWarnings("unchecked")
        void changeData(ConstraintLayout tLayout, String type, Object o) {

            Point currentPoint = findCurrentPoint(tLayout);

            switch (type) {
                case("drug"): {
                    ArrayList<String[]> drugArray = (ArrayList<String[]>) o;
                    fillDrugField(tLayout, currentPoint, drugArray, currentPoint.temperature);
                    break;
                }case("sympt"): {
                    ArrayList<String> symptArray = (ArrayList<String>) o;
                    fillSymptField(tLayout, currentPoint, symptArray, currentPoint.temperature);
                    break;
                }case("note"): {
                    String note = String.valueOf(o);
                    currentPoint.note = note;
                    fillNoteField(tLayout, currentPoint, note, currentPoint.temperature);
                    break;
                }
            }
        }

        Point findCurrentPoint(ConstraintLayout tLayout) {
            long tempId = (long) tLayout.getTag();
            Point currentPoint = null;

            //находим в списке текущую точку
            for(Point p : points) {
                if(p.tempId == tempId) {
                    currentPoint = p;
                    break;
                }
            }
            return currentPoint;
        }
    }

    public void onClickListener(View view) {
        switch (view.getId()) {
            //создаём новую запись
            case(R.id.createPoint): {
                Intent intent = new Intent(getApplicationContext(), PointGenerator.class);
                //Если это первая точка, требуется вернуть объект sickPerson, если нет - это не нужно
                // (и не вызывается диалог выбора больного)
                if(App.getInstance().getPerson() == null) intent.putExtra(EXTRA_FIRST, true);
                startActivityForResult(intent, REQUEST_CODE_ADDPOINT);
                break;
            }
            //смотрим подробности записи
            case(R.id.temperatureLayout): {
                ConstraintLayout tLayout = (ConstraintLayout)view;

                Point currentPoint = generatorDialog.findCurrentPoint(tLayout);
                ArrayList data = currentPoint.getData();

                Intent intent = new Intent(getApplicationContext(), FocusPoint.class);

                intent.putExtra(EXTRA_ALL, data);
                intent.putExtra(EXTRA_ID, (long)tLayout.getTag());

                startActivityForResult(intent, REQUEST_CODE_FOCUSPOINT);
                break;
            }
        }
    }

    //изменяет title, когда определяется больной
    public void changeTitle() {
        int needToLoadLines = Integer.valueOf(PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString(Preferences.CHANGE_NAME, "0"));
        //очищаем главный экран и список точек
        mainLinesHolder.removeAllViews();
        points.clear();
        if(needToLoadLines > 0) {
            int counter = numOfLines;

            GetPointWithPersonAndLastDate getPointWithPersonAndLastDate = new GetPointWithPersonAndLastDate();
            getPointWithPersonAndLastDate.execute(App.getInstance().getPerson().sickId);
            try {
                ArrayList<Point> points = getPointWithPersonAndLastDate.get();
                for(int i=points.size()-1; i>=0; i--) {
                    if(counter == 0) break;
                    ConstraintLayout point = generatorDialog.createPoint(points.get(i), false);
                    mainLinesHolder.addView(point, 0);
                    if(counter > -1) counter--;
                }
                scrollDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        greeter.setVisibility(points.size() > 0 ? GONE : VISIBLE);
        setTitle();
    }

    public void setTitle() {
        MenuItem changePerson = menu.findItem(R.id.menu_change);
        if(App.getInstance().getPerson() != null) {
            //меняет title и делает видимым пункт меню "сменить больного"
            setTitle(App.getInstance().getPerson().name + " болеет :(");
            if (!changePerson.isVisible()) changePerson.setVisible(true);
        } else {
            setTitle("Ти");
            if (changePerson.isVisible()) changePerson.setVisible(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PREFERENCES) {
            if(resultCode == RESULT_OK) {
                int newNum = data.getIntExtra(Preferences.EXTRA_LINES, 15);
                if(newNum != 0) {
                    numOfLines = newNum;
                    if(numOfLines == -1) return;
                    if(points.size() > numOfLines) {
                        int difference = points.size() - numOfLines;
                        points.subList(0, difference).clear();
                        mainLinesHolder.removeViews(0, difference);
                    }
                }
            }
        }
        if (requestCode == REQUEST_CODE_ADDPOINT) {
            if(resultCode == RESULT_OK) {
                //получаем extra
                Bundle bundle = data.getBundleExtra(PointGenerator.EXTRA_DATA);
                float nowTemp = bundle.getFloat("nowTemp");
                long tempId = bundle.getLong("temperatureId");
                @SuppressWarnings("unchecked")
                ArrayList<String[]> drugArray = (ArrayList<String[]>) bundle.getSerializable("drugArray");
                ArrayList<String> symptArray = bundle.getStringArrayList("symptArray");
                String time = bundle.getString("time");
                String date = bundle.getString("date");
                String note = (bundle.getString("note") == null) ? "" : bundle.getString("note");
                boolean changedDateTime = bundle.getBoolean("changedDateTime");
                if(bundle.getBoolean("changedPerson")) changeTitle();
                if(greeter.getVisibility() == VISIBLE) greeter.setVisibility(GONE);
                //создаём точку
                Object[] createdThing = generatorDialog.createPoint(nowTemp, drugArray, symptArray, time, date, tempId, note, false, changedDateTime);

                int position = (int)createdThing[1];
                ConstraintLayout line = (ConstraintLayout) createdThing[0];

                if(position < 0) mainLinesHolder.addView(line);
                else mainLinesHolder.addView(line, position);
            }
        }
        if(requestCode == REQUEST_CODE_FOCUSPOINT) {
            if(resultCode == RESULT_OK) {
                //получаем ID целевого элемента
                long targetId = data.getLongExtra(FocusPoint.EXTRA_TEMPID, 0);
                    //находим нужный элемент в DOM
                    for(int i=0; i<mainLinesHolder.getChildCount(); i++) {
                        ConstraintLayout tLayout = (ConstraintLayout) mainLinesHolder.getChildAt(i);
                        if((long)tLayout.getTag() == targetId) {
                            //Если получена команда на удаление...
                            if (data.hasExtra(FocusPoint.EXTRA_DEL)) {
                                //удаляем view из иерархии родителя и из списка
                                points.remove(generatorDialog.findCurrentPoint(tLayout));
                                mainLinesHolder.removeView(tLayout);
                                if(points.size() == 0) greeter.setVisibility(VISIBLE);
                                break;
                            }
                            //если есть экстра с лекарствами, симптомами или заметками...
                            if(data.hasExtra(FocusPoint.EXTRA_DRUGS)) {
                                //получаем новый массив лекарств в переменную
                                @SuppressWarnings("unchecked")
                                ArrayList<String[]> drugArray = (ArrayList<String[]>) data.getSerializableExtra(FocusPoint.EXTRA_DRUGS);
                                generatorDialog.changeData(tLayout, "drug", drugArray);
                            }
                            if(data.hasExtra(FocusPoint.EXTRA_SYMPTS)) {
                                ArrayList<String> symptArray = data.getStringArrayListExtra(FocusPoint.EXTRA_SYMPTS);
                                generatorDialog.changeData(tLayout, "sympt", symptArray);
                            }
                            if(data.hasExtra(FocusPoint.EXTRA_NOTE)) {
                                String newNote = data.getStringExtra(FocusPoint.EXTRA_NOTE);
                                generatorDialog.changeData(tLayout, "note", newNote);
                            }
                            break;
                        }
                    }
                }
            }
            //прокручиваем scrollView вниз
            scrollDown();
            if(requestCode == REQUEST_CODE_HISTORY) {
                if (resultCode == RESULT_OK) {
                    if(data.getBooleanExtra(History.EXTRA_CHANGE, false)) changeTitle();
                }
            }
        }


    //создаёт меню в "шапке"
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        this.menu = menu;
        if(App.getInstance().getPerson() != null) setTitle();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.menu_clear:
                new ClearScreenDialog().show(getSupportFragmentManager(), "ClearScreenDialog");
                return true;
            case R.id.menu_about:
                Intent intentCredits = new Intent(getApplicationContext(), Credits.class);
                startActivity(intentCredits);
                return true;
            case R.id.menu_change:
                new PickSickDialogFragment().show(getSupportFragmentManager(), "PickSickDialog");
                return true;
            case R.id.menu_prefs:
                Intent intentPrefs = new Intent(getApplicationContext(), Preferences.class);
                startActivityForResult(intentPrefs, REQUEST_CODE_PREFERENCES);
                return true;
            case R.id.menu_history:
                Intent intentHistory = new Intent(getApplicationContext(), History.class);
                startActivityForResult(intentHistory, REQUEST_CODE_HISTORY);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Set<String> pointsSet = new LinkedHashSet<>();
        for(Point point: points) {
            //превращаем Points в json
            Gson gson = new Gson();
            String jsonPoint = gson.toJson(point);
            pointsSet.add(jsonPoint);
        }
        SharedPreferences preferences = getPreferences(Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet("onScreenPoints", pointsSet);
        if(App.getInstance().getPerson() != null) editor.putString("sickName", App.getInstance().getPerson().name);
        editor.apply();
    }
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    //При выборе больного в диалоге
    @Override
    public void onDialogPositiveClick(DialogFragment dialog, final String name, Boolean newName) {
        if(newName) {
            final SickPerson newPerson = new SickPerson();
            newPerson.name = name;
            //отправляем в БД в отдельном потоке

            new Thread(new Runnable() {
                @Override
                public void run() {
                    newPerson.sickId = database.sickPersonDao().insert(newPerson);
                    App.getInstance().setPerson(newPerson);
                }
            }).start();

            App.getInstance().setPerson(newPerson);
            if(menu != null)changeTitle();
        } else {
            SickPersonDaoGetByName sickPersonDaoGetByName = new SickPersonDaoGetByName();
            sickPersonDaoGetByName.execute(name);
            try {
                SickPerson person = sickPersonDaoGetByName.get();
                App.getInstance().setPerson(person);
                //метод не вызывался при загрузке SharedPrefs, когда меню ещё не создано
                if(menu != null)changeTitle();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    //при выборе метода очистки экрана
    @Override
    public void onDialogPositiveClick(DialogFragment dialog, int what) {
        if(points.size() == 0) return;
        switch (what) {
            case ClearScreenDialog.DIALOG_CLEAR_ALL: {
                mainLinesHolder.removeAllViews();
                points.clear();
                greeter.setVisibility(VISIBLE);
                break;
            }
            case ClearScreenDialog.DIALOG_CLEAR_BUT: {
                try {
                    Date lastDate = dateFormat.parse(points.get(points.size()-1).date);
                    int lastRemovePoint = 0;
                    for(int i=points.size() - 2; i>=0; i--) {
                        Date date = dateFormat.parse(points.get(i).date);
                        if(date.compareTo(lastDate) != 0) {
                            lastRemovePoint = i;
                            break;
                        }
                    }
                    points.subList(0, ++lastRemovePoint).clear();
                    mainLinesHolder.removeViews(0, lastRemovePoint);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    static private class SickPersonDaoGetByName extends AsyncTask<String, Void, SickPerson> {
        @Override
        protected SickPerson doInBackground(String... name) {
            return database.sickPersonDao().getByName(name[0]);
        }
    }

    static private class GetPointWithPersonAndLastDate extends AsyncTask<Long, Void, ArrayList<Point>> {
        protected ArrayList<Point> doInBackground(Long... id) {
            ArrayList<Point> points = new ArrayList<>();
            //получаем список температур с последней датой
            ArrayList<Temperature> tempList = (ArrayList<Temperature>) database.temperatureDao()
                    .getAllWithPersonAndLastDate(id[0]);
            if(tempList.size() > 0) {
                for(Temperature t : tempList) {
                    ArrayList<String[]> drugList = new ArrayList<>();
                    ArrayList<ConcreteTemperatureDrug> drugForTemperature = (ArrayList<ConcreteTemperatureDrug>)database.personDrugDao().getConcreteTemperatureDrug(t.tempId);
                    for(ConcreteTemperatureDrug cd : drugForTemperature) drugList.add(new String[]{cd.drug_name, cd.amount, cd.drugUnit});
                    ArrayList<String> symptList = (ArrayList<String>)database.personSymptomDao().getDatePersonSympt(t.tempId);
                    Note n = database.noteDao().getById(t.tempId);
                    String note = (n == null ? "" : n.note);
                    points.add(new Point(t.time, t.date, t.temperature, drugList, symptList, t.tempId, note));
                }
            }
            return points;
        }
    }
}
