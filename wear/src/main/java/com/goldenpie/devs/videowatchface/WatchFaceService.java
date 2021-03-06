package com.goldenpie.devs.videowatchface;

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
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

import com.constants.Constants;
import com.goldenpie.devs.videowatchface.event.FileStoredEvent;
import com.goldenpie.devs.videowatchface.model.db.FileModel;
import com.goldenpie.devs.videowatchface.modules.SortAndStore;
import com.goldenpie.stroketextview.StrokeTextView;
import com.google.android.gms.wearable.DataMap;
import com.hwangjr.rxbus.RxBus;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.thread.EventThread;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class WatchFaceService extends CanvasWatchFaceService {

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    public class Engine extends CanvasWatchFaceService.Engine {

        static final int MSG_UPDATE_TIME = 0;
        private SimpleDateFormat sdf;

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

        @BindView(R.id.gifView)
        GifImageView gifView;
        @BindView(R.id.clock)
        StrokeTextView textClock;
        @BindView(R.id.largeClock)
        StrokeTextView bigTextClock;

        private View myLayout;

        private int specW, specH;

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sdf = new SimpleDateFormat(DateFormat.is24HourFormat(getApplicationContext()) ? "H:mm" : "h:mm a", Locale.getDefault());
                invalidate();
            }
        };

        private Handler mHandler;

        Runnable mStatusChecker = new Runnable() {
            @Override
            public void run() {
                try {
                    invalidate();
                } finally {
                    mHandler.postDelayed(mStatusChecker, Constants.FRAME_RATE_DELAY);
                }
            }
        };

        private GifDrawable gifDrawable;
        private SortAndStore sortAndStore;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            sdf = new SimpleDateFormat(DateFormat.is24HourFormat(getApplicationContext()) ? "H:mm" : "h:mm a", Locale.getDefault());

            RxBus.get().register(this);

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

            mHandler = new Handler();
            startRepeatingTask();
        }

        private void startRepeatingTask() {
            mStatusChecker.run();
        }

        private void stopRepeatingTask() {
            mHandler.removeCallbacks(mStatusChecker);
        }

        @Subscribe(thread = EventThread.MAIN_THREAD)
        public void onEvent(DataMap dataMap) {
            if (sortAndStore == null)
                sortAndStore = new SortAndStore();

            sortAndStore.organizeData(dataMap);
        }

        @Subscribe(thread = EventThread.MAIN_THREAD)
        public void onEvent(final String path) {
            if (path.equals(Constants.REMOVE_WATCHFACE)) {
                removeWatchFace();
            }
        }

        @Subscribe(thread = EventThread.MAIN_THREAD)
        public void onEven(FileStoredEvent event) {
            try {
                gifDrawable = new GifDrawable(event.getFileModel().getData());
            } catch (IOException e) {
                e.printStackTrace();
            }

            playData();
        }

        private void removeWatchFace() {
            if (FileModel.isExist())
                FileModel.getFileModel().delete();

            try {
                gifDrawable = new GifDrawable(getResources(), R.drawable.base_anim);
            } catch (IOException e) {
                e.printStackTrace();
            }

            playData();
        }

        private void playData() {
            if (gifDrawable != null)
                gifView.setImageDrawable(gifDrawable);
        }

        @Override
        public void onDestroy() {
            RxBus.get().unregister(this);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            sdf = new SimpleDateFormat(DateFormat.is24HourFormat(getApplicationContext()) ? "H:mm" : "h:mm a", Locale.getDefault());
            invalidate();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setStatusBarGravity(insets.isRound() ? Gravity.START : Gravity.BOTTOM | Gravity.CENTER)
                    .setHotwordIndicatorGravity(insets.isRound() ? Gravity.CENTER : Gravity.CENTER)
                    .setShowSystemUiTime(false)
                    .build());

            if (insets.isRound())
                textClock.setPadding(0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()), 0, 0);

            specW = View.MeasureSpec.makeMeasureSpec(displaySize.x, View.MeasureSpec.EXACTLY);
            specH = View.MeasureSpec.makeMeasureSpec(displaySize.y, View.MeasureSpec.EXACTLY);

            myLayout.measure(specW, specH);
            myLayout.layout(0, 0, myLayout.getMeasuredWidth(), myLayout.getMeasuredHeight());
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (!inAmbientMode) {
                startRepeatingTask();
                ((GifDrawable) gifView.getDrawable()).start();
            } else {
                stopRepeatingTask();
                ((GifDrawable) gifView.getDrawable()).pause();
            }
            updateTimer();
            Log.i("VieWatchFace", "Ambient changed");
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            Calendar calendar = Calendar.getInstance();

            if (!isInAmbientMode()) {
                gifView.setVisibility(View.VISIBLE);
                bigTextClock.setVisibility(View.INVISIBLE);
                textClock.setVisibility(View.VISIBLE);
            } else {
                bigTextClock.setVisibility(View.VISIBLE);
                textClock.setVisibility(View.INVISIBLE);
                gifView.setVisibility(View.INVISIBLE);
            }

            textClock.setText(sdf.format(calendar.getTime()));
            bigTextClock.setText(sdf.format(calendar.getTime()));

            canvas.drawColor(Color.BLACK);
            canvas.translate(0, 0);
            myLayout.draw(canvas);
        }


        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                startRepeatingTask();
                registerReceiver();
                ((GifDrawable) gifView.getDrawable()).start();
            } else {
                stopRepeatingTask();
                unregisterReceiver();
                ((GifDrawable) gifView.getDrawable()).pause();
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