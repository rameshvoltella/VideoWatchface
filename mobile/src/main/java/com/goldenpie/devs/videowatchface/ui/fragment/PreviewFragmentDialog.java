package com.goldenpie.devs.videowatchface.ui.fragment;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.os.AsyncTaskCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.goldenpie.devs.videowatchface.BuildConfig;
import com.goldenpie.devs.videowatchface.R;
import com.goldenpie.devs.videowatchface.service.ShareService;
import com.goldenpie.devs.videowatchface.utils.DetectWear;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.mariux.teleport.lib.TeleportClient;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
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

    private boolean savedInstanceStateDone;

    private InterstitialAd mInterstitialAd;

    public static PreviewFragmentDialog newInstance(String videoPath, TeleportClient teleportClient) {

        Bundle args = new Bundle();
        args.putString(VIDEO_PATH, videoPath);

        PreviewFragmentDialog fragment = new PreviewFragmentDialog();
        fragment.setTeleportClient(teleportClient);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInterstitialAd = new InterstitialAd(getActivity());
        mInterstitialAd.setAdUnitId(getString(R.string.banner_ad_unit_id));

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                sendGif();
            }
        });
    }

    private void requestAd() {
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("E802AD13223AAE827215D7FF29BB0BDA")
                .build();

        mInterstitialAd.loadAd(adRequest);
    }

    @Override
    public void onResume() {
        super.onResume();

        savedInstanceStateDone = false;
    }

    @Override
    public void onStart() {
        super.onStart();

        savedInstanceStateDone = false;
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        savedInstanceStateDone = true;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.preview_fragment_dialog, container, false);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
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

        if (!BuildConfig.DEBUG) {
            Answers.getInstance().logContentView(new ContentViewEvent()
                    .putContentName("PREVIEW_DIALOG"));
        }

        requestAd();
    }

    @OnClick(R.id.fab)
    protected void onFabClick() {
        if (DetectWear.isConnected()) {

            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            } else {
                sendGif();
            }
        } else
            Toast.makeText(getContext(), R.string.no_connection_to_watch, Toast.LENGTH_SHORT).show();
    }

    @SuppressWarnings("AccessStaticViaInstance")
    private void sendGif() {
        try {
            ShareService.getInstance(teleportClient, getContext())
                    .withCompleteListener(() -> {
                        if (!savedInstanceStateDone) dismiss();
                    })
                    .sendGif(getArguments().getString(VIDEO_PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
