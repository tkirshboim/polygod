package com.kirshboim.polygod;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

        ((TextView) findViewById(R.id.version_name))
                .setText(getString(R.string.version, BuildConfig.VERSION_NAME));
        TextView view = (TextView) findViewById(R.id.about_view);
        view.setText(Html.fromHtml(readFromAssets(this, "about.html")));
        view.setMovementMethod(LinkMovementMethod.getInstance()); // makes links clickable
    }

    private String readFromAssets(Context context, String filename) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open(filename)));

            StringBuilder sb = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                sb.append(line);
                sb.append('\n');
                line = reader.readLine();
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return "";
    }
}

