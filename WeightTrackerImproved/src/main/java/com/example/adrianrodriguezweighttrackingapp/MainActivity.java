package com.example.adrianrodriguezweighttrackingapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// Firebase stuff for signing in users
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;

// This is the main screen for our app.
// It lets users log in or create a new account.
public class MainActivity extends AppCompatActivity {

    // These are for typing in email and password
    private EditText editTextEmail;
    private EditText editTextPassword;

    // This handles all the user login/signup stuff with Firebase
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Connects to our main screen layout

        // Get the Firebase authenticator ready
        mAuth = FirebaseAuth.getInstance();

        // Link our Java code to the design elements in activity_main.xml
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        Button buttonLogin = findViewById(R.id.buttonLogin);
        Button buttonCreate = findViewById(R.id.buttonCreate);

        // What happens when the login button is tapped
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser(); // Call our login function
            }
        });

        // What happens when the create account button is tapped
        buttonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createUser(); // Call our create user function
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if someone is already logged in when the app starts
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser != null) {
            // If they are, show a quick message
            Toast.makeText(MainActivity.this, "Welcome back, " + currentUser.getEmail() + "!", Toast.LENGTH_SHORT).show();
            // And send them straight to the main data screen
            startActivity(new Intent(MainActivity.this, DataDisplayActivity.class));
            finish(); // Close this login screen so they can't go back with the back button
        }
    }

    // Tries to log a user in
    private void loginUser() {
        // Grab text from email and password boxes
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Make sure both fields aren't empty
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(MainActivity.this, "Please fill in both email and password.", Toast.LENGTH_SHORT).show();
            return; // Stop here if something's missing
        }

        // Try to sign in with Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Success! Get the user info
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(MainActivity.this, "Logged in as " + user.getEmail(), Toast.LENGTH_SHORT).show();

                        // Go to the main app screen
                        Intent intent = new Intent(MainActivity.this, DataDisplayActivity.class);
                        startActivity(intent);
                        finish(); // Close this activity
                    } else {
                        // Oh no, login failed. Let's see why.
                        String errorMessage = "Login failed. Check your email and password.";
                        if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                            errorMessage = "No account found with that email.";
                        } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            errorMessage = "Wrong password.";
                        } else if (task.getException() != null) {
                            // For other errors, just show what Firebase says
                            errorMessage = task.getException().getMessage();
                        }
                        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Tries to create a new user account
    private void createUser() {
        // Get text from email and password boxes
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Check if email or password are empty
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(MainActivity.this, "Please enter an email and password.", Toast.LENGTH_SHORT).show();
            return; // Stop if inputs are missing
        }

        // Passwords need to be at least 6 characters long for Firebase
        if (password.length() < 6) {
            Toast.makeText(MainActivity.this, "Password needs to be at least 6 characters.", Toast.LENGTH_SHORT).show();
            return; // Stop if password is too short
        }

        // Try to make a new user with Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Account made. Firebase signs them in automatically.
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(MainActivity.this, "Account created for " + user.getEmail(), Toast.LENGTH_SHORT).show();

                        // Go to the main app screen
                        Intent intent = new Intent(MainActivity.this, DataDisplayActivity.class);
                        startActivity(intent);
                        finish(); // Close this activity
                    } else {
                        // Something went wrong creating the account
                        String errorMessage = "Couldn't create account.";
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            errorMessage = "This email is already used.";
                        } else if (task.getException() != null) {
                            // Show what Firebase says the error is
                            errorMessage = task.getException().getMessage();
                        }
                        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
