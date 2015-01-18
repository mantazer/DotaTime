package mantazer.android.dotatime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;

import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class WatchFaceService extends CanvasWatchFaceService {

    private static final String TAG = "WatchFaceService";

    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

//    private static final long NORMAL_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    private static final long NORMAL_UPDATE_RATE_MS = TimeUnit.MINUTES.toMillis(1);
    private static final long MUTE_UPDATE_RATE_MS = TimeUnit.MINUTES.toMillis(1);


    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        int[] heroPool;
        Random rand;

        private static final int MSG_UPDATE_TIME = 0;

        private Time mTime;

        Resources resources;

        Drawable backgroundDrawable;
        Bitmap mBackgroundBitmap;
        Bitmap mBackgroundScaledBitmap;

        String mAmString;
        String mPmString;

        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mSecondPaint;
        Paint mAmPmPaint;

        private float mYOffset;
        private float mXOffset;

        private boolean mRegisteredTimeZoneReceiver = false;
        private boolean mLowBitAmbient;

        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = NORMAL_UPDATE_RATE_MS
                                    - (timeMs % NORMAL_UPDATE_RATE_MS);
                            mUpdateTimeHandler
                                    .sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle
                            .BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            resources = WatchFaceService.this.getResources();

            heroPool = new int[]{
                    R.drawable.antimage,
                    R.drawable.axe,
                    R.drawable.deathprophet,
                    R.drawable.dragonknight,
                    R.drawable.drow,
                    R.drawable.earthshaker,
                    R.drawable.enchantress,
                    R.drawable.enigma,
                    R.drawable.faceless,
                    R.drawable.io,
                    R.drawable.jakiro,
                    R.drawable.juggernaut,
                    R.drawable.keeper,
                    R.drawable.kunkka,
                    R.drawable.lifestealer,
                    R.drawable.lina,
                    R.drawable.luna,
                    R.drawable.lycan,
                    R.drawable.meepo,
                    R.drawable.naga,
                    R.drawable.nightstalker,
                    R.drawable.omniknight,
                    R.drawable.pudge,
                    R.drawable.qop,
                    R.drawable.riki,
                    R.drawable.shadowfiend,
                    R.drawable.sniper,
                    R.drawable.spiritbreaker,
                    R.drawable.sven,
                    R.drawable.templar,
                    R.drawable.tidehunter,
                    R.drawable.vengeful,
                    R.drawable.venomancer,
                    R.drawable.windranger,
                    R.drawable.witchdoctor,
                    R.drawable.zues
            };

            rand = new Random();

            backgroundDrawable = resources.getDrawable(randomHeroIndex());
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();

            float timeTextSize = resources.getDimension(R.dimen.text_size_time);

            mYOffset = resources.getDimension(R.dimen.y_offset);
            mXOffset = resources.getDimension(R.dimen.x_offset);

            mAmString = resources.getString(R.string.digital_am);
            mPmString = resources.getString(R.string.digital_pm);

            mHourPaint = createTextPaint();
            mHourPaint.setTextSize(timeTextSize);

            mMinutePaint = createTextPaint();
            mMinutePaint.setTextSize(timeTextSize);

            mSecondPaint = createTextPaint();
            mSecondPaint.setTextSize(timeTextSize);

            mAmPmPaint = createTextPaint();
            mAmPmPaint.setTextSize(timeTextSize);

            mTime = new Time();

        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            /* get device features (burn-in, low-bit ambient) */
            boolean burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            mHourPaint.setTypeface(burnInProtection ? NORMAL_TYPEFACE : BOLD_TYPEFACE);

            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);

        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            /* the time changed */
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            /* the wearable switched between modes */
            if (mLowBitAmbient) {
            }

            invalidate();
            updateTimer();
        }

        private String formatTwoDigitNumber(int minute) {
            return String.format("%02d", minute);
        }

        private int convertTo12Hour(int hour) {
            int result = hour % 12;
            return (result == 0) ? 12 : result;
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            mTime.setToNow();

            // Starting point for time string
            float x = canvas.getWidth() / 9f;

            int width = bounds.width();
            int height = bounds.height();


            mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                width, height, true /* filter */);

            canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);

            String hourString = String.valueOf(convertTo12Hour(mTime.hour));
            canvas.drawText(hourString, x, mYOffset, mHourPaint);

            String minuteString = formatTwoDigitNumber(mTime.minute);
            canvas.drawText(minuteString, x + mXOffset, mYOffset, mMinutePaint);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            /* the watch face became visible or invisible */
            if (visible) {
                registerTimeZoneReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();

                int randIndex = randomHeroIndex();
                backgroundDrawable = resources.getDrawable(randIndex);
                mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();

            } else {
                unregisterTimeZoneReceiver();
            }

            updateTimer();
        }

        private void registerTimeZoneReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }

            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterTimeZoneReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }

            mRegisteredTimeZoneReceiver = false;
            WatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private Paint createTextPaint() {
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setAntiAlias(true);

            return paint;
        }

        public int randomHeroIndex() {
            return heroPool[rand.nextInt( heroPool.length )];
        }

    }

}
