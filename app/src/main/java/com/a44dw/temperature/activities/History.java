package com.a44dw.temperature.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.a44dw.temperature.App;
import com.a44dw.temperature.dialogs.DelDialog;
import com.a44dw.temperature.database.AppDatabase;
import com.a44dw.temperature.R;
import com.a44dw.temperature.fragments.SickNamesHistoryFragment;
import com.a44dw.temperature.entities.SickPerson;
import com.a44dw.temperature.fragments.TableHistoryFragment;
import com.a44dw.temperature.pojo.Point;

import java.util.ArrayList;

public class History extends AppCompatActivity
        implements DelDialog.DelDialogListener,
        TableHistoryFragment.OnHistoryTableActionListener,
        View.OnClickListener {

    public static final String EXTRA_CHANGE = "change";
    public static final String EXTRA_SHOW_ON_MAIN = "showOnMain";
    public static final String EXTRA_CHOSEN_NAME = "name";
    private static final String TAG_DEL_DIALOG = "delDialog";

    private AppDatabase database;
    private FragmentTransaction transaction;
    private LinearLayout layoutToDel;
    private SickPerson chosenPerson;
    private boolean changedPerson = false;
    private ActionBar bar;

    private TextView showPointsOnMain;
    private int selectBoxesCheckedCounter = 0;
    private static DisplayMetrics metrics = new DisplayMetrics();

    TableHistoryFragment tableHistoryFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        database = App.getInstance().getDatabase();

        prepareActionBar();
        initUI();

        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        showNamesFragment();
    }

    private void initUI() {
        showPointsOnMain = findViewById(R.id.historyShowOnMainScreen);
        showPointsOnMain.setOnClickListener(this);
    }

    private void showNamesFragment() {
        transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragmentHolder, new SickNamesHistoryFragment());
        transaction.commit();
    }

    private void prepareActionBar() {
        //редактируем ActionBar:
        bar = getSupportActionBar();
        if(bar != null) {
            bar.setElevation(0);
            bar.setTitle("");
            //вешаем стрелку назад
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDisplayShowHomeEnabled(true);
        }
    }

    //При нажатии на кнопку "Назад"
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CHANGE, changedPerson);
        setResult(RESULT_OK, intent);
        finish();
        return true;
    }

    public void onClickListener(View view) {
        switch (view.getId()) {
            //При нажатии на имя...
            case(R.id.sickName): {
                TextView nameHolder = (TextView) view;
                chosenPerson = (SickPerson) ((LinearLayout)nameHolder.getParent().getParent()).getTag();

                showHistoryFragment();

                if(bar != null) bar.setTitle(chosenPerson.getName());
                showPointsOnMain.setVisibility(View.VISIBLE);
                break;
            }
            //при нажатии на крестик рядом с именем
            case(R.id.sickNameDel): {
                //"запоминаем" имя, к-е будем удалять)
                layoutToDel = (LinearLayout)view.getParent().getParent();
                showDelDialog();
                break;
            }
            //При нажатии на строку с датой...
            case(R.id.historyTableDateHolder): {
                ConstraintLayout subHolder = (ConstraintLayout) view.getParent();
                //получаем родителя TextView
                ConstraintLayout mainHolder = (ConstraintLayout) subHolder.getParent();
                //получаем таблицу в его иерархии
                TableLayout table = mainHolder.findViewById(R.id.historyTable);
                //включаем/выключаем таблицу
                if(table.getVisibility() == View.GONE) {
                    table.setVisibility(View.VISIBLE);
                    subHolder.setBackgroundColor(getResources().getColor(R.color.dateBlueDark));
                } else {
                    table.setVisibility(View.GONE);
                    subHolder.setBackgroundColor(getResources().getColor(R.color.dateBlue));
                }
                break;
            }
            //При нажатии на примечание
            case(R.id.historyNote): {
                ImageView noteIcon = (ImageView)view;
                //если сегмент без тэга "заметка", ничего не происходит
                if(noteIcon.getTag() == null) break;
                TableRow row = (TableRow) noteIcon.getParent();
                TableLayout table = (TableLayout) row.getParent();
                int index = table.indexOfChild(row);
                TableRow noteRow = (TableRow) table.getChildAt(++index);
                if(noteRow.getVisibility() == View.GONE) {
                    noteRow.setVisibility(View.VISIBLE);
                    noteIcon.setImageResource(R.drawable.ic_writing_white);
                    noteIcon.setBackgroundColor(getResources().getColor(R.color.light_blue));
                } else {
                    noteRow.setVisibility(View.GONE);
                    noteIcon.setImageResource(R.drawable.ic_writing_on);
                    noteIcon.setBackgroundColor(getResources().getColor(R.color.def));
                }
                break;
            }
        }
    }

    private void showDelDialog() {
        //Создаём диалог и прикладываем к нему ссылку на текст, к-й нужно показать
        DialogFragment dialog = new DelDialog();
        Bundle args = new Bundle();
        args.putInt(DelDialog.TEXT_TO_SHOW, R.string.deldialog_shure_del_name);
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), TAG_DEL_DIALOG);
    }

    private void showHistoryFragment() {
        tableHistoryFragment = new TableHistoryFragment();
        tableHistoryFragment.setHistoryTableActionListener(this);
        transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentHolder, tableHistoryFragment);
        transaction.commit();
    }

    //Если пользователь кликнул в диалоге удаления больного на "ОК"...
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        deleteSickPerson();
    }

    private void deleteSickPerson() {
        //если удаляемый больной = текущий выбранный, удаляем выбранного
        final SickPerson personToDel = (SickPerson) layoutToDel.getTag();
        SickPerson nowPerson = App.getInstance().getPerson();
        if(nowPerson != null) {
            if (personToDel.equals(nowPerson)) {
                App.getInstance().delPerson();
                changedPerson = true;
            }
        }
        //удаляем из базы
        new Thread(new Runnable() {
            @Override
            public void run() {
                database.sickPersonDao().deleteByName(personToDel.getName());
            }
        }).start();
        //удаляем строку с именем
        LinearLayout parentHolder = (LinearLayout) layoutToDel.getParent();
        parentHolder.removeView(layoutToDel);
    }

    //должно вызываться при смене ориентации устройства.
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        LinearLayout holder = findViewById(R.id.tableHolder);
        ConstraintLayout warning = findViewById(R.id.warningLayout);
        if((metrics.widthPixels < 700)&&(holder != null)) {
            warning.setVisibility(View.VISIBLE);
            holder.setVisibility(View.GONE);
        } else if ((metrics.widthPixels > 700)&&(holder != null)) {
            warning.setVisibility(View.GONE);
            holder.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {}

    @Override
    public void onDateSelectCheckedChanged(boolean checked) {
        if(checked) selectBoxesCheckedCounter++;
        else selectBoxesCheckedCounter--;
        if(selectBoxesCheckedCounter > 0) {
            showPointsOnMain.setTypeface(null, Typeface.BOLD);
        } else if(selectBoxesCheckedCounter == 0)
            showPointsOnMain.setTypeface(null, Typeface.NORMAL);
    }

    @Override
    public void onNeedToChangeOrientation(boolean isNeed) {
        if(isNeed) showPointsOnMain.setVisibility(View.GONE);
        else showPointsOnMain.setVisibility(View.VISIBLE);
    }

    @Override
    public SickPerson getChosenPerson() {
        return chosenPerson;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.historyShowOnMainScreen: {
                if(selectBoxesCheckedCounter == 0) return;
                ArrayList<Point> pointsToShow = (ArrayList<Point>) tableHistoryFragment.getSelectedRecords();
                Intent intent = new Intent();
                intent.putExtra(EXTRA_CHANGE, false);
                intent.putExtra(EXTRA_CHOSEN_NAME, chosenPerson);
                intent.putExtra(EXTRA_SHOW_ON_MAIN, pointsToShow);
                setResult(RESULT_OK, intent);
                finish();
            }
        }
    }
}
