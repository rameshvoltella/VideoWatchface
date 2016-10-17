package devs.goldenpie.com.videowatchface.ui.adapter;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.activeandroid.Cache;
import com.activeandroid.content.ContentProvider;
import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import devs.goldenpie.com.videowatchface.R;
import devs.goldenpie.com.videowatchface.model.VideoModel;
import devs.goldenpie.com.videowatchface.ui.fragment.PreviewFragmentDialog;

public class TaskListAdapter extends RecyclerView.Adapter<TaskListAdapter.ViewHolder> {

    private ContentListener contentListener;

    private LayoutInflater inflater;
    private List<VideoModel> models = new ArrayList<>();

    private final ContentObserver contentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            new Handler().postDelayed(() -> updateSelf(), 200);
        }
    };

    public TaskListAdapter(Context context) {
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
        Glide.with(holder.preview.getContext())
                .load(Uri.fromFile(new File(models.get(position).getPath())))
                .placeholder(R.drawable.no_film_preview)
                .into(holder.preview);
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

    public TaskListAdapter setContentListener(ContentListener contentListener) {
        this.contentListener = contentListener;
        return this;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.preview)
        protected AppCompatImageView preview;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @OnClick(R.id.preview)
        protected void onPreviewClick() {
            PreviewFragmentDialog.newInstance(models.get(getAdapterPosition()).getPath())
                    .show(((AppCompatActivity) itemView.getContext()).getSupportFragmentManager(), "preview_dialog");
        }
    }

    public interface ContentListener {
        void onContentChange();
    }
}