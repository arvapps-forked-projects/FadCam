package com.fadcam.ui;

import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.fadcam.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Base Fragment class that provides common functionality for all fragments,
 * including handling of back button press consistently.
 */
public abstract class BaseFragment extends Fragment {
    
    /**
     * Override this method to handle back presses in your fragment.
     * Return true if the back press was handled, false to let the activity handle it.
     */
    protected boolean onBackPressed() {
        return false;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Add a callback for handling back presses
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), 
                new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                android.util.Log.d("OverlayDebug","BaseFragment handleOnBackPressed fragment="+getClass().getSimpleName());
                // First give the fragment a chance to handle the back press
                if (!onBackPressed()) {
                    // If fragment doesn't handle it, navigate to home tab if not already there
                    try {
                        ViewPager2 viewPager = requireActivity().findViewById(R.id.view_pager);
                        if (viewPager != null && viewPager.getCurrentItem() != 0) {
                            viewPager.setCurrentItem(0, true);
                        } else {
                            // If we're already on the home tab or viewPager not found, 
                            // disable this callback and let the system handle it
                            setEnabled(false);
                            // Use the dispatcher directly instead of the deprecated method
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        }
                    } catch (Exception e) {
                        // If anything goes wrong, disable this callback and let the system handle it
                        setEnabled(false);
                        // Use the dispatcher directly instead of the deprecated method
                        requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    }
                }
            }
        });
    }

    // -------------- Fix Start for this method(onDestroyView_cleanup)-----------
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Ensure overlay container visibility updates if this was last fragment
        if(getActivity() instanceof com.fadcam.MainActivity){
            android.util.Log.d("OverlayDebug","BaseFragment onDestroyView fragment="+getClass().getSimpleName()+" backStackBefore="+getActivity().getSupportFragmentManager().getBackStackEntryCount());
            ((com.fadcam.MainActivity) getActivity()).hideOverlayIfNoFragments();
        }
    }
    // -------------- Fix Ended for this method(onDestroyView_cleanup)-----------
} 