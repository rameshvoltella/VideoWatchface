package devs.goldenpie.com.videowatchface.ui.fragment;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mariux.teleport.lib.TeleportClient;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import devs.goldenpie.com.videowatchface.R;
import devs.goldenpie.com.videowatchface.service.ShareService;
import devs.goldenpie.com.videowatchface.utils.DetectWear;
import fr.tvbarthel.lib.blurdialogfragment.SupportBlurDialogFragment;
import lombok.Setter;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

/**
 * @author anton
 * @version 3.4
 * @since 17.10.16
 */
public class PreviewFragmentDialog extends SupportBlurDialogFragment {

    public static final String VIDEO_PATH = "video_path";

    @BindView(R.id.video_player)
    protected GifImageView videoPlayer;

    @Setter
    private TeleportClient teleportClient;

    public static PreviewFragmentDialog newInstance(String videoPath, TeleportClient teleportClient) {

        Bundle args = new Bundle();
        args.putString(VIDEO_PATH, videoPath);

        PreviewFragmentDialog fragment = new PreviewFragmentDialog();
        fragment.setTeleportClient(teleportClient);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.preview_fragment_dialog, container, false);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        ButterKnife.bind(this, view);
        return view;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        GifDrawable gifDrawable = null;
        try {
            gifDrawable = new GifDrawable(getArguments().getString(VIDEO_PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }
        videoPlayer.setImageDrawable(gifDrawable);
    }

    @SuppressWarnings("AccessStaticViaInstance")
    @OnClick(R.id.fab)
    protected void onFabClick() {
        if (DetectWear.isConnected())
            try {
                ShareService.getInstance(teleportClient, getContext()).sendGif(getArguments().getString(VIDEO_PATH));
            } catch (IOException e) {
                e.printStackTrace();
            }
        else
            Toast.makeText(getContext(), "No connection to Watch", Toast.LENGTH_SHORT).show();
    }
}
