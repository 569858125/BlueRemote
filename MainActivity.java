package com.panodic.bluetooth.remote;

import java.lang.reflect.Method;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {
	private final String BLUENAME="Amino-Mini";
	
	private int scanNum=0;
	private TextView tv_show=null;
	private BluetoothProfile profile=null;
	private BluetoothAdapter bluetoothAdapter=null;
	private boolean discovery=false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		printScreen();
		setContentView(R.layout.activity_main);
		tv_show=(TextView) findViewById(R.id.tv_show);
		findViewById(R.id.btn_next).setOnClickListener(this);
		initBlue();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.v("tag","onDestroy");
		stopDiscovery();
		if(bluetoothAdapter!=null)
			unregisterReceiver(receiver);
	}
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		Log.v("tag","onBackPressed");
	}

	private void initBroadcast()
	{
		IntentFilter intentFilter = new IntentFilter();  
		intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);//蓝牙开关状态改变广播
		intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED); //蓝牙绑定状态改变广播
		intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);//蓝牙连接状态改变广播
		intentFilter.addAction(BluetoothDevice.ACTION_FOUND);//扫描到设备的广播
		intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//扫面完毕的广播
		registerReceiver(receiver, intentFilter);  
	}
	private void initBlue()
	{
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null) 
		{
			tv_show.setText(R.string.no_blue);
			return ;
		}
		initBroadcast();
		listenerBlueState();
		try {
			bluetoothAdapter.getProfileProxy(this, connListener,
					BluetoothProfile.INPUT_DEVICE);
		} catch (Exception e) {
			Log.e("tag"," bluetoothAdapter getProfileProxy error=>"+ e.toString());
			e.printStackTrace();
		}
	}
	private void listenerBlueState()
	{
		int state=bluetoothAdapter.getState();
		Log.v("tag", "listenerBlueState bluetoothAdapter state==>"+state);
		switch(state)
		{
		case BluetoothAdapter.STATE_ON:
			Log.i("tag","bluetooth state is open. startDiscovery");
			discovery=true;
			startDiscovery();
			break;
		case BluetoothAdapter.STATE_OFF:
			Log.e("tag","bluetooth state is close. open it");
			tv_show.setText(R.string.opening);
			bluetoothAdapter.enable();
			break;
		}
	}
	private void startDiscovery()
	{
		Log.v("tag", "startDiscovery==>"+discovery);
		if(discovery)
		{
			tv_show.setText(String.format(getString(R.string.searching), ++scanNum));
        	bluetoothAdapter.startDiscovery();
		}
	}
	private void stopDiscovery()
	{
		Log.v("tag", "stopDiscovery==>"+discovery);
		if(discovery)
		{
        	discovery=false;
        	bluetoothAdapter.cancelDiscovery();
		}
	}
	private void checkRemote()
	{
		if(profile==null)
		{
			Log.e("tag","checkRemote false. profile null");
			return ;
		}
		List<BluetoothDevice> mDevices = profile.getConnectedDevices();  
		if(mDevices==null)
		{
			Log.e("tag","checkRemote false. mDevices null");
			return ;
		}
		boolean b=false;
		Log.v("tag","mDevices.size==>"+mDevices.size());
        for (BluetoothDevice device : mDevices) 
        {  
            String name=device.getName();
            Log.i("tag", "onServiceConnected device name: " + name);  
            if(BLUENAME.equalsIgnoreCase(name))
            {
            	b=true;
            }
        } 
		Log.i("tag","checkRemote result==>"+b);
		if(b)
		{
			stopDiscovery();
        	tv_show.setText(R.string.pair_suc);
        }
	}
	
	private BluetoothProfile.ServiceListener connListener=new BluetoothProfile.ServiceListener() {
		
		@Override
		public void onServiceDisconnected(int arg0) {
			Log.e("tag", "onServiceDisconnected==>"+arg0);
		}
		
		@Override
		public void onServiceConnected(int i, BluetoothProfile bluetoothprofile) {
			Log.e("tag", "onServiceConnected="+i+" bluetooth profile="+bluetoothprofile);
			if(bluetoothprofile==null)
				return ;
			if(i== BluetoothProfile.INPUT_DEVICE)
			{
				profile=bluetoothprofile;
				checkRemote();
			}
		}
	};
	BroadcastReceiver receiver=new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			BluetoothDevice device=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			String action=intent.getAction();
			Log.e("tag", action+" device="+device(device));
			if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
			{
				listenerBlueState();
			}
			else if(action.equals(BluetoothDevice.ACTION_FOUND))
			{
				if(device==null)
					return;
				String name=device.getName();
				if(BLUENAME.equalsIgnoreCase(name))
				{
					int state=device.getBondState();
					Log.i("tag", "ACTION_FOUND find target device==>"+name+" getBondState:  "+state);
					switch (state) 
					{
					case BluetoothDevice.BOND_NONE:
						bind(device);
	                    break;    
					case BluetoothDevice.BOND_BONDED:
						connect(device);
	                    break;    
					default:
						break;
					}
				}
			}
			else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
			{
				startDiscovery();
			}
			else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action))
			{    
				if(device==null)
					return;
	            String name=device.getName();
				if(BLUENAME.equalsIgnoreCase(name))
				{
					int state=device.getBondState();
					Log.i("tag", "ACTION_BOND_STATE_CHANGED find target device==>"+name+" getBondState:  "+state);
					if(state==BluetoothDevice.BOND_BONDED) 
	                {    
						connect(device);  
					}
				}
			}
			else if(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action))
			{
				checkRemote();
	        }
		}
	};
	
	private void bind(final BluetoothDevice device)
	{    
		Log.i("tag", "start bind device====>"+device);
	    try {  
	    	Method createBondMethod = BluetoothDevice.class.getMethod("createBond");  
	        createBondMethod.invoke(device);  
	    } catch (Exception e) {  
	    	Log.e("tag", "bind device error=>"+e.toString());
	        e.printStackTrace();  
	    }
	}   
	private void connect(final BluetoothDevice device)
	{    
		Log.i("tag", "start connect device====>"+device);
    	try {  
	        Method method = profile.getClass().getMethod("connect",new Class[] { BluetoothDevice.class });  
	        method.invoke(profile, device);  
	    } catch (Exception e) {  
	    	Log.e("tag", "connect device error=>"+e.toString());
	        e.printStackTrace();  
	    }
	}  
	private String device(BluetoothDevice device)
	{
		if(device==null)
			return "null";
		String name=device.getName();
		String mac=device.getAddress().toString();
		int deviceclass=device.getBluetoothClass().getDeviceClass();
		return "{name="+name+" mac="+mac+" deviceclass="+deviceclass+"}";
	}
	private void printScreen()
    {
        WindowManager mWM = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics me=new DisplayMetrics();
        mWM.getDefaultDisplay().getMetrics(me);
        Log.v("tag","density= "+me.density+" screenWidth= "+me.widthPixels+" screenHeight= "+me.heightPixels);
    }

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		Log.v("tag","onClick==>"+v);
		switch(v.getId())
		{
		case R.id.btn_next:
			setResult(RESULT_OK, null);
			finish();
			break;
		}
	}
}
