package org.opencv.samples.colorblobdetect;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.samples.colorblobdetect.R;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class FirstActivity extends Activity {

    Button button;
    WebView webview;

//    String url = "http://textfiles.com/100/";
    String url = "http://10.192.51.225:8080/blob-upload-master/view.php";
//    String url = "https://www.webscorer.com/resources/templatestart";

    String curr_url;

    ProgressDialog prog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        this.getWindow().requestFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.activity_first);

//        getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);

        button = (Button)findViewById(R.id.button);

        webview = (WebView)findViewById(R.id.webView);
        webview.getSettings().setLoadsImagesAutomatically(true);
        webview.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        prog = new ProgressDialog(this);

        prog.setMessage("Loading...");
        prog.show();

        webview.setWebViewClient(new MyBrowser());
        webview.getSettings().setJavaScriptEnabled(true);
        webview.loadUrl(url);

        webview.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {

//                Uri uri = Uri.parse(url);
//                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//                startActivity(intent);



                String filename= URLUtil.guessFileName(url, contentDisposition, mimetype);
                Log.wtf("MTP", "downloading fileName:" + filename);

                DownloadManager.Request request = new DownloadManager.Request(
                        Uri.parse(url));

                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOCUMENTS + "/Tactile Reader", filename);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);
                Log.wtf("MTP", "DOWNLOAD CATCH..!!!");
//                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); //This is important!
//                intent.addCategory(Intent.CATEGORY_OPENABLE); //CATEGORY.OPENABLE
//                intent.setType("*/*");//any application,any extension
                Toast.makeText(getApplicationContext(), "Downloading File", Toast.LENGTH_LONG).show();
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent i = new Intent(Intent.ACTION_VIEW);
//                i.setData(Uri.parse(url));
//                startActivity(i);
                prog = new ProgressDialog(FirstActivity.this);
                prog.setMessage("Loading...");
                prog.show();
                webview.loadUrl(url);

            }
        });
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(this, StartActivity.class);
        startActivity(intent);
        finish();
//        super.onBackPressed();  // optional depending on your needs

    }



    private class MyBrowser extends WebViewClient {


        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            curr_url = url;
            if(url.endsWith(".txt")){
                String filename = url.substring(url.lastIndexOf('/') + 1);
                DownloadManager.Request request = new DownloadManager.Request(
                        Uri.parse(url));

                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOCUMENTS + "/Tactile Reader", filename);
//                request.setDestinationUri(Uri.parse(path));
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);
                Toast.makeText(getApplicationContext(), "Downloading File", Toast.LENGTH_LONG).show();

            }
            else if(url.endsWith(".mp3")){
//                progress=new ProgressDialog(FirstActivity.this);
//                progress.setMessage("Downloading file");
//                progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//                progress.show();

                Log.wtf("MTP", "DOWNLOADING...!!!");
                File path = getApplicationContext().getFilesDir();
                String filename = Uri.parse(url).getLastPathSegment();
                File file = new File(path, filename);
                Log.wtf("MTP", "HERE1...!!!");

                try{
                    URL downloadUrl = new URL(url);
                    URLConnection conexion = downloadUrl.openConnection();

                    InputStream input = conexion.getInputStream();
                    OutputStream output = new FileOutputStream(file);

                    byte[] buffer = new byte[1024];
                    int len1 = 0;
                    long total = 0;

                    while ((len1 = input.read(buffer)) > 0) {
                        total += len1; //total = total + len1
                        output.write(buffer, 0, len1);
                        Log.wtf("MTP", "writing...!!!");
                    }
                    output.close();

                }

//                try {
//                    FileOutputStream fos = new FileOutputStream(file);
//                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
//                    Log.wtf("MTP", "HERE2...!!!");
//
//                    // Create a URL for the desired page
//                    URL downloadUrl = new URL(url);
//                    Log.wtf("MTP", downloadUrl+"");
//
//                    // Read all the text returned by the server
//                    BufferedReader in = new BufferedReader(new InputStreamReader(downloadUrl.openStream()));
//                    String str;
//                    Log.wtf("MTP", "HERE3...!!!");
//
//                    while ((str = in.readLine()) != null) {
//                        // str is one line of text; readLine() strips the newline character(s)
//                        bw.write(str);
//                        Log.wtf("MTP", str);
//                        bw.newLine();
//                    }
//                    in.close();
//                    bw.close();
//                }
                catch (Exception e){
                    Log.wtf("MTP", "ERRORRR..!!");
                    e.printStackTrace();
                }


//                progress.dismiss();
//                Intent i = new Intent(FirstActivity.this, MainActivity.class);
//                i.putExtra("filename", filename);
//                startActivity(i);

            }
            else{
                view.loadUrl(url);
                if (!prog.isShowing()) {
                    prog.show();
                }
            }
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            System.out.println("on finish");
            if (prog.isShowing()) {
                prog.dismiss();
            }

        }
    }
}


