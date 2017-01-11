package com.kimmy.kmode;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.util.Log;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.telephony.SubscriptionManager;
import android.net.LinkProperties;
import android.net.NetworkRequest;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.ConnectivityManager;
import android.os.Bundle;
import java.util.ArrayList;

public class NetworkRequestMode extends Fragment{

    private static final String TAG = "NetworkRequestMode";
    private Context mContext = null;
    private MainActivity mActivity = null;
    private int mRequest = -1;
    private int mSlotId = 0;
    private NetworkRequestListener mListener = null;
    private NetworkRequest[] mNetworkRequest = null;
    private ConnectivityManager.NetworkCallback[] mNetworkCallback = null;
    private ConnectivityManager mConnMgr = null;
    private boolean mIsAvailable = false;
    private Spinner mRequestSpinner = null;

    private class RequestInfo{
        public int subId = 0;
        public NetworkRequest request = null;
        public ConnectivityManager.NetworkCallback callback = null;
        public RequestInfo(int subId, NetworkRequest request, ConnectivityManager.NetworkCallback callback){
            this.subId = subId;
            this.request = request;
            this.callback = callback;
        }
    }
    private ArrayList<RequestInfo> mRequestInfos = null;

    private ConnectivityManager.NetworkCallback  getNetworkCallback(String subId) {
        final String mSubId = subId;
        final int mPhoneId = SubscriptionManager.getPhoneId(Integer.parseInt(mSubId));

        return new ConnectivityManager.NetworkCallback() {
            @Override
            public void onPreCheck(Network network) {
                Log.d(TAG,"onPreCheck");
            }
            @Override
            public void onAvailable(Network network) {
                Log.d(TAG,"onAvailable");
            }
            @Override
            public void onLosing(Network network, int timeToLive) {
                Log.d(TAG,"onLosing");
            }
            @Override
            public void onLost(Network network) {
                Log.d(TAG,"onLost");
            }
            @Override
            public void onUnavailable() {
                Log.d(TAG,"onUnavailable");
            }
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities nc) {
                Log.d(TAG,"onCapabilitiesChanged");
            }
            @Override
            public void onLinkPropertiesChanged(Network network, LinkProperties lp) {
                Log.d(TAG,"onLinkPropertiesChanged");
            }
        };
    }

    private class NetworkRequestListener implements KMode.ModeListener{
        @Override
        public void changeMode(int mode){
            log("changeMode: mode = "+mode);
            if(mode==KMode.NETWORK_REQUEST_MODE){
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
            log("infoChange: slotId = "+slotId);
            mSlotId = slotId;
        }

        @Override
        public void startMode(){
            if(mIsAvailable)requestNetwork();
        }

        @Override
        public void endMode(){
            if(mIsAvailable)releaseNetwork();
        }
    }

    private void buildView(){
        FragmentTransaction fragmentTransaction = mActivity.getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, this);
        fragmentTransaction.commit();
    }

    private void hideView(){
        mActivity.clearText();
    }

    private void printText(String text){
        mActivity.mInfoText.setText(text);
    }

    public NetworkRequestMode(Context context, MainActivity activity ){
        mContext = context;
        mActivity = activity;
        mRequestInfos = new ArrayList<RequestInfo>();
        KMode.getInstance().addListener(KMode.NETWORK_REQUEST_MODE,new NetworkRequestListener());
        mConnMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        log("NetworkRequestMode created");
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
        View view =  inflater.inflate(R.layout.network_request_mode, container, false);
        initView(view);
        return view;
    }

    private void initView(View view){
        mRequestSpinner = (Spinner)view.findViewById(R.id.requestSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mContext, R.array.network_request_list,android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mRequestSpinner.setAdapter(adapter);
        mRequestSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG,"initView: position = "+position);
                    mRequest = position;
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

    }

    private void requestNetwork(){
        SubscriptionManager subMgr = SubscriptionManager.from(mContext);

        int[] subIds = subMgr.getSubId(mSlotId);//create request from slot 0
        if(subIds.length > 0){
            log("requestNetwork: start request network "+subIds[0]+", mSlotId = "+mSlotId);
            RequestInfo info = new RequestInfo(subIds[0],buildNetworkRequest(subIds[0], getCapability(mRequest)),
                                               getNetworkCallback(String.valueOf(subIds[0])));
            printText("requestNetwork from "+mSlotId+" : "+info.request.toString());
            mConnMgr.requestNetwork(info.request, info.callback);
            mRequestInfos.add(info);
        }else{
            log("requestNetwork: invalid subId");
        }
    }

    private NetworkRequest buildNetworkRequest(int subId, int capability){
        NetworkRequest networkRequest = new NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addCapability(capability)
            .build();
        NetworkCapabilities cap = networkRequest.networkCapabilities.setNetworkSpecifier(String.valueOf(subId));
        log("buildNetworkRequest: networkRequest = "+networkRequest);
        return networkRequest;
    }

    private void releaseNetwork(){
        int capability = getCapability(mRequest);
        boolean isRemove = false;
        log("releaseNetwork: release = "+capability);
        for(RequestInfo info: mRequestInfos){
            log("releaseNetwork: info.request = "+info.request);
            if(info.request.networkCapabilities.hasCapability(capability)){
                printText("releaseNetwork from "+mSlotId+" : "+info.request.toString());
                mConnMgr.unregisterNetworkCallback(info.callback);
                mRequestInfos.remove(info);
                isRemove = true;
                break;
            }
        }
        if(!isRemove){
            printText("Can't find network request");
        }
    }

    private int getCapability(int request){
        log("getCapability: request = "+request);
        if(request==0)return NetworkCapabilities.NET_CAPABILITY_MMS;
        else if(request==1)return NetworkCapabilities.NET_CAPABILITY_SUPL;
        else if(request==2)return NetworkCapabilities.NET_CAPABILITY_INTERNET;
        else return NetworkCapabilities.NET_CAPABILITY_MMS;
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
