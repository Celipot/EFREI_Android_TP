package com.example.flickerapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
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
import java.util.List;
import java.util.Vector;


public class ListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        //instanciating the adapter
        MyAdapter adapter = new MyAdapter();
        ListView list_img = (ListView) findViewById(R.id.list);
        //linking it to the image list object
        list_img.setAdapter(adapter);
        //instanciating a queue
        RequestQueue queue = MySingleton.getInstance(this.getApplicationContext()).getRequestQueue();

        //getting the user current location
        Location currentLoc = getLocation();
        if (currentLoc != null) {
            //if a location has been found print it in logs then search for 10 images around this location (using an asynchronous task)
            Log.i("JFL", "Longitude : " + currentLoc.getLongitude() + " Latitude : " + currentLoc.getLatitude());
            new AsyncFlickrJSONDataForList(adapter).execute("https://api.flickr.com/services/rest/?method=flickr.photos.search&license=4&api_key=fc4f25c5298990861bbffe9781aea0e9&has_geo=1&lat=" + currentLoc.getLatitude() + "&lon=" + currentLoc.getLongitude() + "&per_page=10&format=json");
        }
        else {
            //if not, search for 10 images around a predefined location (latitude = 52.520049 longitude =13.391716) (using an asynchronous task)
            new AsyncFlickrJSONDataForList(adapter).execute("https://api.flickr.com/services/rest/?method=flickr.photos.search&license=4&api_key=fc4f25c5298990861bbffe9781aea0e9&has_geo=1&lat=52.520049&lon=13.391716&per_page=10&format=json");
        }
        //old asynchronous task without location parameters
        //new AsyncFlickrJSONDataForList(adapter).execute("https://www.flickr.com/services/feeds/photos_public.gne?tags=trees&format=json");
    }

    //adapter class
    public class MyAdapter extends BaseAdapter {

        private Vector<String> vector = new Vector<>();

        public void add(String url) {
            vector.add(url);
        }

        @Override
        public int getCount() {
            return vector.size();
        }

        @Override
        public Object getItem(int position) {
            return vector.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            //inflating the layout used to prompt images
            LayoutInflater inflater = LayoutInflater.from(ListActivity.this);
            View viewMyLayout = inflater.inflate(R.layout.bitmaplayout, null);
            ImageView textview = (ImageView) viewMyLayout.findViewById(R.id.image2);

            //listener in charge of changing the bitmaps in the application
            Response.Listener<Bitmap> rep_listener = response -> {
                textview.setImageBitmap(response);
            };

            //instanciating the image request for the image in situed at "position" in the vector
            ImageRequest imageRequest = new ImageRequest(vector.get(position), rep_listener, 0, 0, Bitmap.Config.RGB_565, null);
            //adding the request to the queue
            MySingleton.getInstance(ListActivity.this).addToRequestQueue(imageRequest);
            return viewMyLayout;
        }
    }

    //asynchronous task in charge of getting images' urls
    public class AsyncFlickrJSONDataForList extends AsyncTask<String, Void, JSONObject> {
        private MyAdapter adapter;

        public AsyncFlickrJSONDataForList(MyAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        protected JSONObject doInBackground(String... strings) {
            URL url = null;
            String s = null;

            try {
                //connecting to the url of the image search result
                url = new URL(strings[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    //getting the stream
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    s = readStream(in);
                } finally {
                    urlConnection.disconnect();
                    //returning the json contained in the stream (= the page minus 2 parenthesis and the first word)
                    return new JSONObject(s.substring(14, s.length() - 1));
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(JSONObject json) {
            String url = null;
            //printing the json containing the images in the logs
            Log.i("JFL", json.toString());
            try {
                //extracting the array containing the images in the json
                JSONArray jsonArray = json.getJSONObject("photos").getJSONArray("photo");
                //iterating over this array (over each images)
                for (int i = 0; i < jsonArray.length(); i++) {

                    //old extraction of the url from the json
                    //url = (new JSONObject(jsonArray.getJSONObject(i).getString("media"))).getString("m");

                    //extracting the different parts of the url fron the json
                    String id = jsonArray.getJSONObject(i).getString("id");
                    String server = jsonArray.getJSONObject(i).getString("server");
                    String secret = jsonArray.getJSONObject(i).getString("secret");
                    //putting those differents parts to get the url of the image
                    url = new String("https://live.staticflickr.com/" + server+"/"+id+"_"+secret+".jpg");
                    //adding the url to the adapter
                    adapter.add(url);
                    //printing the url to the logs
                    Log.i("JFL", "Adding to adapter url : " + url);
                }
                adapter.notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //copied redstream method
        private String readStream(InputStream is) throws IOException {
            StringBuilder sb = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(is), 1000);
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                sb.append(line);
            }
            is.close();
            return sb.toString();
        }
    }

    //copied singleton class in charge of creating the queue
    public static class MySingleton {
        private static MySingleton instance;
        private RequestQueue requestQueue;
        private ImageLoader imageLoader;
        private static Context ctx;

        private MySingleton(Context context) {
            ctx = context;
            requestQueue = getRequestQueue();

            imageLoader = new ImageLoader(requestQueue,
                    new ImageLoader.ImageCache() {
                        private final LruCache<String, Bitmap>
                                cache = new LruCache<String, Bitmap>(20);

                        @Override
                        public Bitmap getBitmap(String url) {
                            return cache.get(url);
                        }

                        @Override
                        public void putBitmap(String url, Bitmap bitmap) {
                            cache.put(url, bitmap);
                        }
                    });
        }

        public static synchronized MySingleton getInstance(Context context) {
            if (instance == null) {
                instance = new MySingleton(context);
            }
            return instance;
        }

        public RequestQueue getRequestQueue() {
            if (requestQueue == null) {
                // getApplicationContext() is key, it keeps you from leaking the
                // Activity or BroadcastReceiver if someone passes one in.
                requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
            }
            return requestQueue;
        }

        public <T> void addToRequestQueue(Request<T> req) {
            getRequestQueue().add(req);
        }

        public ImageLoader getImageLoader() {
            return imageLoader;
        }
    }

    //getting the user location
    public Location getLocation() {
        //checking permission to access to the location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //asking for permissions to access the location
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.ACCESS_COARSE_LOCATION},48);
        }
        //instanciating the location manager
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        //getting the different internet providers
        List<String> providers = locationManager.getProviders(true);
        Location currentLoc = null;
        //iterating over  each providers
        for (String provider : providers) {
            //getting location from provider
            Location l = locationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (currentLoc == null || l.getAccuracy() < currentLoc.getAccuracy()) {
                //if a location is found or its accuracy is better of the previously found location
                currentLoc = l;
            }
        }
        //returning the location
        return currentLoc;
    }

}