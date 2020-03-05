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
import com.google.firebase.auth.FirebaseUser;
import com.vunguyen.vface.R;

public class Login2Activity extends AppCompatActivity
{
    ImageView ivBackArrow;
    Button btnLogin;
    TextView tvRegister;
    EditText emailID, password;
    FirebaseAuth mFirebaseAuth;
    TextView tvForgotPwd;

    private FirebaseAuth.AuthStateListener mAuthStateListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //* Hide Notification bar
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_login2);

        mFirebaseAuth = FirebaseAuth.getInstance();
        mAuthStateListener = new FirebaseAuth.AuthStateListener()
        {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser mFirebaseUser = mFirebaseAuth.getCurrentUser();
                if(mFirebaseUser != null)
                {
                    Toast.makeText(Login2Activity.this, "You are logged in", Toast.LENGTH_SHORT).show();
                    openDashBoard();
                }
                else
                {
                    Toast.makeText(Login2Activity.this, "Please Log in", Toast.LENGTH_SHORT).show();
                }
            }
        };

        emailID = findViewById(R.id.emailID);
        password = findViewById(R.id.password);

        ivBackArrow = findViewById(R.id.ivBackArrow);
        ivBackArrow.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                openWelcomeScreen();
            }
        });

        btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(new View.OnClickListener()
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
                    Toast.makeText(Login2Activity.this, "Fields are empty!",Toast.LENGTH_SHORT).show();
                }
                else if (!email.isEmpty() && !pwd.isEmpty())
                {
                    mFirebaseAuth.signInWithEmailAndPassword(email,pwd).addOnCompleteListener(Login2Activity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful())
                            {
                                Toast.makeText(Login2Activity.this, "Login failed. Your email or password is incorrect. Please try again!", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                openDashBoard();
                            }

                        }
                    });
                }
                else
                {
                    Toast.makeText(Login2Activity.this, "Error Occurred!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        tvRegister = findViewById(R.id.tvRegister);
        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openRegister();
            }
        });

        tvForgotPwd = findViewById(R.id.tvForgotPassword);
        tvForgotPwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intoReset = new Intent(Login2Activity.this, ResetPasswordActivity.class);
                startActivity(intoReset);
                finish();
            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    public void openWelcomeScreen()
    {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    public void openDashBoard()
    {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void openRegister()
    {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
        finish();
    }
}
