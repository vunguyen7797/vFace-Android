package com.vunguyen.vface.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.view.LayoutInflaterFactory;

import com.vunguyen.vface.R;

public class ProgressDialogCustom
{
    private Activity activity;
    private Dialog dialog;

    public ProgressDialogCustom(Activity activity)
    {
        this.activity = activity;
    }

    public void startProgressDialog(String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);

        dialog = new Dialog(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.custom_progress_bar_dialog, null);
        if (message != null)
            ((TextView) view.findViewById(R.id.tvMessage)).setText(message);
        dialog.setCancelable(false);
        dialog.setContentView(view);
        dialog.show();
    }

    public void dismissDialog()
    {
        dialog.dismiss();
    }

    public void setCancelable(boolean request)
    {
        dialog.setCancelable(request);
    }
}
