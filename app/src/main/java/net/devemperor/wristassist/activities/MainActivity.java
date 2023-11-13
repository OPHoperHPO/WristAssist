package net.devemperor.wristassist.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Toast;

import androidx.core.splashscreen.SplashScreen;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import net.devemperor.wristassist.BuildConfig;
import net.devemperor.wristassist.R;
import net.devemperor.wristassist.adapters.MainAdapter;
import net.devemperor.wristassist.items.MainItem;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

public class MainActivity extends Activity {

    WearableRecyclerView mainWrv;
    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        sp = getSharedPreferences("net.devemperor.wristassist", MODE_PRIVATE);
        if (sp.getString("net.devemperor.wristassist.userid", null) == null) {
            Random random = new Random();
            sp.edit().putString("net.devemperor.wristassist.userid", String.valueOf(random.nextInt(999999999 - 100000000) + 100000000)).apply();
        }
        FirebaseCrashlytics.getInstance().setUserId(sp.getString("net.devemperor.wristassist.userid", "null"));

        if (getIntent().getBooleanExtra("net.devemperor.wristassist.enter_api_key", false)) {
            Intent intent = new Intent(this, InputActivity.class);
            intent.putExtra("net.devemperor.wristassist.input.title", getString(R.string.wristassist_set_api_key));
            intent.putExtra("net.devemperor.wristassist.input.hint", getString(R.string.wristassist_api_key));
            startActivityForResult(intent, 1340);
        } else if (!sp.getBoolean("net.devemperor.wristassist.onboarding_complete", false)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
        } else if (sp.getInt("net.devemperor.wristassist.last_version_code", 0) < BuildConfig.VERSION_CODE) {
            startActivity(new Intent(this, ChangelogActivity.class));
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainWrv = findViewById(R.id.main_wrv);
        mainWrv.setHasFixedSize(true);
        mainWrv.setEdgeItemsCenteringEnabled(true);
        mainWrv.setLayoutManager(new WearableLinearLayoutManager(this));

        ArrayList<MainItem> menuItems = new ArrayList<>();
        menuItems.add(new MainItem(R.drawable.twotone_add_24, getString(R.string.wristassist_menu_new_chat)));
        menuItems.add(new MainItem(R.drawable.twotone_chat_24, getString(R.string.wristassist_menu_saved_chats)));
        menuItems.add(new MainItem(R.drawable.twotone_settings_24, getString(R.string.wristassist_menu_settings)));
        menuItems.add(new MainItem(R.drawable.twotone_info_24, getString(R.string.wristassist_menu_about)));

        mainWrv.setAdapter(new MainAdapter(menuItems, (menuPosition, longClick) -> {
            Intent intent;
            if (menuPosition == 0 && !longClick) {
                if (sp.getBoolean("net.devemperor.wristassist.hands-free", false)) {
                    try {
                        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                        startActivityForResult(intent, 1341);
                        return;
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                        Toast.makeText(this, R.string.wristassist_no_speech_recognition, Toast.LENGTH_SHORT).show();
                    }
                }
                intent = new Intent(this, InputActivity.class);
                intent.putExtra("net.devemperor.wristassist.input.title", getString(R.string.wristassist_enter_prompt));
                intent.putExtra("net.devemperor.wristassist.input.hint", getString(R.string.wristassist_prompt));
                startActivityForResult(intent, 1337);
            } else if (menuPosition == 0) {
                intent = new Intent(this, InputActivity.class);
                intent.putExtra("net.devemperor.wristassist.input.title", getString(R.string.wristassist_enter_system_prompt));
                intent.putExtra("net.devemperor.wristassist.input.hint", getString(R.string.wristassist_system_prompt));
                intent.putExtra("net.devemperor.wristassist.input.title2", getString(R.string.wristassist_enter_prompt));
                intent.putExtra("net.devemperor.wristassist.input.hint2", getString(R.string.wristassist_prompt));
                startActivityForResult(intent, 1338);
            } else if (menuPosition == 1) {
                intent = new Intent(this, SavedChatsActivity.class);
                startActivity(intent);
            } else if (menuPosition == 2) {
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            } else if (menuPosition == 3) {
                intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
            }
        }));
        mainWrv.requestFocus();
        mainWrv.postDelayed(() -> mainWrv.scrollBy(0, mainWrv.getChildAt(0).getHeight()), 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        Intent intent;
        if (requestCode == 1337 || requestCode == 1341) {
            String query;
            if (requestCode == 1337) query = data.getStringExtra("net.devemperor.wristassist.input.content");
            else query = Objects.requireNonNull(data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)).get(0);
            intent = new Intent(this, ChatActivity.class);
            intent.putExtra("net.devemperor.wristassist.query", query);
            startActivity(intent);
        }
        if (requestCode == 1338) {
            intent = new Intent(this, ChatActivity.class);
            intent.putExtra("net.devemperor.wristassist.query", data.getStringExtra("net.devemperor.wristassist.input.content2"));
            intent.putExtra("net.devemperor.wristassist.system_query", data.getStringExtra("net.devemperor.wristassist.input.content"));
            startActivity(intent);
        }
        if (requestCode == 1340) {
            sp.edit().putString("net.devemperor.wristassist.api_key", data.getStringExtra("net.devemperor.wristassist.input.content")).apply();
            sp.edit().putBoolean("net.devemperor.wristassist.onboarding_complete", true).apply();
        }
    }
}