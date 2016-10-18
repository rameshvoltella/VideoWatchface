package devs.goldenpie.com.videowatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.TextView;

import com.constants.Constants;
import com.google.android.gms.common.api.GoogleApiClient;
import com.mariux.teleport.lib.TeleportClient;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import devs.goldenpie.com.R;
import devs.goldenpie.com.videowatchface.event.FileStoredEvent;
import devs.goldenpie.com.videowatchface.model.FileModel;
import devs.goldenpie.com.videowatchface.modules.SortAndStore;
import devs.goldenpie.com.videowatchface.view.MagicTextView;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class WatchFaceService extends CanvasWatchFaceService {

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    public class Engine extends CanvasWatchFaceService.Engine {

        static final int MSG_UPDATE_TIME = 0;
        static final String CLOCK_FORMAT = "%02d:%02d";

        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        break;
                }
            }
        };

        private final Point displaySize = new Point();

        boolean mRegisteredTimeZoneReceiver = false;

        float mXOffset = 0;
        float mYOffset = 0;

        @BindView(R.id.gifView)
        GifImageView gifView;
        @BindView(R.id.clock)
        MagicTextView textClock;
        @BindView(R.id.largeClock)
        TextView bigTextClock;

        private TeleportClient mTeleportClient;
        private View myLayout;

        private int specW, specH;

        @SuppressWarnings("deprecation")
        private Time mTime;

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        private Timer updateTimer = null;

        private GifDrawable gifDrawable;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            EventBus.getDefault().register(this);
            mTeleportClient = new TeleportClient(getApplicationContext());
            mTeleportClient.getGoogleApiClient().registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {
                    mTeleportClient.sendMessage(Constants.SERVICE_CONNECTED, null);
                }

                @Override
                public void onConnectionSuspended(int i) {
                }
            });
            mTeleportClient.setOnSyncDataItemCallback(new SortAndStore(mTeleportClient));
            mTeleportClient.connect();

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setStatusBarGravity(Gravity.END)
                    .setHotwordIndicatorGravity(Gravity.CENTER)
                    .setShowSystemUiTime(false)
                    .build());

            mTime = new Time();

            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            myLayout = inflater.inflate(R.layout.watchface, null);
            ButterKnife.bind(this, myLayout);

            Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay();
            display.getSize(displaySize);

            textClock.setTypeface(Typeface.createFromAsset(getAssets(), Constants.DEFAULT_FONT));
            bigTextClock.setTypeface(Typeface.createFromAsset(getAssets(), Constants.DEFAULT_FONT));

            if (FileModel.isExist())
                try {
                    gifDrawable = new GifDrawable(FileModel.getFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            playData();
            initTimer();
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onEven(FileStoredEvent event) {
            mTeleportClient.setOnSyncDataItemCallback(new SortAndStore(mTeleportClient));
            try {
                gifDrawable = new GifDrawable(event.getFileModel().getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
            playData();
        }

        private void playData() {
            if (gifDrawable != null)
                gifView.setImageDrawable(gifDrawable);
        }

        private void initTimer() {
            updateTimer = new Timer();
            updateTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    invalidate();
                }
            }, 0, Constants.FRAME_RATE_DELAY);
        }

        private void cancelTimer() {
            if (updateTimer != null)
                updateTimer.cancel();
        }

        @Override
        public void onDestroy() {
            sendDisconnectRequest();
            EventBus.getDefault().unregister(this);
            mTeleportClient.disconnect();
            super.onDestroy();
        }

        private void sendDisconnectRequest() {
            mTeleportClient.sendMessage(Constants.DISCONNECT_REQUEST, null);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            if (insets.isRound()) {
                mYOffset = mXOffset = displaySize.x * 0.1f;
                displaySize.y -= 2 * mXOffset;
                displaySize.x -= 2 * mXOffset;
            } else {
                mXOffset = mYOffset = 0;
            }

            specW = View.MeasureSpec.makeMeasureSpec(displaySize.x, View.MeasureSpec.EXACTLY);
            specH = View.MeasureSpec.makeMeasureSpec(displaySize.y, View.MeasureSpec.EXACTLY);
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (!inAmbientMode) {
                initTimer();
                ((GifDrawable) gifView.getDrawable()).start();
            } else {
                cancelTimer();
                ((GifDrawable) gifView.getDrawable()).pause();
            }
            updateTimer();
            Log.i("VieWatchFace", "Ambient changed");
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            if (!isInAmbientMode()) {
                gifView.setVisibility(View.VISIBLE);
                bigTextClock.setVisibility(View.INVISIBLE);
                textClock.setVisibility(View.VISIBLE);
            } else {
                bigTextClock.setVisibility(View.VISIBLE);
                textClock.setVisibility(View.INVISIBLE);
                gifView.setVisibility(View.INVISIBLE);
            }

            textClock.setText(String.format(Locale.getDefault(), CLOCK_FORMAT, mTime.hour, mTime.minute));
            bigTextClock.setText(String.format(Locale.getDefault(), CLOCK_FORMAT, mTime.hour, mTime.minute));

            myLayout.measure(specW, specH);
            myLayout.layout(0, 0, myLayout.getMeasuredWidth(), myLayout.getMeasuredHeight());

            canvas.drawColor(Color.BLACK);
            canvas.translate(mXOffset, mYOffset);
            myLayout.draw(canvas);
        }


        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                initTimer();
                registerReceiver();
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                cancelTimer();
                unregisterReceiver();
            }
            updateTimer();

            Log.i("VieWatchFace", "Visible changed");
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            WatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }
    }
} 