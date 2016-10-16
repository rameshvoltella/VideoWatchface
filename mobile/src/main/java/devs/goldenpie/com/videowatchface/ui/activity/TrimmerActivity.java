package devs.goldenpie.com.videowatchface.ui.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;

import devs.goldenpie.com.videowatchface.R;
import devs.goldenpie.com.videowatchface.model.VideoModel;
import life.knowledge4.videotrimmer.K4LVideoTrimmer;
import life.knowledge4.videotrimmer.interfaces.OnK4LVideoListener;
import life.knowledge4.videotrimmer.interfaces.OnTrimVideoListener;

public class TrimmerActivity extends AppCompatActivity implements OnTrimVideoListener, OnK4LVideoListener {

    private static final String VIDEO_WATCH_FACE = ".VideoWatchFace";
    public static final String DESTINATION_PATH = Environment.getExternalStorageDirectory() + File.separator + VIDEO_WATCH_FACE + File.separator;

    private K4LVideoTrimmer mVideoTrimmer;
    private ProgressDialog mProgressDialog;

    private FFmpeg fFmpeg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trimmer);

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
        mProgressDialog.setMessage(getString(R.string.trimming_progress));

        mVideoTrimmer = ((K4LVideoTrimmer) findViewById(R.id.timeLine));
        if (mVideoTrimmer != null) {
            mVideoTrimmer.setMaxDuration(10);
            mVideoTrimmer.setOnTrimVideoListener(this);
            mVideoTrimmer.setOnK4LVideoListener(this);
            mVideoTrimmer.setVideoURI(Uri.parse(path));
            mVideoTrimmer.setDestinationPath(DESTINATION_PATH);
            mVideoTrimmer.setVideoInformationVisibility(true);
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
        File copy = new File(DESTINATION_PATH + FilenameUtils.getBaseName(path) + "_scaled." + FilenameUtils.getExtension(path));

        if (!copy.exists())
            copy.createNewFile();

        String cmd = "-i " + path + " -vf scale=320:-1 " + copy.getPath();
        String[] command = cmd.split(" ");
        fFmpeg.execute(command, new FFmpegExecuteResponseHandler() {
            @Override
            public void onSuccess(String message) {
                Log.i("FFmpeg", message);
                try {
                    cropVideo(copy, path);
                } catch (FFmpegCommandAlreadyRunningException | IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onProgress(String message) {
                Log.i("FFmpeg", message);
            }

            @Override
            public void onFailure(String message) {
                Log.e("FFmpeg", message);
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

    private void cropVideo(File input, String path) throws FFmpegCommandAlreadyRunningException, IOException {
        File originalVideo = new File(path);
        File copy = new File(DESTINATION_PATH + FilenameUtils.getBaseName(input.getPath()) + "_cropped." + FilenameUtils.getExtension(input.getPath()));

        if (!copy.exists())
            copy.createNewFile();

        String cmd = "-i " + input.getPath() + " -vf crop=320:320 " + copy.getPath();
        String[] command = cmd.split(" ");
        fFmpeg.execute(command, new FFmpegExecuteResponseHandler() {
            @Override
            public void onSuccess(String message) {
                Log.i("FFmpeg", message);

                if (originalVideo.getParent().contains(VIDEO_WATCH_FACE))
                    originalVideo.delete();
                copy.delete();

                mProgressDialog.cancel();

                runOnUiThread(() -> Toast.makeText(TrimmerActivity.this, getString(R.string.video_saved_at, copy.getPath()), Toast.LENGTH_SHORT).show());

                VideoModel videoModel = new VideoModel();
                videoModel.setPath(copy.getPath());
                videoModel.save();

                finish();
            }

            @Override
            public void onProgress(String message) {
                Log.i("FFmpeg", message);
            }

            @Override
            public void onFailure(String message) {
                Log.e("FFmpeg", message);
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