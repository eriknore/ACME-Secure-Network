package com.bnss.securefiletransfer;

import android.content.Intent;
import android.os.Bundle;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {
    public final static String CLIENT_CERT_ALIAS = "com.bnss.securefiletransfer.CLIENT_CERT_ALIAS";
    private String selectedAlias;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /*
     * Browse for client certificate, if not already in KeyStore
     */
    public void onCertClick(View view){
        KeyChain.choosePrivateKeyAlias(this, new KeyChainAliasCallback() {
            public void alias(String alias) {
                if (alias != null) {
                    selectedAlias = alias;
                } else {
                    Toast t = Toast.makeText(getApplicationContext(), "Select a damn certificate!", Toast.LENGTH_LONG);
                    t.show();
                }
            }
        }, null, null, null, -1, null);
        TextView tv = (TextView) findViewById(R.id.textCertFile);
        tv.setText(selectedAlias);
    }

    public void onConnectClick(View view)  {
        if (selectedAlias != null) {
            System.out.println("Try to pass alias to intent. Alias: " + selectedAlias);
            Intent intent = new Intent(this, SelectFileActivity.class);
            //Maybe we can avoid sending the CERT like this?
            intent.putExtra(CLIENT_CERT_ALIAS, selectedAlias);
            startActivity(intent);
        }
        else {
            Toast t = Toast.makeText(getApplicationContext(), "Select a damn certificate!", Toast.LENGTH_LONG);
            t.show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
