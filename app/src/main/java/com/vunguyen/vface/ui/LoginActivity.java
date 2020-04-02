/*
 * LoginActivity.java
 */
package com.vunguyen.vface.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.vunguyen.vface.R;
import com.vunguyen.vface.helper.LocaleHelper;

/**
 * This class is to implement functions for the login screen activity
 */
public class LoginActivity extends AppCompatActivity
{
    ImageView ivBackArrow;
    Button btnLogin;
    TextView tvRegister;
    EditText emailID, password;
    FirebaseAuth firebaseAuth;
    TextView tvForgotPwd;

    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "en"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // use firebase service for user authentication
        firebaseAuth = FirebaseAuth.getInstance();
        authStateListener = firebaseAuth ->
        {
            FirebaseUser firebaseUser = this.firebaseAuth.getCurrentUser();
            if(firebaseUser != null)
            {
                openDashBoard(firebaseUser.getEmail().replaceAll("[.]",""));
            }
            else
            {

                Toast.makeText(LoginActivity.this, getResources().getString(R.string.please_log_in_toast), Toast.LENGTH_SHORT).show();
            }
        };

        emailID = findViewById(R.id.emailID);
        password = findViewById(R.id.password);

        // back arrow button
        ivBackArrow = findViewById(R.id.ivBackArrow);
        ivBackArrow.setOnClickListener(v -> openWelcomeScreen());

        clickLogin();

        // Register a new account button
        tvRegister = findViewById(R.id.tvRegister);
        tvRegister.setOnClickListener(v -> openRegister());

        // forgot password text button
        tvForgotPwd = findViewById(R.id.tvForgotPassword);
        tvForgotPwd.setOnClickListener(v -> {
            Intent intoReset = new Intent(LoginActivity.this, ResetPasswordActivity.class);
            startActivity(intoReset);
            finish();
        });

    }

    // event for login button
    private void clickLogin()
    {
        btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(v -> {
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
                Toast.makeText(LoginActivity.this, "Fields are empty!",Toast.LENGTH_SHORT).show();
            }
            else if (!email.isEmpty() && !pwd.isEmpty())
            {
                firebaseAuth.signInWithEmailAndPassword(email,pwd).addOnCompleteListener(LoginActivity.this, task -> {
                    if (!task.isSuccessful())
                    {
                        Toast.makeText(LoginActivity.this,
                                "Login failed. Your email or password is incorrect. Please try again!", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        openDashBoard(email.toLowerCase().replaceAll("[.]",""));
                    }
                });
            }
            else
            {
                Toast.makeText(LoginActivity.this, "Error Occurred!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    public void openWelcomeScreen()
    {
        Intent intent = new Intent(this, WelcomeScreenActivity.class);
        startActivity(intent);
        finish();
    }

    public void openDashBoard(String email)
    {
        Intent intent = new Intent(this, DashBoardActivity.class);
        intent.putExtra("ACCOUNT", email);
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
