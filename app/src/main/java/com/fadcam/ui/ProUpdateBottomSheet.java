package com.fadcam.ui;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.fadcam.R;

public class ProUpdateBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_VERSION = "version";
    private static final String ARG_URL = "url";
    private static final String ARG_CURRENT = "current";

    public static ProUpdateBottomSheet newInstance(String version, String url, String current) {
        ProUpdateBottomSheet f = new ProUpdateBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_VERSION, version);
        args.putString(ARG_URL, url);
        args.putString(ARG_CURRENT, current);
        f.setArguments(args);
        return f;
    }

    @NonNull @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            View bottomSheet = ((com.google.android.material.bottomsheet.BottomSheetDialog) dialog)
                    .findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(R.drawable.gradient_pro_bg);
            }
        });
        return dialog;
    }

    @Override
    public int getTheme() {
        return R.style.CustomBottomSheetDialogTheme;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.bottomsheet_pro_update, container, false);

        String ver = getArguments() != null ? getArguments().getString(ARG_VERSION, "") : "";
        String url = getArguments() != null ? getArguments().getString(ARG_URL, "") : "";
        String cur = getArguments() != null ? getArguments().getString(ARG_CURRENT, "") : "";

        TextView tvVer = v.findViewById(R.id.tv_pro_sheet_version);
        if (tvVer != null) {
            String text = "v" + cur + "  >>  v" + ver;
            android.text.SpannableString sp = new android.text.SpannableString(text);
            int curEnd = ("v" + cur).length();
            sp.setSpan(new android.text.style.ForegroundColorSpan(0xFFFFFFFF), 0, curEnd, 0);
            sp.setSpan(new android.text.style.ForegroundColorSpan(0xFFFFB300), curEnd, text.length(), 0);
            tvVer.setText(sp);
        }

        Button btnGet = v.findViewById(R.id.btn_pro_get);
        if (btnGet != null) {
            btnGet.setOnClickListener(view -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception ignored) {}
                dismiss();
            });
        }

        v.findViewById(R.id.btn_pro_dismiss).setOnClickListener(view -> dismiss());
        return v;
    }
}
