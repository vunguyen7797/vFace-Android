/*
 * BaseActivity.java
 */
package com.vunguyen.vface.utilsFirebaseVision.base;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * This class is a base activity
 */
public class BaseActivity extends AppCompatActivity
{
    protected Activity baseActivity = this;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

    }
}
