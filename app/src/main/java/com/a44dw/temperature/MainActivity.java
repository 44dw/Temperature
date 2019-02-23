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

import com.a44dw.temperature.activities.Credits;
import com.a44dw.temperature.activities.FocusPoint;
import com.a44dw.temperature.activities.History;
import com.a44dw.temperature.activities.PointGenerator;
import com.a44dw.temperature.activities.Preferences;
import com.a44dw.temperature.database.AppDatabase;
import com.a44dw.temperature.dialogs.ClearScreenDialog;
import com.a44dw.temperature.dialogs.PickSickDialog;
import com.a44dw.temperature.entities.Note;
import com.a44dw.temperature.entities.SickPerson;
import com.a44dw.temperature.entities.Temperature;
import com.a44dw.temperature.interfaces.PersonSettable;
import com.a44dw.temperature.pojo.ConcreteTemperatureDrug;
import com.a44dw.temperature.pojo.Point;
import com.google.gson.Gson;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity implements PickSickDialog.PickSickDialogListener,
        ClearScreenDialog.ClearScreenDialogListener,
        View.OnClickListener,
        PersonSettable {

    private static PointGeneratorHelper generatorHelper;
    private static DisplayMetrics metrics;
    private ArrayList<Point> points;

    private static final String VERSION_KEY = "version_number";

    public static final int REQUEST_CODE_ADDPOINT = 1;
    public static final int REQUEST_CODE_FOCUSPOINT = 2;
    public static final int REQUEST_CODE_HISTORY = 3;
    public static final int REQUEST_CODE_PREFERENCES = 4;
    public static final String EXTRA_ALL = "data";
    public static final String EXTRA_FIRST = "firstPoint";
    public static final String EXTRA_ID = "tempId";
    public static final String SHARED_PREFS_SICKNAME = "sickName";
    public static final String SHARED_PREFS_POINTS = "onScreenPoints";
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy", Locale.US);
    public static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
    private int numOfLines;

    private AppDatabase database;

    private LinearLayout mainLinesHolder;
    private ConstraintLayout greeter;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        metrics = getScreenMetrics();

        generatorHelper = new PointGeneratorHelper();
        points = new ArrayList<>();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        //получаем ссылку на БД
        database = App.getInstance().getDatabase();

        SharedPreferences preferences = getPreferences(Activity.MODE_PRIVATE);
        loadSickName(preferences);
        numOfLines = Integer.valueOf(PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString(Preferences.NUM_LINES, "15"));
        loadSavedPoints(preferences);

        if(points.size() == 0) greeter.setVisibility(VISIBLE);
        checkIfVersionIsNew();
    }

    private void loadSickName(SharedPreferences preferences) {
        if(!preferences.contains(SHARED_PREFS_SICKNAME)) return;
        String name = preferences.getString(SHARED_PREFS_SICKNAME, null);
        if(name != null) {
            new SickPersonDaoGetByName(this).execute(name);
        }
    }

    private void loadSavedPoints(SharedPreferences preferences) {
        //разворачиваем сохранённые точки, только если есть имя пользователя
        if(!preferences.contains(SHARED_PREFS_POINTS)&&(App.getInstance().getPerson() == null)) return;

        greeter.setVisibility(GONE);
        final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.US);
        Set<String> pointsSet = preferences.getStringSet(SHARED_PREFS_POINTS, null);
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
                        Date date1 = format.parse(point1.getDate() + " " + point1.getTime());
                        Date date2 = format.parse(point2.getDate() + " " + point2.getTime());
                        return (date1.compareTo(date2));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    return 1;
                }
            });
            //рисуем view
            for(Point p : points) {
                ConstraintLayout line = generatorHelper.createPoint(p, true);
                mainLinesHolder.addView(line);
            }
            //прокручивает ScrollView вниз
            scrollDown();
        }
    }

    private DisplayMetrics getScreenMetrics() {
        DisplayMetrics metr = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metr);
        return metr;
    }

    private void initUI() {
        mainLinesHolder = findViewById(R.id.mainTempLinesHolder);
        greeter = findViewById(R.id.mainGreetHolder);
        findViewById(R.id.createPoint).setOnClickListener(this);
    }

    private void checkIfVersionIsNew() {
        long savedVersion = getPreferences(Activity.MODE_PRIVATE).getLong(VERSION_KEY, 0);
        long currentVersion = 0;
        try {
            currentVersion = PackageInfoCompat.getLongVersionCode(getPackageManager().getPackageInfo(getPackageName(), 0));
        } catch (PackageManager.NameNotFoundException e) {e.printStackTrace();}
        if(currentVersion > savedVersion) {
            showWhatsNewDialog();

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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
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
                ConstraintLayout tLayout = (ConstraintLayout)v;

                Point currentPoint = generatorHelper.findCurrentPoint(tLayout);
                HashMap<String, Object> data = currentPoint.getData();

                Intent intent = new Intent(getApplicationContext(), FocusPoint.class);

                intent.putExtra(EXTRA_ALL, data);
                intent.putExtra(EXTRA_ID, (long)tLayout.getTag());

                startActivityForResult(intent, REQUEST_CODE_FOCUSPOINT);
                break;
            }
        }
    }

    @Override
    public void setPerson(SickPerson person) {
        App.getInstance().setPerson(person);
        //метод не вызывался при загрузке SharedPrefs, когда меню ещё не создано
        if(menu != null) changeTitle();
    }

    private class PointGeneratorHelper {

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
                            oldPointDte = dateFormat.parse(points.get(i).getDate());
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
                                    oldPointTime = timeFormat.parse(points.get(i).getTime());
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
            line.setOnClickListener(MainActivity.this);
            return new Object[]{line, position};
        }

        private ConstraintLayout createPoint(Point p, boolean restorePoint) {
            return (ConstraintLayout) createPoint(p.getTemperature(),
                    p.getDrugArray(),
                    p.getSymptArray(),
                    p.getTime(),
                    p.getDate(),
                    p.getTempId(),
                    p.getNote(),
                    restorePoint,
                    false)[0];
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

            if(p != null) p.setSymptArray(symptArray);
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

            if(p != null) p.setDrugArray(drugArray);
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

            if(p != null) p.setNote(note);
        }

        @SuppressWarnings("unchecked")
        void changeData(ConstraintLayout tLayout, String type, Object o) {

            Point currentPoint = findCurrentPoint(tLayout);

            switch (type) {
                case("drug"): {
                    ArrayList<String[]> drugArray = (ArrayList<String[]>) o;
                    fillDrugField(tLayout, currentPoint, drugArray, currentPoint.getTemperature());
                    break;
                }case("sympt"): {
                    ArrayList<String> symptArray = (ArrayList<String>) o;
                    fillSymptField(tLayout, currentPoint, symptArray, currentPoint.getTemperature());
                    break;
                }case("note"): {
                    String note = String.valueOf(o);
                    currentPoint.setNote(note);
                    fillNoteField(tLayout, currentPoint, note, currentPoint.getTemperature());
                    break;
                }
            }
        }

        Point findCurrentPoint(ConstraintLayout tLayout) {
            long tempId = (long) tLayout.getTag();
            Point currentPoint = null;

            //находим в списке текущую точку
            for(Point p : points) {
                if(p.getTempId() == tempId) {
                    currentPoint = p;
                    break;
                }
            }
            return currentPoint;
        }
    }

    //изменяет title, когда определяется больной
    public void changeTitle() {
        boolean needToLoadLines = Integer.valueOf(PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString(Preferences.CHANGE_NAME, "0")) == 1;
        setTitle();
        if((!needToLoadLines)||(App.getInstance().getPerson() == null)) return;

        new GetPointWithPersonAndLastDate(this).execute(App.getInstance().getPerson().getSickId());
    }

    //меняет title и делает видимым пункт меню "сменить больного"
    public void setTitle() {
        //очищаем главный экран и список точек
        clearMainScreen(true);
        MenuItem changePerson = menu.findItem(R.id.menu_change);
        if(App.getInstance().getPerson() != null) {
            setTitle(App.getInstance().getPerson().getName() + " болеет :(");
            if (!changePerson.isVisible()) changePerson.setVisible(true);
        } else {
            setTitle("Ти");
            if (changePerson.isVisible()) changePerson.setVisible(false);
        }
    }

    //вызывается getPointWithPersonAndLastDate после получения записей
    public void addPointsToField(ArrayList<Point> points) {
        greeter.setVisibility(points.size() > 0 ? GONE : VISIBLE);
        int counter = numOfLines;
        for(int i=points.size()-1; i>=0; i--) {
            if(counter == 0) break;
            ConstraintLayout point = generatorHelper.createPoint(points.get(i), false);
            mainLinesHolder.addView(point, 0);
            if(counter > -1) counter--;
        }
        scrollDown();
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
                if(bundle.getBoolean("changedPerson")) setTitle();
                if(greeter.getVisibility() == VISIBLE) greeter.setVisibility(GONE);
                //создаём точку
                Object[] createdThing = generatorHelper.createPoint(nowTemp, drugArray, symptArray, time, date, tempId, note, false, changedDateTime);

                putLineToHolder(createdThing);
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
                            points.remove(generatorHelper.findCurrentPoint(tLayout));
                            mainLinesHolder.removeView(tLayout);
                            if(points.size() == 0) greeter.setVisibility(VISIBLE);
                            break;
                        }
                        //если есть экстра с лекарствами, симптомами или заметками...
                        if(data.hasExtra(FocusPoint.EXTRA_DRUGS)) {
                            //получаем новый массив лекарств в переменную
                            @SuppressWarnings("unchecked")
                            ArrayList<String[]> drugArray = (ArrayList<String[]>) data.getSerializableExtra(FocusPoint.EXTRA_DRUGS);
                            generatorHelper.changeData(tLayout, "drug", drugArray);
                        }
                        if(data.hasExtra(FocusPoint.EXTRA_SYMPTS)) {
                            ArrayList<String> symptArray = data.getStringArrayListExtra(FocusPoint.EXTRA_SYMPTS);
                            generatorHelper.changeData(tLayout, "sympt", symptArray);
                        }
                        if(data.hasExtra(FocusPoint.EXTRA_NOTE)) {
                            String newNote = data.getStringExtra(FocusPoint.EXTRA_NOTE);
                            generatorHelper.changeData(tLayout, "note", newNote);
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
                if(data.getBooleanExtra(History.EXTRA_CHANGE, false)) setTitle();
                else if(data.hasExtra(History.EXTRA_SHOW_ON_MAIN)) {
                    SickPerson personToShow = (SickPerson) data.getSerializableExtra(History.EXTRA_CHOSEN_NAME);
                    ArrayList<Point> poinsToShow = (ArrayList<Point>) data.getSerializableExtra(History.EXTRA_SHOW_ON_MAIN);
                    SickPerson appPerson = App.getInstance().getPerson();
                    if((appPerson == null)||(!appPerson.equals(personToShow))) {
                        App.getInstance().setPerson(personToShow);
                        changeTitle();
                    }
                    clearMainScreen(false);
                    if(poinsToShow.size() > numOfLines) {
                        numOfLines = findNeededMinLinesNum(poinsToShow.size());
                        PreferenceManager.getDefaultSharedPreferences(this).edit()
                                .putString(Preferences.NUM_LINES, String.valueOf(numOfLines))
                                .apply();
                    }
                    for (Point point : poinsToShow) {
                        mainLinesHolder.addView(generatorHelper.createPoint(point, false));
                    }
                    scrollDown();
                }
            }
        }
    }

    private int findNeededMinLinesNum(int size) {
        String[] numLinesArray = getResources().getStringArray(R.array.pref_values_lines);
        for(String stringNum : numLinesArray) {
            int nowNum = Integer.valueOf(stringNum);
            if(nowNum >= size) return nowNum;
        }
        return -1;
    }

    //добавляет запись на главный экран с учётом позиции
    private void putLineToHolder(Object[] createdThing) {
        int position = (int)createdThing[1];
        ConstraintLayout line = (ConstraintLayout) createdThing[0];

        if(position < 0) mainLinesHolder.addView(line);
        else mainLinesHolder.addView(line, position);
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
                new PickSickDialog().show(getSupportFragmentManager(), "PickSickDialog");
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
        editor.putStringSet(SHARED_PREFS_POINTS, pointsSet);
        if(App.getInstance().getPerson() != null) editor.putString(SHARED_PREFS_SICKNAME, App.getInstance().getPerson().getName());
        editor.apply();
    }

    //При выборе больного в диалоге
    @Override
    public void onDialogPositiveClick(DialogFragment dialog, SickPerson person) {
        if(!person.isExist()) {
            new InsertPersonInDB(this).execute(person);
        } else {
            setPerson(person);
        }
    }

    //при выборе метода очистки экрана
    @Override
    public void onDialogPositiveClick(DialogFragment dialog, int what) {
        if(points.size() == 0) return;
        switch (what) {
            case ClearScreenDialog.DIALOG_CLEAR_ALL: {
                clearMainScreen(true);
                break;
            }
            case ClearScreenDialog.DIALOG_CLEAR_BUT: {
                removeLinesExceptLast();
                break;
            }
        }
    }

    private void removeLinesExceptLast() {
        try {
            Date lastDate = dateFormat.parse(points.get(points.size()-1).getDate());
            int lastRemovePoint = 0;
            for(int i=points.size() - 2; i>=0; i--) {
                Date date = dateFormat.parse(points.get(i).getDate());
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
    }

    private void clearMainScreen(boolean showGreeter) {
        mainLinesHolder.removeAllViews();
        points.clear();
        greeter.setVisibility(showGreeter ? VISIBLE : GONE);
    }


    static private class SickPersonDaoGetByName extends AsyncTask<String, Void, SickPerson> {

        private WeakReference<MainActivity> wrActivity;

        public SickPersonDaoGetByName(MainActivity activity) {
            this.wrActivity = new WeakReference<>(activity);
        }

        @Override
        protected SickPerson doInBackground(String... name) {
            return wrActivity.get().database.sickPersonDao().getByName(name[0]);
        }

        @Override
        protected void onPostExecute(SickPerson sickPerson) {
            wrActivity.get().setPerson(sickPerson);
        }
    }

    static private class GetPointWithPersonAndLastDate extends AsyncTask<Long, Void, ArrayList<Point>> {

        private WeakReference<MainActivity> wrActivity;

        public GetPointWithPersonAndLastDate(MainActivity activity) {
            this.wrActivity = new WeakReference<>(activity);
        }

        protected ArrayList<Point> doInBackground(Long... id) {
            ArrayList<Point> points = new ArrayList<>();
            //получаем список температур с последней датой
            ArrayList<Temperature> tempList = (ArrayList<Temperature>) wrActivity.get().database.temperatureDao()
                    .getAllWithPersonAndLastDate(id[0]);
            if(tempList.size() > 0) {
                for(Temperature t : tempList) {
                    ArrayList<String[]> drugList = new ArrayList<>();
                    ArrayList<ConcreteTemperatureDrug> drugForTemperature = (ArrayList<ConcreteTemperatureDrug>)wrActivity.get().database.personDrugDao().getConcreteTemperatureDrug(t.tempId);
                    for(ConcreteTemperatureDrug cd : drugForTemperature) drugList.add(new String[]{cd.drug_name, cd.amount, cd.drugUnit});
                    ArrayList<String> symptList = (ArrayList<String>)wrActivity.get().database.personSymptomDao().getDatePersonSympt(t.tempId);
                    Note n = wrActivity.get().database.noteDao().getById(t.tempId);
                    String note = (n == null ? "" : n.note);
                    points.add(new Point(t.time, t.date, t.temperature, drugList, symptList, t.tempId, note));
                }
            }
            return points;
        }

        @Override
        protected void onPostExecute(ArrayList<Point> points) {
            wrActivity.get().addPointsToField(points);
        }
    }

    static public class InsertPersonInDB extends AsyncTask<SickPerson, Void, SickPerson> {

        WeakReference<PersonSettable> wrActivity;
        AppDatabase database;

        public InsertPersonInDB(PersonSettable activity) {
            this.wrActivity = new WeakReference<>(activity);
            this.database = App.getInstance().getDatabase();
        }

        @Override
        protected SickPerson doInBackground(SickPerson... sickPeople) {
            sickPeople[0].setSickId(database.sickPersonDao().insert(sickPeople[0]));
            return sickPeople[0];
        }

        @Override
        protected void onPostExecute(SickPerson person) {
            wrActivity.get().setPerson(person);
        }
    }
}
