package org.nqmgaming.aneko.ui;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.nqmgaming.aneko.service.AnimationService;
import org.nqmgaming.aneko.R;
import org.tamanegi.aneko.NekoSkin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String KEY_ICON = "icon";
    private static final String KEY_LABEL = "label";
    private static final String KEY_COMPONENT = "component";

    Context context;
    ListPreference Skin;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.neko_prefs, rootKey);
        Skin = findPreference("motion.skin");
        assert Skin != null;

        Skin.setEntries(getEntries("Neko (Built-in)", "(Installed)"));
        Skin.setEntryValues(getEntries("", ""));
        if (Skin.getEntries().length < 2) Skin.setValueIndex(0);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();
        if ("get.skin".equals(key)) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(context.getString(R.string.skin_search_uri)));

            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(context, R.string.msg_market_not_found, Toast.LENGTH_SHORT).show();
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    private CharSequence[] getEntries(String PreValue, String external) {
        List<Map<String, Object>> InstalledList = createListDataApk();

        CharSequence[] list = new CharSequence[InstalledList.size()];
        list[0] = PreValue;
        int count = 1;

        for (Map<String, Object> map : InstalledList) {
            String component = (map.get(KEY_COMPONENT) + "").replace("ComponentInfo{", "").replace("}", "");
            String name = map.get(KEY_LABEL) + " " + external;
            if (component.equals("org.nqmgaming.aneko/org.tamanegi.aneko.NekoSkin")) continue;
            list[count++] = PreValue.isEmpty() ? component : name;
        }
        return list;
    }

    private List<Map<String, Object>> createListDataApk() {
        PackageManager pm = context.getPackageManager();

        Intent[] internals = {new Intent(context, NekoSkin.class),};
        Intent intent = new Intent(AnimationService.ACTION_GET_SKIN);
        List<ResolveInfo> activities = pm.queryIntentActivityOptions(null, internals, intent, 0);

        List<Map<String, Object>> list = new ArrayList<>();

        for (ResolveInfo info : activities) {
            ComponentName comp = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
            Map<String, Object> data = new HashMap<>();
            data.put(KEY_ICON, info.loadIcon(pm));
            data.put(KEY_LABEL, info.loadLabel(pm));
            data.put(KEY_COMPONENT, comp);
            list.add(data);
        }
        return list;
    }
}