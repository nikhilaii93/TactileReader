package org.opencv.samples.colorblobdetect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.opencv.samples.colorblobdetect.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StartActivity extends Activity {

    Spinner spinner;
    Button newCon, scan;
    SharedPreferences sp;
    TextView colortv;
    RadioGroup or;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        sp = PreferenceManager.getDefaultSharedPreferences(this);

        spinner = (Spinner)findViewById(R.id.spinner);

        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/Tactile Reader";
        ArrayList<String> filenames = new ArrayList<>();
        Log.wtf("MTP", "Path: " + path);
        File f = new File(path);
        File files[] = f.listFiles();
        Log.d("MTP", "Size: "+ files.length);
        for (int i=0; i < files.length; i++) {
            filenames.add(files[i].getName());
            Log.d("MTP", "FileName:" + files[i].getName());
        }

        if(filenames==null){
            filenames.add(0, "No saved Contexts");
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filenames);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spinner.setAdapter(dataAdapter);

        colortv = (TextView)findViewById(R.id.colortv);
        colortv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(StartActivity.this, SelectColorActivity.class);
                startActivity(i);
                finish();
            }
        });

        JSONArray savedColor = new JSONArray();
        try {
            savedColor = new JSONArray(sp.getString("touched_color_rgba", "[]"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(!(savedColor==null || savedColor.length()==0)){
            try {
                colortv.setBackgroundColor(Color.argb(savedColor.getInt(3), savedColor.getInt(0), savedColor.getInt(1), savedColor.getInt(2)));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        int checkedOr = sp.getInt("orientation", 1);

        or = (RadioGroup)findViewById(R.id.orient);
        Utility.orientation = or.indexOfChild(findViewById(checkedOr)) + 1;
        or.check(checkedOr);
        or.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                int index = group.indexOfChild(findViewById(group.getCheckedRadioButtonId())) + 1;
                Log.wtf("OCVSample::Activity", "Orientation: " + index);

                sp.edit().putInt("orientation", checkedId).apply();
                Utility.orientation = index;

//                Log.wtf("OCVSample::Activity", "Orientation: " + Utility.orientation);



            }
        });

        newCon = (Button)findViewById(R.id.newbutton);
        newCon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StartActivity.this, FirstActivity.class);
                startActivity(intent);
                finish();
            }
        });

        scan = (Button)findViewById(R.id.scanbutton);
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sp.edit().putString("context_name", spinner.getSelectedItem().toString()).apply();
                Log.wtf("MTP", "context_name: " + sp.getString("context_name", "nonon"));
                Intent intent = new Intent(StartActivity.this, ColorBlobDetectionActivity.class);
                startActivity(intent);
            }
        });

    }

}
