package com.kimmy.kmode;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.util.Log;
import android.content.Context;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.os.Bundle;
import android.os.Environment;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

import android.telephony.TelephonyManager;

public class RilOemHookMode extends Fragment{

    private static final String TAG = "RilOemHookMode";
    private Context mContext = null;
    private MainActivity mActivity = null;
    private boolean mIsAvailable = false;
    private CheckBox mNvCheckBox = null;
    private CheckBox mFileCheckBox = null;
    private EditText mNvText = null;
    private EditText mNvValue = null;
    private TelephonyManager mTelephonyManager = null;

    private static final String EXTERNAL_FILE_PATH = "kmode_config";
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static final int sizeOfNv = 8;
    private static final int sizeOfContent = 4;
    private static final int numberOfBitsInAHalfByte = 4;
    private static final int halfByte = 0x0F;
    private static final String NV_READ_HEADER = "5155414c434f4d4d6e0009000400";
    private static final String NV_WRITE_HEADER = "5155414c434f4d4d6f0009000400";

    private class RilOemHookListener implements KMode.ModeListener{
        @Override
        public void changeMode(int mode){
            log("changeMode: mode = "+mode);
            if(mode==KMode.RIL_OEM_HOOK_MODE){
                buildView();
                mIsAvailable = true;
            }
            else {
                hideView();
                mIsAvailable = false;
            }
        }

        @Override
        public void infoChange(int slotId){
        }

        @Override
        public void startMode(){
            if(mIsAvailable)requestOemHook();
        }

        @Override
        public void endMode(){
        }
    }

    private void buildView(){
        log("buildView");
        FragmentTransaction fragmentTransaction = mActivity.getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, this);
        fragmentTransaction.commit();
    }

    private void hideView(){
        log("hideView");
    }

    public RilOemHookMode(Context context, MainActivity activity ){
        mContext = context;
        mActivity = activity;
        KMode.getInstance().addListener(KMode.RIL_OEM_HOOK_MODE,new RilOemHookListener());
        mTelephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        log("RilOemHookMode created");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "[onCreate]");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "[onCreateView]");
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.ril_oem_hook_mode, container, false);
        initView(view);
        return view;
    }

    private void initView(View view){
        mNvCheckBox = (CheckBox)view.findViewById(R.id.NvCheckBox);
        mFileCheckBox = (CheckBox)view.findViewById(R.id.FileCheckBox);
        mNvText = (EditText)view.findViewById(R.id.NvEidtText);//read
        mNvValue = (EditText)view.findViewById(R.id.NvEidtValue);//write
    }

    private void requestOemHook(){
        byte[] oemResp = new byte[10];
        byte[] data;
        String nvId = null;
        String cmd = null;
        String content = mNvValue.getText().toString();//init
        //String cmd = "5155414c434f4d4d6e00090004000000b2030000";
        //String cmd = "5155414c434f4d4d6e0009000400000061000100";
        if(mNvCheckBox.isChecked() && !mNvText.getText().toString().equals("")){
            nvId = decToHex(Integer.parseInt(mNvText.getText().toString()),sizeOfNv);
            nvId = convertNvIdFormat(nvId);
            log("requestOemHook: content = "+content);
            if(content.equals("")){
                content = "0000";
                cmd = NV_READ_HEADER;
            }else{
                cmd = NV_WRITE_HEADER;
                content = decToHex(Integer.parseInt(content), sizeOfContent);
                content = convertNvIdFormat(content);
            }
            cmd = cmd+nvId+content;
        }else if(mFileCheckBox.isChecked()){
            cmd = loadFile(EXTERNAL_FILE_PATH);
            if(cmd.length() == 0){
                printText("Null Command");
                return;
            }
        }
        log("requestOemHook: cmd = "+cmd);

        if(cmd != null){
            data = hexStringToBytes(cmd);
            if(mTelephonyManager!=null){
                mTelephonyManager.invokeOemRilRequestRaw(data, oemResp);
                printText("request: "+cmd+", response: "+bytesToHex(oemResp));
            }
            else log("mTelephonyManager is null");
        }
    }

    private String convertNvIdFormat(String nvId){
        String convertNv = "";
        for(int i=nvId.length()-1; i>=0; --i){
            if(i-1>=0 && (!nvId.substring(i-1,i).equals("0") || !nvId.substring(i,i+1).equals("0"))){
                convertNv = convertNv+nvId.substring(i-1,i)+nvId.substring(i,i+1);
                --i;
            }else{
                convertNv = nvId.substring(i,i+1) + convertNv;
            }
        }
        Log.d(TAG,"convertNvIdFormat: convertNv = "+convertNv);
        return convertNv;
    }

    private String decToHex(int dec, int size) {
        StringBuilder hexBuilder = new StringBuilder(size);
        hexBuilder.setLength(size);
        for (int i = size - 1; i >= 0; --i)
        {
            int j = dec & halfByte;
            hexBuilder.setCharAt(i, hexArray[j]);
            dec >>= numberOfBitsInAHalfByte;
        }
        return hexBuilder.toString(); 
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private String loadFile(String path){
        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(sdcard,path);
        StringBuilder text = new StringBuilder();
        try{
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while((line=br.readLine())!=null){
                text.append(line);
                break;
            }
        }catch(IOException ex){

        }
        log("loadFile: text = "+text);
        return text.toString();
    }

    private byte[] hexStringToBytes(String s) {
        byte[] ret;
        if (s == null) return null;
        int sz = s.length();
        ret = new byte[sz/2];
        for (int i=0 ; i <sz ; i+=2) {
            ret[i/2] = (byte) ((hexCharToInt(s.charAt(i)) << 4)
                    | hexCharToInt(s.charAt(i+1)));
        }
        return ret;
    }

    private int hexCharToInt(char c) {
        if (c >= '0' && c <= '9') return (c - '0');
        if (c >= 'A' && c <= 'F') return (c - 'A' + 10);
        if (c >= 'a' && c <= 'f') return (c - 'a' + 10);

        throw new RuntimeException ("invalid hex char '" + c + "'");
    }

    private void printText(String text){
        mActivity.mInfoText.setText(text);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void log(String text){
        Log.d(TAG,text);
    }
}
