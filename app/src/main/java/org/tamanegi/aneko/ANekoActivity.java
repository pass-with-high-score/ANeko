package org.tamanegi.aneko;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ANekoActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent();
        intent.setClassName("org.nqmgaming.aneko", "org.nqmgaming.aneko.presentation.ANekoActivity");
        startActivity(intent);
        finish();
    }
}