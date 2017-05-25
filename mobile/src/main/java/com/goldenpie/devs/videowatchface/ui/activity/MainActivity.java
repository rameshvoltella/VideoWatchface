package com.goldenpie.devs.videowatchface.ui.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.constants.Constants;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.CustomEvent;
import com.github.clans.fab.FloatingActionMenu;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.goldenpie.devs.videowatchface.BuildConfig;
import com.goldenpie.devs.videowatchface.R;
import com.goldenpie.devs.videowatchface.event.WatchFaceSenderEvent;
import com.goldenpie.devs.videowatchface.model.VideoModel;
import com.goldenpie.devs.videowatchface.ui.BaseActivity;
import com.goldenpie.devs.videowatchface.ui.adapter.WatchFaceAdapter;
import com.goldenpie.devs.videowatchface.utils.ApplicationPreference;
import com.goldenpie.devs.videowatchface.utils.DetectWear;
import com.goldenpie.stroketextview.StrokeTextView;
import com.google.android.gms.wearable.Node;
import com.hwangjr.rxbus.RxBus;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.thread.EventThread;
import com.mariux.teleport.lib.TeleportClient;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import life.knowledge4.videotrimmer.utils.FileUtils;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

/**
 * Created by EvilDev on 14.10.2016.
 */
public class MainActivity extends BaseActivity implements WatchFaceAdapter.ContentListener, DetectWear.NodesListener {

    private static final int REQUEST_VIDEO_TRIMMER = 0x01;

    private static final int REQUEST_STORAGE_READ_ACCESS_PERMISSION = 101;
    private static final int REQUEST_STORAGE_READ_ACCESS_PERMISSION_TO_RECORD = 102;

    static final String EXTRA_VIDEO_PATH = "EXTRA_VIDEO_PATH";

    @BindView(R.id.empty_list_layout)
    protected View emptyListLayout;
    @BindView(R.id.list)
    protected RecyclerView list;
    @BindView(R.id.status_textview)
    protected AppCompatTextView status;
    @BindView(R.id.fab)
    protected FloatingActionMenu actionMenu;
    @BindView(R.id.selected_gif)
    protected GifImageView selectedGif;
    @BindView(R.id.remove_button)
    protected View removeButton;
    @BindView(R.id.clock)
    protected StrokeTextView clock;

    private WatchFaceAdapter adapter;

    private boolean pickVideo = false;
    private ProgressDialog mProgressDialog;
    private TeleportClient mTeleportClient;

    private ApplicationPreference applicationPreference;

    private BroadcastReceiver timeBroadcastReceiver;
    private SimpleDateFormat timeFormat;

    @Override
    protected int getContentView() {
        return R.layout.activity_main;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RxBus.get().register(this);
        mTeleportClient = new TeleportClient(this);
        applicationPreference = new ApplicationPreference(this);
        DetectWear.setNodesListener(this);

        timeFormat = new SimpleDateFormat(DateFormat.is24HourFormat(getApplicationContext()) ? "H:mm" : "h:mm a", Locale.getDefault());
        status.setText(getString(R.string.watch_status, getString(R.string.not_connected)));

        clock.setTypeface(Typeface.createFromAsset(getAssets(), Constants.DEFAULT_FONT));
        clock.setText(timeFormat.format(new Date()));

        setupList();

        if (!TextUtils.isEmpty(applicationPreference.getCurrentGif())) {
            try {
                selectedGif.setImageDrawable(new GifDrawable(applicationPreference.getCurrentGif()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            removeButton.setVisibility(View.GONE);
        }

        actionMenu.setClosedOnTouchOutside(true);

        if (!BuildConfig.DEBUG) {
            Answers.getInstance().logContentView(new ContentViewEvent()
                    .putContentName("MAIN_ACTIVITY"));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mTeleportClient.connect();

        timeBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0)
                    clock.setText(timeFormat.format(new Date()));
            }
        };

        registerReceiver(timeBroadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));

    }

    @Override
    protected void onStop() {
        super.onStop();
        mTeleportClient.disconnect();
        if (timeBroadcastReceiver != null)
            unregisterReceiver(timeBroadcastReceiver);
    }

    private void setupList() {
        adapter = new WatchFaceAdapter(this, mTeleportClient);
        adapter.setContentListener(this);
        adapter.updateSelf();

        list.setAdapter(adapter);

        list.setLayoutManager(new GridLayoutManager(this, 3));
    }

    @OnClick(R.id.remove_button)
    protected void onRemoveClick() {
        if (DetectWear.isConnected()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.remove_dialog_title)
                    .setMessage(R.string.remove_dialog_message)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                        mTeleportClient.sendMessage(Constants.REMOVE_WATCHFACE, null);
                        applicationPreference.setCurrentGif("");

                        try {
                            selectedGif.setImageDrawable(new GifDrawable(getResources(), R.drawable.base_anim));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        removeButton.setVisibility(View.GONE);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
                    .show();
        } else {
            Toast.makeText(this, R.string.no_connection_to_watch, Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.record)
    protected void onRecordClick() {
        actionMenu.close(true);
        pickVideo = true;
        openVideoCapture();
    }

    @OnClick(R.id.share_button)
    protected void onShareClick() {
        showShareDialog();
    }

    @OnClick(R.id.select_video)
    protected void onSelectClick() {
        actionMenu.close(true);
        pickFromGallery(true);
    }

    @OnClick(R.id.select_gif)
    protected void onGifClick() {
        actionMenu.close(true);
        pickFromGallery(false);
    }

    private void openVideoCapture() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, getString(R.string.permission_read_storage_rationale), REQUEST_STORAGE_READ_ACCESS_PERMISSION_TO_RECORD);
        } else {
            Intent videoCapture = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            startActivityForResult(videoCapture, REQUEST_VIDEO_TRIMMER);
        }
    }

    private void pickFromGallery(boolean isVideo) {
        pickVideo = isVideo;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, getString(R.string.permission_read_storage_rationale), REQUEST_STORAGE_READ_ACCESS_PERMISSION);
        } else {
            if (!BuildConfig.DEBUG)
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("PICK")
                        .putContentType(isVideo ? "VIDEO" : "GIF"));

            Intent intent = new Intent();
            if (!isVideo)
                intent.setType("image/gif");
            else
                intent.setTypeAndNormalize("video/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, getString(isVideo ? R.string.label_select_video : R.string.label_select_gif)), REQUEST_VIDEO_TRIMMER);
        }
    }


    @Override
    public void onContentChange() {
        if (adapter.isEmpty()) {
            list.setVisibility(View.GONE);
            emptyListLayout.setVisibility(View.VISIBLE);
        } else {
            emptyListLayout.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_STORAGE_READ_ACCESS_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickFromGallery(pickVideo);
                }
                break;
            case REQUEST_STORAGE_READ_ACCESS_PERMISSION_TO_RECORD:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openVideoCapture();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_VIDEO_TRIMMER) {
                final Uri selectedUri = data.getData();
                if (selectedUri != null) {
                    if (pickVideo)
                        startTrimActivity(selectedUri);
                    else try {
                        processGif(selectedUri);
                    } catch (IOException | FFmpegCommandAlreadyRunningException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, R.string.toast_cannot_retrieve_selected_video, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void processGif(Uri selectedUri) throws IOException, FFmpegCommandAlreadyRunningException {
        String path = FileUtils.getPath(this, selectedUri);
        FFmpeg fFmpeg = FFmpeg.getInstance(this);

        try {
            fFmpeg.loadBinary(new FFmpegLoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    Log.e("TrimmerActivity", "FFMPEG not supported");
                }

                @Override
                public void onSuccess() {

                }

                @Override
                public void onStart() {

                }

                @Override
                public void onFinish() {

                }
            });
        } catch (FFmpegNotSupportedException e) {
            e.printStackTrace();
            return;
        }

        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setTitle(getString(R.string.choose_watch_gif));
            mProgressDialog.setMessage(getString(R.string.gif_trimming_progress));
        }

        if (!mProgressDialog.isShowing())
            mProgressDialog.show();

        String filename = TrimmerActivity.DESTINATION_PATH + FilenameUtils.getBaseName(path) + "_scaled.gif";
        File copy = new File(filename);

        int i = 0;
        while (copy.exists()) {
            copy = new File(String.format(Locale.getDefault(),
                    "%s/%s_scaled_(%d).gif", TrimmerActivity.DESTINATION_PATH, FilenameUtils.getBaseName(path), i));
            i++;
        }

        copy.createNewFile();

        String cmd = "-y -v debug -i " + path + " -r 15 -vf scale=w=320:h=320:force_original_aspect_ratio=increase,crop=320:320 -threads 2 " + copy.getPath();
        String[] command = cmd.split(" ");

        File finalCopy = copy;

        fFmpeg.execute(command, new FFmpegExecuteResponseHandler() {
            @Override
            public void onSuccess(String message) {
                Log.i("FFmpeg", message);

                mProgressDialog.cancel();

                runOnUiThread(() -> Toast.makeText(MainActivity.this, R.string.all_done, Toast.LENGTH_SHORT).show());

                VideoModel videoModel = new VideoModel();
                videoModel.setPath(finalCopy.getPath());
                videoModel.save();

                if (!BuildConfig.DEBUG)
                    Answers.getInstance().logCustom(new CustomEvent("GIF_EXECUTED"));
            }

            @Override
            public void onProgress(String message) {
                String[] strings = message.split("\n");
                for (String string : strings) {
                    Log.i("FFmpeg", string);
                }
            }

            @Override
            public void onFailure(String message) {
                String[] strings = message.split("\n");
                for (String string : strings) {
                    Log.e("FFmpeg", string);
                }

                if (mProgressDialog.isShowing())
                    mProgressDialog.dismiss();
                Toast.makeText(MainActivity.this, R.string.we_can_not_convert_this, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStart() {
                Log.i("FFmpeg", "on start");
            }

            @Override
            public void onFinish() {
                Log.i("FFmpeg", "on finish");
            }
        });
    }

    private void startTrimActivity(Uri selectedUri) {
        Intent intent = new Intent(this, TrimmerActivity.class);
        intent.putExtra(EXTRA_VIDEO_PATH, FileUtils.getPath(this, selectedUri));
        startActivity(intent);
    }

    @Override
    public void nodesChanged(ArrayList<Node> nodes) {
        if (DetectWear.isConnected())
            status.setText(getString(R.string.watch_status, getString(R.string.connected)));
    }

    @Override
    public void onNoConnectedNode() {
        status.setText(getString(R.string.watch_status, getString(R.string.not_connected)));
    }

    @Override
    public void onNewConnectedNode(Node node) {
        if (DetectWear.isConnected())
            status.setText(getString(R.string.watch_status, getString(R.string.connected)));
    }

    @Override
    public void onBackPressed() {
        if (actionMenu.isOpened())
            actionMenu.close(true);
        else
            super.onBackPressed();
    }

    @Subscribe(thread = EventThread.MAIN_THREAD)
    public void onWatchFaceSended(WatchFaceSenderEvent event) {
        if (!BuildConfig.DEBUG)
            Answers.getInstance().logCustom(new CustomEvent("WATCH_FACE_APPLIED"));

        applicationPreference.setCurrentGif(event.getPath());
        try {
            selectedGif.setImageDrawable(new GifDrawable(event.getPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        removeButton.setVisibility(View.VISIBLE);
    }
}
