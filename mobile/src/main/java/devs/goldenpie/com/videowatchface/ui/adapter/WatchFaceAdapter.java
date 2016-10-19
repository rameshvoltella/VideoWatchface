package devs.goldenpie.com.videowatchface.ui.adapter;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.activeandroid.Cache;
import com.activeandroid.content.ContentProvider;
import com.mariux.teleport.lib.TeleportClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import devs.goldenpie.com.videowatchface.R;
import devs.goldenpie.com.videowatchface.model.VideoModel;
import devs.goldenpie.com.videowatchface.ui.fragment.PreviewFragmentDialog;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class WatchFaceAdapter extends RecyclerView.Adapter<WatchFaceAdapter.ViewHolder> {

    private ContentListener contentListener;

    private LayoutInflater inflater;
    private TeleportClient teleportClient;

    private List<VideoModel> models = new ArrayList<>();

    private final ContentObserver contentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            new Handler().postDelayed(() -> updateSelf(), 200);
        }
    };

    public WatchFaceAdapter(Context context, TeleportClient mTeleportClient) {
        this.teleportClient = mTeleportClient;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        Uri uri = ContentProvider.createUri(VideoModel.class, null);
        Cache.getContext().getContentResolver().registerContentObserver(uri, true, contentObserver);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.video_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        GifDrawable gifDrawable = null;
        try {
            gifDrawable = new GifDrawable(models.get(position).getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        holder.preview.setImageDrawable(gifDrawable);
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    public void updateSelf() {
        models.clear();
        models.addAll(VideoModel.getAll());

        Collections.reverse(models);

        notifyDataSetChanged();

        if (contentListener != null)
            contentListener.onContentChange();
    }

    public boolean isEmpty() {
        return models.isEmpty();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (contentObserver != null)
            Cache.getContext().getContentResolver().unregisterContentObserver(contentObserver);
    }

    public WatchFaceAdapter setContentListener(ContentListener contentListener) {
        this.contentListener = contentListener;
        return this;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.preview)
        protected GifImageView preview;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @OnClick(R.id.preview)
        protected void onPreviewClick() {
            PreviewFragmentDialog.newInstance(models.get(getAdapterPosition()).getPath(), teleportClient)
                    .show(((AppCompatActivity) itemView.getContext()).getSupportFragmentManager(), "preview_dialog");
        }

        @OnLongClick(R.id.preview)
        protected boolean onLongPreviewClick() {
            new AlertDialog.Builder(itemView.getContext())
                    .setTitle("Remove this video?")
                    .setMessage("Created video will be removed from the device and you will not can recover it")
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                        VideoModel videoModel = models.get(getAdapterPosition());
                        File file = new File(videoModel.getPath());
                        file.delete();
                        videoModel.delete();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
                    .show();
            return true;
        }
    }

    public interface ContentListener {
        void onContentChange();
    }
}