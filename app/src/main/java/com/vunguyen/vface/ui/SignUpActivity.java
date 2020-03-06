package com.vunguyen.vface.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.vunguyen.vface.R;

public class SignUpActivity extends AppCompatActivity
{
    ImageView ivBackArrow;
    TextView tvLogin;
    EditText emailID, password;
    Button btnSignUp;
    FirebaseAuth mFirebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_sign_up);

        mFirebaseAuth = FirebaseAuth.getInstance();
        emailID = findViewById(R.id.emailID);
        btnSignUp = findViewById(R.id.btnSignUp);

        btnSignUp.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                String email = emailID.getText().toString();
                String pwd = password.getText().toString();

                if (email.isEmpty())
                {
                    emailID.setError("Please enter your email");
                    emailID.requestFocus();
                }
                else if(pwd.isEmpty())
                {
                    password.setError("Please enter your password");
                    password.requestFocus();
                }
                else if (email.isEmpty() && pwd.isEmpty())
                {
                    Toast.makeText(SignUpActivity.this, "Fields are empty!",Toast.LENGTH_SHORT).show();
                }
                else if (!email.isEmpty() && !pwd.isEmpty())
                {
                   mFirebaseAuth.createUserWithEmailAndPassword(email,pwd).addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                       @Override
                       public void onComplete(@NonNull Task<AuthResult> task) {
                           if (!task.isSuccessful())
                           {
                               Toast.makeText(SignUpActivity.this, "Register failed. Please try again!",Toast.LENGTH_SHORT).show();
                           }
                           else
                           {
                               Toast.makeText(SignUpActivity.this, "Registered Successful. Please log in.", Toast.LENGTH_SHORT).show();
                               startActivity(new Intent(SignUpActivity.this, Login2Activity.class));
                           }
                       }
                   });
                }
                else
                {
                    Toast.makeText(SignUpActivity.this, "Error Occurred!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        password = findViewById(R.id.password);


        ivBackArrow = findViewById(R.id.ivBackArrow);
        ivBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWelcome();
            }
        });

        tvLogin = findViewById(R.id.txtLogin);
        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLogin();
            }
        });
    }

    public void openWelcome()
    {
        Intent intent = new Intent(this, WelcomeScreenActivity.class );
        startActivity(intent);
        finish();
    }

    public void openLogin()
    {
        Intent intent = new Intent(this, Login2Activity.class);
        startActivity(intent);
        finish();
    }
}
