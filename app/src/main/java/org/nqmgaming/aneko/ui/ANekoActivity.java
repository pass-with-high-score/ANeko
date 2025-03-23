package org.nqmgaming.aneko.ui;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.kieronquinn.monetcompat.app.MonetCompatActivity;
import com.kieronquinn.monetcompat.view.MonetSwitch;

import org.nqmgaming.aneko.service.AnimationService;
import org.nqmgaming.aneko.R;

public class ANekoActivity extends MonetCompatActivity {

    SharedPreferences prefs;
    MonetSwitch motionToggle;

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
                startAnimationService();
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

    private void startAnimationService() {
        prefs.edit().putBoolean(AnimationService.PREF_KEY_VISIBLE, true).apply();
        startService(new Intent(this, AnimationService.class).setAction(AnimationService.ACTION_START));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "Storage Permission wasn't granted!!", Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }
}
