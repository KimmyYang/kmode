package com.kimmy.kmode;

import android.content.Context;
import android.app.Activity;
import android.util.Log;
import java.util.ArrayList;

public class KMode{

    private static final String TAG = "KMode";
    private static KMode mInstance = null;
    private NetworkRequestMode mNwMode = null;
    private RilOemHookMode mOemHookMode= null;
    public static final int BASE_MODE = 0;
    public static final int RIL_OEM_HOOK_MODE = BASE_MODE + 1;
    public static final int NETWORK_REQUEST_MODE = BASE_MODE + 2;
    private int mMode = BASE_MODE;
    private ArrayList<Record> mRecordList = null;

    public interface ModeListener{
        public void changeMode(int mode);
        public void startMode();
        public void endMode();
        public void infoChange(int slotId);
    }

    private class Record{
        public int id = -1;
        public ModeListener callback = null;
        public Record(int id, ModeListener listener){
            this.id = id;
            this.callback = listener;
        }
    }

    private KMode(){
        mRecordList = new ArrayList<Record>();
    }

    public void createSubMode(Context context, MainActivity activity){
        mNwMode = new NetworkRequestMode(context,activity);
        mOemHookMode = new RilOemHookMode(context,activity);
    }

    public static KMode createMode(){
        if(mInstance == null){
            mInstance = new KMode();
        }
        return mInstance;
    }

    public static KMode getInstance(){
        return mInstance;
    }

    public void addListener(int id, ModeListener listener){
        mRecordList.add(new Record(id, listener));
    }

    public void setMode(int mode){
        mMode = mode;
        for(Record record: mRecordList){
            record.callback.changeMode(mode);
        }
    }

    public void startMode(){
        for(Record record: mRecordList){
            record.callback.startMode();
        }
    }

    public void endMode(){
        for(Record record: mRecordList){
            record.callback.endMode();
        }
    }

    public void setSlotId(int slotId){
	for(Record record: mRecordList){
            record.callback.infoChange(slotId);
        }
    }

    private void log(String text){
        Log.d(TAG,text);
    }
}
