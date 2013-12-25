package com.hermescavern.bestboxingtimer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.rtp.AudioStream;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class MainActivity extends ActionBarActivity {

    private ShareActionProvider mShareActionProvider;

    private enum TimerState {
        RUNNING, STOPPED, FINISHED, PAUSED;
    }

    private enum RoundState {
        WORKING, RESTING;
    }

    private TimerState mTimerState = TimerState.FINISHED;
    private RoundState mRoundState = RoundState.WORKING;
    private CountDownTimer mTimer;
    private TextView mCounterText;
    private Button mStartButton;
    private Button mResetButton;
    private TextView mRoundText;
    private long mCurrentTime;
    private int mRound = 1;
    private AudioManager am;
    private BroadcastReceiver mReceiver = new MyBroadcastReceiver();
    private MyAudiosFocusListener myAudiosFocusListener = new MyAudiosFocusListener();
    private boolean mWarningTriggered = false;

    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        mCounterText = (TextView) findViewById(R.id.counter_text);

        mStartButton = (Button) findViewById(R.id.start_button);
        mStartButton.setOnClickListener(new StartButtonClickEvent());
        mResetButton = (Button) findViewById(R.id.reset_button);
        mResetButton.setOnClickListener(new ResetButtonClickEvent());

        mRoundText = (TextView) findViewById(R.id.round_text);

        paintTime(getRoundTime());

        IntentFilter filter = new IntentFilter();
        filter.addAction(BroadcastMessages.ROUND_TIME_UPDATED);
        registerReceiver(mReceiver, filter);

        AdView adView = (AdView)this.findViewById(R.id.adView);
        if(adView != null){
            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .addTestDevice("907D4387EBE72BBAC7589D68B70965E7")
                    .addTestDevice("1FEE3785D790C2E915308DD4A8C37C58")
                    .build();
            adView.loadAd(adRequest);
        }

        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        am.abandonAudioFocus(myAudiosFocusListener);
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem menuItem = menu.findItem(R.id.social_share);

        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                EasyTracker easyTracker = EasyTracker.getInstance(MainActivity.this);
                easyTracker.send(MapBuilder
                        .createEvent("ui_event",     // Event category (required)
                                "menu",  // Event action (required)
                                "share_with",   // Event label
                                null)            // Event value
                        .build()
                );

                Intent intent = new Intent();
                intent.setType("text/plain");
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));


                Intent newIntent = Intent.createChooser(intent, getString(R.string.share_title));
                startActivity(newIntent);
                // startActivity(intent);

                return false;
            }
        });



        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, MyPreferencesActivity.class);
            startActivity(intent);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private long getInitialTime(){
        if(mRoundState == RoundState.WORKING)
            return getRoundTime();
        else if(mRoundState == RoundState.RESTING)
            return getRestTime();

        return getRoundTime();
    }

    private long getRoundTime(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String roundTimeText = sp.getString(getString(R.string.round_time_pref_key), "60");
        return Integer.parseInt(roundTimeText)*1000;
    }

    private long getRestTime(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String restTimeText = sp.getString(getString(R.string.rest_time_pref_key), "60");
        return Integer.parseInt(restTimeText)*1000;
    }

    private int getTotalRounds(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String totalRoundsText = sp.getString(getString(R.string.rounds_pref_key), "1");
        return Integer.parseInt(totalRoundsText);
    }

    private int getWarningTime(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String warningTimeText = sp.getString(getString(R.string.warning_time_pref_key), "0");
        return Integer.parseInt(warningTimeText)*1000;
    }

    private void startWorking(){
        mRoundState = RoundState.WORKING;
        playStartRoundRing();
        mCounterText.setTextColor(getResources().getColor(R.color.working_text_color));
        startTimer();
    }

    private void startResting(){
        mRoundState = RoundState.RESTING;
        mCounterText.setTextColor(getResources().getColor(R.color.resting_text_color));
        startTimer();
    }

    private void workoutFinished(){
        mRound = 1;
        mRoundText.setText("Round 1");
        paintTime(getRoundTime());
    }

    private void startTimer(){
        mCurrentTime = 0;
        paintTime(getInitialTime());
        mTimer = createTimer(getInitialTime()+1000);
        mTimer.start();
        mTimerState = TimerState.RUNNING;
        mWarningTriggered = false;
        mStartButton.setText(R.string.pause);
        mStartButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.pause, 0, 0, 0);
    }

    private void pauseTimer(){
        mTimer.cancel();
        mTimerState = TimerState.PAUSED;
        mStartButton.setText(R.string.start);
        mTimer = null;
        mStartButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.play, 0, 0, 0);
    }

    private void resumeTimer(){
        mTimer = createTimer(mCurrentTime);
        mTimer.start();
        mTimerState = TimerState.RUNNING;
        mStartButton.setText(R.string.pause);
        mStartButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.pause, 0, 0, 0);
    }

    private void stopTimer(){
        mTimerState = TimerState.STOPPED;
        if(mTimer != null)
            mTimer.cancel();
    }

    private void timerFinished(){
        stopTimer();
        resetTimer();
    }

    private void resetTimer(){
        mCounterText.setText("00:00");
        mTimerState = TimerState.FINISHED;
        mStartButton.setText(R.string.start);
        mTimer = null;
        mCounterText.setTextColor(getResources().getColor(R.color.working_text_color));
        mStartButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.play, 0, 0, 0);
    }

    private void paintTime(long milis){
        int totalSeconds = (int) (milis / 1000);

        int minutes = totalSeconds / 60;
        int seconds = totalSeconds - (minutes * 60);

        String minutesText = minutes < 10 ? "0" + minutes : minutes+"";
        String secondsText = seconds < 10 ? "0" + seconds : seconds+"";
        mCounterText.setText(minutesText + ":" + secondsText);
    }

    private class StartButtonClickEvent implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if(mTimerState == TimerState.FINISHED){
                startWorking();
            }else if(mTimerState == TimerState.PAUSED){
                resumeTimer();
            }else if(mTimerState == TimerState.RUNNING){
                pauseTimer();
            }
        }
    }

    private class ResetButtonClickEvent implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            stopTimer();
            resetTimer();
            workoutFinished();
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(BroadcastMessages.ROUND_TIME_UPDATED)){
                if(mRoundState == RoundState.WORKING && mTimerState == TimerState.FINISHED)
                    paintTime(getRoundTime());
            }
        }
    }

    private class MyAudiosFocusListener implements AudioManager.OnAudioFocusChangeListener {

        @Override
        public void onAudioFocusChange(int focusChange) {

        }
    }

    private CountDownTimer createTimer(long duration){

        return new CountDownTimer(duration, 200){
            boolean firstPaint = true;

            @Override
            public void onTick(final long millisUntilFinished) {
                long warningTime = getWarningTime() + 1000;

                if(mWarningTriggered == false
                        && mRoundState == RoundState.WORKING
                        && warningTime > 0
                        && millisUntilFinished < warningTime
                        && (getRoundTime() + 1000) != warningTime){

                    playRoundWarningRing();
                    mWarningTriggered = true;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        paintTime(millisUntilFinished);
                        mCurrentTime = millisUntilFinished;
                    }
                });
            }

            @Override
            public void onFinish() {
                timerFinished();
                if(mRoundState == RoundState.WORKING)
                    playEndRoundRing();

                if(mRound < getTotalRounds()){
                    if(mRoundState == RoundState.WORKING){
                        if(getRestTime() > 0){
                            startResting();
                        }else{
                            callToStartWorking();
                        }
                    }else if(mRoundState == RoundState.RESTING){
                        callToStartWorking();
                    }
                }else{
                    workoutFinished();
                }
            }

            private void callToStartWorking(){
                mRound++;
                String roundFormat = getString(R.string.round_text_format);
                String roundText = String.format(roundFormat, mRound);
                mRoundText.setText(roundText);

                startWorking();
            }
        };
    }

    private void playStartRoundRing(){
        final MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.round_start);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.release();
            }
        });
        mediaPlayer.start();
    }

    private void playRoundWarningRing(){
        final MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.round_warning);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.release();
            }
        });
        mediaPlayer.start();
    }

    private void playEndRoundRing(){
        final MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.round_end);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.release();
            }
        });
        mediaPlayer.start();
    }
}
