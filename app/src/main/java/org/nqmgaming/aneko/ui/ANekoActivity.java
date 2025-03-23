package org.nqmgaming.aneko.ui;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.kieronquinn.monetcompat.app.MonetCompatActivity;
import com.kieronquinn.monetcompat.view.MonetSwitch;

import org.nqmgaming.aneko.service.AnimationService;
import org.nqmgaming.aneko.R;

public class ANekoActivity extends MonetCompatActivity {

    private static final int REQUEST_STORAGE_PERMISSION = 1;
    SharedPreferences prefs;
    MonetSwitch motionToggle;

    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    prefs.edit().putBoolean(AnimationService.PREF_KEY_NOTIFICATION_ENABLE, true).apply();
                } else {
                    prefs.edit().putBoolean(AnimationService.PREF_KEY_NOTIFICATION_ENABLE, false).apply();
                }
            });

    SharedPreferences.OnSharedPreferenceChangeListener prefsListener = (sharedPreferences, key) -> {
        assert key != null;
        if (key.equals(AnimationService.PREF_KEY_ENABLE)) {
            motionToggle.setChecked(prefs.getBoolean(AnimationService.PREF_KEY_ENABLE, false));
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_neko);
        prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(prefsListener);

        motionToggle = findViewById(R.id.motionEnable);
        motionToggle.setChecked(prefs.getBoolean(AnimationService.PREF_KEY_ENABLE, false));
        if (motionToggle.isChecked()) startAnimationService();

        motionToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                motionToggle.setChecked(false);
            } else {
                prefs.edit().putBoolean(AnimationService.PREF_KEY_ENABLE, isChecked).apply();
                checkNotificationPermission();
            }
        });


        Fragment fragment;
        fragment = getSupportFragmentManager().findFragmentById(R.id.neko_prefs);
        if (savedInstanceState == null || fragment == null) {
            fragment = new SettingsFragment();
        }

        Bundle bundle = new Bundle(0);
        fragment.setArguments(bundle);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.neko_prefs, fragment)
                .commit();
    }

    /**
     * Check and request notification permission if needed (Android 13+)
     */
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }

        }
        startAnimationService();
    }


    private void startAnimationService() {
        prefs.edit().putBoolean(AnimationService.PREF_KEY_VISIBLE, true).apply();
        startService(new Intent(this, AnimationService.class).setAction(AnimationService.ACTION_START));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "Storage Permission wasn't granted!!", Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }
}
