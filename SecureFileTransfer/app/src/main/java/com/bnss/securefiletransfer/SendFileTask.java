package com.bnss.securefiletransfer;

import android.content.Context;
import android.os.AsyncTask;
import android.security.KeyChain;
import android.util.Base64;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.crypto.SecretKey;
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
public class SendFileTask extends AsyncTask<String, Boolean, Void> {
    private final User targetUser;
    private byte[] data;
    private Context context;
    private String clientCertAlias;
    private String boundary;
    private static final String LINE_FEED = "\r\n";
    private static final String charset = "UTF-8";
    private OnTaskCompleted listener;
    private String message = "File sent successfully!";
    private String fileName;

    public SendFileTask(Context c, String clientCertAlias, User targetUser, String fileName, byte[] data, OnTaskCompleted listener) {
        this.targetUser = targetUser;
        this.data = data;
        this.context = c;
        this.clientCertAlias = clientCertAlias;
        this.listener = listener;
        this.fileName = fileName;
    }

    @Override
    protected Void doInBackground(String[] meh) {
        HttpsURLConnection urlConnection = null;
        SuperKeyManager s = null;
        // Tell the URLConnection to use a SocketFactory from our SSLContext
        URL url = null;
        HttpsURLConnection conn = null;
        X509Certificate[] certs = null;
        PrivateKey privatekey = null;
        // creates a unique boundary based on time stamp
        boundary = "===" + System.currentTimeMillis() + "===";
        try {
            certs = KeyChain.getCertificateChain(context, clientCertAlias);
            privatekey = KeyChain.getPrivateKey(context, clientCertAlias);
        } catch (Exception e) {
            message = "Sending file failed. \n Something wrong with certificate";
            e.printStackTrace();
        }
        try {
            url = new URL("https://172.31.212.103/api/api.php");
            urlConnection = (HttpsURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            message = "Sending file failed. \n Something wrong with URL";
        }

        try {
            s = new SuperKeyManager(clientCertAlias, certs, privatekey);
        } catch (javax.security.cert.CertificateException e) {
            e.printStackTrace();
            message = "Sending file failed. \n Something wrong our KeyManager";
        }

        // Create an SSLContext that uses our TrustManager
        SSLContext sslContext = null; //SuperKeyManager.setForConnection(urlConnection, getApplicationContext(), )
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

        //Generate a symmetric key
        SecretKey sym_key = StaticCryptos.generateSymKey();


        //Encrypt file with the sym_key
        byte[][] ret = StaticCryptos.encrypt(sym_key, this.data);
        byte[] enc_data = ret[0];
        byte[] iv = ret[1];
        //Encrypt symmetric key with targetUsers publickey
        byte[] enc_sym_key = StaticCryptos.encSymKey(sym_key, targetUser.getPublicKey());
        try {
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
            urlConnection.setConnectTimeout(3000);
            urlConnection.setReadTimeout(3000);
            urlConnection.setChunkedStreamingMode(-1); // use default chunk size
            urlConnection.setDoOutput(true);



            System.out.println("Sendfiletask, enc_sym_key length: " + enc_sym_key.length);
            System.out.println("Sendfiletask, enc_sym_key data: " + Base64.encodeToString(enc_sym_key, Base64.DEFAULT));
            System.out.println("this.data.length = " + this.data.length);

            MultipartEntityBuilder mpb = MultipartEntityBuilder.create();
            mpb.setBoundary(boundary);
            mpb.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            mpb.addPart("name", new StringBody(targetUser.getName(), ContentType.DEFAULT_TEXT));
            mpb.addPart("req_status", new StringBody("2", ContentType.DEFAULT_TEXT));
            mpb.addPart("upload", new StringBody("true", ContentType.DEFAULT_TEXT));
            mpb.addPart("sym_key", new StringBody(Base64.encodeToString(enc_sym_key, Base64.DEFAULT),ContentType.DEFAULT_TEXT));
            mpb.addPart("iv", new StringBody(Base64.encodeToString(iv, Base64.DEFAULT),ContentType.DEFAULT_TEXT));
            mpb.addBinaryBody("file", enc_data, ContentType.DEFAULT_BINARY, fileName);
            //mpb.addBinaryBody("file", data, ContentType.DEFAULT_BINARY, fileName); // sends unencrypted!

            System.out.println("mpb: " + mpb.toString());
            //Tries to write the query and the file to the SSLConnection
            try {
                mpb.build().writeTo(urlConnection.getOutputStream());
            } catch (Exception e){
                e.printStackTrace();
            }

            InputStreamReader in = new InputStreamReader(urlConnection.getInputStream());
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(in);
            String line;
            for (; ; ) {
                line = br.readLine();
                if (line == null) {
                    break;
                } else {
                    sb.append(line);
                }
            }

            response = sb.toString();
            System.out.println("Response is: " + response);

        } catch (IOException e) {
            e.printStackTrace();
            message = "Sending file failed. \n Something wrong with sending the file";
        }
        urlConnection.disconnect();
        return null;
    }

    @Override
    public void onPostExecute(Void v){
        listener.onSendFileCompleted(message);
    }
}