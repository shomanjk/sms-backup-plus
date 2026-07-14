package com.zegoggles.smssync.activity;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.zegoggles.smssync.R;
import com.zegoggles.smssync.preferences.Preferences;

public abstract class ThemeActivity extends AppCompatActivity {
    private static final int SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR = 16;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        final int themeResId = new Preferences(this).getAppTheme();
        setTheme(themeResId);
        if (VERSION.SDK_INT >= 26) {
            setNavBarColor(themeResId);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        applySystemBarInsets();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        applySystemBarInsets();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        applySystemBarInsets();
    }

    /**
     * Pad content for status / navigation / gesture bars. Required when targeting
     * SDK 35+, where the system draws apps edge-to-edge by default.
     */
    private void applySystemBarInsets() {
        final View content = findViewById(android.R.id.content);
        if (content instanceof ViewGroup && ((ViewGroup) content).getChildCount() > 0) {
            final View root = ((ViewGroup) content).getChildAt(0);
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
                final Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return windowInsets.inset(bars);
            });
            ViewCompat.requestApplyInsets(root);
        }
    }

    @TargetApi(26)
    private void setNavBarColor(@StyleRes final int themeId) {
        final int navBarColor = getResources().getColor(
            themeId == R.style.SMSBackupPlusTheme_Light ?
            R.color.navigation_bar_light : R.color.navigation_bar_dark, null);

        getWindow().setNavigationBarColor(navBarColor);

        int visibility = getWindow().getDecorView().getSystemUiVisibility();
        if (themeId == R.style.SMSBackupPlusTheme_Light) {
            visibility |= SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        } else {
            visibility &= ~(SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        getWindow().getDecorView().setSystemUiVisibility(visibility);
    }
}
