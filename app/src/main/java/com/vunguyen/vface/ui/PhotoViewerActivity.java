package com.vunguyen.vface.ui;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.vunguyen.vface.R;
import com.vunguyen.vface.utils.base.PublicMethods;

import static com.vunguyen.vface.utils.base.Cons.IMG_EXTRA_KEY;
import static com.vunguyen.vface.utils.base.Cons.IMG_FILE;

public class PhotoViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_viewer);

        if (getIntent().hasExtra(IMG_EXTRA_KEY)) {
            ImageView imageView = findViewById(R.id.image);
            String imagePath = getIntent().getStringExtra(IMG_EXTRA_KEY);
            Uri uriImage = Uri.parse(imagePath);
            Log.i("EXECUTE", "URI: " + uriImage);
            imageView.setImageBitmap(PublicMethods.getBitmapByPath(imagePath, IMG_FILE));
        }
    }
}
