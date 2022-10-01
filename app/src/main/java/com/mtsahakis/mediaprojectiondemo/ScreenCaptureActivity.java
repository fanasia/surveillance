package com.mtsahakis.mediaprojectiondemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Target;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static android.accounts.AccountManager.KEY_PASSWORD;
import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;


public class ScreenCaptureActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 100;
    final Handler handler = new Handler();
    final int delay = 5000; // 1000 milliseconds == 1 second
    final Handler oshandler = new Handler();
    final int osdelay = 1000; // 1000 milliseconds == 1 second

    SharedPreferences prefs = null;
    final String KEY_USERNAME = "username_key";

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private String defaultUsername;
    private EditText etUsername;

    /****************************************** Activity Lifecycle methods ************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // start projection
        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startLocationService();
                startProjection();
            }
        });

        // stop projection
        Button stopButton = findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                stopLocationService();
                stopProjection();

                Toast.makeText(getApplication().getApplicationContext(), "Service is stopped", Toast.LENGTH_SHORT).show();
            }
        });

        // generate random ID and set to log
        String randomID = UUID.randomUUID().toString();

        // setting shared preferences and save username
        prefs = getSharedPreferences("com.mtsahakis.mediaprojectiondemo", MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        TextView usernameValueTextView = (TextView) findViewById(R.id.usernameValue);
        if (prefs.getString(KEY_USERNAME, "").equals("")) {
            defaultUsername = randomID;

            ed.putString(KEY_USERNAME, defaultUsername);
            ed.commit();

            setUsernameInOSLog(defaultUsername);

            // setting default username to usernameValue
            usernameValueTextView.setText(defaultUsername);
        } else {
            String username = prefs.getString(KEY_USERNAME, defaultUsername);

            setUsernameInOSLog(username);

            // setting default username to usernameValue
            usernameValueTextView.setText(username);
        }

        // edit username button
        Button editUsernameButton = findViewById(R.id.editUsernameButton);
        editUsernameButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // show pop up
                LayoutInflater inflater = getLayoutInflater();
                View alertLayout = inflater.inflate(R.layout.layout_custom_dialog, null);
                etUsername = alertLayout.findViewById(R.id.usernameInput);

                // setting last username as a username in editText
                String username = prefs.getString(KEY_USERNAME, defaultUsername);
                etUsername.setText(username);

                AlertDialog.Builder builder = new AlertDialog.Builder(ScreenCaptureActivity.this);
                builder.setTitle("Input your username");
                builder.setView(alertLayout);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // update username in shared preferences
                        SharedPreferences.Editor ed = prefs.edit();
                        String newUsername = etUsername.getText().toString();

                        ed.putString(KEY_USERNAME, newUsername);
                        ed.commit();

                        // setting default username to usernameValue
                        TextView usernameValueTextView = (TextView) findViewById(R.id.usernameValue);
                        usernameValueTextView.setText(newUsername);

                        // set username in oslog
                        setUsernameInOSLog(newUsername);
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // asking permission
        enableMyLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // input username if it's first run
        if (prefs.getBoolean("first_run", true)) {
            // show pop up
            LayoutInflater inflater = getLayoutInflater();
            View alertLayout = inflater.inflate(R.layout.layout_custom_dialog, null);
            etUsername = alertLayout.findViewById(R.id.usernameInput);

            AlertDialog.Builder builder = new AlertDialog.Builder(ScreenCaptureActivity.this);
            builder.setTitle("Input your username");
            builder.setView(alertLayout);

            // Set up the buttons
            builder.setCancelable(false);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // update username in shared preferences
                    SharedPreferences.Editor ed = prefs.edit();
                    String newUsername = etUsername.getText().toString();

                    ed.putString(KEY_USERNAME, newUsername);
                    ed.commit();

                    // setting default username to usernameValue
                    TextView usernameValueTextView = (TextView) findViewById(R.id.usernameValue);
                    usernameValueTextView.setText(newUsername);

                    // set username in oslog
                    setUsernameInOSLog(newUsername);
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();

            // using the following line to edit/commit prefs
            prefs.edit().putBoolean("first_run", false).commit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(com.mtsahakis.mediaprojectiondemo.ScreenCaptureService.getStartIntent(this, resultCode, data));
                } else {
                    startService(com.mtsahakis.mediaprojectiondemo.ScreenCaptureService.getStartIntent(this, resultCode, data));
                }
            }
        }
    }

    /****************************************** Location ******************************************/

    private void enableMyLocation() {
        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {

            // give explanation
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_location_permission)
                        .setMessage(R.string.text_location_permission)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(ScreenCaptureActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        })
                        .create()
                        .show();
            } else {
                // request the permission.
                ActivityCompat.requestPermissions(ScreenCaptureActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        } else {
            Toast.makeText(this, "Location permission (already) granted", Toast.LENGTH_SHORT).show();
        }
    }

    @TargetApi(30)
    private void enableBackgroundLocation() {
        if (checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.title_location_permission)
                    .setMessage(R.string.text_background_location_permission)
                    .setPositiveButton(R.string.update_settings, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //Prompt the user once explanation has been shown
                            ActivityCompat.requestPermissions(ScreenCaptureActivity.this,
                                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                    MY_PERMISSIONS_REQUEST_LOCATION);
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    })
                    .create()
                    .show();
        }
    }

    private boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(this,
                permission) != PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        //ask for background permission
                        enableBackgroundLocation();
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    // show toast
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }

        }
    }

    private void startLocationService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this,LocationService.class));
        } else {
            startService(new Intent(this, LocationService.class));
        }
    }

    private void stopLocationService() {
        stopService(new Intent(this, LocationService.class));
    }

    private void setUsernameInOSLog(String newUsername) {
        ((OSLog) this.getApplication()).setUsername(newUsername);
    }

    /****************************************** UI Widget Callbacks *******************************/
    private void startProjection() {
        MediaProjectionManager mProjectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
        handler.postDelayed(new Runnable() {
            public void run() {
                File externalFilesDir = getExternalFilesDir(null);
                String mStoreDir = externalFilesDir.getAbsolutePath() + "/oslog/";
                File dir = new File(mStoreDir);
                if (dir.exists()) {
                    File[] files = dir.listFiles();
                    Arrays.sort(files);
                    if (files != null) {

                        for (int i = 0; i < files.length; ++i) {
                            File file = files[i];
                            //file.delete();
                            new RetrieveFeedTask().execute(file.getName());
                        }
                    }
                }
                handler.postDelayed(this, delay);
            }
        }, delay);

        /*oshandler.postDelayed(new Runnable() {
            public void run() {
                try {
                    Log.e(TAG, "???? os os "+ getOSLog().get("username").toString() );
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                oshandler.postDelayed(this, osdelay);
                FileOutputStream fos = null;
                try {
                    File externalFilesDir = getExternalFilesDir(null);
                    String mStoreDir = externalFilesDir.getAbsolutePath() + "/oslog/";
                    fos = new FileOutputStream(mStoreDir + (System.currentTimeMillis() / 1000) + ".txt");
                    byte[] bytesArray = getOSLog().toString().getBytes();

                    fos.write(bytesArray);
                    fos.flush();

                }catch (Exception e) {
                    Log.e(TAG,e.toString());

                }



            }
        }, osdelay);*/
    }

    private void stopProjection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(com.mtsahakis.mediaprojectiondemo.ScreenCaptureService.getStopIntent(this));
        } else {
            startService(com.mtsahakis.mediaprojectiondemo.ScreenCaptureService.getStopIntent(this));
        }
    }

    /*private class DownloadFilesTask extends AsyncTask<String, Integer, Long> {
        protected Long doInBackground(String... urls) {
            int count = urls.length;
            long totalSize = 0;
            for (int i = 0; i < count; i++) {
                Log.e(TAG,"???? "+i);
            }
            return totalSize;
        }

        protected void onProgressUpdate(Integer... progress) {

        }

        protected void onPostExecute(Long result) {

        }
    }*/

    public JSONObject getOSLog() {
        JSONObject json = new JSONObject();
        Log.e(TAG, "JSON " + ((OSLog) this.getApplication()).toString());
        try {
            json = new JSONObject(((OSLog) this.getApplication()).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    class RetrieveFeedTask extends AsyncTask<String, Void, String> {

        private Exception exception;

        protected String doInBackground(String... fnames) {
            try {
                String res = "";
                File externalFilesDir = getExternalFilesDir(null);
                for (String fname : fnames) {
                    String osPath = externalFilesDir.getAbsolutePath() + "/oslog/" + fname;
                    String photoPath = externalFilesDir.getAbsolutePath() + "/screenshots/" + fname.replaceAll(".txt", ".png");
                    if ((new File(photoPath).exists()) == false) {
                        Log.e(TAG, "file not exist " + photoPath);
                        new File(osPath).delete();
                        break;
                    }
                    HttpsTrustManager.allowAllSSL();
                    HttpClient httpClient = new DefaultHttpClient();
                    httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
                    HttpPost postRequest = new HttpPost("https://entity.cs.helsinki.fi/upload.php");
//                    HttpPost postRequest = new HttpPost("https://localhost:5000/upload.php");
                    //MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                    builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                    JSONObject info = readFromFile(osPath);
                    if (info.length() == 0) {
                        new File(osPath).delete();
                        new File(photoPath).delete();
                        break;
                    }
                    info.put("filename", fname.replaceAll(".txt", ""));
                    String extra = info.toString();
                    Log.i("extra", extra);
                    //reqEntity.addPart("extra", new StringBody(extra, "text/plain", StandardCharsets.UTF_8));
                    //reqEntity.addPart("lang", new StringBody("en"));
                    //reqEntity.addPart("username", new StringBody(getMacAddr()));
                    builder.addPart("extra", new StringBody(extra, ContentType.TEXT_PLAIN));
                    builder.addPart("lang", new StringBody("eng", ContentType.TEXT_PLAIN));
                    // username is the one in shared preferences
                    String username = prefs.getString(KEY_USERNAME, defaultUsername);
//                    builder.addPart("username", new StringBody(getMacAddr(), ContentType.TEXT_PLAIN));
                    builder.addPart("username", new StringBody(username, ContentType.TEXT_PLAIN));
                    // building image
                    try {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        Bitmap bitmap = BitmapFactory.decodeFile(photoPath, options);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, bos);
                        byte[] data = bos.toByteArray();
                        // to find the image name in the folder no?
                        String photoName = fname.replaceAll(".txt", ".jpeg");
                        ByteArrayBody bab = new ByteArrayBody(data, photoName);
                        builder.addPart("image", bab);
                    } catch (Exception e) {
                        Log.v("Exception in Image", ""+e);
                        builder.addPart("image", new StringBody("", ContentType.TEXT_PLAIN));
                        builder.addPart("exception_in_image", new StringBody(e.toString(), ContentType.TEXT_PLAIN));
                    }
                    HttpEntity entity = builder.build();
                    postRequest.setEntity(entity);
                    HttpResponse response = httpClient.execute(postRequest);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                    String sResponse;
                    StringBuilder s = new StringBuilder();
                    while ((sResponse = reader.readLine()) != null) {
                        s = s.append(sResponse);
                    }
                    res = s.toString();
                    Log.i("HTTP Response", res);
                    if (res.equals("file uploaded")) {
                        Log.e(TAG, "response " + s + "delete ..." + osPath);
                        File osFile = new File(osPath);
                        File photoFile = new File(photoPath);
                        osFile.delete();
                        photoFile.delete();
                    }
                }

                return res;
            } catch (Exception e) {
                Log.e("Exception send request:", e.toString());
                this.exception = e;

                return "";
            } finally {
            }
        }

        protected void onPostExecute(String feed) {
            // TODO: check this.exception
            // TODO: do something with the feed
        }
    }

    public static class HttpsTrustManager implements X509TrustManager {

        private static TrustManager[] trustManagers;
        private static final X509Certificate[] _AcceptedIssuers = new X509Certificate[]{};

        @Override
        public void checkClientTrusted(
                java.security.cert.X509Certificate[] x509Certificates, String s)
                throws java.security.cert.CertificateException {

        }

        @Override
        public void checkServerTrusted(
                java.security.cert.X509Certificate[] x509Certificates, String s)
                throws java.security.cert.CertificateException {

        }

        public boolean isClientTrusted(X509Certificate[] chain) {
            return true;
        }

        public boolean isServerTrusted(X509Certificate[] chain) {
            return true;
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return _AcceptedIssuers;
        }

        public static void allowAllSSL() {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }

            });

            SSLContext context = null;
            if (trustManagers == null) {
                trustManagers = new TrustManager[]{new HttpsTrustManager()};
            }

            try {
                context = SSLContext.getInstance("TLS");
                context.init(null, trustManagers, new SecureRandom());
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }

            HttpsURLConnection.setDefaultSSLSocketFactory(context
                    .getSocketFactory());
        }

    }

    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }

                Log.e(TAG, "MacAddressFay = " + res1.toString());
                return res1.toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "02:00:00:00:00:00";
    }

    public JSONObject readFromFile(String osPath) {
        JSONObject res = new JSONObject();
        //Get the text file
        File file = new File(osPath);

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
            }
            res = new JSONObject(String.valueOf(text));
            Log.e(TAG, "read os " + res);
            br.close();
        } catch (IOException | JSONException e) {
            //You'll need to add proper error handling here
            Log.e(TAG, "read os " + e.toString());
        }
        return res;
    }
}