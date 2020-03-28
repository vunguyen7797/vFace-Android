/*
 * ManageAccountActivity.java
 */
package com.vunguyen.vface.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.squareup.picasso.Picasso;
import com.vunguyen.vface.R;
import com.vunguyen.vface.helper.LocaleHelper;

/**
 * This class implements methods to edit the user account profile
 */
public class ManageAccountActivity extends AppCompatActivity
{
    String account;
    TextInputEditText etDisplayName;
    ImageView ivProfilePhoto;

    private static final int GALLERY_REQUEST_CODE = 1;
    Uri profileUri = null;
    FirebaseUser user;
    boolean newProfilePhoto = false;
    private static FirebaseStorage storage = FirebaseStorage.getInstance();

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "en"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_account);

        account = getIntent().getStringExtra("ACCOUNT");
        // get current logged in user from firebase
        user = FirebaseAuth.getInstance().getCurrentUser();

        etDisplayName = findViewById(R.id.etDisplayName);
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);

        newProfilePhoto = setPhotoAfterCropped();
        if (newProfilePhoto)
        {
            displayInfo(0); // display without photo
        }
        else
            displayInfo(1); // display with photo
    }

    // set the cropped image to image view for later update
    private boolean setPhotoAfterCropped()
    {
        boolean indicator;
        profileUri = getIntent().getData();
        Log.i("EXECUTE", "URI AFTER CROPPED: " + profileUri);
        if (profileUri != null)
        {
            Picasso.get().load(profileUri).into(ivProfilePhoto);
            indicator = true;
        }
        else
            indicator = false;
        return indicator;
    }

    // display current information from database of the user
    private void displayInfo(int option)
    {
        if (user != null && user.getDisplayName() != null)
        {
            etDisplayName.setText(user.getDisplayName());
        }
        else
            Log.i("EXECUTE", "NAME IS NULL");

        if (user!= null && user.getPhotoUrl() != null && option == 1)
        {
            profileUri = user.getPhotoUrl();
            Log.i("EXECUTE", "LOADED URI: " + profileUri);
            if (profileUri != null )
                Picasso.get().load(profileUri).into(ivProfilePhoto);
        }
        else
            Log.i("EXECUTE", "PHOTO IS NULL");
    }

    // Response from picking image activities
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK) {
            profileUri = data.getData();
            Log.i("EXECUTE", "URI PICKED: " + profileUri);
            // go to the crop image view activity to crop photo after image was picked
            Intent intent = new Intent(ManageAccountActivity.this, CropImageActivity.class);
            intent.setData(profileUri);
            goToActivity(intent);
        } else if (resultCode != RESULT_OK) {
            Log.i("EXECUTE", "NO IMAGE CHOSEN");
            recreate();
        }
    }

    // Update display name and profile photo to firebase database
    public void updateProfile()
    {
        if (etDisplayName.getText() != null)
        {
            Log.i("EXECUTE", "Uri updated: " + profileUri);
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(etDisplayName.getText().toString())
                    .setPhotoUri(profileUri)
                    .build();

            assert user != null;
            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task ->
                    {
                        if (task.isSuccessful())
                        {
                            Toast.makeText(getApplicationContext(),
                                    R.string.profile_updated, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
        else
            etDisplayName.requestFocus();
    }

    // go to another activity
    private void goToActivity(Intent intent)
    {
        intent.putExtra("ACCOUNT", account);
        startActivity(intent);
        finish();
    }

    // click the back button on navigation bar
    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(ManageAccountActivity.this, SettingsActivity.class);
        goToActivity(intent);
    }

    // click on back arrow button
    public void btnBackClick(View view)
    {
        onBackPressed();
    }

    // click the Done button
    public void btnUpdateProfile(View view)
    {
        updateProfile();
    }

    // click the photo to choose image
    public void btnPickImage(View view)
    {
        // Open the gallery to choose image
        Intent intPickImage = new Intent(Intent.ACTION_PICK);
        intPickImage.setType("image/*");
        String[] mimeTypes = {"image/jpeg", "image/png"};
        intPickImage.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intPickImage, GALLERY_REQUEST_CODE);
    }
}
