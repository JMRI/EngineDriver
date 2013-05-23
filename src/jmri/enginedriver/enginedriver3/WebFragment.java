package jmri.enginedriver.enginedriver3;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

@SuppressLint("SetJavaScriptEnabled")
class WebFragment extends ED3Fragment {
	
	private String url;
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	static WebFragment newInstance(int fragNum, String fragType, String fragName, String fragData) {
		Log.d(Consts.DEBUG_TAG, "in WebFragment.newInstance()for " + fragName + " (" + fragNum + ")" + " type " + fragType);
		WebFragment f = new WebFragment();

		// Store variables for retrieval 
		Bundle args = new Bundle();
		args.putInt("fragNum", fragNum);
		args.putString("fragType", fragType);
		args.putString("fragName", fragName);
		args.putString("fragData", fragName);
		f.setArguments(args);

		return f;
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.d(Consts.DEBUG_TAG, "in ED3Fragment.onActivityCreated() for " + getName() + " (" + getNum() + ")" + " type " + getType());
		super.onActivityCreated(savedInstanceState);
		mainapp=(EngineDriver3Application)getActivity().getApplication();  //set pointer to app

		WebView webview = (WebView)view.findViewById(R.id.webview);
		String url = mainapp.EDFrags.get(getNum()).getData();
		if (url.indexOf("://") < 0) {  //if protocol included, use url as is, else append protocol and port
			url = "http://" + mainapp.getServer() + ":" + mainapp.getWebPort() + url;
		}
		Log.d(Consts.DEBUG_TAG, "in ED3Fragment.onActivityCreated() setting url= " + url);
		View tv = view.findViewById(R.id.title);  //put the url as the title TODO: remove this after testing
		if (tv != null) {
			((TextView)tv).setText(getName() + " (" + getNum() + ") " + url);
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
		webview.loadUrl(url);
	}

//	public WebFragment() {
//		super();
//	}
}