package com.fadcam.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fadcam.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * MiniAppInfoBottomSheetFragment
 * Shows information about mini apps, including coming soon status and features.
 */
public class MiniAppInfoBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_APP_ID = "app_id";

    public static MiniAppInfoBottomSheetFragment newInstance(String appId) {
        MiniAppInfoBottomSheetFragment fragment = new MiniAppInfoBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_APP_ID, appId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_mini_app_info_sheet, container, false);
    }

    @Override
    public void onViewCreated(
        @NonNull View view,
        @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        String appId = getArguments() != null ? getArguments().getString(ARG_APP_ID) : "";

        // Bind views
        TextView titleTv = view.findViewById(R.id.mini_app_title);
        TextView descTv = view.findViewById(R.id.mini_app_description);
        TextView comingSoonTv = view.findViewById(R.id.mini_app_coming_soon_text);
        LinearLayout featuresContainer = view.findViewById(R.id.mini_app_features_container);
        ImageView closeBtn = view.findViewById(R.id.mini_app_close_btn);

        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> dismiss());
        }

        // Populate data based on app ID
        populateMiniAppInfo(appId, titleTv, descTv, comingSoonTv, featuresContainer);
    }

    private void populateMiniAppInfo(
        String appId,
        TextView titleTv,
        TextView descTv,
        TextView comingSoonTv,
        LinearLayout featuresContainer
    ) {
        switch (appId) {
            case "compass":
                if (titleTv != null) titleTv.setText(R.string.mini_app_compass_title);
                if (descTv != null) descTv.setText(R.string.mini_app_compass_desc);
                if (comingSoonTv != null) comingSoonTv.setText(R.string.mini_app_coming_soon_desc);
                addFeatures(featuresContainer, new String[]{
                    getString(R.string.mini_app_compass_feature_bearing),
                    getString(R.string.mini_app_compass_feature_cardinal),
                    getString(R.string.mini_app_compass_feature_mag_true)
                });
                break;

            case "sound_meter":
                if (titleTv != null) titleTv.setText(R.string.mini_app_sound_meter_title);
                if (descTv != null) descTv.setText(R.string.mini_app_sound_meter_desc);
                if (comingSoonTv != null) comingSoonTv.setText(R.string.mini_app_coming_soon_desc);
                addFeatures(featuresContainer, new String[]{
                    getString(R.string.mini_app_sound_meter_feature_realtime),
                    getString(R.string.mini_app_sound_meter_feature_peak),
                    getString(R.string.mini_app_sound_meter_feature_avg)
                });
                break;

            case "sensor_dashboard":
                if (titleTv != null) titleTv.setText(R.string.mini_app_sensor_dashboard_title);
                if (descTv != null) descTv.setText(R.string.mini_app_sensor_dashboard_desc);
                if (comingSoonTv != null) comingSoonTv.setText(R.string.mini_app_coming_soon_desc);
                addFeatures(featuresContainer, new String[]{
                    getString(R.string.mini_app_sensor_dashboard_feature_accel),
                    getString(R.string.mini_app_sensor_dashboard_feature_gyro),
                    getString(R.string.mini_app_sensor_dashboard_feature_mag)
                });
                break;

            default:
                if (titleTv != null) titleTv.setText("Mini App");
                if (descTv != null) descTv.setText("Learn more about this upcoming feature");
        }
    }

    private void addFeatures(LinearLayout container, String[] features) {
        if (container == null || features == null) return;

        container.removeAllViews();

        for (String feature : features) {
            LinearLayout featureItem = new LinearLayout(requireContext());
            featureItem.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            featureItem.setOrientation(LinearLayout.HORIZONTAL);
            featureItem.setPadding(0, 12, 0, 12);

            // Bullet point icon
            TextView bulletTv = new TextView(requireContext());
            bulletTv.setText("•");
            bulletTv.setTextColor(getResources().getColor(android.R.color.white, null));
            bulletTv.setTextSize(16);
            bulletTv.setLayoutParams(new LinearLayout.LayoutParams(
                32,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            bulletTv.setGravity(android.view.Gravity.CENTER);

            // Feature text
            TextView featureTv = new TextView(requireContext());
            featureTv.setText(feature);
            featureTv.setTextColor(getResources().getColor(android.R.color.white, null));
            featureTv.setTextSize(14);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
            );
            params.setMargins(8, 0, 0, 0);
            featureTv.setLayoutParams(params);

            featureItem.addView(bulletTv);
            featureItem.addView(featureTv);
            container.addView(featureItem);
        }
    }

    @Override
    public int getTheme() {
        return R.style.CustomBottomSheetDialogTheme;
    }
}
