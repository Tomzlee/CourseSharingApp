package com.example.coursesharingapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            // Set up toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                Log.d(TAG, "Toolbar set as support action bar");
            } else {
                Log.e(TAG, "Toolbar is null");
                Toast.makeText(this, "Toolbar not found", Toast.LENGTH_SHORT).show();
            }

            // Set up navigation
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);

            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
                Log.d(TAG, "NavController obtained from NavHostFragment");

                // Configure app bar to know about the nav destinations
                appBarConfiguration = new AppBarConfiguration.Builder(
                        R.id.loginFragment, R.id.homeFragment)
                        .build();

                NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
                Log.d(TAG, "Navigation set up successfully");
            } else {
                Log.e(TAG, "NavHostFragment is null");
                Toast.makeText(this, "Navigation fragment not found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during setup: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error during app initialization: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        try {
            return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
        } catch (Exception e) {
            Log.e(TAG, "Error during navigation up: " + e.getMessage());
            e.printStackTrace();
            return super.onSupportNavigateUp();
        }
    }
}