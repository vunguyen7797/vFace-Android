package com.vunguyen.vface.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.vunguyen.vface.R;

public class ResetPasswordActivity extends AppCompatActivity {
    EditText emailID;
    Button btnSend;
    FirebaseAuth mFirebaseAuth;
    ImageView ivBackArrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_reset_password);


        mFirebaseAuth = FirebaseAuth.getInstance();

        emailID = findViewById(R.id.emailID);
        btnSend = findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailID.getText().toString();

                if (email.isEmpty())
                {
                    Toast.makeText(ResetPasswordActivity.this, "Please enter your valid email address", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    mFirebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful())
                            {
                                Toast.makeText(ResetPasswordActivity.this, "Reset link is sent to your email.", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(ResetPasswordActivity.this, Login2Activity.class));
                            }
                            else
                            {
                                String message = task.getException().getMessage();
                                Toast.makeText(ResetPasswordActivity.this, "Error Occurred!", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });
                }

            }
        });

        ivBackArrow = findViewById(R.id.ivBackArrow);
        ivBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ResetPasswordActivity.this, Login2Activity.class));
                finish();
            }
        });
    }
}
