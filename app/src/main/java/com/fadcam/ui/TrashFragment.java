package com.fadcam.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fadcam.R;
import com.fadcam.model.TrashItem;
import com.fadcam.utils.TrashManager;
import com.fadcam.SharedPreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;
import androidx.appcompat.app.AlertDialog;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.content.Intent;
import android.net.Uri;
import java.io.File;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class TrashFragment extends Fragment implements TrashAdapter.OnTrashItemInteractionListener {

    private static final String TAG = "TrashFragment";
    private RecyclerView recyclerViewTrashItems;
    private TrashAdapter trashAdapter;
    private List<TrashItem> trashItems = new ArrayList<>();
    private Button buttonRestoreSelected;
    private Button buttonDeleteSelectedPermanently;
    private Button buttonEmptyAllTrash;
    private MaterialToolbar toolbar;
    private TextView textViewEmptyTrash;
    private View emptyTrashLayout;
    private AlertDialog restoreProgressDialog;
    private ExecutorService executorService;
    private TextView tvAutoDeleteInfo;
    private SharedPreferencesManager sharedPreferencesManager;

    public TrashFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        executorService = Executors.newSingleThreadExecutor();
        setHasOptionsMenu(true);
        sharedPreferencesManager = SharedPreferencesManager.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_trash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toolbar = view.findViewById(R.id.trash_toolbar);
        recyclerViewTrashItems = view.findViewById(R.id.recycler_view_trash_items);
        buttonRestoreSelected = view.findViewById(R.id.button_restore_selected);
        buttonDeleteSelectedPermanently = view.findViewById(R.id.button_delete_selected_permanently);
        buttonEmptyAllTrash = view.findViewById(R.id.button_empty_all_trash);
        textViewEmptyTrash = view.findViewById(R.id.empty_trash_text_view);
        emptyTrashLayout = view.findViewById(R.id.empty_trash_layout);
        tvAutoDeleteInfo = view.findViewById(R.id.tvAutoDeleteInfo);

        setupToolbar();
        setupRecyclerView();
        setupButtonListeners();
        updateAutoDeleteInfoText();

        // Auto-delete old items first, then load
        if (getContext() != null) {
            int autoDeleteMinutes = sharedPreferencesManager.getTrashAutoDeleteMinutes();
            int autoDeletedCount = TrashManager.autoDeleteExpiredItems(getContext(), autoDeleteMinutes);
            if (autoDeletedCount > 0) {
                Toast.makeText(getContext(), getString(R.string.trash_auto_deleted_toast, autoDeletedCount), Toast.LENGTH_LONG).show();
            }
        }
        loadTrashItems();
    }

    private void setupToolbar() {
        if (toolbar != null && getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            activity.setSupportActionBar(toolbar);
            toolbar.setTitle(getString(R.string.trash_fragment_title_text));
            toolbar.setNavigationIcon(R.drawable.ic_close);
            toolbar.setNavigationOnClickListener(v -> {
                try {
                    if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                        getParentFragmentManager().popBackStack();
                    } else {
                        if (getActivity() != null) getActivity().onBackPressed(); 
                    }
                    View overlayContainer = requireActivity().findViewById(R.id.overlay_fragment_container);
                    if (overlayContainer != null) {
                        overlayContainer.setVisibility(View.GONE);
                    } else {
                        Log.w(TAG, "Could not find R.id.overlay_fragment_container to hide it.");
                    }
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Toolbar navigation up failed (manual popBackStack)", e);
                }
            });
        } else {
            Log.e(TAG, "Toolbar is null or Activity is not AppCompatActivity, cannot set up toolbar as ActionBar.");
        }
    }

    private void setupRecyclerView() {
        if (getContext() == null) return;
        trashAdapter = new TrashAdapter(getContext(), trashItems, this, null);
        recyclerViewTrashItems.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewTrashItems.setAdapter(trashAdapter);
    }

    private void setupButtonListeners() {
        buttonRestoreSelected.setOnClickListener(v -> {
            List<TrashItem> selectedItems = trashAdapter.getSelectedItems();
            if (selectedItems.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.trash_no_items_selected_toast), Toast.LENGTH_SHORT).show();
                return;
            }
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.trash_dialog_restore_title))
                    .setMessage(getString(R.string.trash_dialog_restore_message, selectedItems.size()))
                    .setNegativeButton(getString(R.string.universal_cancel), (dialog, which) -> {
                        onRestoreFinished(false, getString(R.string.trash_restore_cancelled_toast));
                    })
                    .setPositiveButton(getString(R.string.universal_restore), (dialog, which) -> {
                        onRestoreStarted(selectedItems.size());
                        if (executorService == null || executorService.isShutdown()) {
                            executorService = Executors.newSingleThreadExecutor(); // Re-initialize if shutdown
                        }
                        executorService.submit(() -> {
                            boolean success = TrashManager.restoreItemsFromTrash(getContext(), selectedItems);
                            String message = success ? getString(R.string.trash_restore_success_toast, selectedItems.size())
                                                     : getString(R.string.trash_restore_fail_toast);
                            
                            // Post result back to main thread
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    onRestoreFinished(success, message);
                                    if (success) {
                                        loadTrashItems(); // Refresh list only on success
                                    }
                                });
                            }
                        });
                    })
                    .show();
        });

        buttonDeleteSelectedPermanently.setOnClickListener(v -> {
            List<TrashItem> selectedItems = trashAdapter.getSelectedItems();
            if (selectedItems.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.trash_empty_toast_message), Toast.LENGTH_SHORT).show();
                return;
            }

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.dialog_permanently_delete_title))
                    .setMessage(getString(R.string.dialog_permanently_delete_message, selectedItems.size()))
                    .setNegativeButton(getString(R.string.universal_cancel), null)
                    .setPositiveButton(getString(R.string.universal_delete), (dialog, which) -> {
                        if (TrashManager.permanentlyDeleteItems(getContext(), selectedItems)) {
                            Toast.makeText(getContext(), getString(R.string.trash_items_deleted_toast, selectedItems.size()), Toast.LENGTH_SHORT).show();
                            loadTrashItems();
                        } else {
                            Toast.makeText(getContext(), getString(R.string.trash_error_deleting_items_toast), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
        });

        buttonEmptyAllTrash.setOnClickListener(v -> {
            if (trashItems.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.trash_empty_toast_message), Toast.LENGTH_SHORT).show();
                return;
            }
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.trash_dialog_empty_all_title))
                    .setMessage(getString(R.string.dialog_empty_all_trash_message))
                    .setNegativeButton(getString(R.string.universal_cancel), null)
                    .setPositiveButton(getString(R.string.trash_button_empty_all_action), (dialog, which) -> {
                        if (TrashManager.emptyAllTrash(getContext())) {
                            Toast.makeText(getContext(), getString(R.string.trash_emptied_toast), Toast.LENGTH_SHORT).show();
                            loadTrashItems();
                        } else {
                            Toast.makeText(getContext(), getString(R.string.trash_error_deleting_items_toast), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
        });
        updateActionButtonsState(); // Initial state
    }

    private void loadTrashItems() {
        if (getContext() == null) return;
        List<TrashItem> loadedItems = TrashManager.loadTrashMetadata(getContext());
        trashItems.clear();
        trashItems.addAll(loadedItems);
        if (trashAdapter != null) {
            trashAdapter.notifyDataSetChanged();
        }
        updateActionButtonsState();
        checkEmptyState();
    }

    private void updateActionButtonsState() {
        // Example: enable buttons only if items are selected, or if trash is not empty for "Empty All"
        boolean anySelected = trashAdapter != null && trashAdapter.getSelectedItemsCount() > 0;
        buttonRestoreSelected.setEnabled(anySelected);
        buttonDeleteSelectedPermanently.setEnabled(anySelected);
        buttonEmptyAllTrash.setEnabled(!trashItems.isEmpty());
    }

    private void checkEmptyState() {
        if (trashItems.isEmpty()) {
            recyclerViewTrashItems.setVisibility(View.GONE);
            if (emptyTrashLayout != null) emptyTrashLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerViewTrashItems.setVisibility(View.VISIBLE);
            if (emptyTrashLayout != null) emptyTrashLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemSelectedStateChanged(boolean anySelected) {
        buttonRestoreSelected.setEnabled(anySelected);
        buttonDeleteSelectedPermanently.setEnabled(anySelected);
    }

    @Override
    public void onPlayVideoRequested(TrashItem item) {
        if (getContext() == null || item == null || item.getTrashFileName() == null) {
            Toast.makeText(getContext(), "Cannot play video. Invalid item data.", Toast.LENGTH_SHORT).show();
            return;
        }

        File trashDirectory = TrashManager.getTrashDirectory(getContext());
        if (trashDirectory == null) {
            Toast.makeText(getContext(), "Cannot access trash directory.", Toast.LENGTH_SHORT).show();
            return;
        }

        File trashedVideoFile = new File(trashDirectory, item.getTrashFileName());

        if (!trashedVideoFile.exists()) {
            Log.e(TAG, "Trashed video file does not exist: " + trashedVideoFile.getAbsolutePath());
            Toast.makeText(getContext(), "Video file not found in trash.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(getContext(), VideoPlayerActivity.class);
            intent.setData(Uri.fromFile(trashedVideoFile)); 
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting VideoPlayerActivity for trash item: " + trashedVideoFile.getAbsolutePath(), e);
            Toast.makeText(getContext(), "Error playing video.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRestoreStarted(int itemCount) {
        if (getActivity() == null || getContext() == null) return;
        getActivity().runOnUiThread(() -> {
            if (restoreProgressDialog != null && restoreProgressDialog.isShowing()) {
                restoreProgressDialog.dismiss();
            }
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View dialogView = inflater.inflate(R.layout.dialog_progress, null);

            TextView progressText = dialogView.findViewById(R.id.progress_text);
            if (progressText != null) {
                progressText.setText("Restoring " + itemCount + " item(s)...");
            }
            builder.setView(dialogView);
            builder.setCancelable(false);
            restoreProgressDialog = builder.create();
            if (!restoreProgressDialog.isShowing()) {
                restoreProgressDialog.show();
            }
        });
    }

    @Override
    public void onRestoreFinished(boolean success, String message) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if (restoreProgressDialog != null && restoreProgressDialog.isShowing()) {
                restoreProgressDialog.dismiss();
                restoreProgressDialog = null;
            }
            if (getContext() != null && message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onItemCheckChanged(TrashItem item, boolean isChecked) {
        // This method is called by the adapter when a checkbox state changes.
        // The main purpose is to update the enabled state of action buttons.
        if (getView() != null) { // Ensure fragment is still active
            updateActionButtonsState();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.trash_options_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_trash_auto_delete_settings) {
            showAutoDeleteSettingsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAutoDeleteSettingsDialog() {
        if (getContext() == null || sharedPreferencesManager == null) {
            Log.e(TAG, "Cannot show auto-delete settings dialog, context or prefs manager is null.");
            return;
        }

        final String[] items = {
                getString(R.string.auto_delete_1_hour),
                getString(R.string.auto_delete_5_hours),
                getString(R.string.auto_delete_10_hours),
                getString(R.string.auto_delete_1_day),
                getString(R.string.auto_delete_7_days),
                getString(R.string.auto_delete_30_days),
                getString(R.string.auto_delete_60_days),
                getString(R.string.auto_delete_90_days),
                getString(R.string.auto_delete_never)
        };

        final int[] valuesInMinutes = {
                60,          // 1 Hour
                5 * 60,      // 5 Hours
                10 * 60,     // 10 Hours
                1 * 24 * 60, // 1 Day
                7 * 24 * 60, // 7 Days
                30 * 24 * 60,// 30 Days
                60 * 24 * 60,// 60 Days
                90 * 24 * 60,// 90 Days
                SharedPreferencesManager.TRASH_AUTO_DELETE_NEVER
        };

        int currentSettingMinutes = sharedPreferencesManager.getTrashAutoDeleteMinutes();
        int checkedItem = -1;

        for (int i = 0; i < valuesInMinutes.length; i++) {
            if (valuesInMinutes[i] == currentSettingMinutes) {
                checkedItem = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.auto_delete_dialog_title))
                .setSingleChoiceItems(items, checkedItem, (dialog, which) -> {
                    // Action on item selection (optional, could update a temporary variable)
                })
                .setPositiveButton(getString(R.string.auto_delete_save_setting), (dialog, which) -> {
                    AlertDialog alertDialog = (AlertDialog) dialog;
                    int selectedPosition = alertDialog.getListView().getCheckedItemPosition();
                    if (selectedPosition != -1 && selectedPosition < valuesInMinutes.length) {
                        int selectedMinutes = valuesInMinutes[selectedPosition];
                        sharedPreferencesManager.setTrashAutoDeleteMinutes(selectedMinutes);
                        updateAutoDeleteInfoText();
                        Log.d(TAG, "Auto-delete setting updated to: " + selectedMinutes + " minutes.");
                        
                        boolean itemsWereAutoDeleted = false;
                        if (getContext() != null) {
                            int autoDeletedCount = TrashManager.autoDeleteExpiredItems(getContext(), selectedMinutes);
                            if (autoDeletedCount > 0) {
                                Toast.makeText(getContext(), getString(R.string.trash_auto_deleted_toast, autoDeletedCount), Toast.LENGTH_LONG).show();
                                itemsWereAutoDeleted = true;
                            }
                        }
                        loadTrashItems(); 
                        if (trashAdapter != null && !itemsWereAutoDeleted) { 
                            trashAdapter.notifyDataSetChanged();
                        }
                    }
                })
                .setNegativeButton(getString(R.string.universal_cancel), null)
                .show();
    }

    private void updateAutoDeleteInfoText() {
        if (tvAutoDeleteInfo == null || sharedPreferencesManager == null || getContext() == null) return;

        int totalMinutes = sharedPreferencesManager.getTrashAutoDeleteMinutes();

        if (totalMinutes == SharedPreferencesManager.TRASH_AUTO_DELETE_NEVER) {
            tvAutoDeleteInfo.setText(getString(R.string.trash_auto_delete_info_manual));
        } else if (totalMinutes < 60) { // Less than an hour, show in minutes (though current options are >= 1 hour)
             // This case isn't strictly needed with current options but good for future flexibility
            tvAutoDeleteInfo.setText(String.format(Locale.getDefault(), "Items are automatically deleted after %d minutes.", totalMinutes));
        } else if (totalMinutes < (24 * 60)) { // Less than a day, show in hours
            int hours = totalMinutes / 60;
            tvAutoDeleteInfo.setText(getResources().getQuantityString(R.plurals.trash_auto_delete_info_hours, hours, hours));
        } else { // Show in days
            int days = totalMinutes / (24 * 60);
            tvAutoDeleteInfo.setText(getResources().getQuantityString(R.plurals.trash_auto_delete_info_days, days, days));
        }
    }

    // TODO: Create TrashAdapter class
    // TODO: Implement logic for restore, permanent delete, empty all
    // TODO: Implement auto-deletion of files older than 30 days (perhaps in TrashManager and called periodically or on fragment load)
} 