package com.kimmy.kmode;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.view.View;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.RelativeLayout;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.view.LayoutInflater;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.Manifest.permission;
import android.Manifest;
import android.content.pm.PackageManager;
import android.telephony.SubscriptionManager;

public class MainActivity extends Activity {
    private final String VERSION = "2.2";
    private final String TAG = "kmode";
    private Spinner mModeSpinner = null;
    private Spinner mSlotIdSpinner = null;
    private int mKmodeType = -1;
    private KMode mMode = null;
    //private String mNVDisableSIMBlock = "5155414c434f4d4d6f00090009000000a20101000100000000";
    private Button mStartBtn = null;//positive
    private Button mEndBtn = null;//negative
    public TextView mInfoText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMode = KMode.createMode();
        mMode.createSubMode(getApplicationContext(),this);
        Log.i(TAG, "onCreate: KMode Version : "+VERSION);
    }

    protected  void onStart(){
        super.onStart();
        initLayout();
    }

    private void initLayout(){
        //spinner
        mModeSpinner = (Spinner)findViewById(R.id.kmode_type_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.kmode_type_list,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mModeSpinner.setAdapter(adapter);
        mModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0)return;
                mKmodeType = position;
                if(mMode!=null)mMode.setMode(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        enableSlotIdView(true);//always enable slot id spinner

        //button
        mStartBtn = (Button)findViewById(R.id.EnableBtn);
        mStartBtn.setOnClickListener(new  Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(mMode!=null)mMode.startMode();
            }
        });
        mEndBtn = (Button)findViewById(R.id.DisableBtn);
        mEndBtn.setOnClickListener(new  Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(mMode!=null)mMode.endMode();
            }
        });
        //enableEndButton(false);
        //text
        mInfoText = (TextView)findViewById(R.id.infoText);
    }

    private void enableSlotIdView(boolean enable){
        log("enableSlotIdView: enable = "+enable);
        if(mSlotIdSpinner != null && enable){
            mSlotIdSpinner.setVisibility(View.VISIBLE);
        }
        else if(mSlotIdSpinner != null && !enable){
            mSlotIdSpinner.setVisibility(View.GONE);
        }
        else{//init
            mSlotIdSpinner = (Spinner)findViewById(R.id.slot_spinner);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.slot_id_list,
                android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSlotIdSpinner.setAdapter(adapter);
            mSlotIdSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG,"showNetworkView: slotId = "+position);
                    if(mMode!=null)mMode.setSlotId(position);
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
    }

    private void enableEndButton(boolean enable){
        if(mEndBtn==null)return;
        if(enable)mEndBtn.setVisibility(View.VISIBLE);
        else mEndBtn.setVisibility(View.GONE);
    }

    public TextView getInfoText(){
        return mInfoText;
    }

    public void clearText(){
        mInfoText.setText("");
    }

    @Override
    public void onResume(){
        super.onResume();
        mModeSpinner.setSelection(mKmodeType);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void log(String log){
        Log.d(TAG,log);
    }
}
