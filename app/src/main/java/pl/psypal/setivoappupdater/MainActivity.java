package pl.psypal.setivoappupdater;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2core.DownloadBlock;
import com.tonyodev.fetch2core.Func;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import io.fabric.sdk.android.Fabric;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import butterknife.BindView;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity {

    private static final String serverUrl = "http://188.116.3.175/setivo";
    private static final String configFileName = "config";
    private static String configFile;

    @BindView(R.id.check_button)
    Button check;
    @BindView(R.id.check_result)
    TextView checkResult;
    @BindView(R.id.update_button)
    Button update;
    @BindView(R.id.progress)
    ProgressBar progressBar;
    @BindView(R.id.cancel_button)
    Button cancel;

    private Fetch fetch;
    private SetivoConfig setivoConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        File appDir = getFilesDir();
        for (String file : appDir.list()) {
            new File(file).delete();
        }

        configFile = getFilesDir() + "/" + configFileName;

        ButterKnife.bind(this);

        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                versionCheck();
            }
        });

        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissions();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!fetch.isClosed()) {
                    fetch.cancelAll();
                    fetch.close();
                    progressBar.setProgress(0);
                    cancel.setVisibility(View.INVISIBLE);
                }
            }
        });
    }


    public void versionCheck() {

        FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(this)
                .setDownloadConcurrentLimit(1)
                .build();

        fetch = Fetch.Impl.getInstance(fetchConfiguration);
        String currentConfigFile = configFile + "_" + System.currentTimeMillis();
        final Request request = new Request(serverUrl + "/" + configFileName, currentConfigFile);
        request.setNetworkType(NetworkType.ALL);

        fetch.addListener(new FetchListener() {
            @Override
            public void onAdded(@NotNull Download download) {

            }

            @Override
            public void onQueued(@NotNull Download download, boolean b) {

            }

            @Override
            public void onWaitingNetwork(@NotNull Download download) {

            }

            @Override
            public void onCompleted(@NotNull Download download) {
                try {
                    if (request.getId() == download.getId()) {
                        Gson gson = new Gson();
                        try {
                            String json = getFileContents(download.getFile());
                            setivoConfig = gson.fromJson(json, SetivoConfig.class);
                            new File(download.getFile()).delete();
                            int currentVersion = getResources().getInteger(R.integer.version);
                            if (currentVersion < setivoConfig.getVersion()) {
                                checkResult.setText("Dostępna nowa wersja aplikacji");
                                update.setEnabled(true);
                            } else if (currentVersion == setivoConfig.getVersion()) {
                                checkResult.setText("Aplikacja jeste aktualna");
                            }
                        } catch (JsonSyntaxException e) {
                            Toast.makeText(MainActivity.this, "Błąd składni pliku konfiguracyjnego", Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                fetch.close();
            }

            @Override
            public void onError(@NotNull Download download, @NotNull Error error, @Nullable Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onDownloadBlockUpdated(@NotNull Download download, @NotNull DownloadBlock downloadBlock, int i) {

            }

            @Override
            public void onStarted(@NotNull Download download, @NotNull List<? extends DownloadBlock> list, int i) {

            }

            @Override
            public void onProgress(@NotNull Download download, long l, long l1) {

            }

            @Override
            public void onPaused(@NotNull Download download) {

            }

            @Override
            public void onResumed(@NotNull Download download) {

            }

            @Override
            public void onCancelled(@NotNull Download download) {

            }

            @Override
            public void onRemoved(@NotNull Download download) {

            }

            @Override
            public void onDeleted(@NotNull Download download) {

            }
        });

        fetch.enqueue(request, new Func<Request>() {
            @Override
            public void call(@NotNull Request result) {
                Toast.makeText(MainActivity.this, "Sprawdzam aktualną wersję aplikacji", Toast.LENGTH_LONG).show();
            }
        }, new Func<Error>() {
            @Override
            public void call(@NotNull Error result) {
                Toast.makeText(MainActivity.this, "Nie mogę sprawdzić aktualnej wersji aplikacji", Toast.LENGTH_LONG).show();
            }
        });


    }

    public void checkPermissions() {
        int writeExternalStoragePermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (writeExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else {
            updateApp();
        }
    }

    public String getFileContents(final String file) throws IOException {
        final InputStream inputStream = new FileInputStream(new File(file));
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        final StringBuilder stringBuilder = new StringBuilder();

        boolean done = false;

        while (!done) {
            final String line = reader.readLine();
            done = (line == null);

            if (line != null) {
                stringBuilder.append(line);
            }
        }

        reader.close();
        inputStream.close();

        return stringBuilder.toString();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            updateApp();
        } else {
            Toast.makeText(MainActivity.this, "Bez tych uprawnień nie można pobrać aktualizacji", Toast.LENGTH_LONG).show();
        }
    }

    public void updateApp() {


        FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(this)
                .setDownloadConcurrentLimit(1)
                .build();

        fetch = Fetch.Impl.getInstance(fetchConfiguration);

        String destinationPath = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            destinationPath = getFilesDir() + "/" + setivoConfig.getFile() + "_" + System.currentTimeMillis();

        } else {
            File publicDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/Setivo");
            if (publicDir.exists()) {
                publicDir.delete();
            }
            publicDir.mkdir();
            destinationPath = publicDir.getAbsolutePath() + "/" + setivoConfig.getFile() + "_" + System.currentTimeMillis();
        }
        final Request request = new Request(serverUrl + "/" + setivoConfig.getFile(), destinationPath);
        request.setNetworkType(NetworkType.ALL);

        final String destPath = destinationPath;
        fetch.addListener(new FetchListener() {
            @Override
            public void onAdded(@NotNull Download download) {

            }

            @Override
            public void onQueued(@NotNull Download download, boolean b) {

            }

            @Override
            public void onWaitingNetwork(@NotNull Download download) {

            }

            @Override
            public void onCompleted(@NotNull Download download) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Uri apkURI = FileProvider.getUriForFile(MainActivity.this, MainActivity.this.getApplicationContext().getPackageName() + ".provider", new File(destPath));
                    intent.setDataAndType(apkURI, "application/vnd.android.package-archive");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } else {
                    intent.setDataAndType(Uri.fromFile(new File(destPath)), "application/vnd.android.package-archive");
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // without this flag android returned a intent error!
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                progressBar.setProgress(0);
                update.setEnabled(false);
                checkResult.setText("Brak informacji na temat aktualnej wersji");
                cancel.setVisibility(View.GONE);
                fetch.close();
                MainActivity.this.startActivity(intent);
            }

            @Override
            public void onError(@NotNull Download download, @NotNull Error error, @Nullable Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onDownloadBlockUpdated(@NotNull Download download, @NotNull DownloadBlock downloadBlock, int i) {

            }

            @Override
            public void onStarted(@NotNull Download download, @NotNull List<? extends DownloadBlock> list, int i) {
                cancel.setVisibility(View.VISIBLE);
            }

            @Override
            public void onProgress(@NotNull Download download, long l, long l1) {
                if (request.getId() == download.getId()) {
                    progressBar.setProgress(download.getProgress());
                }
            }

            @Override
            public void onPaused(@NotNull Download download) {

            }

            @Override
            public void onResumed(@NotNull Download download) {

            }

            @Override
            public void onCancelled(@NotNull Download download) {

            }

            @Override
            public void onRemoved(@NotNull Download download) {

            }

            @Override
            public void onDeleted(@NotNull Download download) {

            }
        });

        fetch.enqueue(request, new Func<Request>() {
            @Override
            public void call(@NotNull Request result) {
                Toast.makeText(MainActivity.this, "Pobieram aktualną wersję aplikacji", Toast.LENGTH_LONG).show();
            }
        }, new Func<Error>() {
            @Override
            public void call(@NotNull Error result) {
                Toast.makeText(MainActivity.this, "Nie mogę pobrać aktualnej wersji aplikacji", Toast.LENGTH_LONG).show();
            }
        });

    }

}

