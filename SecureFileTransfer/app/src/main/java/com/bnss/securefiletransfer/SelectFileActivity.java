package com.bnss.securefiletransfer;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


public class SelectFileActivity extends ActionBarActivity implements OnTaskCompleted {
    private static final int OPEN_REQUEST_CODE = 41;
    private ArrayList<User> users;
    private String clientCertAlias = "";
    private Uri path = null;
    private User selectedUser = null;
    private String fileName = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_file);

        Intent intent = getIntent();
        String[] tmp = {intent.getStringExtra(MainActivity.CLIENT_CERT_ALIAS)};
        clientCertAlias = tmp[0];
        //System.out.println("In SelectFileAct, Received alias: " + clientCertAlias);

        new GetUsersTask(getApplicationContext(), clientCertAlias, (OnTaskCompleted) this).execute(clientCertAlias);

        new GetFilesTask(getApplicationContext(), clientCertAlias, (OnTaskCompleted) this).execute(clientCertAlias);

        Spinner s = (Spinner) findViewById(R.id.userSpinner);

    }

    public void browseFile(View v){
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); //     intent.setType("cer/*") ??
        startActivityForResult(intent, OPEN_REQUEST_CODE);
    }

    public void sendFile(View v){
        if (path == null){
            Toast.makeText(getApplicationContext(), "You haven't selected a file yet", Toast.LENGTH_SHORT).show();
        } else if (selectedUser == null){
            Toast.makeText(getApplicationContext(), "You haven't selected a user to send a file to yet", Toast.LENGTH_SHORT).show();
        }
        else {
            try {
                byte[] data = null;
                InputStream in = this
                        .getContentResolver()
                        .openInputStream(path);
                data = getBytes(in);
                new SendFileTask(getApplicationContext(), clientCertAlias, selectedUser,
                        fileName, data, (OnTaskCompleted) this).execute();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_select_file, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        if (resultCode == Activity.RESULT_OK) {
            // Will return "image:x*"
            path = resultData.getData();
            String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
            ContentResolver cr = getApplicationContext().getContentResolver();
            Cursor metaCursor = cr.query(path, projection, null, null, null);
            if (metaCursor != null) {
                try {
                    if (metaCursor.moveToFirst()) {
                        fileName = metaCursor.getString(0);
                    }
                } finally {
                    metaCursor.close();
                }
            }
            String scheme = path.getScheme();
            String shortPath = path.getLastPathSegment();

            TextView v = (TextView) findViewById(R.id.textSelectedFile);
            v.setText(fileName);
        }
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {

        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;

        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        return byteBuffer.toByteArray();
    }

    @Override
    public void onGetUsersCompleted(ArrayList<User> u) {
        this.users = u;
        String[] stringusers = new String[users.size()];
        for (int i = 0; i < users.size(); i++){
            stringusers[i] = users.get(i).getName();
        }
        Spinner spinner = (Spinner) findViewById(R.id.userSpinner);
        if (users.size() > 0)
            selectedUser = users.get(0);
        // Create an ArrayAdapter using the string array and a default spinner layout
        spinner.setAdapter(new ArrayAdapter<String>(this, R.layout.spinner_item, stringusers));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                System.out.println("Selecting user with position: " + position);
                selectedUser = users.get(position);
                System.out.println("Selected user: " + selectedUser.toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                System.out.println("Setting user selected to null");
                selectedUser = null;
            }

        });

    }

    @Override
    public void onGetFilesCompleted() {
        Toast.makeText(this.getApplicationContext(), "Finished fetching files", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGetFileCompleted(String fileName) {
//        Toast.makeText(this.getApplicationContext(), "Finished fetching file " + fileName, Toast.LENGTH_SHORT).show();
    System.out.println("Finished fetching file " + fileName);
    }

    @Override
    public void onSendFileCompleted(String message) {
        Toast.makeText(this.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
