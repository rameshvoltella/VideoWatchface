package devs.goldenpie.com.videowatchface.ui.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionMenu;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.google.android.gms.wearable.Node;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import devs.goldenpie.com.videowatchface.R;
import devs.goldenpie.com.videowatchface.model.VideoModel;
import devs.goldenpie.com.videowatchface.ui.adapter.TaskListAdapter;
import devs.goldenpie.com.videowatchface.utils.DetectWear;
import life.knowledge4.videotrimmer.utils.FileUtils;

/**
 * Created by EvilDev on 14.10.2016.
 */
public class MainActivity extends BaseActivity implements TaskListAdapter.ContentListener, DetectWear.NodesListener {
    private static final int REQUEST_VIDEO_TRIMMER = 0x01;
    private static final int REQUEST_STORAGE_READ_ACCESS_PERMISSION = 101;
    static final String EXTRA_VIDEO_PATH = "EXTRA_VIDEO_PATH";

    @BindView(R.id.empty_list_layout)
    protected View emptyListLayout;
    @BindView(R.id.list)
    protected RecyclerView list;
    @BindView(R.id.status_textview)
    protected AppCompatTextView status;
    @BindView(R.id.fab)
    protected FloatingActionMenu actionMenu;

    private TaskListAdapter adapter;

    private boolean pickVideo = false;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        DetectWear.init(this);

        status.setText(getString(R.string.watch_status, getString(R.string.not_connected)));

        setupList();

        DetectWear.setNodesListener(this);
    }

    private void setupList() {
        adapter = new TaskListAdapter(this);
        adapter.setContentListener(this);
        adapter.updateSelf();

        list.setAdapter(adapter);

        list.setLayoutManager(new GridLayoutManager(this, 3));
    }

    @OnClick(R.id.record)
    protected void onRecordClick() {
        actionMenu.close(true);
        openVideoCapture();
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
        Intent videoCapture = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(videoCapture, REQUEST_VIDEO_TRIMMER);
    }

    private void pickFromGallery(boolean isVideo) {
        pickVideo = isVideo;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, getString(R.string.permission_read_storage_rationale), REQUEST_STORAGE_READ_ACCESS_PERMISSION);
        } else {
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

        String cmd = "-y -v debug -i " + path + " -r 15 -vf scale=w=320:h=320:force_original_aspect_ratio=increase,crop=320:320 -threads 12 " + copy.getPath();
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
}
