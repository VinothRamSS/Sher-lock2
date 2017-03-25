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
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Vinoth Ram S S on 23-03-2017.
 */

public class GestureBuilderActivity extends ListActivity
{

    private static final int STATUS_SUCCESS = 0;
    private static final int STATUS_CANCELLED = 1;
    private static final int STATUS_NO_STORAGE = 2;
    private static final int STATUS_NOT_LOADED = 3;

    private static final int MENU_ID_RENAME = 1;
    private static final int MENU_ID_REMOVE = 2;

    // IDs for dialog boxes
    private static final int DIALOG_RENAME_GESTURE = 1;
    private static final int DIALOG_USER_OPTIONS = 2;
    private static final int DIALOG_NEW_USER = 3;

    private static final int REQUEST_NEW_GESTURE = 1;
    private static final int LOCK_APP_GESTURE = 1;

    // Type: long (id)
    private static final String GESTURES_INFO_ID = "gestures.info_id";

    private final File mStoreFile = new File(Environment.getExternalStorageDirectory(), "gestures");

    protected File mUserDirectory;
    protected File mAppDirectory;

    private final Comparator<NamedGesture> mSorter = new Comparator<NamedGesture>() {
        public int compare(NamedGesture object1, NamedGesture object2)
        {
            return object1.name.compareTo(object2.name);
        }
    };

    /**
     * List of users
     */
    protected ArrayList<String> mUserList;

    /**
     * Name of new user
     */
    protected String mNewUserName;

    /**
     * Name of selected user
     */
    protected String mSelectedUserName;

    private static GestureLibrary sStore;

    private GesturesAdapter mAdapter,appAdapter;
    private UsersLoadTask mTask;
    private TextView mEmpty;

    // Fields to track dialogs
    private Dialog mRenameDialog;
    private Dialog mNewUserDialog;
    private Dialog mUserOptionsDialog;
    private Dialog mLockAppDialog;
    private EditText mInput;

    private NamedGesture mCurrentRenameGesture;

    private PackageManager packageManager = null;
    private List<ApplicationInfo> applist = null;
    private ApplicationAdapter listadaptor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.users_list);
        packageManager = getPackageManager();
        new UsersLoadTask().execute();
        mAdapter = new GesturesAdapter(this);
        setListAdapter(mAdapter);

        if (sStore == null)
        {
            sStore = GestureLibraries.fromFile(mStoreFile);
        }
        mEmpty = (TextView) findViewById(android.R.id.empty);

        mUserDirectory = new File(Environment.getExternalStorageDirectory() + "/" + getString(R.string.root_dir) + "/");
        mUserDirectory.mkdirs();

        // Load users into the list view
        this.loadUsers();
    }

    /**
     * TODO Probably not used anymore
     * 
     * @return
     */
    static GestureLibrary getStore()
    {
        return sStore;
    }

    /**
     * Starts a new activity for training a new user
     */
    private void startUserTrainingSession()
    {
        Intent intent = new Intent(this, CreateActivity.class);
        intent.putExtra(getString(R.string.user_name), mNewUserName);
        intent.putExtra(getString(R.string.activity_type), getString(R.string.train));
        startActivityForResult(intent, REQUEST_NEW_GESTURE);
    }

    /**
     * Starts a new activity for retraining a user
     */
    private void startUserRetrainingSession()
    {
        Intent intent = new Intent(this, CreateActivity.class);
        intent.putExtra(getString(R.string.user_name), mSelectedUserName);
        intent.putExtra(getString(R.string.activity_type), getString(R.string.retrain));
        startActivityForResult(intent, REQUEST_NEW_GESTURE);
    }

    /**
     * Starts a new activity for authenticating a user
     */
    private void startUserAuthenticationSession()
    {
        Intent intent = new Intent(this, CreateActivity.class);
        intent.putExtra(getString(R.string.user_name), mSelectedUserName);
        intent.putExtra(getString(R.string.activity_type), getString(R.string.authenticate));
        startActivityForResult(intent, REQUEST_NEW_GESTURE);
    }

    private void startAppLockSession() {
       // this.setContentView(R.layout.app_list);
        Intent intent=new Intent(this,LockActivity.class);
        //intent.putExtra(getString(R.string.button_calc),mSelectedUserName);
        startActivity(intent);
        //startActivity(intent);
    }



    public void reloadUsers(View v)
    {
        loadUsers();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK)
        {
            switch (requestCode)
            {
            case REQUEST_NEW_GESTURE:
                loadUsers();
                break;
                //case LOAD_NEW_GESTURE:
                //loadUsers();
                //break;
            }
        }
    }

    /**
     * Looks in the app directory and adds a user for every folder in it
     */
    private void loadUsers()
    {
        if (mTask != null && mTask.getStatus() != UsersLoadTask.Status.FINISHED)
        {
            mTask.cancel(true);
        }
        mTask = (UsersLoadTask) new UsersLoadTask().execute();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if (mTask != null && mTask.getStatus() != UsersLoadTask.Status.FINISHED)
        {
            mTask.cancel(true);
            mTask = null;
        }

        cleanupRenameDialog();
        cleanupUserOptionsDialog();
    }

    /**
     * Dismisses the user options dialog and resets selected user
     */
    private void cleanupUserOptionsDialog()
    {
        if (mUserOptionsDialog != null)
        {
            mUserOptionsDialog.dismiss();
            mUserOptionsDialog = null;
        }
        mSelectedUserName = null;
    }

    /**
     * TODO Not sure what this does
     */
    private void checkForEmpty()
    {
        if (mAdapter.getCount() == 0)
        {
            mEmpty.setText(R.string.gestures_empty);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        if (mCurrentRenameGesture != null)
        {
            outState.putLong(GESTURES_INFO_ID, mCurrentRenameGesture.gesture.getID());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle state)
    {
        super.onRestoreInstanceState(state);

        long id = state.getLong(GESTURES_INFO_ID, -1);
        if (id != -1)
        {
            final Set<String> entries = sStore.getGestureEntries();
            out: for (String name : entries)
            {
                for (Gesture gesture : sStore.getGestures(name))
                {
                    if (gesture.getID() == id)
                    {
                        mCurrentRenameGesture = new NamedGesture();
                        mCurrentRenameGesture.name = name;
                        mCurrentRenameGesture.gesture = gesture;
                        break out;
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {

        // TODO store the name of the user that was clicked
        mSelectedUserName = ((TextView) l.getChildAt(position)).getText().toString();
        Log.d(GestureBuilderActivity.class.getName(), "User with name " + mSelectedUserName + " was selected");
        showDialog(DIALOG_USER_OPTIONS);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final NamedGesture gesture = (NamedGesture) menuInfo.targetView.getTag();

        switch (item.getItemId())
        {
        case MENU_ID_RENAME:
            renameGesture(gesture);
            return true;
        case MENU_ID_REMOVE:
            // deleteGesture(gesture);
            return true;
        }

        return super.onContextItemSelected(item);
    }

    /**
     * TODO probably not used anymore
     * 
     * @param gesture
     */
    private void renameGesture(NamedGesture gesture)
    {
        mCurrentRenameGesture = gesture;
        showDialog(DIALOG_RENAME_GESTURE);
    }

    @Override
    protected Dialog onCreateDialog(int id)
    {
        if (id == DIALOG_RENAME_GESTURE)
        {
            return createRenameDialog();
        }
        else if (id == DIALOG_USER_OPTIONS)
        {
            mUserOptionsDialog = createUserOptionsDialog();
            return mUserOptionsDialog;
        }
        else if (id == DIALOG_NEW_USER)
        {
            mNewUserDialog = createNewUserDialog();
            return mNewUserDialog;
        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog)
    {
        super.onPrepareDialog(id, dialog);
        if (id == DIALOG_RENAME_GESTURE)
        {
            mInput.setText(mCurrentRenameGesture.name);
        }
    }

    @SuppressWarnings("deprecation")
    public void onNewUserButtonPress(View v)
    {
        showDialog(DIALOG_NEW_USER);
    }

    /**
     * Creates the dialog to enter a user name and continue on to the training session
     * 
     * @return The dialog to display
     */
    private Dialog createNewUserDialog()
    {
        final View layout = View.inflate(this, R.layout.dialog_rename, null);
        mInput = (EditText) layout.findViewById(R.id.name);
        // ((TextView) layout.findViewById(R.id.label))
        // .setText(R.string.gestures_rename_label);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(0);
        builder.setTitle(getString(R.string.gestures_rename_title));
        builder.setCancelable(true);
        builder.setOnCancelListener(new Dialog.OnCancelListener() {
            public void onCancel(DialogInterface dialog)
            {
                mInput.setText("");
                mInput.setError(null);
                cleanupNewUserDialog();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel_action), new Dialog.OnClickListener() {
            public void onClick(DialogInterface dialog, int which)
            {
                mInput.setText("");
                mInput.setError(null);
                cleanupNewUserDialog();
            }
        });
        builder.setPositiveButton(getString(R.string.rename_action), new Dialog.OnClickListener() {
            public void onClick(DialogInterface dialog, int which)
            {
                mNewUserName = mInput.getText().toString();
                mInput.setText("");
                mInput.setError(null);
                // Make sure the user name field is not empty
                if (mNewUserName.length() == 0)
                {
                    Toast.makeText(GestureBuilderActivity.this, "Please enter a valid name", Toast.LENGTH_LONG).show();
                }
                else
                {
                    startUserTrainingSession();   
                }
            }
        });
        builder.setView(layout);
        return builder.create();
    }

    /**
     * Dismisses the new user dialog and resets the new user name
     */
    private void cleanupNewUserDialog()
    {
        if (mNewUserDialog != null)
        {
            mNewUserDialog.dismiss();
            mNewUserDialog = null;
        }
        mNewUserName = null;
    }

    /**
     * Creates the dialog for user options when the user name is clicked
     * 
     * @return The dialog to display
     */
    private Dialog createUserOptionsDialog()
    {
        // Create the menu to show on single press of user name

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(0);
        builder.setTitle(getString(R.string.user_options_dialog));
        builder.setCancelable(true);
        CharSequence[] options = new CharSequence[4];
        options[0] = getString(R.string.authenticate_user);
        options[1] = getString(R.string.retrain_user);
        options[2] = getString(R.string.delete_user);
        options[3] = getString(R.string.lock_apps);
        builder.setItems(options, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                switch (which)
                {
                case 0:
                    Log.d(GestureBuilderActivity.class.getName(), "authenticate");
                    startUserAuthenticationSession();
                    break;
                case 1:
                    Log.d(GestureBuilderActivity.class.getName(), "retrain");
                    startUserRetrainingSession();
                    break;
                case 2:
                    Log.d(GestureBuilderActivity.class.getName(), "delete");
                    deleteUser(mSelectedUserName);
                    break;
                case 3:
                    Log.d(GestureBuilderActivity.class.getName(),"lock apps");
                    startAppLockSession();
                }
            }
        });
        return builder.create();
    }

    /**
     * TODO probably not used anymore
     * 
     * @return
     */
    private Dialog createRenameDialog()
    {
        final View layout = View.inflate(this, R.layout.dialog_rename, null);
        mInput = (EditText) layout.findViewById(R.id.name);
        ((TextView) layout.findViewById(R.id.label)).setText(R.string.gestures_rename_label);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(0);
        builder.setTitle(getString(R.string.gestures_rename_title));
        builder.setCancelable(true);
        builder.setOnCancelListener(new Dialog.OnCancelListener() {
            public void onCancel(DialogInterface dialog)
            {
                cleanupRenameDialog();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel_action), new Dialog.OnClickListener() {
            public void onClick(DialogInterface dialog, int which)
            {
                cleanupRenameDialog();
            }
        });
        builder.setPositiveButton(getString(R.string.rename_action), new Dialog.OnClickListener() {
            public void onClick(DialogInterface dialog, int which)
            {
                changeGestureName();
            }
        });
        builder.setView(layout);
        return builder.create();
    }

    /**
     * TODO probably not used anymore
     */
    private void changeGestureName()
    {
        final String name = mInput.getText().toString();
        if (!TextUtils.isEmpty(name))
        {
            final NamedGesture renameGesture = mCurrentRenameGesture;
            final GesturesAdapter adapter = mAdapter;
            final int count = adapter.getCount();
            final GesturesAdapter adapter1=appAdapter;
            // Simple linear search, there should not be enough items to warrant
            // a more sophisticated search
            // for (int i = 0; i < count; i++)
            // {
            // final NamedGesture gesture = adapter.getItem(i);
            // if (gesture.gesture.getID() == renameGesture.gesture.getID())
            // {
            // sStore.removeGesture(gesture.name, gesture.gesture);
            // gesture.name = mInput.getText().toString();
            // sStore.addGesture(gesture.name, gesture.gesture);
            // break;
            // }
            // }

            adapter.notifyDataSetChanged();
        }
        mCurrentRenameGesture = null;
    }

    /**
     * TODO probably not used anymore
     */
    private void cleanupRenameDialog()
    {
        if (mRenameDialog != null)
        {
            mRenameDialog.dismiss();
            mRenameDialog = null;
        }
        mCurrentRenameGesture = null;
    }
    /**
     * Deletes the user from the list and removes the appropriate directory
     * 
     * @param user The user to delete
     */
    private void deleteUser(String user)
    {
        final GesturesAdapter adapter = mAdapter;
        adapter.setNotifyOnChange(false);

        // This could be done better
        for (File file : mUserDirectory.listFiles())
        {
            if (file.getName().equals(user))
            {
                file.delete();
                break;
            }
        }

        adapter.remove(user);
        // adapter.sort(mSorter);
        checkForEmpty();
        adapter.notifyDataSetChanged();

        Toast.makeText(this, R.string.gestures_delete_success, Toast.LENGTH_SHORT).show();
    }

    /*private List<ApplicationInfo> checkForLaunchIntent(List<ApplicationInfo> list) {
        ArrayList<ApplicationInfo> applist = new ArrayList<ApplicationInfo>();
        for (ApplicationInfo info : list) {
            try {
                if (null != packageManager.getLaunchIntentForPackage(info.packageName)) {
                    applist.add(info);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return applist;
    }*/
    /**
     * Async task for loading the table with user names
     */
    private class UsersLoadTask extends AsyncTask<Void, String, Integer>
    {
        private int mThumbnailSize;
        private int mThumbnailInset;
        private int mPathColor;
        private ProgressDialog progress = null;

        protected void onPreExecute(int result)
        {
            super.onPreExecute();
            /*if (result==LOCK_APP_GESTURE)
            {
                progress = ProgressDialog.show(GestureBuilderActivity.this, null, "Loading application info...");
            }*/
            final Resources resources = getResources();
            mPathColor = resources.getColor(R.color.gesture_color);
            mThumbnailInset = (int) resources.getDimension(R.dimen.gesture_thumbnail_inset);
            mThumbnailSize = (int) resources.getDimension(R.dimen.gesture_thumbnail_size);
            findViewById(R.id.addButton).setEnabled(false);
            mAdapter.setNotifyOnChange(false);
            mAdapter.clear();
        }
        @Override
        protected Integer doInBackground(Void... params)
        {
            if (isCancelled()) return STATUS_CANCELLED;
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
            {
                return STATUS_NO_STORAGE;
            }

            final GestureLibrary store = sStore;
            ArrayList<String> users = new ArrayList<String>();
            Log.d(GestureBuilderActivity.class.getName(), mUserDirectory.getName());
            /*if (mAppDirectory.isDirectory())
            {
                for (File file : mUserDirectory.listFiles()) {
                    if (file.isDirectory()) {
                        applist = checkForLaunchIntent(packageManager.getInstalledApplications(PackageManager.GET_META_DATA));
                        listadaptor = new ApplicationAdapter(GestureBuilderActivity.this, R.layout.row, applist);
                    }
                }
            }*/
            if (mUserDirectory.isDirectory())
            {
                for (File file : mUserDirectory.listFiles())
                {
                    if (file.isDirectory())
                    {
                        users.add(file.getName());
                        publishProgress(file.getName());
                    }
                }
                return STATUS_SUCCESS;
            }
            return STATUS_NOT_LOADED;
        }
        @Override
        protected void onProgressUpdate(String... values)
        {
            super.onProgressUpdate(values);
            final GesturesAdapter adapter = mAdapter;
            adapter.setNotifyOnChange(false);
            for (String user : values)
            {
                adapter.add(user);
            }
            adapter.notifyDataSetChanged();
        }
        @Override
        protected void onPostExecute(Integer result)
        {
            super.onPostExecute(result);

            if (result == STATUS_NO_STORAGE)
            {
                getListView().setVisibility(View.GONE);
                mEmpty.setVisibility(View.VISIBLE);
                mEmpty.setText(getString(R.string.gestures_error_loading, mStoreFile.getAbsolutePath()));
            }
           /* else if(result== LOCK_APP_GESTURE)
            {
                setListAdapter(listadaptor);
                progress.dismiss();
            }*/
            else
            {
                findViewById(R.id.addButton).setEnabled(true);
                checkForEmpty();
            }
        }
    }

    public void onUserClick(View v)
    {

    }
    static class NamedGesture
    {
        String name;
        Gesture gesture;
    }
    private class GesturesAdapter extends ArrayAdapter<String>
    {
        private final LayoutInflater mInflater;

        public GesturesAdapter(Context context)
        {
            super(context, 0);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            if (convertView == null)
            {
                convertView = mInflater.inflate(R.layout.gestures_item, parent, false);
            }

            final String gesture = getItem(position);
            final TextView label = (TextView) convertView;

            label.setTag(gesture);
            label.setText(this.getItem(position));

            return convertView;
        }
    }
}
