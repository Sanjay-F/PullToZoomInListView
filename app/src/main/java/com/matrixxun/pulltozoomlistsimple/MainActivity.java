package com.matrixxun.pulltozoomlistsimple;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioGroup;

public class MainActivity extends Activity {
    private static final int INVALID_POINTER_ID = -1;
    PullToZoomListView listView;
    private String[] adapterData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (PullToZoomListView) findViewById(R.id.listview);
        adapterData = new String[]{"Activity", "Service", "Content Provider", "Intent", "BroadcastReceiver", "ADT", "Sqlite3", "HttpClient", "DDMS", "Android Studio", "Fragment", "Loader"};

        listView.setAdapter(new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1, adapterData));
        listView.getHeaderView().setImageResource(R.drawable.ttt);
        listView.getHeaderView().setScaleType(ImageView.ScaleType.CENTER_CROP);



        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.am_style_rg);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.stick:
                        listView.setHeadScrollModel(PullToZoomListView.STICK);
                        break;
                    case R.id.par:
                        listView.setCollapseFactor(PullToZoomListView.PARALLAX);
                        break;
                    case R.id.normal:
                        listView.setHeadScrollModel(PullToZoomListView.NORMAL);
                        break;

                }
            }
        });

        findViewById(R.id.am_enable_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listView.allowZoom(!listView.isEnableZoom());
            }
        });
    }



}
