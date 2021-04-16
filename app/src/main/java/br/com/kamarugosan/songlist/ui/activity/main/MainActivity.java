package br.com.kamarugosan.songlist.ui.activity.main;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import java.util.List;
import java.util.Objects;

import br.com.kamarugosan.songlist.R;
import br.com.kamarugosan.songlist.model.Song;
import br.com.kamarugosan.songlist.storage.SongBackup;
import br.com.kamarugosan.songlist.model.SongViewModel;

public class MainActivity extends AppCompatActivity {
    private static final String THREAD_NAME_LOAD_SONG_FILES = "loadSongFiles";

    private NavController navController;
    private SongViewModel viewModel;
    private MainBroadcastReceiver broadcastReceiver;

    public MainActivity() {
        super(R.layout.activity_main);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_SongList);

        super.onCreate(savedInstanceState);

        setup();
        loadList();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }

        super.onDestroy();
    }

    public void loadList() {
        new Thread(() -> {
            List<Song> list = SongBackup.loadAll(MainActivity.this);
            viewModel.postSongList(list);
        }, THREAD_NAME_LOAD_SONG_FILES).start();
    }

    private void setup() {
        viewModel = new ViewModelProvider(this).get(SongViewModel.class);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_container);
        navController = Objects.requireNonNull(navHostFragment).getNavController();

        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        NavigationUI.setupActionBarWithNavController(this, navController);

        broadcastReceiver = new MainBroadcastReceiver(this);
        registerReceiver(broadcastReceiver, MainBroadcastReceiver.getIntentFilter());
    }
}