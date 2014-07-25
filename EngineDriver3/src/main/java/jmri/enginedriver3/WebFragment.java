package jmri.enginedriver3;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

@SuppressLint("SetJavaScriptEnabled")
public class WebFragment extends DynaFragment {
	
	private String _url;
	
	public String getUrl() {
		return _url;
	}

	public void setUrl(String url) {
		this._url = url;
	}

	/**---------------------------------------------------------------------------------------------**
	 * create a new fragment of this type, and populate basic settings in bundle 
	 * Note: this is a static method, called from the activity's getItem() when new ones are needed */	
	static WebFragment newInstance(int fragNum, String fragType, String fragName, String fragData) {
//		Log.d(Consts.DEBUG_TAG, "in WebFragment.newInstance()for " + fragName + " (" + fragNum + ")" + " type " + fragType);
		WebFragment f = new WebFragment();

		// Store variables for retrieval 
		Bundle args = new Bundle();
		args.putInt("fragNum", fragNum);
		args.putString("fragType", fragType);
		args.putString("fragName", fragName);
		args.putString("fragData", fragData);
		f.setArguments(args);

		return f;
	}

    @Override
    public void onStart() {
        super.onStart();
        //only set this when fragment is ready to receive messages
        mainApp.dynaFrags.get(getFragNum()).setHandler(new Fragment_Handler());
    }

    @Override
    public void onStop() {
        //clear this to avoid late messages
        mainApp.dynaFrags.get(getFragNum()).setHandler(null);
        super.onStop();
    }

    /**---------------------------------------------------------------------------------------------**
	 * Called when new fragment is created, retrieves stuff from bundle and sets instance members	 
	 * Note: only override if extra values are needed                                               */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		Log.d(Consts.DEBUG_TAG, "in WebFragment.onCreate() for " + getFragName() + " (" + getFragNum() + ")" + " type " + getFragType());
		
		//fetch additional stored data from bundle and copy into member variable _url
		_url =  (getArguments() != null ? getArguments().getString("fragData") : "");

	}

	/**---------------------------------------------------------------------------------------------**
	 * inflate and populate the proper xml layout for this fragment type
	 *    runs before activity starts, note does not call super		 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//		Log.d(Consts.DEBUG_TAG, "in ED3Fragment.onCreateView() for " + getFragName() + " (" + getFragNum() + ")" + " type " + getFragType());
		//choose the proper layout xml for this fragment's type
		int rx = R.layout.web_fragment;
		//inflate the proper layout xml and remember it in fragment
		view = inflater.inflate(rx, container, false);  
		View tv = view.findViewById(R.id.title);  //all fragment views currently have title element
		if (tv != null) {
			((TextView)tv).setText(getFragName() + " (" + getFragNum() + ")");
		}
		return view;
	}
	/**---------------------------------------------------------------------------------------------**
	 * tells the fragment that its activity has completed its own Activity.onCreate().	 
	 * Note: calls super.  Only override if additional processing is needed                           */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
//		Log.d(Consts.DEBUG_TAG, "in WebFragment.onActivityCreated() for " + getFragName() + " (" + getFragNum() + ")" + " type " + getFragType());
		super.onActivityCreated(savedInstanceState);	

		WebView webview = (WebView)view.findViewById(R.id.webview);
        if (mainApp.getServer() == null) {
            _url = "file:///android_asset/not_connected.html";
        } else if (_url.indexOf("://") < 0) {  //if protocol included, use _url as is, else append protocol and port
			_url = "http://" + mainApp.getServer() + ":" + mainApp.getWebPort() + _url;
		}
//		Log.d(Consts.DEBUG_TAG, "in WebFragment.onActivityCreated() setting url= " + _url);
		View tv = view.findViewById(R.id.title);  //put the _url as the title TODO: remove this after testing
		if (tv != null) {
			((TextView)tv).setText(getFragName() + " (" + getFragNum() + ") " + _url);
		}
		webview.getSettings().setJavaScriptEnabled(true);
		webview.getSettings().setBuiltInZoomControls(true); //Enable Multitouch if supported
		webview.getSettings().setUseWideViewPort(true);		// Enable greater zoom-out
		//				webview.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
		//				webview.setInitialScale((int)(100 * scale));
		webview.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		webview.getSettings().setDomStorageEnabled(true);

		// open all links inside the current view (don't start external web browser)
		WebViewClient EDWebClient = new WebViewClient()	{
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String  url) {
				return false;
			}
			public void onReceivedError(WebView view, int errorCod,String description, String failingUrl) {
				Log.e(Consts.DEBUG_TAG, "webview error: " + description + " url: " + failingUrl);
			}
		};
		webview.setWebViewClient(EDWebClient);
		webview.loadUrl(_url);
	}
    private class Fragment_Handler extends Handler {
        @Override
        public void handleMessage(Message msg) {
//            Log.d(Consts.DEBUG_TAG, "in ConnectFragment.handleMessage()");
            switch (msg.what) {
                case MessageType.CONNECTED:  //TODO: redraw the screen to reflect connectedness
                    Log.d(Consts.DEBUG_TAG, "in WebFragment.handleMessage() CONNECTED");
//                    discoveredServerListAdapter.notifyDataSetChanged();
                    break;
                default:
                    Log.d(Consts.DEBUG_TAG, "in WebFragment.handleMessage() not handled");
            }  //end of switch msg.what
            super.handleMessage(msg);
        }
    }

}