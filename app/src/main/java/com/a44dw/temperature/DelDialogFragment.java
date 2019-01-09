package com.a44dw.temperature;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

public class DelDialogFragment extends DialogFragment {

    DelDialogListener dListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int textToShow = getArguments().getInt("textToShow");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.deldialog_title)
                .setMessage(textToShow)
                .setPositiveButton(R.string.deldialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dListener.onDialogPositiveClick(DelDialogFragment.this);
                    }
                })
                .setNegativeButton(R.string.deldialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dListener.onDialogNegativeClick(DelDialogFragment.this);
                    }
                });
        return builder.create();
    }

    public interface DelDialogListener {
        void onDialogPositiveClick(DialogFragment dialog);
        void onDialogNegativeClick(DialogFragment dialog);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        dListener = (DelDialogListener)getActivity();
    }
}
