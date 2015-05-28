package com.bnss.securefiletransfer;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.security.KeyChain;
import android.security.KeyChainException;
import android.util.Base64;

import org.apache.commons.io.IOUtils;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.crypto.SecretKey;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.io.FilenameUtils;


/**
 * Returns an arraylist of Strings with the paths to the decrypted files
 */
public class GetFilesTask extends AsyncTask<String, Boolean, ArrayList<String>> {

    private final Context context;
    private final String clientCertAlias;
    private OnTaskCompleted listener;

    public GetFilesTask(Context c, String clientCertAlias, OnTaskCompleted listener) {
        this.context = c;
        this.clientCertAlias = clientCertAlias;
        this.listener = listener;
    }

    private HttpsURLConnection connect(int req, int id) {
        HttpsURLConnection urlConnection = null;
        SuperKeyManager s = null;
        // Tell the URLConnection to use a SocketFactory from our SSLContext
        URL url = null;
        HttpsURLConnection conn = null;
        X509Certificate[] certs = null;
        PrivateKey privatekey = null;
        boolean done = false;
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
            urlConnection = (HttpsURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            s = new SuperKeyManager(clientCertAlias, certs, privatekey);
        } catch (javax.security.cert.CertificateException e) {
            e.printStackTrace();
        }

        // Create an SSLContext that uses our TrustManager
        SSLContext sslContext = null;
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
            sslContext.init(new KeyManager[]{s}, tms, null);
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
            String query = "";
            if(req == 3){
                query = "req_status=" + req;
                System.out.println("Query is: " + query);
            }else if( req== 4){
                query = "req_status=" + req+"&id="+id;
                System.out.println("Query is: " + query);
            }else{
                return null;
            }
            try (OutputStream output = urlConnection.getOutputStream()) {
                output.write(query.getBytes());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


       return urlConnection;
    }
    @Override
    protected ArrayList<String> doInBackground(String[] meh) {
        PrivateKey privatekey = null;
        try{
             privatekey = KeyChain.getPrivateKey(context, clientCertAlias);
        }catch(KeyChainException e){
            e.printStackTrace();
        }catch(InterruptedException e){
            e.printStackTrace();
        }

        String response = null;
        String fileName = null;

        HttpsURLConnection httpsCon = connect(3,0);
        try{
        InputStreamReader in = new InputStreamReader(httpsCon.getInputStream());
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
            httpsCon.disconnect();
            response = sb.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList<String> p = new ArrayList<String>();
        JSONObject jsonO = new JSONObject();
        JSONArray arr = new JSONArray();
        try {
            arr = new JSONArray(response);
            //Decrypt files one by one and put somewhere on disk. Optionally provide some user feedback per file
           if(arr.length() > 0){
            for(int i=0; i < arr.length(); i++) {
                //Fetch encrypted symmetric key from response
                JSONObject o = arr.getJSONObject(i);
                byte[] enc_sym_key = Base64.decode(o.getString("sym_key"), Base64.DEFAULT);
                byte[] iv = Base64.decode(o.getString("iv"), Base64.DEFAULT);

                //Decrypt the symmetric key
                Key sym_key = StaticCryptos.decSymKey(enc_sym_key, privatekey);
                HttpsURLConnection httpsCon2 = null;

                try {
                    //New connection
                    httpsCon2 = connect(4,o.getInt("id"));
                } catch (Exception e) {
                    System.out.println("Something wrong with the urlConnection");
                }

                try {

                    DataInputStream in = new DataInputStream(httpsCon2.getInputStream());
                    //set the path where we want to save the file
                    File SDCardRoot = Environment.getExternalStorageDirectory();
                    System.out.println("Got SDCardRoot=  " + SDCardRoot.toString());
                    //create a new file, to save the downloaded file
                    fileName = o.getString("filename");
                    System.out.println("Got filename=  " + fileName);
                    File file = new File(SDCardRoot, fileName);
                    System.out.println("filePath: " + file.getPath());
                    FileOutputStream fileOutput = new FileOutputStream(file);
                    int downloadedSize = 0; // For progressbar and error checking
                    int totalSize = o.getInt("size");
                System.out.println("totalSize: " + totalSize);
                    byte[] data;

                    byte[] enc_data = new byte[totalSize];
                    in.readFully(enc_data);
                    // Decryption of the data
                     data = StaticCryptos.decrypt(sym_key,enc_data, iv);
                     fileOutput.write(data, 0, data.length);

                    // TODO: check if downloadedSize == totalSize

                    fileOutput.close();
                    httpsCon2.disconnect();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


                //Notify the UI that a file has been downloaded, decrypted and saved
                if(true) {
                    listener.onGetFileCompleted(fileName);
                    p.add("Insert path for file here");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        for(String paths : p) {
            System.out.println(paths.toString());
        }
        return p;
    }

    @Override
    public void onPostExecute(ArrayList<String> results){
        listener.onGetFilesCompleted();
    }
}
