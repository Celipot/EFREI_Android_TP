package com.example.tp2_androidv2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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

        //button starting the authentification process
        Button authButton = findViewById(R.id.button_first);
        authButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //login
                EditText login = (EditText)findViewById(R.id.text_input);
                //password
                EditText pwd = (EditText)findViewById(R.id.text_input2);
                //authentification result
                TextView textView = (TextView)findViewById(R.id.textView);
                //thread in charge of the authentification
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        URL url = null;
                        try{
                            //connection to the httpbin
                            url = new URL("https://httpbin.org/basic-auth/bob/sympa");
                            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                            String basicAuth = "Basic " + Base64.encodeToString((login.getText() + ":" +pwd.getText()).getBytes(),Base64.NO_WRAP);
                            //adding credentials to the connection request
                            urlConnection.setRequestProperty ("Authorization", basicAuth);
                            try{
                                //reading the stream obtained from the url
                                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                                String s = readStream(in);
                                //printing the stream in logs
                                Log.i("JFL", s);
                                //using runonuithread to change the textview in the main thread
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            JSONObject json = new JSONObject(s);
                                            //changing the textview with the result of the authentification
                                            textView.setText("Authentification = " + json.getString("authenticated"));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }
                            finally{
                                urlConnection.disconnect();
                            }
                        }
                        catch(MalformedURLException e) {e.printStackTrace();}
                        catch(IOException e) {e.printStackTrace();}

                    }

                });
                //starting the authentification thread
                thread.start();

            }
        });

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