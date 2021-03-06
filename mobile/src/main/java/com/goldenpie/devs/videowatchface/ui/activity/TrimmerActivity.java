package com.goldenpie.devs.videowatchface.ui.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.CustomEvent;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.goldenpie.devs.videowatchface.BuildConfig;
import com.goldenpie.devs.videowatchface.R;
import com.goldenpie.devs.videowatchface.model.VideoModel;
import com.goldenpie.devs.videowatchface.ui.BaseActivity;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import butterknife.BindView;
import life.knowledge4.videotrimmer.K4LVideoTrimmer;
import life.knowledge4.videotrimmer.interfaces.OnK4LVideoListener;
import life.knowledge4.videotrimmer.interfaces.OnTrimVideoListener;

public class TrimmerActivity extends BaseActivity implements OnTrimVideoListener, OnK4LVideoListener {

    public static final String VIDEO_WATCH_FACE = ".VideoWatchFace";
    public static final String DESTINATION_PATH = Environment.getExternalStorageDirectory() + File.separator + VIDEO_WATCH_FACE + File.separator;

    @BindView(R.id.timeLine)
    protected K4LVideoTrimmer mVideoTrimmer;

    private ProgressDialog mProgressDialog;

    private FFmpeg fFmpeg;

    @Override
    protected int getContentView() {
        return R.layout.activity_trimmer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fFmpeg = FFmpeg.getInstance(this);

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
        }

        Intent extraIntent = getIntent();
        String path = "";

        if (extraIntent != null) {
            path = extraIntent.getStringExtra(MainActivity.EXTRA_VIDEO_PATH);
        }

        //setting progressbar
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setTitle(getString(R.string.choose_watch_video));
        mProgressDialog.setMessage(getString(R.string.video_trimming_progress));

        if (mVideoTrimmer != null) {
            mVideoTrimmer.setMaxDuration(10);
            mVideoTrimmer.setOnTrimVideoListener(this);
            mVideoTrimmer.setOnK4LVideoListener(this);
            mVideoTrimmer.setVideoURI(Uri.parse(path));
            mVideoTrimmer.setDestinationPath(DESTINATION_PATH);
            mVideoTrimmer.setVideoInformationVisibility(true);
        }

        if (!BuildConfig.DEBUG) {
            Answers.getInstance().logContentView(new ContentViewEvent()
                    .putContentName("TRIMMING_ACTIVITY"));
        }
    }

    @Override
    public void onTrimStarted() {
        mProgressDialog.show();
    }

    @Override
    public void getResult(final Uri uri) {
        try {
            scaleVideo(uri.getPath());
        } catch (FFmpegCommandAlreadyRunningException | IOException e) {
            e.printStackTrace();
        }
    }

    private void scaleVideo(String path) throws FFmpegCommandAlreadyRunningException, IOException {
        if (!mProgressDialog.isShowing())
            mProgressDialog.show();

        File originalVideo = new File(path);
        String filename = DESTINATION_PATH + FilenameUtils.getBaseName(path) + "_scaled."
                + "gif";
        File copy = new File(filename);

        int i = 0;
        while (copy.exists()) {
            copy = new File(String.format(Locale.getDefault(),
                    "%s/%s_scaled_(%d).gif", DESTINATION_PATH, FilenameUtils.getBaseName(path), i));
            i++;
        }

        copy.createNewFile();

        String cmd = "-y -v debug -i " + path + " -r 15 -vf scale=w=320:h=320:force_original_aspect_ratio=increase,crop=320:320 -threads 2 -gifflags +transdiff -y " + copy.getPath();
        String[] command = cmd.split(" ");

        File finalCopy = copy;

        fFmpeg.execute(command, new FFmpegExecuteResponseHandler() {
            @Override
            public void onSuccess(String message) {
                Log.i("FFmpeg", message);

                if (originalVideo.getParent().contains(VIDEO_WATCH_FACE))
                    originalVideo.delete();

                mProgressDialog.cancel();

                runOnUiThread(() -> Toast.makeText(TrimmerActivity.this, R.string.all_done, Toast.LENGTH_SHORT).show());

                VideoModel videoModel = new VideoModel();
                videoModel.setPath(finalCopy.getPath());
                videoModel.save();

                if (!BuildConfig.DEBUG)
                    Answers.getInstance().logCustom(new CustomEvent("VIDEO_EXECUTED"));

                finish();
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
                Toast.makeText(TrimmerActivity.this, R.string.we_can_not_convert_this, Toast.LENGTH_SHORT).show();
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

    @Override
    public void cancelAction() {
        mProgressDialog.cancel();
        mVideoTrimmer.destroy();
        finish();
    }

    @Override
    public void onError(final String message) {
        mProgressDialog.cancel();

        runOnUiThread(() -> Toast.makeText(TrimmerActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onVideoPrepared() {
        runOnUiThread(() -> Toast.makeText(TrimmerActivity.this, "Preparing reviews..", Toast.LENGTH_SHORT).show());
    }
}