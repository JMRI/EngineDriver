package jmri.enginedriver3;

import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.Inet4Address;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

/**
* Created by stevet on 7/16/2014.
*/
class JmdnsRunnable implements Runnable {

    private RetainedTaskFragment retainedTaskFragment;
    //        private Activity activity = null;  //this is now in the outer class
    //    private String jmdnsType = "_workstation._tcp.local.";
//    private String jmdnsType = "_http._tcp.local.";
    private String jmdnsType = "_withrottle._tcp.local.";
    private JmDNS jmdns = null;
    private ServiceListener listener = null;
    private ServiceInfo serviceInfo;
    android.net.wifi.WifiManager.MulticastLock multicastLock;
//    Handler jmdnsRunnableHandler;

    public JmdnsRunnable(RetainedTaskFragment in_retainedTaskFragment) {
        Log.d(Consts.DEBUG_TAG, "in JmdnsRunnable()");
        retainedTaskFragment = in_retainedTaskFragment;
//            this.activity = activity;  //save ref to original activity  TODO: remove or update when config changed
//        onAttach(activity);
    }

    @Override
    public void run() {
        Log.d(Consts.DEBUG_TAG, "starting JmdnsRunnable.run()");
        Looper.prepare();
        retainedTaskFragment.jmdnsRunnableHandler = new Jmdns_Handler();
        startJmdns();
        Looper.loop();
        Log.d(Consts.DEBUG_TAG, "ending JmdnsRunnable.run()");
    }

    private class Jmdns_Handler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            Log.d(Consts.DEBUG_TAG, "in JmdnsRunnable.handleMessage()");
            switch (msg.what) {
                case message_type.SHUTDOWN :
                    stopJmdns();
                    retainedTaskFragment.jmdnsRunnableHandler.getLooper().quit(); //stop the looper
                    break;
            }  //end of switch msg.what
            super.handleMessage(msg);
        }
    }

    private void startJmdns() {
        Log.d(Consts.DEBUG_TAG, "Starting Jmdns listeners");
        android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager) retainedTaskFragment.activity.getSystemService(android.content.Context.WIFI_SERVICE);
        multicastLock = wifi.createMulticastLock("engine_driver");
        multicastLock.setReferenceCounted(true);
        multicastLock.acquire();
        WifiInfo wifiinfo = wifi.getConnectionInfo();
        int intAddr= wifiinfo.getIpAddress();
        byte[] byteAddr = new byte[] { (byte)(intAddr & 0xff), (byte)(intAddr >> 8 & 0xff), (byte)(intAddr >> 16 & 0xff),
                (byte)(intAddr >> 24 & 0xff) };
        try {
            Inet4Address deviceAddr = (Inet4Address) Inet4Address.getByAddress(byteAddr);
            String deviceName = deviceAddr.toString().substring(1);		//strip off leading /
            Log.d(Consts.DEBUG_TAG,"start_jmdns: local IP addr " + deviceName);
            jmdns=JmDNS.create(deviceAddr, deviceName);  //pass ip as name to avoid hostname lookup attempt
            jmdns.addServiceListener(jmdnsType, listener = new ServiceListener() {

                @Override
                public void serviceResolved(ServiceEvent ev) {
                    String additions = "";
                    if (ev.getInfo().getInetAddresses() != null && ev.getInfo().getInetAddresses().length > 0) {
                        additions = ev.getInfo().getInetAddresses()[0].getHostAddress();
                    }
                    Log.d(Consts.DEBUG_TAG, "Service resolved: " + ev.getInfo().getQualifiedName() + " port:" + ev.getInfo().getPort() + additions);
                }

                @Override
                public void serviceRemoved(ServiceEvent ev) {
                    Log.d(Consts.DEBUG_TAG, "Service removed: " + ev.getName());
                }

                @Override
                public void serviceAdded(ServiceEvent event) {
                    // Required to force serviceResolved to be called again (after the first search)
                    jmdns.requestServiceInfo(event.getType(), event.getName(), 1);
                }
            });
//            serviceInfo = ServiceInfo.create("_test._tcp.local.", "AndroidTest", 0, "plain test service from android");
//            jmdns.registerService(serviceInfo);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
    private void stopJmdns() {
        try {
            Log.d(Consts.DEBUG_TAG,"removing jmdns listener");
            jmdns.removeServiceListener(jmdnsType, listener);
            multicastLock.release();
        }
        catch(Exception e) {
            Log.d(Consts.DEBUG_TAG,"exception in jmdns.removeServiceListener()");
        }
        try {
            Log.d(Consts.DEBUG_TAG,"calling jmdns.close()");
            jmdns.close();
            Log.d(Consts.DEBUG_TAG,"after jmdns.close()");
        }
        catch (Exception e) {
            Log.d(Consts.DEBUG_TAG,"exception in jmdns.close()");
        }
        jmdns = null;
    }

}
