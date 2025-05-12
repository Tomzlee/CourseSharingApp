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
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private AuthRepository authRepository;
    private int currentTabId = R.id.homeFragment;
    private int previousTabId = R.id.homeFragment;

    // Keep track of tab navigation history
    private final ArrayList<Integer> tabHistory = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        authRepository = new AuthRepository();

        // Initialize tab history with home as the starting point
        tabHistory.add(R.id.homeFragment);

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

            // Listen for bottom navigation selection
            binding.bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                // Update tab history when selecting a new tab
                if (tabHistory.isEmpty() || tabHistory.get(tabHistory.size() - 1) != itemId) {
                    // Remove this tab from history if it exists (to avoid duplicates)
                    tabHistory.remove((Integer) itemId);
                    // Add to history
                    tabHistory.add(itemId);
                }

                return NavigationUI.onNavDestinationSelected(item, navController);
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
            } else if (destinationId == R.id.profileFragment || destinationId == R.id.playlistsFragment) {
                // Navigate to the previous tab in history
                if (tabHistory.size() > 1) {
                    // Remove current tab from history
                    tabHistory.remove(tabHistory.size() - 1);
                    // Get the previous tab
                    int previousTabId = tabHistory.get(tabHistory.size() - 1);
                    // Navigate to previous tab
                    binding.bottomNavigation.setSelectedItemId(previousTabId);
                    return;
                } else {
                    // If no history, default to home
                    binding.bottomNavigation.setSelectedItemId(R.id.homeFragment);
                    return;
                }
            }
        }

        // For other fragments (course details, etc.), use normal back navigation
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