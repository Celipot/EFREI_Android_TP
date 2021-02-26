package com.example.flickerapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //button to show an image from flicker
        Button imgButton = findViewById(R.id.button);
        imgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //start asynchronous task reading photos of tree from flicker
                new AsyncFlickrJSONData().execute("https://www.flickr.com/services/feeds/photos_public.gne?tags=trees&format=json");
            }
        });
        //button redirecting to the image list activity
        Button toList = findViewById(R.id.button2);
        toList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //change of activity
                Intent myIntent = new Intent(MainActivity.this, ListActivity.class);
                startActivity(myIntent);
            }
        });
    }

    //asynchronous task reading the images page
    public class AsyncFlickrJSONData extends AsyncTask<String, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(String... strings) {
            //url of the image page
            URL url = null;
            //stream of the image page
            String s = null;

            try{
                //connecting to the url
                url = new URL(strings[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try{
                    //reading the stream
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    s = readStream(in);
                } finally {
                    urlConnection.disconnect();
                    //returning the json contained inside the page (= the page minus 2 parenthesis and the first word)
                    return new JSONObject(s.substring(15,s.length()-1));
                }
            }
            catch(MalformedURLException e) {e.printStackTrace();}
            catch(IOException | JSONException e) {e.printStackTrace();}
            return null;
        }

        protected void onPostExecute(JSONObject json) {
            String url = null;
            //showing the json in logs
            Log.i("JFL", json.toString());
            try {
                //decoding the image url from the json
                url = (new JSONObject(json.getJSONArray("items").getJSONObject(0).getString("media"))).getString("m");
                Log.i("JFL", url);
                //starting asynchronous task in charge of downloading the image
                new AsyncBitmapDownloader().execute(url);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //copied readstream method
        private String readStream(InputStream is) throws IOException {
            StringBuilder sb = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(is),1000);
            for (String line = r.readLine(); line != null; line =r.readLine()){
                sb.append(line);
            }
            is.close();
            return sb.toString();
        }
    }

    //asynchronous task downloading the image
    public class AsyncBitmapDownloader extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... strings) {
            URL url = null;
            Bitmap bm = null;
            try{
                //connecting to the image url
                url = new URL(strings[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    //getting the bitmap from the stream
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    bm = BitmapFactory.decodeStream(in);
                } finally {
                    urlConnection.disconnect();
                    //returning the bitmap
                    return bm;
                }
            }
            catch(IOException e) {e.printStackTrace();}
            return bm;
        }
        protected void onPostExecute(Bitmap bm) {
            //change the image on the app by the extracted bitmap
            ImageView img = (ImageView) findViewById(R.id.image);
            img.setImageBitmap(bm);
        }
    }
}

