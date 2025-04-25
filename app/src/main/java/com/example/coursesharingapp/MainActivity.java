package com.example.coursesharingapp;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.coursesharingapp.databinding.ActivityMainBinding;
import com.example.coursesharingapp.repository.AuthRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        authRepository = new AuthRepository();

        // Setup Navigation
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Set up bottom navigation with navController
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

            // Handle fragment changes to properly show/hide the bottom navigation
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                handleDestinationChange(destination);
            });

            // Make bottom navigation reselection just select the item
            binding.bottomNavigation.setOnItemReselectedListener(item -> {
                // Do nothing on reselection - prevents recreating the fragment
            });
        }

        // Check if user is already logged in
        checkAuthState();
    }

    private void handleDestinationChange(NavDestination destination) {
        int destinationId = destination.getId();

        // Hide bottom navigation on auth screens
        if (destinationId == R.id.loginFragment ||
                destinationId == R.id.registerFragment) {
            binding.bottomNavigation.setVisibility(View.GONE);
        } else {
            binding.bottomNavigation.setVisibility(View.VISIBLE);
        }

        // We don't need to do anything special here for the main fragments
        // The onBackPressed method will handle the back button behavior
    }

    @Override
    public void onBackPressed() {
        NavDestination currentDestination = navController.getCurrentDestination();
        if (currentDestination != null) {
            int destinationId = currentDestination.getId();

            // For main bottom navigation destinations
            if (destinationId == R.id.homeFragment) {
                // Exit the app when back is pressed from home
                finish();
                return;
            } else if (destinationId == R.id.profileFragment) {
                // Navigate to home fragment when back is pressed from profile
                navController.navigate(R.id.homeFragment);
                return;
            }
        }

        super.onBackPressed();
    }

    private void checkAuthState() {
        FirebaseUser currentUser = authRepository.getCurrentUser();

        if (currentUser == null) {
            // User is not logged in, navigate to login
            if (navController.getCurrentDestination() != null &&
                    navController.getCurrentDestination().getId() != R.id.loginFragment) {
                navController.navigate(R.id.loginFragment);
            }
        } else {
            // User is logged in, navigate to home if coming from login/register
            if (navController.getCurrentDestination() != null &&
                    (navController.getCurrentDestination().getId() == R.id.loginFragment ||
                            navController.getCurrentDestination().getId() == R.id.registerFragment)) {
                navController.navigate(R.id.homeFragment);
            }
        }
    }
}