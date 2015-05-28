package com.bnss.securefiletransfer;

import android.content.Context;
import android.os.AsyncTask;
import android.security.KeyChain;
import android.security.KeyChainException;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by luceat on 2/25/15.
 */
public class GetUsersTask extends AsyncTask<String, Boolean, ArrayList<User>> {

    private final Context context;
    private final String clientCertAlias;
    private OnTaskCompleted listener;


    public GetUsersTask(Context c, String clientCertAlias, OnTaskCompleted listener){
        this.context = c;
        this.clientCertAlias = clientCertAlias;
        this.listener = listener;
    }
    @Override
    protected ArrayList<User> doInBackground(String[] meh) {
        HttpsURLConnection urlConnection = null;
        SuperKeyManager s = null;
        // Tell the URLConnection to use a SocketFactory from our SSLContext
        URL url = null;
        HttpsURLConnection conn = null;
        X509Certificate[] certs = null;
        PrivateKey privatekey = null;
        try {
            certs = KeyChain.getCertificateChain(context, clientCertAlias);
            privatekey = KeyChain.getPrivateKey(context, clientCertAlias);
        } catch (KeyChainException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            url = new URL("https://172.31.212.103/api/api.php");
            urlConnection = (HttpsURLConnection)url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            s = new SuperKeyManager(clientCertAlias, certs, privatekey);
        } catch (javax.security.cert.CertificateException e) {
            e.printStackTrace();
        }

        // Create an SSLContext that uses our TrustManager
        SSLContext sslContext =  null; //SuperKeyManager.setForConnection(urlConnection, getApplicationContext(), )
        try {
            sslContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        TrustManagerFactory tmf = null;
        try {
            tmf = TrustManagerFactory.getInstance("X509");
            tmf.init((KeyStore) null);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }


        TrustManager[] tms = tmf.getTrustManagers();

        try {
            sslContext.init(new KeyManager[] {s}, tms, null);
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setEnabledProtocols(new String[]{"TLSv1"});
        SSLSocketFactory sf = sslContext.getSocketFactory();
        urlConnection.setSSLSocketFactory(sf);
        urlConnection.setHostnameVerifier(new AllowAllHostnameVerifier());
        String response = "";
        try {
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setConnectTimeout(3000);
            urlConnection.setReadTimeout(3000);
            String query = "req_status=1";
            try (OutputStream output = urlConnection.getOutputStream()) {
                output.write(query.getBytes());
            }
            InputStreamReader in = new InputStreamReader(urlConnection.getInputStream());
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(in);
            String line;
            for (;;){
                line=br.readLine();
                if(line == null){
                    break;
                } else {
                    sb.append(line);
                }
            }

            response = sb.toString();
            System.out.println("GetUsersResponse: ");
            System.out.println(response);

        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList<User> u = new ArrayList<User>();
        JSONArray arr;
        try {
            arr = new JSONArray(response);
            for(int i=0; i < arr.length(); i++){
                JSONObject o = arr.getJSONObject(i);
                u.add(new User(o.getInt("id"), o.getString("name"), o.getString("pub_key")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        System.out.println("Printing user list:");
        for(User user : u) {
            System.out.println(user.toString());
        }
        urlConnection.disconnect();
        return u;
    }

    @Override
    public void onPostExecute(ArrayList<User> u){
        listener.onGetUsersCompleted(u);
    }
}
