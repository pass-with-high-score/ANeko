package com.nqmgaming.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import timber.log.Timber;

public class SkinActivity extends Activity {
    private static final String ANEKO_PACKAGE = "org.tamanegi.aneko";
    private static final String ANEKO_ACTIVITY =
            "org.tamanegi.aneko.ANekoActivity";

    private static final Uri ANEKO_MARKET_URI =
            Uri.parse("market://search?q=" + ANEKO_PACKAGE);

    @Override
    public void onResume() {
        super.onResume();

        boolean package_found = false;
        try {
            PackageManager pm = getPackageManager();
            package_found = (pm.getPackageInfo(ANEKO_PACKAGE, 0) != null);
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }

        int msg_id;
        final Intent intent;
        if (package_found) {
            msg_id = R.string.msg_usage;
            intent = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setClassName(ANEKO_PACKAGE, ANEKO_ACTIVITY);
        } else {
            msg_id = R.string.msg_no_package;
            intent = new Intent(Intent.ACTION_VIEW, ANEKO_MARKET_URI);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(msg_id)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Timber.e(e);
                        Toast.makeText(SkinActivity.this,
                                        R.string.msg_unexpected_err,
                                        Toast.LENGTH_SHORT)
                                .show();
                    }

                    finish();
                })
                .setOnCancelListener(
                        dialog -> finish())
                .show();
    }
}