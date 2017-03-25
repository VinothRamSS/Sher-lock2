/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.vinothramss.Sher_Lock;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Vinoth Ram S S on 23-03-2017.
 */

public class CreateActivity extends Activity
{
    private static final int TRAINING_SESSION_COUNT_MAX = 10;
    private static final int TRAINING_SESSION_COUNT_MIN = 5;
    private static final float LENGTH_THRESHOLD = 120.0f;

    protected Gesture mGesture;
    protected ArrayList<Gesture> mSavedGestureList;
    protected View mFinishSessionButton;
    protected View mDiscardButton;
    protected View mSaveButton;
    protected View mAuthenticateButton;
    protected View mLockButton;
    protected GestureOverlayView mGestureOverlay;
    protected Doodle mGestureValue;
    protected String mUserName;
    protected String mActivityType;
    protected File mUserDir;
    protected File mUserFile;
    protected TextView mTrainingSessionName;
    protected TextView mAuthenticationSessionName;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_gesture);
        mFinishSessionButton = findViewById(R.id.finishSession);
        mDiscardButton = findViewById(R.id.discard);
        mSaveButton = findViewById(R.id.saveGesture);
        mAuthenticateButton = findViewById(R.id.authenticateGesture);
        mTrainingSessionName = (TextView) findViewById(R.id.trainingSessionName);
        mGestureOverlay = (GestureOverlayView) findViewById(R.id.gestures_overlay);
        mFinishSessionButton.setEnabled(false);
        mDiscardButton.setEnabled(false);
        mSaveButton.setEnabled(false);
        mGestureValue = null;
        mUserName = (String) this.getIntent().getExtras().get(getString(R.string.user_name));
        mActivityType = (String) this.getIntent().getExtras().get(getString(R.string.activity_type));
        mUserDir = new File(Environment.getExternalStorageDirectory() + "/" + getString(R.string.root_dir) + "/" + mUserName + "/");
        mUserFile = new File(mUserDir.getAbsolutePath() + mUserName);
        if (mActivityType.equals(getString(R.string.train)))
        {
            mTrainingSessionName.setText(mUserName + "'s Training Session");
            mUserDir.mkdirs();
        }
        else if (mActivityType.equals(getString(R.string.retrain)))
        {
            mTrainingSessionName.setText("Retrain " + mUserName + "'s Doodle");
            deletePreviousSession();
        }
        else if (mActivityType.equals(getString(R.string.authenticate)))
        {
            setContentView(R.layout.authenticate_gesture);
            mAuthenticationSessionName = (TextView) findViewById(R.id.authenticationSessionName);
            mAuthenticationSessionName.setText("Authenticate with " + mUserName + "'s Doodle");
            mGestureOverlay = (GestureOverlayView) findViewById(R.id.authenticate_overlay);
            mAuthenticateButton = findViewById(R.id.authenticateGesture);
            mAuthenticateButton.setEnabled(false);
        }
        else if (mActivityType.equals(getString(R.string.lock_apps)))
        {
            //setContentView(R.layout.app_list);
           // mLockButton = findViewById(R.id.authenticateGesture);
           // mLockButton.setEnabled(false);
        }
        else
        {
            Log.d(CreateActivity.class.getName().toString(), "ERROR: Invalid activity type");
            return;
        }
        mGestureOverlay.addOnGestureListener(new GesturesProcessor());
        mSavedGestureList = new ArrayList<Gesture>();
    }
    private void deletePreviousSession()
    {
        for (File file : mUserDir.listFiles())
        {
            file.delete();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        if (mGesture != null)
        {
            outState.putParcelable("gesture", mGesture);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        mGesture = savedInstanceState.getParcelable("gesture");
        if (mGesture != null)
        {
            final GestureOverlayView overlay = (GestureOverlayView) findViewById(R.id.gestures_overlay);
            overlay.post(new Runnable() {
                public void run()
                {
                    overlay.setGesture(mGesture);
                }
            });

            mFinishSessionButton.setEnabled(true);
        }
    }
    public void onAuthenticateButtonPress(View v)
    {
        GestureLibrary userStore = GestureLibraries.fromFile(mUserFile);
        userStore.load();

        ArrayList<Gesture> gesturesFromFile = new ArrayList<Gesture>();
        for (String entry : userStore.getGestureEntries())
        {
            gesturesFromFile.addAll(userStore.getGestures(entry));
        }
        Doodle prediction = new Doodle(gesturesFromFile);
        if (prediction.authenticate(mGesture))
        {
            Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
            mGestureOverlay.clear(false);
            mAuthenticateButton.setEnabled(false);
        }
        else
        {
            Toast.makeText(this, "Incorrect. Please try again.", Toast.LENGTH_SHORT).show();
            mGestureOverlay.clear(false);
            mAuthenticateButton.setEnabled(false);
        }
    }
    public void onSaveButtonPress(View v)
    {
        if (mGesture != null)
        {
            mSaveButton.setEnabled(false);
            mDiscardButton.setEnabled(false);
            final GestureLibrary store = GestureBuilderActivity.getStore();
            setResult(RESULT_OK);
            final String path = new File(Environment.getExternalStorageDirectory(), "gestures").getAbsolutePath();
            boolean haveMinGesturesBeenDrawn = mSavedGestureList.size() >= TRAINING_SESSION_COUNT_MIN;
            boolean haveMaxGesturesBeenDrawn = mSavedGestureList.size() >= TRAINING_SESSION_COUNT_MAX;
            if (!haveMaxGesturesBeenDrawn)
            {
                if (!haveMinGesturesBeenDrawn)
                {
                    mSavedGestureList.add(mGesture);
                    mGestureValue = new Doodle(mSavedGestureList);
                    store.save();
                    Toast.makeText(this, getString(R.string.save_success), Toast.LENGTH_SHORT).show();
                }
                else if (haveMinGesturesBeenDrawn)
                {
                    if (mGestureValue.authenticate(mGesture))
                    {
                        mGestureValue = new Doodle(mSavedGestureList);
                        Toast.makeText(this, getString(R.string.save_success), Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(this, getString(R.string.invalid_gesture), Toast.LENGTH_LONG).show();
                    }
                }
            }
            if (mSavedGestureList.size() >= TRAINING_SESSION_COUNT_MIN)
            {
                mFinishSessionButton.setEnabled(true);
                mFinishSessionButton.setBackgroundColor(0xffffffff);
            }
            mGestureOverlay.clear(false);
        }
        else
        {
            setResult(RESULT_CANCELED);
        }
    }
    public void onDiscardButtonPress(View v)
    {
        setResult(RESULT_CANCELED);
        mGestureOverlay.clear(false);
    }
    public void onFinishSessionButtonPress(View v)
    {
        GestureLibrary userStore = GestureLibraries.fromFile(mUserFile);
        for (Gesture gesture : mSavedGestureList)
        {
            userStore.addGesture(gesture.toString(), gesture);
        }
        userStore.save();
        finish();
    }
    private class GesturesProcessor implements GestureOverlayView.OnGestureListener
    {
        public void onGestureStarted(GestureOverlayView overlay, MotionEvent event)
        {
            mSaveButton.setEnabled(false);
            mDiscardButton.setEnabled(false);
            mGesture = null;
        }
        public void onGesture(GestureOverlayView overlay, MotionEvent event)
        {
        }
        public void onGestureEnded(GestureOverlayView overlay, MotionEvent event)
        {
            mGesture = overlay.getGesture();
            if (mGesture.getLength() < LENGTH_THRESHOLD)
            {
                overlay.clear(false);
                mGesture = null;
            }
            else
            {
                mSaveButton.setEnabled(true);
                mDiscardButton.setEnabled(true);
                if (mAuthenticateButton != null)
                {
                    mAuthenticateButton.setEnabled(true);
                }
            }
        }
        public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event)
        {
        }
    }
}