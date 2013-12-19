package com.hermescavern.bestboxingtimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {

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
    private ImageButton mStartButton;
    private ImageButton mResetButton;
    private TextView mStartText;
    private TextView mResetText;
    private TextView mRoundText;
    private long mCurrentTime;
    private int mRound = 1;
    private BroadcastReceiver mReceiver = new MyBroadcastReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        mCounterText = (TextView) findViewById(R.id.counter_text);

        mStartButton = (ImageButton) findViewById(R.id.start_button);
        mStartButton.setOnClickListener(new StartButtonClickEvent());
        mResetButton = (ImageButton) findViewById(R.id.reset_button);
        mResetButton.setOnClickListener(new ResetButtonClickEvent());

        mStartText = (TextView) findViewById(R.id.start_text);
        mResetText = (TextView) findViewById(R.id.reset_text);
        mRoundText = (TextView) findViewById(R.id.round_text);

        paintTime(getRoundTime());

        IntentFilter filter = new IntentFilter();
        filter.addAction(BroadcastMessages.ROUND_TIME_UPDATED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
    }

    private void startTimer(){
        mCurrentTime = 0;
        paintTime(getInitialTime());
        mTimer = createTimer(getInitialTime()+1000);
        mTimer.start();
        mTimerState = TimerState.RUNNING;
        mStartText.setText(R.string.pause);
        mStartButton.setImageResource(R.drawable.pause);
    }

    private void pauseTimer(){
        mTimer.cancel();
        mTimerState = TimerState.PAUSED;
        mStartText.setText(R.string.start);
        mTimer = null;
        mStartButton.setImageResource(R.drawable.play);
    }

    private void resumeTimer(){
        mTimer = createTimer(mCurrentTime);
        mTimer.start();
        mTimerState = TimerState.RUNNING;
        mStartText.setText(R.string.pause);
        mStartButton.setImageResource(R.drawable.pause);
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
        mStartText.setText(R.string.start);
        mTimer = null;
        mStartButton.setImageResource(R.drawable.play);
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

    private CountDownTimer createTimer(long duration){

        return new CountDownTimer(duration, 200){
            boolean firstPaint = true;

            @Override
            public void onTick(final long millisUntilFinished) {

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
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.round_start);
        mediaPlayer.start();
    }

    private void playEndRoundRing(){
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.round_end);
        mediaPlayer.start();
    }
}
