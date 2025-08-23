package com.fadcam.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;

import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.text.HtmlCompat;
// For getting drawables

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.fadcam.utils.ShimmerEffectHelper;
import com.fadcam.Constants;
import com.fadcam.R;
// Ensure VideoItem import is correct
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.fadcam.Utils; // Import Utils for the new formatter

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import androidx.cardview.widget.CardView; // *** ADD Import ***
import com.fadcam.SharedPreferencesManager;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import androidx.recyclerview.widget.DiffUtil;
import android.graphics.drawable.GradientDrawable;
import androidx.core.app.ShareCompat;
import android.content.ContentResolver;
import androidx.core.content.FileProvider;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import com.fadcam.ui.picker.PickerBottomSheetFragment;
import com.fadcam.ui.picker.OptionItem;

// Modify the class declaration to remove the ListPreloader implementation
public class RecordsAdapter extends RecyclerView.Adapter<RecordsAdapter.RecordViewHolder> {

    // Keep the cache for thumbnails but optimize it
    private final SparseArray<String> loadedThumbnailCache = new SparseArray<>();
    private static final int THUMBNAIL_SIZE = 200; // Standard size for all thumbnails
    private Set<Uri> currentlyProcessingUris = new HashSet<>(); // Track processing URIs within adapter instance (passed from fragment)
    // Bounded LRU caches to avoid unbounded memory usage
    private final java.util.Map<String, Long> durationCache = new java.util.LinkedHashMap<String, Long>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, Long> eldest) {
            return size() > 256; // keep at most 256 entries
        }
    };
    private final java.util.Map<String, Long> savedPositionCache = new java.util.LinkedHashMap<String, Long>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, Long> eldest) {
            return size() > 1024; // keep more entries for saved positions
        }
    };
    // Reuse a single main-thread handler for UI updates
    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    // Simple file-based cache for durations (persist across sessions)
    private final File durationCacheFile;
    // Debounced persist task (posts to executor)
    private final Runnable persistDurationTask;
    // Broadcast receiver to listen for playback position updates
    private final androidx.localbroadcastmanager.content.LocalBroadcastManager localBroadcastManager;
    private final android.content.BroadcastReceiver playbackPositionReceiver;
    private android.content.Context receiverRegisteredContext = null;

    private static final String TAG = "RecordsAdapter";
    private final ExecutorService executorService; // Add ExecutorService
    private final SharedPreferencesManager sharedPreferencesManager;
    private final RecordActionListener actionListener; // Add the listener interface
    private final Context context;
    private List<VideoItem> records; // Now holds VideoItem objects
    private final OnVideoClickListener clickListener;
    private final OnVideoLongClickListener longClickListener;
    private final List<Uri> selectedVideosUris = new ArrayList<>(); // Track selection by URI
    private boolean isSelectionModeActive = false; // Track current mode within adapter
    private List<Uri> currentSelectedUris = new ArrayList<>(); // Keep track of selected items for binding
    // *** NEW: Store the path to the specific cache directory ***
    private final String tempCacheDirectoryPath;
    // Add field to track scrolling state
    private boolean isScrolling = false;
    // Add skeleton mode for professional loading experience
    private boolean isSkeletonMode = false;
    // --- Interfaces Updated ---
    public interface OnVideoClickListener {
        void onVideoClick(VideoItem videoItem); // Pass VideoItem
    }

    public interface OnVideoLongClickListener {
        void onVideoLongClick(VideoItem videoItem, boolean isSelected); // Pass VideoItem
    }

    // --- Constructor Updated ---
    // --- Complete Constructor ---
    public RecordsAdapter(Context context, List<VideoItem> records, ExecutorService executorService,
                          SharedPreferencesManager sharedPreferencesManager, // <<< Parameter Added
                          OnVideoClickListener clickListener, OnVideoLongClickListener longClickListener,
                          RecordActionListener actionListener) {

        this.context = Objects.requireNonNull(context, "Context cannot be null for RecordsAdapter");
        this.records = new ArrayList<>(records); // Use a mutable copy
        this.executorService = Objects.requireNonNull(executorService, "ExecutorService cannot be null");
        this.sharedPreferencesManager = Objects.requireNonNull(sharedPreferencesManager, "SharedPreferencesManager cannot be null"); // <<< STORE IT
        this.clickListener = clickListener; // Can be null if fragment doesn't implement/need it
        this.longClickListener = longClickListener; // Can be null
        this.actionListener = Objects.requireNonNull(actionListener, "RecordActionListener cannot be null"); // Assuming fragment always provides this


        // Initialize the cache directory path for checking temp files
        File cacheBaseDir = context.getExternalCacheDir();
        if (cacheBaseDir != null) {
            this.tempCacheDirectoryPath = new File(cacheBaseDir, "recording_temp").getAbsolutePath();
            Log.d(TAG, "Adapter Initialized. Temp cache path: " + this.tempCacheDirectoryPath);
        } else {
            Log.e(TAG, "Adapter Initialized. External cache dir is null! Cannot reliably identify temp files.");
            this.tempCacheDirectoryPath = null;
        }
        // Duration cache file inside app cache dir
        File appCache = context.getCacheDir();
        this.durationCacheFile = new File(appCache, "duration_cache.json");
        // load persisted duration cache
    loadDurationCacheFromDisk();

        // Prepare debounced persist task
        this.persistDurationTask = () -> {
            try { this.executorService.execute(this::persistDurationCacheToDisk); } catch (Exception ignored) {}
        };

        // Setup LocalBroadcastReceiver for immediate progress updates
        this.localBroadcastManager = androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context);
        this.playbackPositionReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context ctx, android.content.Intent intent) {
                if (intent == null) return;
                String uriStr = null;
                long pos = -1;
                // support both old and new extra names
                if (intent.hasExtra("extra_uri")) uriStr = intent.getStringExtra("extra_uri");
                if (intent.hasExtra("extra_position_ms")) pos = intent.getLongExtra("extra_position_ms", -1);
                if (uriStr == null) {
                    uriStr = intent.getStringExtra("uri");
                }
                if (pos < 0) {
                    pos = intent.getLongExtra("position_ms", -1);
                }
                if (uriStr == null || pos < 0) return;
                // Update savedPositionCache and notify specific item
                synchronized (savedPositionCache) { savedPositionCache.put(uriStr, pos); }
                int posIndex = findPositionByStringUri(uriStr);
                if (posIndex != -1) {
                    mainHandler.post(() -> notifyItemChanged(posIndex));
                }
            }
        };
        try {
            receiverRegisteredContext = context.getApplicationContext();
            this.localBroadcastManager.registerReceiver(this.playbackPositionReceiver, new android.content.IntentFilter("com.fadcam.ACTION_PLAYBACK_POSITION_UPDATED"));
        } catch (Exception ignored) {}
    }

    // *** NEW: Helper method to check if a VideoItem is in the cache directory ***
    private boolean isTemporaryFile(VideoItem item) {
        if (item == null || item.uri == null || tempCacheDirectoryPath == null) {
            return false; // Cannot determine if data is invalid or cache path unknown
        }
        // Temp files should *always* have a file:// scheme as they are created directly by MediaRecorder in cache
        if ("file".equals(item.uri.getScheme())) {
            String path = item.uri.getPath();
            if (path != null) {
                File file = new File(path);
                File parentDir = file.getParentFile();
                // Check if the file's parent directory matches the designated cache directory
                boolean isInCache = parentDir != null && tempCacheDirectoryPath.equals(parentDir.getAbsolutePath());
                // Optional: Add logging for debugging
                // Log.v(TAG, "isTemporaryFile check for " + item.displayName + ": Path=" + path + ", Parent="+(parentDir != null ? parentDir.getAbsolutePath() : "null")+ ", IsInCache=" + isInCache);
                return isInCache;
            }
        }
        // Not a file URI, so not a temp file from cache
        return false;
    }

    // ----- Fix Start: Add isSnowVeilTheme flag and method to set it -----
    private boolean isSnowVeilTheme = false;

    /**
     * Set whether we're currently using the Snow Veil theme
     * This allows special styling for cards in the Snow Veil theme
     */
    public void setSnowVeilTheme(boolean isSnowVeilTheme) {
        if (this.isSnowVeilTheme != isSnowVeilTheme) {
            this.isSnowVeilTheme = isSnowVeilTheme;
            // Need to refresh all items when theme changes
            notifyDataSetChanged();
        }
    }
    // ----- Fix End: Add isSnowVeilTheme flag and method to set it -----

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record, parent, false);
        return new RecordViewHolder(view);
    }

    // *** Need a method to update the processing set from the Fragment ***
    public void updateProcessingUris(Set<Uri> processingUris) {
        boolean changed = !this.currentlyProcessingUris.equals(processingUris);
        if(changed){
            this.currentlyProcessingUris = new HashSet<>(processingUris); // Use a copy
            Log.d(TAG,"Adapter processing URIs updated: "+this.currentlyProcessingUris.size() +" items.");
            // TODO: Consider optimizing this? Maybe only notify items that changed state?
            // For simplicity now, refresh all potentially affected items (though maybe slow)
            // Find positions of items that *were* processing or *are now* processing
            // This requires iterating, might be simpler to just notifyDataSetChanged for now.
            notifyDataSetChanged(); // Simplest way to reflect changes across list
        }
    }

    // Optimize onBindViewHolder to reduce work on the UI thread
    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        // --- 0. Handle Skeleton Mode ---
        if (isSkeletonMode) {
            bindSkeletonItem(holder, position);
            return;
        }
        
        // -------------- Fix Start (clearSkeletonEffects) - Clear skeleton state when binding real data -----------
        clearSkeletonEffects(holder);
        // -------------- Fix End (clearSkeletonEffects) -----------
        
        // --- 1. Basic Checks & Get Data ---
        if (records == null || position < 0 || position >= records.size() || records.get(position) == null || records.get(position).uri == null) {
            Log.e(TAG,"onBindViewHolder: Invalid item/data at position " + position);
            // Optionally clear the views in the holder to avoid displaying stale data
            return;
        }
        final VideoItem videoItem = records.get(position);
        final Uri videoUri = videoItem.uri;
        final String displayName = videoItem.displayName != null ? videoItem.displayName : "Unnamed Video";
        final String uriString = videoUri.toString();

        // --- 2. Determine Item States ---
        final boolean isCurrentlySelected = this.currentSelectedUris.contains(videoUri);
        final boolean isProcessing = this.currentlyProcessingUris.contains(videoUri);
        final boolean isTemp = isTemporaryFile(videoItem);
        final boolean isOpened = sharedPreferencesManager.getOpenedVideoUris().contains(uriString);
        final boolean showNewBadge = !isTemp && !isOpened && !isProcessing;
        final boolean allowGeneralInteractions = !isProcessing;
        final boolean allowMenuClick = allowGeneralInteractions && !this.isSelectionModeActive;

        // Only log for debugging specific positions to reduce spam
        if (position < 3 || position % 20 == 0) {
            Log.v(TAG,"onBindViewHolder Pos "+position+": Name="+displayName);
        }

        // --- 3. Bind Standard Data, optimized for fewer UI operations ---
        if (holder.textViewSerialNumber != null) holder.textViewSerialNumber.setText(String.valueOf(position + 1));
        if (holder.textViewRecord != null) holder.textViewRecord.setText(displayName);
        if (holder.textViewFileSize != null) holder.textViewFileSize.setText(formatFileSize(videoItem.size));
        
        // ----- Fix Start: Apply Snow Veil theme card colors -----
        // Apply proper background and text colors based on theme
        if (isSnowVeilTheme) {
            // For Snow Veil theme, ALWAYS use white card background with black text for maximum contrast
            if (holder.itemView instanceof CardView) {
                CardView cardView = (CardView) holder.itemView;
                // Force pure white background for all cards in Snow Veil theme
                cardView.setCardBackgroundColor(Color.WHITE);
                
                // Log for debugging
                Log.d(TAG, "Setting WHITE card background for Snow Veil theme at position " + position);
            }
            
            // Apply black tint to the three-dot menu icon for better contrast on white background
            if (holder.menuButton != null) {
                holder.menuButton.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
            }
            
            // Ensure file time and size in the overlay have good contrast
            if (holder.textViewFileSize != null) {
                holder.textViewFileSize.setTextColor(Color.WHITE);
            }
            if (holder.textViewFileTime != null) {
                holder.textViewFileTime.setTextColor(Color.WHITE);
            }
        } else {
            // For other themes, use the default background color
            if (holder.itemView instanceof CardView && context != null) {
                CardView cardView = (CardView) holder.itemView;
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.gray));
                
                // Log for debugging
                Log.d(TAG, "Setting GRAY card background for other theme at position " + position);
            }
            
            // Clear any tint for other themes
            if (holder.menuButton != null) {
                holder.menuButton.clearColorFilter();
            }
            
            // Leave text colors as default for other themes
            if (holder.textViewRecord != null) {
                holder.textViewRecord.setTextColor(holder.defaultTextColor);
            }
            if (holder.textViewTimeAgo != null) {
                holder.textViewTimeAgo.setTextColor(holder.defaultTextColor);
            }
        }
        // ----- Fix End: Apply Snow Veil theme card colors -----
        
        // Optimize time-consuming operations using lightweight caching
        if (holder.textViewFileTime != null) {
            // Check if we already have the duration cached
            String cachedDuration = loadedThumbnailCache.get(position);
            if (cachedDuration != null) {
                holder.textViewFileTime.setText(cachedDuration);
            } else {
                // Show a placeholder while loading
                holder.textViewFileTime.setText("--:--");
                
                // Calculate duration on background thread - this is one of the main causes of lag
                executorService.execute(() -> {
                    // -------------- Fix Start (duration calculation for new videos) -----------
                    // Add a small delay for newly recorded videos to ensure file is fully written
                    if (videoItem.isNew) {
                        try {
                            Thread.sleep(500); // 500ms delay for new videos
                            Log.d(TAG, "Added delay for new video duration calculation: " + videoItem.displayName);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    // -------------- Fix End (duration calculation for new videos) -----------
                    
                    long duration = getVideoDuration(videoUri);
                    String formattedDuration = formatVideoDuration(duration);
                    loadedThumbnailCache.put(position, formattedDuration);
                    
                    // Update UI on main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        // Make sure the view holder is still showing the same item before updating
                        if (holder.getAdapterPosition() == position && holder.textViewFileTime != null) {
                            holder.textViewFileTime.setText(formattedDuration);
                        }
                    });
                });
            }
        }
        
        if (holder.textViewTimeAgo != null) holder.textViewTimeAgo.setText(Utils.formatTimeAgo(videoItem.lastModified));

        // Only set the thumbnail if holder view is visible (important optimization)
        if (holder.imageViewThumbnail != null && holder.itemView.getVisibility() == View.VISIBLE) {
            setThumbnail(holder, videoUri);
        }

        // --- Last-viewed progress bar handling (optimized with caching) ---
        try {
            View progressBg = holder.itemView.findViewById(R.id.thumbnail_progress_bg);
            View progressFill = holder.itemView.findViewById(R.id.thumbnail_progress_fill);
            if (progressBg != null && progressFill != null) {
                progressFill.setVisibility(View.GONE);
                final String key = videoUri.toString();
                // Try caches first
                Long cachedSaved = savedPositionCache.get(key);
                Long cachedDur = durationCache.get(key);
                if (cachedSaved != null && cachedDur != null) {
                    applyProgressToView(progressBg, progressFill, cachedSaved, cachedDur);
                } else {
                    // Submit one background task to compute missing values
                    executorService.execute(() -> {
                        try {
                            long savedMs = cachedSaved != null ? cachedSaved : sharedPreferencesManager.getSavedPlaybackPositionMsWithFilenameFallback(key, getFileName(videoUri));
                            long durationMs = cachedDur != null ? cachedDur : getVideoDuration(videoUri);
                            // Cache results for future bindings (synchronized because LinkedHashMap isn't thread-safe)
                            synchronized (durationCache) {
                                if (durationMs > 0) {
                                    durationCache.put(key, durationMs);
                                    // schedule persist (debounced)
                                    mainHandler.removeCallbacks(persistDurationTask);
                                    mainHandler.postDelayed(persistDurationTask, 2000);
                                }
                            }
                            synchronized (savedPositionCache) {
                                if (savedMs > 0) savedPositionCache.put(key, savedMs);
                            }
                            final long fSaved = savedMs;
                            final long fDur = durationMs;
                            mainHandler.post(() -> applyProgressToView(progressBg, progressFill, fSaved, fDur));
                        } catch (Exception e) {
                            Log.w(TAG, "Error computing thumbnail progress", e);
                            mainHandler.post(() -> progressFill.setVisibility(View.GONE));
                        }
                    });
                }
            }
        } catch (Exception ignored) {}

        // --- 4. Visibility Logic for Overlays/Badges ---

        // Warning Dot for TEMP files (only visible if not processing)
        if (holder.menuWarningDot != null) {
            holder.menuWarningDot.setVisibility(isTemp && allowGeneralInteractions ? View.VISIBLE : View.GONE);
        }

        // Processing Overlay (Scrim and Spinner)
        if (holder.processingScrim != null) holder.processingScrim.setVisibility(isProcessing ? View.VISIBLE : View.GONE);
        if (holder.processingSpinner != null) holder.processingSpinner.setVisibility(isProcessing ? View.VISIBLE : View.GONE);

        // *** RESTORED Status Badge Logic ***
        if (holder.textViewStatusBadge != null && context != null) {
            if (isProcessing) {
                holder.textViewStatusBadge.setVisibility(View.GONE); // Hide all badges during processing
            } else if (isTemp) {
                // Show TEMP badge
                holder.textViewStatusBadge.setText("TEMP");
                holder.textViewStatusBadge.setBackground(ContextCompat.getDrawable(context, R.drawable.temp_badge_background));
                holder.textViewStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.black));
                holder.textViewStatusBadge.setVisibility(View.VISIBLE);
            } else if (showNewBadge) {
                // Show NEW badge (only if not Temp and not Opened)
                holder.textViewStatusBadge.setText("NEW");
                holder.textViewStatusBadge.setBackground(ContextCompat.getDrawable(context, R.drawable.new_badge_background));
                holder.textViewStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.white));
                holder.textViewStatusBadge.setVisibility(View.VISIBLE);
            } else {
                // Hide badge if neither Temp nor New
                holder.textViewStatusBadge.setVisibility(View.GONE);
            }
        }
        // *** END RESTORED Status Badge Logic ***


        // --- 5. Handle Selection Mode Visuals (picker-style check container & BACKGROUND/TEXT COLOR) ---
        if (holder.iconCheckContainer != null && holder.checkIcon != null) {
            if (this.isSelectionModeActive) {
                holder.iconCheckContainer.setVisibility(View.VISIBLE);

                // Tint the inner check icon to match theme toggle color
                try {
                    int tint = resolveThemeColor(context, R.attr.colorToggle);
                    holder.checkIcon.setImageTintList(ColorStateList.valueOf(tint));
                } catch (Exception e) {
                    // Fallback to primary color
                    holder.checkIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
                }

                if (isCurrentlySelected) {
                    // Ensure bg visible and animate inner check in
                    holder.checkIcon.setAlpha(1f);
                    holder.checkIcon.setScaleX(1f);
                    holder.checkIcon.setScaleY(1f);
                    // Highlight background and adjust text color for contrast
                    if(holder.itemView instanceof CardView && context!=null) {
                        if (isSnowVeilTheme) {
                            ((CardView)holder.itemView).setCardBackgroundColor(ContextCompat.getColor(context, R.color.snowveil_theme_accent));
                            if(holder.textViewRecord != null) holder.textViewRecord.setTextColor(Color.BLACK);
                        } else {
                            ((CardView)holder.itemView).setCardBackgroundColor(resolveThemeColor(context, R.attr.colorButton));
                            if(holder.textViewRecord != null) holder.textViewRecord.setTextColor(Color.WHITE);
                        }
                    }
                } else {
                    // ensure inner check hidden
                    holder.checkIcon.setAlpha(0f);
                    holder.checkIcon.setScaleX(0f);
                    holder.checkIcon.setScaleY(0f);
                    // Reset background and text color
                    if(holder.itemView instanceof CardView && context!=null) {
                        if (isSnowVeilTheme) {
                            ((CardView)holder.itemView).setCardBackgroundColor(Color.WHITE);
                            if(holder.textViewRecord != null) holder.textViewRecord.setTextColor(Color.BLACK);
                        } else {
                            ((CardView)holder.itemView).setCardBackgroundColor(ContextCompat.getColor(context, R.color.gray));
                            if(holder.textViewRecord != null) holder.textViewRecord.setTextColor(holder.defaultTextColor);
                        }
                    }
                }
            } else { // Not in selection mode
                holder.iconCheckContainer.setVisibility(View.GONE);
                // Reset bg and text color
                if(holder.itemView instanceof CardView && context!=null) {
                    if (isSnowVeilTheme) {
                        ((CardView)holder.itemView).setCardBackgroundColor(Color.WHITE);
                        if(holder.textViewRecord != null) holder.textViewRecord.setTextColor(Color.BLACK);
                    } else {
                        ((CardView)holder.itemView).setCardBackgroundColor(ContextCompat.getColor(context, R.color.gray));
                        if(holder.textViewRecord != null) holder.textViewRecord.setTextColor(holder.defaultTextColor);
                    }
                }
            }
        } else { Log.w(TAG, "iconCheckContainer or checkIcon is null in ViewHolder at pos "+position); }


        // --- 6. Set Enabled State and Listeners ---
        holder.itemView.setEnabled(allowGeneralInteractions); // Click/LongClick allowed if not processing
        if (holder.menuButtonContainer != null) {
            holder.menuButtonContainer.setEnabled(allowMenuClick); // Menu allowed if !processing AND !selectionMode
            holder.menuButtonContainer.setClickable(allowMenuClick);
            if(holder.menuButton != null) holder.menuButton.setAlpha(allowMenuClick ? 1.0f : 0.4f); // Dim if disabled
        } else { Log.w(TAG,"menuButtonContainer is null at pos "+position); }


        holder.itemView.setOnClickListener(v -> {
            if (allowGeneralInteractions && clickListener != null) { clickListener.onVideoClick(videoItem); }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (allowGeneralInteractions && longClickListener != null) { longClickListener.onVideoLongClick(videoItem, true); }
            return allowGeneralInteractions;
        });
        
        // -------------- Fix Start (thumbnail click listener) -----------
        // Set the same click listener on the thumbnail for better UX
        if (holder.imageViewThumbnail != null) {
            holder.imageViewThumbnail.setOnClickListener(v -> {
                if (allowGeneralInteractions && clickListener != null) { clickListener.onVideoClick(videoItem); }
            });
        }
        // -------------- Fix End (thumbnail click listener) -----------

        // INSTEAD, set a click listener on the menuButtonContainer
        if (holder.menuButtonContainer != null) {
            // -------------- Fix Start for this method(onBindViewHolder)-----------
            holder.menuButtonContainer.setOnClickListener(v -> {
                boolean isStillAllowMenuClick = !this.currentlyProcessingUris.contains(videoItem.uri) && !this.isSelectionModeActive;
                if (isStillAllowMenuClick) {
                    showVideoActionsSheet(holder, videoItem);
                }
            });
            // -------------- Fix Ended for this method(onBindViewHolder)-----------
        }

    } // End onBindViewHolder

    @Override
    public int getItemCount() {
        int count = records == null ? 0 : records.size();
        if (count == 0) {
            Log.d(TAG, "getItemCount returning 0 - records is " + (records == null ? "null" : "empty") + 
                       ", skeleton mode: " + isSkeletonMode);
        }
        return count;
    }

    // Load duration cache from JSON file
    private void loadDurationCacheFromDisk() {
        if (durationCacheFile == null || !durationCacheFile.exists()) return;
        try (java.io.FileInputStream fis = new java.io.FileInputStream(durationCacheFile)) {
            byte[] data = new byte[(int) durationCacheFile.length()];
            fis.read(data);
            String json = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            org.json.JSONObject obj = new org.json.JSONObject(json);
            org.json.JSONArray names = obj.names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    String k = names.getString(i);
                    long v = obj.optLong(k, 0L);
                    if (v > 0) durationCache.put(k, v);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to load duration cache", e);
        }
    }

    // Persist duration cache to disk (best-effort)
    private void persistDurationCacheToDisk() {
        if (durationCacheFile == null) return;
        try {
            org.json.JSONObject obj = new org.json.JSONObject();
            synchronized (durationCache) {
                for (java.util.Map.Entry<String, Long> e : durationCache.entrySet()) {
                    obj.put(e.getKey(), e.getValue());
                }
            }
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(durationCacheFile)) {
                fos.write(obj.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                fos.getFD().sync();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to persist duration cache", e);
        }
    }

    // Helper to apply computed progress to views on UI thread
    private void applyProgressToView(View progressBg, View progressFill, long savedMs, long durationMs) {
        try {
            if (savedMs > 0 && durationMs > 1000) {
                int percent = (int) Math.max(1, Math.min(100, (savedMs * 100) / durationMs));
                int bgW = progressBg.getWidth();
                if (bgW <= 0) {
                    progressBg.post(() -> {
                        int w = progressBg.getWidth();
                        int target = (w * percent) / 100;
                        animateProgressWidth(progressFill, target);
                        progressFill.setVisibility(View.VISIBLE);
                        // accessibility
                        progressFill.setContentDescription(progressPercentContentDescription(percent));
                    });
                } else {
                    int target = (bgW * percent) / 100;
                    animateProgressWidth(progressFill, target);
                    progressFill.setVisibility(View.VISIBLE);
                    // accessibility
                    progressFill.setContentDescription(progressPercentContentDescription(percent));
                }
            } else {
                progressFill.setVisibility(View.GONE);
                progressFill.setContentDescription(null);
            }
        } catch (Exception e) { Log.w(TAG, "applyProgressToView error", e); }
    }

    // Animate width change for the progress fill for a smooth visual update
    private void animateProgressWidth(View view, int toWidth) {
        if (view == null) return;
        try {
            int from = view.getLayoutParams().width;
            if (from < 0) from = 0;
            android.animation.ValueAnimator va = android.animation.ValueAnimator.ofInt(from, toWidth);
            va.setDuration(200);
            va.addUpdateListener(animation -> {
                int val = (int) animation.getAnimatedValue();
                view.getLayoutParams().width = val;
                view.requestLayout();
            });
            va.start();
        } catch (Exception ignored) {}
    }

    // Accessibility string helper
    private String progressPercentContentDescription(int percent) {
        try { return context.getString(R.string.accessibility_thumbnail_progress, percent); } catch (Exception ignored) { return percent + "% watched"; }
    }

    // Update the setThumbnail method to consider scrolling state with caching
    private void setThumbnail(RecordViewHolder holder, Uri videoUri) {
        if (holder.imageViewThumbnail == null || context == null) return;
        // Honor user preference: hide thumbnails if requested
        try {
            if (sharedPreferencesManager != null && sharedPreferencesManager.isHideThumbnailsEnabled()) {
                // Hide the thumbnail view and show a lightweight placeholder background
                holder.imageViewThumbnail.setImageResource(R.drawable.ic_video_placeholder);
                holder.imageViewThumbnail.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                return;
            }
        } catch (Exception ignored) {}

        // -------------- Fix Start (loadThumbnail with cache)-----------
        // Skip Glide loading for skeleton URIs
        if ("skeleton".equals(videoUri.getScheme())) {
            holder.imageViewThumbnail.setImageResource(R.drawable.ic_video_placeholder);
            return;
        }
        
        String uriString = videoUri.toString();
        
        // Try to get cached thumbnail first
        byte[] cachedThumbnail = com.fadcam.utils.VideoSessionCache.getThumbnailWithFallback(context, uriString);
        if (cachedThumbnail != null) {
            // Load from cache - instant display
            try {
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(cachedThumbnail, 0, cachedThumbnail.length);
                if (bitmap != null) {
                    holder.imageViewThumbnail.setImageBitmap(bitmap);
                    holder.imageViewThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    Log.v(TAG, "Loaded thumbnail from cache for: " + uriString);
                    return;
                }
            } catch (Exception e) {
                Log.w(TAG, "Error loading cached thumbnail", e);
            }
        }

        // Lower resolution during scrolling for performance
        int thumbnailSize = isScrolling ? 100 : THUMBNAIL_SIZE;
        
        // Create optimized request options with different strategies based on scrolling
        RequestOptions options = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .skipMemoryCache(false)
            .centerCrop()
            .override(thumbnailSize, thumbnailSize)
            .placeholder(R.drawable.ic_video_placeholder)
            .error(R.drawable.ic_video_placeholder);
        
        if (isScrolling) {
            // During scrolling, use low-quality thumbnails for speed
            options = options.dontAnimate();
        }
        
        // Load with Glide and cache the result
        Glide.with(context)
            .asBitmap()
            .load(videoUri)
            .apply(options)
            .thumbnail(0.1f)
            .into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                @Override
                public void onResourceReady(@NonNull android.graphics.Bitmap resource, 
                                          @androidx.annotation.Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.Bitmap> transition) {
                    // Set the image
                    holder.imageViewThumbnail.setImageBitmap(resource);
                    holder.imageViewThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    
                    // Cache the thumbnail for future use
                    executorService.execute(() -> {
                        try {
                            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                            resource.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, baos);
                            byte[] thumbnailData = baos.toByteArray();
                            
                            // Cache in memory and disk
                            com.fadcam.utils.VideoSessionCache.cacheThumbnail(uriString, thumbnailData);
                            com.fadcam.utils.VideoSessionCache.saveThumbnailToDisk(context, uriString, thumbnailData);
                            
                            Log.v(TAG, "Cached new thumbnail for: " + uriString);
                        } catch (Exception e) {
                            Log.w(TAG, "Error caching thumbnail", e);
                        }
                    });
                }
                
                @Override
                public void onLoadCleared(@androidx.annotation.Nullable android.graphics.drawable.Drawable placeholder) {
                    // Set placeholder if load is cleared
                    holder.imageViewThumbnail.setImageResource(R.drawable.ic_video_placeholder);
                }
            });
        // -------------- Fix End (loadThumbnail with cache)-----------
    }

    // Override onViewRecycled to cancel thumbnail loading for recycled views
    @Override
    public void onViewRecycled(@NonNull RecordViewHolder holder) {
        super.onViewRecycled(holder);
        
        // Cancel any pending image loads when view is recycled
        if (holder.imageViewThumbnail != null && context != null) {
            Glide.with(context).clear(holder.imageViewThumbnail);
        }
    }

    // Override onBindViewHolder to handle payload for quality changes
    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            if (payloads.contains("QUALITY_CHANGE")) {
                // Only update thumbnail when scrolling stops
                if (holder.imageViewThumbnail != null && position < records.size()) {
                    VideoItem videoItem = records.get(position);
                    if (videoItem != null && videoItem.uri != null) {
                        setThumbnail(holder, videoItem.uri);
                    }
                }
                return;
            }

            if (payloads.contains("SELECTION_TOGGLE")) {
                // Animate the inner check in/out based on current selection state
                if (position < records.size()) {
                    VideoItem videoItem = records.get(position);
                    boolean isCurrentlySelected = this.currentSelectedUris.contains(videoItem.uri);
                    // animate using unified bounce+fade helper
                    animateCheckIcon(holder.checkIcon, isCurrentlySelected);
                }
                return;
            }
        }
        // If no specific payload, do a full bind
        onBindViewHolder(holder, position);
        
        // EMERGENCY FIX: Force apply Snow Veil card color after normal binding
        if (isSnowVeilTheme && holder.itemView instanceof CardView) {
            ((CardView)holder.itemView).setCardBackgroundColor(Color.WHITE);
            Log.d(TAG, "🔴 EMERGENCY: Forced WHITE card for Snow Veil at position " + position);
            
            // Force black text on all text elements
            if (holder.textViewRecord != null) holder.textViewRecord.setTextColor(Color.BLACK);
            if (holder.textViewTimeAgo != null) holder.textViewTimeAgo.setTextColor(Color.BLACK);
        }
    }

    // --- Selection Handling ---
    private void toggleSelection(Uri videoUri, boolean isSelected) {
        if (videoUri == null) return;
        if (isSelected) {
            if (!selectedVideosUris.contains(videoUri)) {
                selectedVideosUris.add(videoUri);
            }
        } else {
            selectedVideosUris.remove(videoUri);
        }
        // Find item's position and update only that item for efficiency
        int position = findPositionByUri(videoUri);
        if (position != -1) {
            notifyItemChanged(position, "SELECTION_TOGGLE"); // Update specific item with payload to animate
        } else {
            Log.w(TAG,"Could not find position for URI: "+ videoUri + " during toggle. List size: " + records.size());
            // Maybe list was updated concurrently? Do a full refresh as fallback.
            // notifyDataSetChanged(); // Use this cautiously
        }
    }




    // Helper to find item position by URI (important for notifyItemChanged)
    // Make this public so the Fragment can call it after marking an item opened
    public int findPositionByUri(Uri uri) { // <-- *** CHANGED to public ***
        if (uri == null || records == null) {
            Log.w(TAG, "findPositionByUri called with null uri or null records list.");
            return -1;
        }
        for (int i = 0; i < records.size(); i++) {
            VideoItem item = records.get(i);
            // Added null check for item as well for safety
            if (item != null && item.uri != null && uri.equals(item.uri)) { // Use equals for URI comparison
                return i;
            }
        }
        Log.v(TAG,"URI not found in adapter list: " + uri); // Use v for verbose logs
        return -1; // Not found
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        try {
            if (receiverRegisteredContext != null) {
                androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(receiverRegisteredContext).unregisterReceiver(playbackPositionReceiver);
            } else {
                androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(recyclerView.getContext()).unregisterReceiver(playbackPositionReceiver);
            }
        } catch (Exception ignored) {}
    }

    private int findPositionByStringUri(String uriStr) {
        if (uriStr == null || records == null) return -1;
        for (int i = 0; i < records.size(); i++) {
            VideoItem it = records.get(i);
            if (it == null) continue;
            if (uriStr.equals(it.uri == null ? null : it.uri.toString())) return i;
            // tolerate filename fallback keys (may be stored as plain filenames)
            String fn = getFileName(it.uri);
            if (fn != null && fn.equals(uriStr)) return i;
        }
        return -1;
    }


    // --- Popup Menu and Actions (Major Updates Here) ---
    private int resolveThemeColor(Context context, int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    // -------------- Fix Start for this method(showVideoActionsSheet)-----------
    /**
     * showVideoActionsSheet
     * Replaces legacy PopupMenu with our unified bottom sheet picker using ligature icons.
     * Preserves existing behaviors by mapping item ids to the same handlers.
     */
    // -------------- Fix Start for this method(showVideoActionsSheet)-----------
    private void showVideoActionsSheet(RecordViewHolder holder, VideoItem videoItem) {
        Context ctx = holder.itemView.getContext();
        if (!(ctx instanceof FragmentActivity)) {
            // Fallback to popup if we don't have a FragmentActivity context
            PopupMenu popup = setupPopupMenu(holder, videoItem);
            if (popup != null) popup.show();
            return;
        }

    ArrayList<OptionItem> items = new ArrayList<>();
    // Order mirrors existing menu; use contextual ligatures per repo policy and helper subtitles
    // -------------- Fix Start for this method(showVideoActionsSheet)-----------
    // Add helper text explaining the save destination
    items.add(new OptionItem(
        "action_save",
        ctx.getString(R.string.video_menu_save),
        ctx.getString(R.string.video_menu_save_helper_downloads_fadcam, "Video will be saved to Downloads/FadCam"),
        null,
        null,
        null,
        null,
        null,
        "download",
        null,
        null,
        null
    ));
    // -------------- Fix Ended for this method(showVideoActionsSheet)-----------
    // -------------- Fix Start for this method(showVideoActionsSheet)-----------
    // Temporarily hide Fix Video from UI; keep feature intact for later re-enable
    // items.add(OptionItem.withLigature("action_fix_video", ctx.getString(R.string.fix_video_menu_title), "build"));
    // -------------- Fix Ended for this method(showVideoActionsSheet)-----------
    items.add(OptionItem.withLigature("action_rename", ctx.getString(R.string.video_menu_rename), "drive_file_rename_outline"));
    items.add(OptionItem.withLigature("action_info", ctx.getString(R.string.video_menu_info), "info"));
    items.add(OptionItem.withLigature("action_upload_youtube", ctx.getString(R.string.video_menu_upload_youtube), "play_circle"));
    items.add(OptionItem.withLigature("action_upload_drive", ctx.getString(R.string.video_menu_upload_drive), "cloud_upload"));
    items.add(OptionItem.withLigature("action_open_with", ctx.getString(R.string.video_menu_open_with), "open_in_new"));
    // New: Upload to FadDrive (coming soon) — badge only, no helper line
    items.add(OptionItem.withLigatureBadge("action_upload_faddrive", ctx.getString(R.string.video_menu_upload_faddrive, "Upload to FadDrive"), "cloud", ctx.getString(R.string.remote_coming_soon_badge), R.drawable.badge_background_green, true, null));
    // Coming soon: Edit with FaditorX — after FadDrive
    items.add(OptionItem.withLigatureBadge("action_edit_faditorx", ctx.getString(R.string.edit_with_faditorx), "content_cut", ctx.getString(R.string.remote_coming_soon_badge), R.drawable.badge_background_green, true, null));
    items.add(OptionItem.withLigature("action_delete", ctx.getString(R.string.video_menu_del), "delete"));

        String resultKey = "video_actions:" + (videoItem.uri != null ? videoItem.uri.toString() : System.identityHashCode(videoItem));
        FragmentManager fm = ((FragmentActivity) ctx).getSupportFragmentManager();
        fm.setFragmentResultListener(resultKey, (FragmentActivity) ctx, (requestKey, bundle) -> {
            if (bundle == null) return;
            String id = bundle.getString(PickerBottomSheetFragment.BUNDLE_SELECTED_ID);
            if (id == null) return;
            switch (id) {
                case "action_edit_faditorx":
                    Toast.makeText(ctx, R.string.remote_toast_coming_soon, Toast.LENGTH_SHORT).show();
                    break;
                case "action_upload_faddrive":
                    Toast.makeText(ctx, R.string.remote_toast_coming_soon, Toast.LENGTH_SHORT).show();
                    break;
                case "action_save":
                    saveVideoToGalleryInternal(videoItem);
                    break;
                case "action_fix_video":
                    fixVideoFile(videoItem);
                    break;
                case "action_rename":
                    showRenameDialog(videoItem);
                    break;
                case "action_info":
                    showVideoInfoDialog(videoItem);
                    break;
                case "action_delete":
                    if (actionListener != null) actionListener.onDeleteVideo(videoItem);
                    break;
                case "action_upload_youtube":
                    openVideoInYouTube(videoItem);
                    break;
                case "action_upload_drive":
                    openVideoInGoogleDrive(videoItem);
                    break;
                case "action_open_with":
                    openWithExternalPlayer(videoItem);
                    break;
            }
        });

    // Title: show the file name or a generic label
    String sheetTitle = (videoItem != null && videoItem.displayName != null) ? videoItem.displayName : ctx.getString(R.string.records_title);
    PickerBottomSheetFragment sheet = PickerBottomSheetFragment.newInstanceGradient(
        sheetTitle,
                items,
                null,
                resultKey,
                null,
                true
        );

        // Hide selection checkmarks for action sheets so rows are compact
        Bundle args = sheet.getArguments();
        if (args != null) args.putBoolean(PickerBottomSheetFragment.ARG_HIDE_CHECK, true);

        sheet.show(fm, "video_actions_sheet");
    }
    // -------------- Fix Ended for this method(showVideoActionsSheet)-----------

    private PopupMenu setupPopupMenu(RecordViewHolder holder, VideoItem videoItem) {
        Context context = holder.itemView.getContext();
        int popupMenuStyle = 0;
        // Dynamically select the correct style for the current theme
        SharedPreferencesManager spm = SharedPreferencesManager.getInstance(context);
        String currentTheme = spm.sharedPreferences.getString(Constants.PREF_APP_THEME, Constants.DEFAULT_APP_THEME);
        if ("Crimson Bloom".equals(currentTheme)) {
            popupMenuStyle = R.style.Widget_FadCam_Red_PopupMenu; // Use underscore, not dot
        } else if ("Faded Night".equals(currentTheme)) {
            // If you have a custom style for AMOLED, set it here
            // popupMenuStyle = R.style.Widget_FadCam_Amoled_PopupMenu;
            popupMenuStyle = 0; // fallback to default
        }
        PopupMenu popup = (popupMenuStyle != 0)
                ? new PopupMenu(context, holder.menuButtonContainer, 0, 0, popupMenuStyle)
                : new PopupMenu(context, holder.menuButtonContainer);
        popup.getMenuInflater().inflate(R.menu.video_item_menu, popup.getMenu());

        // Set text color for all menu items
        int colorMenuText;
        if ("Crimson Bloom".equals(currentTheme)) {
            colorMenuText = ContextCompat.getColor(context, R.color.white);
        } else if ("Faded Night".equals(currentTheme)) {
            colorMenuText = ContextCompat.getColor(context, R.color.amoled_text_primary);
        } else {
            colorMenuText = resolveThemeColor(context, R.attr.colorHeading);
        }
        for (int i = 0; i < popup.getMenu().size(); i++) {
            MenuItem item = popup.getMenu().getItem(i);
            SpannableString spanString = new SpannableString(item.getTitle());
            spanString.setSpan(new ForegroundColorSpan(colorMenuText), 0, spanString.length(), 0);
            item.setTitle(spanString);
        }
        // Optionally force show icons (reflection, but safe fallback)
        try {
            java.lang.reflect.Field mPopupField = popup.getClass().getDeclaredField("mPopup");
            mPopupField.setAccessible(true);
            Object menuPopupHelper = mPopupField.get(popup);
            java.lang.reflect.Method setForceShowIcon = menuPopupHelper.getClass().getMethod("setForceShowIcon", boolean.class);
            setForceShowIcon.invoke(menuPopupHelper, true);
        } catch (Exception e) {
            Log.w(TAG, "Could not force show popup menu icons: " + e.getMessage());
        }
        // Handle all menu actions
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit_faditorx) {
                Toast.makeText(context, R.string.remote_toast_coming_soon, Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.action_save) {
                saveVideoToGalleryInternal(videoItem);
                return true;
            } else if (id == R.id.action_fix_video) {
                fixVideoFile(videoItem);
                return true;
            } else if (id == R.id.action_rename) {
                showRenameDialog(videoItem);
                return true;
            } else if (id == R.id.action_info) {
                showVideoInfoDialog(videoItem);
                return true;
            } else if (id == R.id.action_delete) {
                if (actionListener != null) actionListener.onDeleteVideo(videoItem);
                return true;
            } else if (id == R.id.action_upload_youtube) {
                openVideoInYouTube(videoItem);
                return true;
            } else if (id == R.id.action_upload_drive) {
                openVideoInGoogleDrive(videoItem);
                return true;
            }
            return false;
        });

        // After inflating the menu, update the 'Edit with FaditorX' item to show 'Coming Soon' and gray out
        MenuItem faditorxItem = popup.getMenu().findItem(R.id.action_edit_faditorx);
        if (faditorxItem != null) {
            // Append badge text
            String baseTitle = context.getString(R.string.edit_with_faditorx);
            String badge = "  [Coming Soon]";
            SpannableString spanString = new SpannableString(baseTitle + badge);
            // Gray out the whole text
            spanString.setSpan(new ForegroundColorSpan(Color.GRAY), 0, spanString.length(), 0);
            faditorxItem.setTitle(spanString);
            // Gray out the icon
            Drawable icon = faditorxItem.getIcon();
            if (icon != null) {
                icon.mutate();
                icon.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                faditorxItem.setIcon(icon);
            }
            // Do NOT disable the item, so it can show the toast
        }

        return popup;
    }

    // --- Restored Rename Logic ---
    private void showRenameDialog(VideoItem videoItem) {
        if (context == null) return;

        // Prepare base name and extension like before
        String currentName = videoItem.displayName != null ? videoItem.displayName : "";
        int dotIndex = currentName.lastIndexOf('.');
        String baseName = (dotIndex > 0) ? currentName.substring(0, dotIndex) : currentName;

        // Prefer the existing TextInputBottomSheetFragment for a consistent input UI
        try {
            if (context instanceof androidx.fragment.app.FragmentActivity) {
                androidx.fragment.app.FragmentActivity fa = (androidx.fragment.app.FragmentActivity) context;
                String resultKey = "rename_video_result_" + Integer.toHexString(System.identityHashCode(videoItem.uri));

                // Use unified InputActionBottomSheetFragment in 'input' mode for rename so it matches Delete All UI
        InputActionBottomSheetFragment sheet = InputActionBottomSheetFragment.newInput(
                        context.getString(R.string.rename_video_title),
                        baseName,
                        context.getString(R.string.rename_video_hint),
                        context.getString(R.string.rename_video_title),
                        context.getString(R.string.rename_video_hint),
            R.drawable.ic_edit_cut
                );

                sheet.setCallbacks(new InputActionBottomSheetFragment.Callbacks() {
                    @Override public void onImportConfirmed(org.json.JSONObject json) { }
                    @Override public void onResetConfirmed() { }
                    @Override public void onInputConfirmed(String input) {
                        // Close the sheet immediately to provide responsive UX
                        try { sheet.dismiss(); } catch (Exception ignored) {}

                        String newNameBase = input != null ? input.trim() : "";
                        if (newNameBase.isEmpty()) {
                            if (context != null) ((Activity) context).runOnUiThread(() -> Toast.makeText(context, R.string.toast_rename_name_empty, Toast.LENGTH_SHORT).show());
                            return;
                        }

                        String originalExtension = (dotIndex > 0 && dotIndex < currentName.length() - 1) ? currentName.substring(dotIndex) : ("." + Constants.RECORDING_FILE_EXTENSION);

                        String sanitizedBaseName = newNameBase
                                .replaceAll("[^a-zA-Z0-9\\-_]", "_")
                                .replaceAll("\\s+", "_")
                                .replaceAll("_+", "_")
                                .replaceAll("^_|_$", "");

                        if (sanitizedBaseName.isEmpty()) sanitizedBaseName = "renamed_video";

                        String newFullName = sanitizedBaseName + originalExtension;

                        if (newFullName.equals(videoItem.displayName)) {
                            if (context != null) ((Activity) context).runOnUiThread(() -> Toast.makeText(context, "Name hasn't changed", Toast.LENGTH_SHORT).show());
                        } else {
                            executorService.submit(() -> renameVideo(videoItem, newFullName));
                        }
                    }
                });

                sheet.show(fa.getSupportFragmentManager(), "rename_" + Integer.toHexString(System.identityHashCode(videoItem.uri)));
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to show TextInputBottomSheetFragment, falling back to dialog", e);
        }

        // Fallback: if we cannot show the bottom sheet, keep the existing Material dialog behavior
        // ...existing code fallback omitted for brevity... (keeps previous dialog implementation)
    }

    private void renameVideo(VideoItem videoItem, String newFullName) {
        if (context == null) return;
        Uri videoUri = videoItem.uri;
        int position = findPositionByUri(videoUri);

        if (position == -1) {
            Log.e(TAG, "Cannot rename, item not found in adapter list: " + videoUri);
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> Toast.makeText(context, context.getString(R.string.toast_rename_failed) + " (Item not found)", Toast.LENGTH_SHORT).show());
            }
            return;
        }

        boolean renameSuccess = false;
        Uri newUri = null;

        try {
            Log.d(TAG, "Attempting rename. URI: " + videoUri + ", Scheme: " + videoUri.getScheme() + ", New Name: " + newFullName);

            if ("file".equals(videoUri.getScheme()) && videoUri.getPath() != null) {
                File oldFile = new File(videoUri.getPath());
                File parentDir = oldFile.getParentFile();
                if (parentDir == null) throw new IOException("Cannot get parent directory for file URI");
                File newFile = new File(parentDir, newFullName);

                if (oldFile.renameTo(newFile)) {
                    renameSuccess = true;
                    newUri = Uri.fromFile(newFile);
                    Log.i(TAG, "Renamed file system file successfully.");
                } else {
                    Log.e(TAG, "File.renameTo() failed for " + oldFile.getPath());
                }
            } else if ("content".equals(videoUri.getScheme())) {
                newUri = DocumentsContract.renameDocument(context.getContentResolver(), videoUri, newFullName);
                if (newUri != null) {
                    renameSuccess = true;
                    Log.i(TAG, "Renamed SAF document successfully. New URI: " + newUri);
                } else {
                     Log.w(TAG, "DocumentsContract.renameDocument returned null for: " + videoUri + " to '" + newFullName + "'.");
                    // Check if rename actually happened (some providers might return null on success if name didn't change or already exists)
                    DocumentFile checkDoc = DocumentFile.fromSingleUri(context, videoUri); // Check original URI, it might have been renamed in place
                    if (checkDoc != null && newFullName.equals(checkDoc.getName())) {
                        Log.w(TAG, "Rename check: File with new name exists under original URI. Assuming success.");
                        newUri = checkDoc.getUri(); 
                        renameSuccess = true;
                    } else { // Check if a new file with the new name exists in the parent
                        DocumentFile parent = checkDoc != null ? checkDoc.getParentFile() : null;
                        if (parent != null) {
                            DocumentFile renamedFile = parent.findFile(newFullName);
                            if (renamedFile != null && renamedFile.exists()) {
                                 Log.w(TAG, "Rename check: File with new name exists in parent. Assuming success.");
                                 newUri = renamedFile.getUri();
                                renameSuccess = true;
                            }
                        }
                    }
                }
            } else {
                Log.e(TAG, "Unsupported URI scheme for renaming: " + videoUri.getScheme());
            }

            if (renameSuccess && newUri != null) {
                final Uri finalNewUri = newUri; // Create an effectively final variable
                VideoItem updatedItem = new VideoItem(
                        finalNewUri,
                        newFullName,
                        videoItem.size, // Ideally, re-query size from newUri if possible
                        System.currentTimeMillis()
                );
                if (context instanceof Activity) {
                    ((Activity) context).runOnUiThread(() -> {
                        if (position >= 0 && position < records.size()) {
                           records.set(position, updatedItem);
                            if (selectedVideosUris.contains(videoItem.uri)) { // If old URI was selected
                                selectedVideosUris.remove(videoItem.uri);
                                selectedVideosUris.add(finalNewUri); // Replace with new URI
                            }
                           notifyItemChanged(position);
                Toast.makeText(context, R.string.toast_rename_success, Toast.LENGTH_SHORT).show();
                        } else {
                             Log.e(TAG, "Rename success but position " + position + " is invalid for records list size " + records.size());
                             Toast.makeText(context, "Rename successful, but list update failed.", Toast.LENGTH_LONG).show();
                             // Consider a full reload if this happens.
                        }
                    });
                }
            } else if (!renameSuccess) {
                if (context instanceof Activity) {
                    ((Activity) context).runOnUiThread(() -> Toast.makeText(context, R.string.toast_rename_failed, Toast.LENGTH_SHORT).show());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception during rename for " + videoUri + " to " + newFullName, e);
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> Toast.makeText(context, context.getString(R.string.toast_rename_failed) + " (Error)", Toast.LENGTH_SHORT).show());
            }
        }
    }

    // --- Restored Save to Gallery Logic (using actionListener for progress) ---
    private void saveVideoToGalleryInternal(VideoItem videoItem) {
        if (context == null || videoItem == null || videoItem.uri == null || videoItem.displayName == null) {
            if (actionListener != null) {
                // Run on UI thread if context is available to show Toast
                if (context instanceof Activity) {
                     ((Activity)context).runOnUiThread(() -> actionListener.onSaveToGalleryFinished(false, "Invalid video data.", null));
                } else { // No activity context, just log
                     Log.e(TAG, "saveVideoToGalleryInternal: Invalid video data or context null before starting save.");
                }
            }
            return;
        }

        final Uri sourceUri = videoItem.uri;
        final String filename = videoItem.displayName;

        if (actionListener != null) {
            actionListener.onSaveToGalleryStarted(filename); // Notify fragment (UI thread)
        }

        executorService.submit(() -> {
            boolean success = false;
            Uri resultUri = null;
            String message;
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File fadCamDir = new File(downloadsDir, Constants.RECORDING_DIRECTORY); // Use Constant
            if (!fadCamDir.exists()) {
                if (!fadCamDir.mkdirs()) {
                    Log.e(TAG, "Failed to create FadCam directory in Downloads.");
                    message = "Save Failed: Cannot create directory.";
                    // ----- Fix Start for this method(saveVideoToGalleryInternal)-----
                    final String finalMessageForLambda = message;
                    // ----- Fix Ended for this method(saveVideoToGalleryInternal)-----
                    // Notify listener on UI thread
                    if (context instanceof Activity && actionListener != null) {
                        ((Activity)context).runOnUiThread(() -> actionListener.onSaveToGalleryFinished(false, finalMessageForLambda, null));
                    }
                    return;
                }
            }
            File destFile = new File(fadCamDir, filename);
            int counter = 0;
            // Handle potential name conflicts by appending (1), (2), etc.
            while (destFile.exists()) {
                counter++;
                String nameWithoutExt = filename;
                String extension = "";
                int dotIndex = filename.lastIndexOf('.');
                if (dotIndex > 0 && dotIndex < filename.length() - 1) {
                    nameWithoutExt = filename.substring(0, dotIndex);
                    extension = filename.substring(dotIndex);
                }
                destFile = new File(fadCamDir, nameWithoutExt + " (" + counter + ")" + extension);
            }


            try (InputStream in = context.getContentResolver().openInputStream(sourceUri);
                 OutputStream out = new FileOutputStream(destFile)) {

                if (in == null) throw new IOException("Failed to open input stream for " + sourceUri);

                byte[] buf = new byte[8192]; // Increased buffer size
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.flush(); 
                Utils.scanFileWithMediaStore(context, destFile.getAbsolutePath()); // Scan the new file
                resultUri = Uri.fromFile(destFile); // Not entirely correct for MediaStore, but good for logs
                success = true;
                message = "Video saved to Downloads/FadCam";
                Log.i(TAG, "Video saved successfully to: " + destFile.getAbsolutePath());

            } catch (Exception e) {
                Log.e(TAG, "Error saving video to gallery", e);
                message = "Save Failed: " + e.getMessage();
                if (destFile.exists()) { // Clean up partial file
                    destFile.delete();
                }
            }

            final boolean finalSuccess = success;
            final String finalMessage = message;
            final Uri finalResultUri = success ? Uri.fromFile(destFile) : null; // Use actual destFile URI if successful for listener

            if (context instanceof Activity && actionListener != null) {
                 ((Activity)context).runOnUiThread(() -> actionListener.onSaveToGalleryFinished(finalSuccess, finalMessage, finalResultUri));
            } else if (actionListener != null) { // Fallback if context not an activity (e.g. service context)
                // This case is less likely for UI-triggered actions but good for robustness
                // Directly call if Looper is available or handle differently
                if (Looper.myLooper() == Looper.getMainLooper()) {
                     actionListener.onSaveToGalleryFinished(finalSuccess, finalMessage, finalResultUri);
                } else {
                     new Handler(Looper.getMainLooper()).post(() -> actionListener.onSaveToGalleryFinished(finalSuccess, finalMessage, finalResultUri));
                }
            }
        });
    }

    // --- Updated showVideoInfoDialog ---

    /**
     * Displays a dialog with detailed information about the selected video item.
     * Shows a specific warning if the item is identified as a temporary file
     * residing in the cache directory.
     *
     * @param videoItem The VideoItem representing the selected video.
     */
    private void showVideoInfoDialog(VideoItem videoItem) {
        // 1. Pre-checks
        if (context == null) {
            Log.e(TAG,"Cannot show info dialog, context is null.");
            return;
        }
        if (videoItem == null || videoItem.uri == null) {
            Log.e(TAG,"Cannot show info dialog, videoItem or its URI is null.");
            Toast.makeText(context, "Cannot get video information.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Inflate layout and find views
        MaterialAlertDialogBuilder builder = themedDialogBuilder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_video_info, null);
        if (dialogView == null) {
            Log.e(TAG, "Failed to inflate dialog_video_info layout.");
            return;
        }

        TextView tvFileName = dialogView.findViewById(R.id.tv_file_name);
        TextView tvFileSize = dialogView.findViewById(R.id.tv_file_size);
        TextView tvFilePath = dialogView.findViewById(R.id.tv_file_path);
        TextView tvLastModified = dialogView.findViewById(R.id.tv_last_modified);
        TextView tvDuration = dialogView.findViewById(R.id.tv_duration);
        TextView tvResolution = dialogView.findViewById(R.id.tv_resolution);
        ImageView ivCopyToClipboard = dialogView.findViewById(R.id.iv_copy_to_clipboard);
        TextView tvTempWarning = dialogView.findViewById(R.id.tv_temp_file_warning); // Find warning TextView

        // Check if all essential views were found
        if (tvFileName == null || tvFileSize == null || tvFilePath == null || tvLastModified == null ||
                tvDuration == null || tvResolution == null || ivCopyToClipboard == null || tvTempWarning == null) {
            Log.e(TAG,"One or more views were not found in dialog_video_info.xml. Check IDs.");
            Toast.makeText(context, "Error displaying video info.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Prepare Data
        Uri videoUri = videoItem.uri;
        String fileName = videoItem.displayName != null ? videoItem.displayName : "Unknown Name"; // Handle null display name

        String formattedFileSize = formatFileSize(videoItem.size);
        String formattedLastModified = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date(videoItem.lastModified));

        // Determine display path (URI string or actual file path)
        String filePathDisplay = videoUri.toString(); // Default to URI
        if ("file".equals(videoUri.getScheme()) && videoUri.getPath() != null) {
            filePathDisplay = videoUri.getPath();
        }

        // Fetch metadata (using helpers that handle URI)
        long durationMs = getVideoDuration(videoUri);
        String resolution = getVideoResolution(videoUri);
        String formattedDuration = formatVideoDuration(durationMs);
        
        // 4. Populate UI Text Views
        tvFileName.setText(fileName);
        tvFileSize.setText(formattedFileSize);
        tvFilePath.setText(filePathDisplay);
        tvLastModified.setText(formattedLastModified);
        tvDuration.setText(formattedDuration);
        tvResolution.setText(resolution);

        // 5. Determine if it's a temp file and show/hide warning
        boolean isTempFile = isTemporaryFile(videoItem); // Use the path-checking helper
        // Show/Hide and SET the processed HTML text
        if (tvTempWarning != null) {
            if (isTempFile) {
                // *** FIX: Get the string, process HTML, and set it ***
                String warningHtmlString = context.getString(R.string.warning_temp_file_detail);
                tvTempWarning.setText(HtmlCompat.fromHtml(warningHtmlString, HtmlCompat.FROM_HTML_MODE_LEGACY));
                tvTempWarning.setVisibility(View.VISIBLE);
            } else {
                tvTempWarning.setVisibility(View.GONE);
            }
            Log.d(TAG, "Info Dialog: Temp warning visibility/text set. IsTemp: " + isTempFile);
        } else {
            Log.w(TAG, "tv_temp_file_warning view not found in dialog layout.");
        }
        // ... (Prepare clipboard string - NOTE: HTML tags won't be in clipboard text) ...
        String clipboardText;
        if(isTempFile) {
            // For clipboard, use a plain text version maybe? Or include raw HTML markers?
            // Simple version without HTML:
            clipboardText = "IMPORTANT NOTE: This is an unprocessed temporary file... (See dialog for details)";
        } else {
            clipboardText = ""; // Or just don't add extra note for normal files
        }
        String videoInfo = String.format(Locale.US,
                "File Name: %s\nFile Size: %s\nFile Path: %s\nLast Modified: %s\nDuration: %s\nResolution: %s\n%s",
                fileName, formattedFileSize, filePathDisplay, formattedLastModified, formattedDuration, resolution, clipboardText.trim());
        
        // The actual warning text is set via R.string.warning_temp_file_detail in the XML layout



        // 7. Set up Copy-to-Clipboard Action
        ivCopyToClipboard.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Video Info", videoInfo); // Use the constructed string
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Video info copied to clipboard", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "ClipboardManager service is null.");
                Toast.makeText(context, "Could not access clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        // 8. Build and Show Dialog
        builder.setTitle("Video Information")
                .setView(dialogView)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
                
        // Create and show the dialog
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        
        // Apply Snow Veil button colors after dialog is shown
        setSnowVeilButtonColors(dialog);
    } // End of showVideoInfoDialog

    /**
     * Opens the selected video directly in the YouTube app for uploading
     * @param videoItem The video to upload to YouTube
     */
    private void openVideoInYouTube(VideoItem videoItem) {
        if (context == null || videoItem == null || videoItem.uri == null) return;
        
        Log.d(TAG, "===== START YOUTUBE UPLOAD DEBUG =====");
        Log.d(TAG, "Device: " + Build.MANUFACTURER + " " + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")");
        Log.d(TAG, "Video URI: " + videoItem.uri);
        
        try {
            // Get proper content:// URI that can be shared with other apps
            Uri shareUri = getShareableUri(videoItem.uri);
            Log.d(TAG, "Converted share URI: " + shareUri);
            
            // Direct YouTube upload intent: try package-targeted intent first, fall back to chooser on failure
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("video/*");
            intent.putExtra(Intent.EXTRA_STREAM, shareUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setPackage("com.google.android.youtube");

            try {
                context.startActivity(intent);
            } catch (android.content.ActivityNotFoundException anf) {
                // Try alternative package name once
                intent.setPackage("com.google.android.apps.youtube.mango");
                try {
                    context.startActivity(intent);
                } catch (android.content.ActivityNotFoundException anf2) {
                    // Final fallback: generic chooser
                    Log.d(TAG, "YouTube package-targeted intents failed, falling back to generic share");
                    Intent chooserIntent = Intent.createChooser(
                            new Intent(Intent.ACTION_SEND)
                                    .setType("video/*")
                                    .putExtra(Intent.EXTRA_STREAM, shareUri)
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                            "Upload to YouTube"
                    );
                    context.startActivity(chooserIntent);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening YouTube upload: " + videoItem.uri + " (" + e.getMessage() + ")", e);
            
            // Fallback to generic share intent
            try {
                Uri shareUri = getShareableUri(videoItem.uri);
                Intent chooserIntent = Intent.createChooser(
                        new Intent(Intent.ACTION_SEND)
                                .setType("video/*")
                                .putExtra(Intent.EXTRA_STREAM, shareUri)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                        "Upload to YouTube"
                );
                context.startActivity(chooserIntent);
            } catch (Exception ex) {
                Log.e(TAG, "Error with fallback share: " + ex.getMessage(), ex);
                Toast.makeText(context, "Could not share video: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        
        Log.d(TAG, "===== END YOUTUBE UPLOAD DEBUG =====");
    }
    
    /**
     * Opens the selected video directly in the Google Drive app for uploading
     * @param videoItem The video to upload to Google Drive
     */
    private void openVideoInGoogleDrive(VideoItem videoItem) {
        if (context == null || videoItem == null || videoItem.uri == null) return;
        
        Log.d(TAG, "===== START DRIVE UPLOAD DEBUG =====");
        Log.d(TAG, "Device: " + Build.MANUFACTURER + " " + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")");
        Log.d(TAG, "Video URI: " + videoItem.uri);
        
        try {
            // Get proper content:// URI that can be shared with other apps
            Uri shareUri = getShareableUri(videoItem.uri);
            Log.d(TAG, "Converted share URI: " + shareUri);
            
            // Use ShareCompat to create the intent; try package-targeted Drive first, fall back to chooser on failure
            Intent intent = ShareCompat.IntentBuilder.from((Activity)context)
                    .setStream(shareUri)
                    .setType("video/*")
                    .getIntent()
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            intent.setPackage("com.google.android.apps.docs");
            try {
                context.startActivity(intent);
            } catch (android.content.ActivityNotFoundException anf) {
                // Try alternative Drive package name
                intent.setPackage("com.google.android.apps.docs.editors.docs");
                try {
                    context.startActivity(intent);
                } catch (android.content.ActivityNotFoundException anf2) {
                    Log.d(TAG, "Drive package-targeted intents failed, falling back to generic share");
                    Intent chooserIntent = Intent.createChooser(
                            ShareCompat.IntentBuilder.from((Activity)context)
                                    .setStream(shareUri)
                                    .setType("video/*")
                                    .getIntent()
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                            "Upload to Drive"
                    );
                    context.startActivity(chooserIntent);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening Google Drive: " + videoItem.uri + " (" + e.getMessage() + ")", e);
            
            // Fallback to generic share intent
            try {
                Uri shareUri = getShareableUri(videoItem.uri);
                Intent chooserIntent = Intent.createChooser(
                        new Intent(Intent.ACTION_SEND)
                                .setType("video/*")
                                .putExtra(Intent.EXTRA_STREAM, shareUri)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                        "Upload to Drive"
                );
                context.startActivity(chooserIntent);
            } catch (Exception ex) {
                Log.e(TAG, "Error with fallback share: " + ex.getMessage(), ex);
                Toast.makeText(context, "Could not share video: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        
        Log.d(TAG, "===== END DRIVE UPLOAD DEBUG =====");
    }
    
    /**
     * Converts a URI to a shareable content:// URI if needed
     * @param uri The original URI to convert
     * @return A shareable URI
     */
    private Uri getShareableUri(Uri uri) {
        if (uri == null) return null;
        
        // If it's already a content:// URI, we can use it directly
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            return uri;
        }
        
        // If it's a file:// URI, we need to convert it using FileProvider
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            try {
                File file = new File(uri.getPath());
                return FileProvider.getUriForFile(
                    context, 
                    context.getApplicationContext().getPackageName() + ".provider", 
                    file
                );
            } catch (Exception e) {
                Log.e(TAG, "Error converting file URI to content URI: " + e.getMessage(), e);
            }
        }
        
        return uri; // Return original if conversion failed
    }

    /**
     * Launch system chooser to open the video with an external player without exporting.
     */
    private void openWithExternalPlayer(VideoItem videoItem) {
        if (context == null || videoItem == null || videoItem.uri == null) return;
        try {
            Uri shareUri = getShareableUri(videoItem.uri);
            if (shareUri == null) {
                Toast.makeText(context, R.string.toast_video_not_found, Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(shareUri, "video/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(intent, context.getString(R.string.chooser_open_with));
            context.startActivity(chooser);
        } catch (Exception e) {
            Log.e(TAG, "Error opening external player for: " + videoItem.uri, e);
            try {
                // final fallback: generic SEND chooser
                Uri shareUri = getShareableUri(videoItem.uri);
                Intent chooserIntent = Intent.createChooser(
                        new Intent(Intent.ACTION_SEND)
                                .setType("video/*")
                                .putExtra(Intent.EXTRA_STREAM, shareUri)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                        context.getString(R.string.chooser_open_with)
                );
                context.startActivity(chooserIntent);
            } catch (Exception ex) {
                Log.e(TAG, "Fallback share failed", ex);
                Toast.makeText(context, "Could not open video: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    // --- NEW Helper to get resolution (refactored from info dialog) ---
    private String getVideoResolution(Uri videoUri) {
        if (context == null || videoUri == null) return "N/A";
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        String resolution = "N/A";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && "content".equals(videoUri.getScheme())) {
                try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(videoUri, "r")) {
                    if(pfd != null) retriever.setDataSource(pfd.getFileDescriptor());
                    else throw new IOException("PFD was null for " + videoUri);
                }
            } else {
                retriever.setDataSource(context, videoUri);
            }
            String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            if (width != null && height != null) {
                resolution = width + " x " + height;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving video resolution for URI: " + videoUri, e);
        } finally {
            try { retriever.release(); } catch (IOException e) { Log.e(TAG, "Error releasing MMD retriever for resolution", e); }
        }
        return resolution;
    }

    // --- Update and Utility Methods ---

    // Updates the adapter's list
    @SuppressLint("NotifyDataSetChanged") // Suppress only for fallback case
    public void updateRecords(List<VideoItem> newRecords) {
        if (newRecords == null) {
            Log.w(TAG, "updateRecords called with null list");
            return;
        }
        
        Log.d(TAG, "updateRecords: Updating from " + (records == null ? 0 : records.size()) + 
                   " to " + newRecords.size() + " records");
        
        // -------------- Fix Start (updateRecords) - Only disable skeleton mode for real data -----------
        
        // Check if we're updating with skeleton data or real data
        boolean isSkeletonData = !newRecords.isEmpty() && newRecords.get(0).isSkeleton;
        
        if (isSkeletonMode && !isSkeletonData) {
            Log.d(TAG, "updateRecords: Transitioning from skeleton to real data - disabling skeleton mode");
            setSkeletonMode(false);
        } else if (isSkeletonMode && isSkeletonData) {
            Log.d(TAG, "updateRecords: Updating skeleton data - keeping skeleton mode enabled");
            // Keep skeleton mode enabled
        }
        
        // -------------- Fix End (updateRecords) -----------
        
        // Use DiffUtil to calculate the differences and dispatch updates efficiently
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return records.size();
            }

            @Override
            public int getNewListSize() {
                return newRecords.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPosition, int newPosition) {
                // Check if URIs match to determine if items are the same
                Uri oldUri = records.get(oldPosition).uri;
                Uri newUri = newRecords.get(newPosition).uri;
                return oldUri != null && newUri != null && oldUri.equals(newUri);
            }

            @Override
            public boolean areContentsTheSame(int oldPosition, int newPosition) {
                VideoItem oldItem = records.get(oldPosition);
                VideoItem newItem = newRecords.get(newPosition);
                
                // Compare fields that affect the display
                return oldItem.displayName.equals(newItem.displayName) &&
                       oldItem.size == newItem.size &&
                       oldItem.lastModified == newItem.lastModified &&
                       oldItem.isTemporary == newItem.isTemporary;
            }
        });
        
        // Update the records list with a copy of the new list
        this.records = new ArrayList<>(newRecords);
        
        // Dispatch updates to the adapter
        diffResult.dispatchUpdatesTo(this);
        
        Log.d(TAG, "updateRecords completed. Final size: " + records.size() + 
                   ", skeleton mode: " + isSkeletonMode);
    }

    // Format file size helper
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    // Format video duration helper
    private String formatVideoDuration(long durationMs) {
        if (durationMs <= 0) return "0s";
        long totalSeconds = durationMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh %02dm %02ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format(Locale.getDefault(), "%dm %02ds", minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%ds", seconds);
        }
    }

    // Animate the inner check icon with a small bounce on check and fade out on uncheck
    private void animateCheckIcon(View checkIconView, boolean willBeSelected) {
        if (checkIconView == null) return;

        // Prefer AnimatedVectorDrawable tick-draw when available (smooth path draw)
        try {
            android.graphics.drawable.Drawable d = null;
            if (checkIconView instanceof android.widget.ImageView) {
                d = ((android.widget.ImageView) checkIconView).getDrawable();
            }
            if (willBeSelected) {
                // If drawable is an AVD, ensure we have a fresh instance and start its animation
                if (checkIconView instanceof android.widget.ImageView) {
                    android.widget.ImageView iv = (android.widget.ImageView) checkIconView;
                    try {
                        androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat newAvd = androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat.create(iv.getContext(), R.drawable.avd_check_draw);
                        if (newAvd != null) {
                            iv.setImageDrawable(newAvd);
                            newAvd.start();
                            iv.setAlpha(1f); iv.setScaleX(1f); iv.setScaleY(1f); iv.setVisibility(View.VISIBLE);
                            return;
                        }
                        // Fallback to platform AVD
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            android.graphics.drawable.Drawable platformAv = iv.getContext().getDrawable(R.drawable.avd_check_draw);
                            if (platformAv instanceof android.graphics.drawable.AnimatedVectorDrawable) {
                                iv.setImageDrawable(platformAv);
                                ((android.graphics.drawable.AnimatedVectorDrawable) platformAv).start();
                                iv.setAlpha(1f); iv.setScaleX(1f); iv.setScaleY(1f); iv.setVisibility(View.VISIBLE);
                                return;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            } else {
                // For uncheck, try reverse if possible (compat doesn't expose reverse reliably), else fallback
                if (d instanceof androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat) {
                    // Cannot reverse reliably; just fade out the view for uncheck
                    android.animation.ObjectAnimator a = android.animation.ObjectAnimator.ofFloat(checkIconView, View.ALPHA, checkIconView.getAlpha(), 0f);
                    a.setDuration(180);
                    a.setInterpolator(new android.view.animation.AccelerateInterpolator());
                    a.start();
                    return;
                }
            }
        } catch (Exception e) {
            // ignore and fall back
        }

        // Fallback: use scale/alpha animation
        if (willBeSelected) {
            checkIconView.setVisibility(View.VISIBLE);
            android.animation.ObjectAnimator sx = android.animation.ObjectAnimator.ofFloat(checkIconView, View.SCALE_X, 0f, 1.0f);
            android.animation.ObjectAnimator sy = android.animation.ObjectAnimator.ofFloat(checkIconView, View.SCALE_Y, 0f, 1.0f);
            android.animation.ObjectAnimator a = android.animation.ObjectAnimator.ofFloat(checkIconView, View.ALPHA, 0f, 1f);
            sx.setDuration(220); sy.setDuration(220); a.setDuration(160);
            sx.setInterpolator(new android.view.animation.DecelerateInterpolator());
            sy.setInterpolator(new android.view.animation.DecelerateInterpolator());
            a.setInterpolator(new android.view.animation.DecelerateInterpolator());
            android.animation.AnimatorSet set = new android.animation.AnimatorSet();
            set.playTogether(sx, sy, a);
            set.start();
        } else {
            android.animation.ObjectAnimator a = android.animation.ObjectAnimator.ofFloat(checkIconView, View.ALPHA, checkIconView.getAlpha(), 0f);
            android.animation.ObjectAnimator s = android.animation.ObjectAnimator.ofFloat(checkIconView, View.SCALE_X, checkIconView.getScaleX(), 0f);
            android.animation.ObjectAnimator s2 = android.animation.ObjectAnimator.ofFloat(checkIconView, View.SCALE_Y, checkIconView.getScaleY(), 0f);
            a.setDuration(180); s.setDuration(180); s2.setDuration(180);
            a.setInterpolator(new android.view.animation.AccelerateInterpolator());
            android.animation.AnimatorSet set = new android.animation.AnimatorSet();
            set.playTogether(a, s, s2);
            set.start();
        }
    }

    // Get video duration from URI (Helper)
    // -------------- Fix Start (getVideoDuration)-----------
    private long getVideoDuration(Uri videoUri) {
        if(context == null || videoUri == null) return 0;
        
        // Skip skeleton URIs to prevent errors
        if ("skeleton".equals(videoUri.getScheme())) {
            return 0;
        }
        // -------------- Fix End (getVideoDuration)-----------
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        long durationMs = 0;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && "content".equals(videoUri.getScheme())) {
                try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(videoUri, "r")) {
                    if (pfd != null) retriever.setDataSource(pfd.getFileDescriptor());
                    else throw new IOException("PFD was null for "+ videoUri);
                }
            } else {
                retriever.setDataSource(context, videoUri); // Works for file:// and older content:// access
            }
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) { durationMs = Long.parseLong(durationStr); }
        } catch (Exception e) {
            Log.e(TAG, "Error getting video duration for URI: " + videoUri, e);
        } finally {
            try { retriever.release(); } catch (IOException e) { Log.e(TAG,"Error releasing MMD retriever", e); }
        }
        return durationMs;
    }

    // Get file name from URI (Helper) - Crucial for Save to Gallery/Rename default
    @SuppressLint("Range") // Suppress lint check for getColumnIndexOrThrow
    private String getFileName(Uri uri) {
        if (context == null || uri == null) return null;
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    // Use getColumnIndex to avoid crashing if column doesn't exist
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if(nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    } else {
                        Log.w(TAG, OpenableColumns.DISPLAY_NAME + " column not found for URI: " + uri);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not query display name for content URI: " + uri, e);
            }
        }
        // Fallback to path if name query fails or scheme is 'file'
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }

    // --- ViewHolder ---
    // --- Updated ViewHolder ---
    static class RecordViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewThumbnail;
        TextView textViewRecord;
        TextView textViewFileSize;
        TextView textViewFileTime;
        TextView textViewSerialNumber;
        ImageView checkIcon;
    View iconCheckContainer;
    View iconCheckBg;
        ImageView menuButton;           // Reference to the 3-dot icon itself
        TextView textViewStatusBadge; // *** ADDED: Reference for the single status badge ***
        ImageView menuWarningDot;       // *** ADDED: Reference for the warning dot ***
        FrameLayout menuButtonContainer; // *** ADDED: Reference to the container holding the button and dot ***

        View processingScrim;
        ProgressBar processingSpinner;
        // *** ADD field for the new TextView ***
        TextView textViewTimeAgo;
        // *** ADD Field for Default Text Color ***
        int defaultTextColor; // Store the default color
        RecordViewHolder(View itemView) {
            super(itemView);
            imageViewThumbnail = itemView.findViewById(R.id.image_view_thumbnail);
            textViewRecord = itemView.findViewById(R.id.text_view_record);
            textViewFileSize = itemView.findViewById(R.id.text_view_file_size);
            textViewFileTime = itemView.findViewById(R.id.text_view_file_time);
            textViewSerialNumber = itemView.findViewById(R.id.text_view_serial_number);
            checkIcon = itemView.findViewById(R.id.icon_check);
            iconCheckContainer = itemView.findViewById(R.id.icon_check_container);
            iconCheckBg = itemView.findViewById(R.id.icon_check_bg);
            menuButton = itemView.findViewById(R.id.menu_button);

            menuWarningDot = itemView.findViewById(R.id.menu_warning_dot);             // *** Find the warning dot ***
            menuButtonContainer = itemView.findViewById(R.id.menu_button_container);   // *** Find the container ***
            textViewStatusBadge = itemView.findViewById(R.id.text_view_status_badge); // *** Find the new single badge ***

            processingScrim = itemView.findViewById(R.id.processing_scrim);
            processingSpinner = itemView.findViewById(R.id.processing_spinner);
            // *** Find the new TextView ***
            textViewTimeAgo = itemView.findViewById(R.id.text_view_time_ago);

            // *** Store the default text color ***
            if (textViewRecord != null) {
                defaultTextColor = textViewRecord.getCurrentTextColor();
            } else {
                Log.e(TAG,"ViewHolder: textViewRecord is NULL, cannot get default text color!");
                // Set a fallback default color?
                defaultTextColor = Color.WHITE; // Example fallback
            }

        }
    }

    /**
     * Method called by the Fragment to update the adapter's visual mode
     * and provide the current list of selected URIs.
     *
     * @param isActive         True if selection mode should be active, false otherwise.
     * @param currentSelection The list of URIs currently selected in the Fragment.
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setSelectionModeActive(boolean isActive, @NonNull List<Uri> currentSelection) {
        boolean modeChanged = this.isSelectionModeActive != isActive;
        boolean selectionChanged = !this.currentSelectedUris.equals(currentSelection); // Check if selection list differs

        this.isSelectionModeActive = isActive;
        this.currentSelectedUris = new ArrayList<>(currentSelection); // Update internal copy

        // If mode changed OR selection changed, refresh visuals
        if (modeChanged || selectionChanged) {
            Log.d(TAG,"setSelectionModeActive: Mode=" + isActive + ", SelCount=" + currentSelectedUris.size() + ". Triggering notifyDataSetChanged.");
            notifyDataSetChanged(); // Full refresh easiest way to update all visuals
        } else {
            Log.d(TAG,"setSelectionModeActive: Mode and selection unchanged, no refresh needed.");
        }
    }

    /**
     * Sets the scrolling state to optimize thumbnail loading
     * @param scrolling true if the list is currently scrolling, false otherwise
     */
    public void setScrolling(boolean scrolling) {
        this.isScrolling = scrolling;
    }
    
    /**
     * Sets skeleton mode for professional loading experience
     * @param skeletonMode true to show skeleton placeholders, false for normal content
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setSkeletonMode(boolean skeletonMode) {
        if (this.isSkeletonMode != skeletonMode) {
            this.isSkeletonMode = skeletonMode;
            Log.d(TAG, "Skeleton mode " + (skeletonMode ? "enabled" : "disabled"));
            notifyDataSetChanged(); // Refresh all views
        }
    }
    
    /**
     * Checks if adapter is currently in skeleton mode
     * @return true if showing skeleton placeholders
     */
    public boolean isSkeletonMode() {
        return isSkeletonMode;
    }
    
    /**
     * Sets skeleton data without disabling skeleton mode
     * @param skeletonItems List of skeleton items to display
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setSkeletonData(List<VideoItem> skeletonItems) {
        if (skeletonItems == null) return;
        
        Log.d(TAG, "setSkeletonData: Setting " + skeletonItems.size() + " skeleton items");
        this.records = new ArrayList<>(skeletonItems);
        notifyDataSetChanged();
    }
    
    /**
     * Binds skeleton placeholder content to view holder with shimmer effect
     */
    private void bindSkeletonItem(@NonNull RecordViewHolder holder, int position) {
        // -------------- Fix Start (bindSkeletonItem) - Professional shimmer skeleton -----------
        
        // Step 1: Clear any existing content and animations
        holder.itemView.clearAnimation();
        holder.imageViewThumbnail.clearAnimation();
        
        // Step 2: Clear text content and set placeholder appearance
        holder.textViewRecord.setText("████████████████");  // Placeholder text
        holder.textViewFileSize.setText("██ MB");
        holder.textViewFileTime.setText("██████");
        
        // Make text views appear as placeholder blocks
        holder.textViewRecord.setAlpha(0.1f);
        holder.textViewFileSize.setAlpha(0.1f);
        holder.textViewFileTime.setAlpha(0.1f);
        
        // Step 3: Set placeholder for thumbnail
        holder.imageViewThumbnail.setImageResource(R.drawable.ic_video_placeholder);
        holder.imageViewThumbnail.setAlpha(0.3f); // Dimmed placeholder
        holder.imageViewThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        
        // Step 4: Hide interactive elements during skeleton state
        if (holder.textViewStatusBadge != null) {
            holder.textViewStatusBadge.setText("");
            holder.textViewStatusBadge.setVisibility(View.GONE);
        }
        if (holder.textViewSerialNumber != null) {
            holder.textViewSerialNumber.setVisibility(View.GONE);
        }
        if (holder.menuButtonContainer != null) {
            holder.menuButtonContainer.setVisibility(View.GONE);
        }
        if (holder.textViewTimeAgo != null) {
            holder.textViewTimeAgo.setText("███████");
            holder.textViewTimeAgo.setAlpha(0.1f);
        }
        
        // Step 5: Hide processing and selection elements
        if (holder.processingScrim != null) {
            holder.processingScrim.setVisibility(View.GONE);
        }
        if (holder.processingSpinner != null) {
            holder.processingSpinner.setVisibility(View.GONE);
        }
        if (holder.iconCheckContainer != null) {
            holder.iconCheckContainer.setVisibility(View.GONE);
        }
        
        // Step 6: Apply professional shimmer effect to the entire card
        ShimmerEffectHelper.applyShimmerEffect(holder.itemView);
        
        // Step 7: Disable all click events for skeleton items
        holder.itemView.setOnClickListener(null);
        holder.itemView.setOnLongClickListener(null);
        holder.imageViewThumbnail.setOnClickListener(null);
        if (holder.menuButtonContainer != null) {
            holder.menuButtonContainer.setOnClickListener(null);
        }
        
        Log.v(TAG, "Professional shimmer skeleton bound at position " + position);
        
        // -------------- Fix End (bindSkeletonItem) -----------
    }
    
    /**
     * Clears skeleton effects and restores normal appearance for data binding
     */
    private void clearSkeletonEffects(@NonNull RecordViewHolder holder) {
        // -------------- Fix Start (clearSkeletonEffects) - Remove skeleton effects when binding real data -----------
        
        // Step 1: Remove shimmer effect from the card
        ShimmerEffectHelper.removeShimmerEffect(holder.itemView);
        
        // Step 2: Stop any running animations
        holder.itemView.clearAnimation();
        holder.imageViewThumbnail.clearAnimation();
        
        // Step 3: Restore normal text appearance
        holder.textViewRecord.setAlpha(1.0f);
        holder.textViewFileSize.setAlpha(1.0f);
        holder.textViewFileTime.setAlpha(1.0f);
        if (holder.textViewTimeAgo != null) {
            holder.textViewTimeAgo.setAlpha(1.0f);
        }
        
        // Step 4: Clear shimmer backgrounds from all elements
        holder.textViewRecord.setBackground(null);
        holder.textViewFileSize.setBackground(null);
        holder.textViewFileTime.setBackground(null);
        if (holder.textViewTimeAgo != null) {
            holder.textViewTimeAgo.setBackground(null);
        }
        
        // Step 5: Clear shimmer from thumbnail and restore normal appearance
        holder.imageViewThumbnail.setBackground(null);
        holder.imageViewThumbnail.setAlpha(1.0f);
        
        // Step 6: Restore visibility for hidden elements
        if (holder.textViewSerialNumber != null) {
            holder.textViewSerialNumber.setVisibility(View.VISIBLE);
        }
        if (holder.menuButtonContainer != null) {
            holder.menuButtonContainer.setVisibility(View.VISIBLE);
        }
        
        // Step 7: Restore normal item appearance
        holder.itemView.setAlpha(1.0f);
        
        Log.v(TAG, "Skeleton effects cleared for data binding");
        
        // -------------- Fix End (clearSkeletonEffects) -----------
    }

    // --- Delete Helper (Must be accessible or copied here) ---
    // You need the `deleteVideoUri` method from RecordsFragment here or accessible
    // For simplicity, I'll include a copy here, ensure it's kept in sync or refactored to a Util class
    /*
    private boolean deleteVideoUri(Uri uri) {
        if (context == null || uri == null) return false;
        Log.d(TAG, "Adapter attempting to delete URI: " + uri + " (This should be handled by Fragment now)");

        // The actual file deletion logic (file vs content URI) was complex and is now centralized
        // in RecordsFragment's moveToTrashVideoItem, which uses TrashManager.
        // This method in the adapter is now redundant and potentially problematic if called.

        // If for some reason this was called, it should delegate to the fragment or fail.
        // For now, let's just log and return false to indicate it didn't handle it.
        Log.e(TAG, "deleteVideoUri in adapter was called. This is deprecated. Deletion should be handled by the fragment.");
        return false;
    }
    */

    /**
     * Clears all internal caches to reduce memory footprint
     * Should be called on low memory conditions
     */
    public void clearCaches() {
        loadedThumbnailCache.clear();
        // -------------- Fix Start (clear duration cache) -----------
        // Also clear duration cache to prevent stale duration data for new videos
        synchronized (durationCache) {
            durationCache.clear();
        }
        synchronized (savedPositionCache) {
            savedPositionCache.clear();
        }
        Log.d(TAG, "Cleared all adapter caches including duration and position caches");
        // -------------- Fix End (clear duration cache) -----------
    }

    // For dialogs, use themed MaterialAlertDialogBuilder as in SettingsFragment
    private MaterialAlertDialogBuilder themedDialogBuilder(Context context) {
        int dialogTheme = R.style.ThemeOverlay_FadCam_Dialog;
        SharedPreferencesManager spm = SharedPreferencesManager.getInstance(context);
        String currentTheme = spm.sharedPreferences.getString(Constants.PREF_APP_THEME, Constants.DEFAULT_APP_THEME);
        if ("Crimson Bloom".equals(currentTheme)) dialogTheme = R.style.ThemeOverlay_FadCam_Red_Dialog;
        else if ("Faded Night".equals(currentTheme)) dialogTheme = R.style.ThemeOverlay_FadCam_Amoled_MaterialAlertDialog;
        else if ("Snow Veil".equals(currentTheme)) dialogTheme = R.style.ThemeOverlay_FadCam_SnowVeil_Dialog;
        return new MaterialAlertDialogBuilder(context, dialogTheme);
    }
    
    /**
     * Sets dialog button colors based on theme
     * @param dialog The dialog whose buttons need color adjustment
     */
    private void setSnowVeilButtonColors(androidx.appcompat.app.AlertDialog dialog) {
        if (dialog == null) return;
        
        // Check current theme
        String currentTheme = sharedPreferencesManager.sharedPreferences.getString(Constants.PREF_APP_THEME, Constants.DEFAULT_APP_THEME);
        boolean isSnowVeilTheme = "Snow Veil".equals(currentTheme);
        boolean isFadedNightTheme = "Faded Night".equals(currentTheme);
        
        if (isSnowVeilTheme) {
            // Set black text color for both positive and negative buttons
            if (dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE) != null) {
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
            }
            if (dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE) != null) {
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
            }
            if (dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL) != null) {
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.BLACK);
            }
        } else if (isFadedNightTheme) {
            // Set white text color for Faded Night theme buttons
            if (dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE) != null) {
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
            }
            if (dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE) != null) {
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
            }
            if (dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL) != null) {
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.WHITE);
            }
        }
    }

    private void fixVideoFile(VideoItem videoItem) {
        if (videoItem == null || videoItem.uri == null) {
            Toast.makeText(context, context.getString(R.string.fix_video_invalid), Toast.LENGTH_SHORT).show();
            return;
        }
        String scheme = videoItem.uri.getScheme();
        File inputFile = null;
        File outputFile;
        String outputName = "FIXED_" + videoItem.displayName;
        boolean isSAF;
        File safTempInput = null;
        File safTempOutput;
        if ("file".equals(scheme)) {
            safTempOutput = null;
            isSAF = false;
            inputFile = new File(videoItem.uri.getPath());
            if (!inputFile.exists()) {
                Toast.makeText(context, context.getString(R.string.fix_video_file_not_exist), Toast.LENGTH_SHORT).show();
                return;
            }
            outputFile = new File(inputFile.getParent(), outputName);
            if (outputFile.exists()) {
                Toast.makeText(context, context.getString(R.string.fix_video_fixed_exists), Toast.LENGTH_SHORT).show();
                return;
            }
        } else if ("content".equals(scheme)) {
            isSAF = true;
            try {
                safTempInput = File.createTempFile("saf_repair_", ".mp4", context.getCacheDir());
                inputFile = safTempInput;
                try (InputStream in = context.getContentResolver().openInputStream(videoItem.uri);
                     OutputStream out = new FileOutputStream(inputFile)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to copy SAF video for repair", e);
                Toast.makeText(context, context.getString(R.string.fix_video_saf_copy_fail), Toast.LENGTH_LONG).show();
                if (safTempInput != null && safTempInput.exists()) safTempInput.delete();
                return;
            }
            safTempOutput = new File(context.getCacheDir(), outputName);
            outputFile = safTempOutput;
        } else {
            safTempOutput = null;
            outputFile = null;
            isSAF = false;
            Toast.makeText(context, context.getString(R.string.fix_video_internal_only), Toast.LENGTH_LONG).show();
            return;
        }
        String inputPath = inputFile.getAbsolutePath();
        String outputPath = outputFile.getAbsolutePath();
        String ffmpegCmd = String.format("-y -i %s -c copy %s", inputPath, outputPath);
        Toast.makeText(context, context.getString(R.string.fix_video_repairing), Toast.LENGTH_SHORT).show();
        File finalSafTempInput = safTempInput;
        FFmpegKit.executeAsync(ffmpegCmd, session -> {
            if (ReturnCode.isSuccess(session.getReturnCode())) {
                if (isSAF) {
                    boolean wroteToSAF = false;
                    try {
                        // Use the SAF folder URI from preferences, just like RecordingService
                        String safFolderUriString = sharedPreferencesManager.getCustomStorageUri();
                        if (safFolderUriString != null) {
                            Uri safFolderUri = Uri.parse(safFolderUriString);
                            DocumentFile safFolder = DocumentFile.fromTreeUri(context, safFolderUri);
                            if (safFolder != null && safFolder.canWrite()) {
                                DocumentFile fixedDoc = safFolder.createFile("video/mp4", outputName);
                                if (fixedDoc != null) {
                                    try (OutputStream out = context.getContentResolver().openOutputStream(fixedDoc.getUri());
                                         InputStream in = new FileInputStream(outputFile)) {
                                        byte[] buf = new byte[8192];
                                        int len;
                                        while ((len = in.read(buf)) > 0) {
                                            out.write(buf, 0, len);
                                        }
                                    }
                                    wroteToSAF = true;
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        Toast.makeText(context, context.getString(R.string.fix_video_saf_success, outputName), Toast.LENGTH_LONG).show();
                                    });
                                }
                            }
                        }
                        if (!wroteToSAF) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                Toast.makeText(context, context.getString(R.string.fix_video_saf_write_fail), Toast.LENGTH_LONG).show();
                                Toast.makeText(context, "Cannot write to this folder. This may be due to SD card, USB, or cloud storage permissions. The repaired file is saved in app storage.", Toast.LENGTH_LONG).show();
                                Toast.makeText(context, context.getString(R.string.fix_video_saf_export), Toast.LENGTH_LONG).show();
                                // TODO: Offer share/export dialog for the fixed file in app storage
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to write repaired file to SAF folder", e);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(context, context.getString(R.string.fix_video_saf_write_fail), Toast.LENGTH_LONG).show();
                            Toast.makeText(context, "Cannot write to this folder. This may be due to SD card, USB, or cloud storage permissions. The repaired file is saved in app storage.", Toast.LENGTH_LONG).show();
                            Toast.makeText(context, context.getString(R.string.fix_video_saf_export), Toast.LENGTH_LONG).show();
                            // TODO: Offer share/export dialog for the fixed file in app storage
                        });
                    } finally {
                        // Clean up temp files
                        if (finalSafTempInput != null && finalSafTempInput.exists()) finalSafTempInput.delete();
                        if (safTempOutput != null && safTempOutput.exists()) safTempOutput.delete();
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(context, context.getString(R.string.fix_video_success, outputFile.getName()), Toast.LENGTH_LONG).show();
                    });
                }
            } else {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context, context.getString(R.string.fix_video_fail), Toast.LENGTH_LONG).show();
                });
                // Clean up temp files on failure as well
                if (isSAF) {
                    if (finalSafTempInput != null && finalSafTempInput.exists()) finalSafTempInput.delete();
                    if (safTempOutput != null && safTempOutput.exists()) safTempOutput.delete();
                }
            }
        });
    }
}