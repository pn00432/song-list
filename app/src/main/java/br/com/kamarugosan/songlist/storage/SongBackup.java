package br.com.kamarugosan.songlist.storage;

import android.app.Activity;
import android.content.res.AssetManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import br.com.kamarugosan.songlist.model.Song;

public class SongBackup {
    /**
     * The folder the songs are stored on the assets.
     */
    public static final String SONGS_FOLDER = "songs";

    /**
     * The directory for the imported songs.
     */
    public static final String IMPORTED_SONGS_DIR = "imported";

    /**
     * The custom file extension for the files used by the app.
     */
    public static final String CUSTOM_FILE_EXTENSION = "ksheet";

    /**
     * Loads all songs from the default asset directory and the imported songs directory and sorts
     * them alphabetically.
     *
     * @param activity the {@link Activity} to get the assets and files directory from
     * @return all the loaded songs. Ignores the ones that could not be parsed
     */
    @NonNull
    public static List<Song> loadAll(@NonNull Activity activity) {
        final List<Song> list = new ArrayList<>();
        final Gson gson = new Gson();

        try {
            // Reading default songs
            AssetManager assets = activity.getAssets();
            String[] assetsList = assets.list(SONGS_FOLDER);

            for (String file : assetsList) {
                InputStream fileInputStream = assets.open(SONGS_FOLDER + "/" + file);

                Song song = readSongFile(fileInputStream, gson);
                if (song != null) {
                    list.add(song);
                }
            }

            // Reading imported songs
            File importedDir = new File(activity.getFilesDir(), IMPORTED_SONGS_DIR);
            if (importedDir.exists() && importedDir.isDirectory()) {
                String[] importedFiles = importedDir.list();
                if (importedFiles != null) {
                    for (String importedFile : importedFiles) {
                        File file = new File(importedDir, importedFile);
                        InputStream fileInputStream = new FileInputStream(file);

                        Song song = readSongFile(fileInputStream, gson);

                        if (song != null) {
                            song.setImported(true);
                            song.setFilePath(file.getAbsolutePath());
                            list.add(song);
                        }
                    }
                }
            }

            list.sort((o1, o2) -> {
                int titleComparison = o1.getTitle().compareTo(o2.getTitle());
                if (titleComparison != 0) {
                    return titleComparison;
                }

                return o1.getArtist().compareTo(o2.getTitle());
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Reads the given input stream and tries to parse it into a {@link Song}.
     *
     * @param fileInputStream the file to parse into a {@link Song}
     * @param gson            the {@link Gson} instance to parse the file's content
     * @return the parsed song. Null if the song could not be parsed
     */
    @Nullable
    private static Song readSongFile(InputStream fileInputStream, Gson gson) {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream))) {
            StringBuilder fileText = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                fileText.append(line).append(System.lineSeparator());
            }

            reader.close();

            String json = fileText.toString();
            return gson.fromJson(json, Song.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Writes the given {@link Song} as a file to the imported songs directory.
     *
     * @param activity the {@link Activity} to get the files directory from
     * @param song     the song to save
     * @return true if the song was successfully saved, false otherwise
     */
    public static boolean save(@NonNull Activity activity, Song song) {
        File importedDir = new File(activity.getFilesDir(), SongBackup.IMPORTED_SONGS_DIR);

        if (!importedDir.exists()) {
            if (!importedDir.mkdir()) {
                return false;
            }
        }

        File dest = new File(importedDir, System.currentTimeMillis() + "." + SongBackup.CUSTOM_FILE_EXTENSION);

        try {
            if (!dest.createNewFile()) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dest), StandardCharsets.UTF_8))) {
            Gson gson = new Gson();

            writer.write(gson.toJson(song));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Copies the files compatible with this app from a zip file into the imported songs directory.
     *
     * @param activity the {@link Activity} to get the files directory from
     * @param data     the {@link Uri} that points to a zip file
     * @return true if the files were copied, false otherwise
     */
    public static boolean copyFromZip(@NonNull Activity activity, Uri data) {
        File importedDir = new File(activity.getFilesDir(), SongBackup.IMPORTED_SONGS_DIR);

        if (!importedDir.exists()) {
            if (!importedDir.mkdir()) {
                return false;
            }
        }

        try (InputStream fis = activity.getContentResolver().openInputStream(data); BufferedInputStream bis = new BufferedInputStream(fis); ZipInputStream zipStream = new ZipInputStream(bis)) {
            byte[] buffer = new byte[2048];

            ZipEntry entry;

            while ((entry = zipStream.getNextEntry()) != null) {
                File unzippedDest = new File(importedDir, entry.getName());

                String name = entry.getName();
                String[] split = name.split("[.]");

                if (!SongBackup.CUSTOM_FILE_EXTENSION.equals(split[split.length - 1])) {
                    continue;
                }

                int attempt = 1;
                while (unzippedDest.exists()) {
                    unzippedDest = new File(importedDir, name.substring(0, name.length() - 7) + "(" + attempt + ")" + "." + SongBackup.CUSTOM_FILE_EXTENSION);
                    attempt++;
                }

                try (FileOutputStream fos = new FileOutputStream(unzippedDest); BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length)) {
                    int len;
                    while ((len = zipStream.read(buffer)) > 0) {
                        bos.write(buffer, 0, len);
                    }
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}