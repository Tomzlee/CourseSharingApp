package com.example.coursesharingapp.ui.home;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.firebase.auth.FirebaseUser;

public class HomeViewPagerAdapter extends FragmentStateAdapter {

    private static final int NUM_PAGES = 2;
    private FirebaseUser currentUser;

    public HomeViewPagerAdapter(@NonNull Fragment fragment, FirebaseUser currentUser) {
        super(fragment);
        this.currentUser = currentUser;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new AllCoursesFragment();
        } else {
            return new MyCoursesFragment();
        }
    }

    @Override
    public int getItemCount() {
        return NUM_PAGES;
    }
}