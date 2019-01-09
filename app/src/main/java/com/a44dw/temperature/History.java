package com.a44dw.temperature;

import android.content.Intent;
import android.content.res.Configuration;
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

public class History extends AppCompatActivity
        implements DelDialogFragment.DelDialogListener {

    AppDatabase database;
    FragmentTransaction transaction;
    //Выбранное имя, будет использоваться в TableHistoryFragment
    static String chosenName;
    LinearLayout nameToDel;
    boolean changedPerson;
    ActionBar bar;
    public static final String EXTRA_CHANGE = "change";
    private static DisplayMetrics metrics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        database = App.getInstance().getDatabase();
        //редактируем ActionBar:
        bar = getSupportActionBar();
        if(bar != null) {
            bar.setElevation(0);
            bar.setTitle("");
            //вешаем стрелку назад
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDisplayShowHomeEnabled(true);
        }
        metrics = new DisplayMetrics();
        changedPerson = false;
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragmentHolder, new SickNamesHistoryFragment());
        transaction.commit();
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
                //Заполняем переменную выбранного имени для TableHistoryFragment
                chosenName = nameHolder.getText().toString();
                //прячем дескриптор и поля с прочими именами
                transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragmentHolder, new TableHistoryFragment());
                transaction.commit();
                if((bar != null)&&(chosenName != null))bar.setTitle(chosenName);
                break;
            }
            //при нажатии на крестик рядом с именем
            case(R.id.sickNameDel): {
                //"запоминаем" имя, к-е будем удалять)
                nameToDel = (LinearLayout) view.getParent().getParent();
                //Создаём диалог и прикладываем к нему ссылку на текст, к-й нужно показать
                DialogFragment dialog = new DelDialogFragment();
                Bundle args = new Bundle();
                args.putInt("textToShow", R.string.deldialog_shure_del_name);
                dialog.setArguments(args);
                dialog.show(getSupportFragmentManager(), "DelDialogFragment");
                break;
            }
            //При нажатии на строку с датой...
            case(R.id.historyTableDateHolder): {
                //получаем родителя TextView
                LinearLayout holder = (LinearLayout) view.getParent();
                //получаем таблицу в его иерархии
                TableLayout table = holder.findViewById(R.id.historyTable);
                //включаем/выключаем таблицу
                if(table.getVisibility() == View.GONE) {
                    table.setVisibility(View.VISIBLE);
                    view.setBackgroundColor(getResources().getColor(R.color.dateBlueDark));
                } else {
                    table.setVisibility(View.GONE);
                    view.setBackgroundColor(getResources().getColor(R.color.dateBlue));
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
    //Если пользователь кликнул в диалоге удаления больного на "ОК"...
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        //если удаляемый больной = текущий выбранный, удаляем выбранного
        final String name = nameToDel.getTag().toString();
        SickPerson nowPerson = App.getInstance().getPerson();
        if(nowPerson != null) {
            if (name.equals(nowPerson.name)) {
                App.getInstance().delPerson();
                changedPerson = true;
            }
        }
        //удаляем из базы
        new Thread(new Runnable() {
            @Override
            public void run() {
                database.sickPersonDao().deleteByName(name);
            }
        }).start();
        //удаляем строку с именем
        LinearLayout parentHolder = (LinearLayout) nameToDel.getParent();
        parentHolder.removeView(nameToDel);
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
}
