package jmri.enginedriver.enginedriver3;


import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import jmri.enginedriver.enginedriver3.R;
import jmri.enginedriver.enginedriver3.Consts;
import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class EngineDriver3Activity extends SherlockFragmentActivity {

	private ED3FragmentPagerAdapter mAdapter;
	private ViewPager mPager;
	public ActionBar mActionBar;
	private static int fragmentsPerScreen = 1; //will be changed later

	private static EngineDriver3Application mainapp; // hold pointer to mainapp

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(Consts.DEBUG_TAG,"in EngineDriver3Activity.onCreate()");

		super.onCreate(savedInstanceState);

		mainapp=(EngineDriver3Application)getApplication();

		try {
			setContentView(R.layout.main);
			mActionBar = getSupportActionBar();
			mAdapter = new ED3FragmentPagerAdapter(getSupportFragmentManager());
			mAdapter.setActionBar(mActionBar);
			mPager = (ViewPager)findViewById(R.id.pager);
			mPager.setAdapter(mAdapter);

			mPager.setOnPageChangeListener(new OnPageChangeListener() {

				@Override
				public void onPageScrollStateChanged(int arg0) {
					// TODO Auto-generated method stub
				}
				@Override
				public void onPageScrolled(int arg0, float arg1, int arg2) {
					// TODO Auto-generated method stub
				}
				@Override
				public void onPageSelected(int arg0) {
					Log.d("ViewPager", "onPageSelected: " + arg0);
					mActionBar.getTabAt(arg0).select();
				}
			} );

			mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

			//build tabs for each defined fragment
			for (int i = 0; i < mainapp.EDFrags.size(); i++) {
				Tab tab = mActionBar
						.newTab()
						.setText(mainapp.EDFrags.get(i).getName())
						.setTabListener(
								new TabListener<SherlockFragment>(this, i + "", mPager));
				mActionBar.addTab(tab);
			}
		} catch (Exception e) {
			Log.e("ViewPager exception:", e.toString());
		}

		//for testing, hard-code the available screen width based on orientation  TODO: replace this with calculation
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			fragmentsPerScreen = 5;
			mActionBar.setDisplayShowTitleEnabled(false);  //this interferes with the actionbar's fling
		} else {
			fragmentsPerScreen = 2;
			mActionBar.setDisplayShowTitleEnabled(true);
		}

		mActionBar.getTabAt(2).select();  //always start up with the connection tab  TODO: do this only when disconnected
	}

	public static class ED3FragmentPagerAdapter extends FragmentPagerAdapter {
		ActionBar mActionBar;

		public ED3FragmentPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public int getCount() {
			return mainapp.EDFrags.size();
		}

		@Override
		public SherlockFragment getItem(int position) {
			ED3Fragment f = null;
			f = ED3Fragment.newInstance(position);
			return f;

		}

		public void setActionBar( ActionBar bar ) {
			mActionBar = bar;
		}

		@Override
		public float getPageWidth(final int position) {
			//return fraction of screen used by this fragment#, based on width
			return (float) mainapp.EDFrags.get(position).getWidth()/fragmentsPerScreen;
		}
	}

	public static class ED3Fragment extends SherlockFragment {
		int 	mNum;  //fragment's index (key)
		String 	mName; //fragment's title
		String 	mType; //fragment's type (WEB, LIST, 
		View	mView; //the view object for this fragment

		/**
		 * Create a new instance of Fragment, providing "fragNum"
		 * as an argument.
		 */
		static ED3Fragment newInstance(int fragNum) {
			ED3Fragment f = new ED3Fragment();

			// Supply fragNum input as an argument, use as key to EDFrags.
			Bundle args = new Bundle();
			args.putInt("fragNum", fragNum);
			f.setArguments(args);

			return f;
		}

		/**
		 * When creating, retrieve this instance's fragNumber from its arguments, and store
		 *   it, the fragment name, and fragment type for quick reference later.
		 */
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			mNum =  (getArguments() != null ? getArguments().getInt("fragNum") : 1);
			mType = mainapp.EDFrags.get(mNum).getType();
			mName = mainapp.EDFrags.get(mNum).getName();
			Log.d(Consts.DEBUG_TAG, "in ED3Fragment.onCreate() for " + mName + " (" + mNum + ")" + " type " + mType);
		}

		/** inflate the proper xml layout for the fragment type
		 *    runs before activity starts		 */
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			Log.d(Consts.DEBUG_TAG, "in ED3Fragment.onCreateView() for " + mName + " (" + mNum + ")" + " type " + mType);
			//choose the proper layout xml for this fragment's type
			int rx = R.layout.list_fragment;  //default to list for now
			if (mType == Consts.WEB) {
				rx = R.layout.web_fragment;
			} else if (mType == Consts.LIST) {
				rx = R.layout.list_fragment;
			}
			//inflate the proper layout xml and remember it in fragment
			mView = inflater.inflate(rx, container, false);  
			View tv = mView.findViewById(R.id.title);  //all fragment views currently have title element
			if (tv != null) {
				((TextView)tv).setText(mName + " (" + mNum + ")");
			}
			return mView;
		}

		/** set up proper data and processing for this fragment type
		 *    runs after activity starts		 */
		@SuppressLint("SetJavaScriptEnabled")
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			Log.d(Consts.DEBUG_TAG, "in ED3Fragment.onActivityCreated() for " + mName + " (" + mNum + ")" + " type " + mType);
			super.onActivityCreated(savedInstanceState);

			if (mType == Consts.WEB) {
				WebView webview = (WebView)mView.findViewById(R.id.webview);
				String url = mainapp.EDFrags.get(mNum).getData();
				Log.d(Consts.DEBUG_TAG, "in ED3Fragment.onActivityCreated() setting url= " + url);
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
			if (mType == Consts.LIST) {
				ListView listview = (ListView) mView.findViewById(R.id.listview);
				int liid = android.R.layout.simple_list_item_1;  //use default layout and populate with some data
				listview.setAdapter(new ArrayAdapter<String>(getActivity(), liid, sCheeseStrings));
				listview.setOnItemClickListener(new viewlist_item());
				//hide the empty message (since we know we put stuff into the good list above)
				View ev = mView.findViewById(android.R.id.empty);
				ev.setVisibility(View.GONE);
			}
		}

		//available click listeners for a viewlist item
		public class viewlist_item implements android.widget.AdapterView.OnItemClickListener	  {

			public void onItemClick(android.widget.AdapterView<?> parent, View v, int position, long id)	    {
				Log.i("FragmentList", "Item clicked: " + id + " on " + mName + "(" + mNum + ")");	    
			};
		}	  

	}

	public static final String[] sCheeseStrings = {
		"Abbaye de Belloc", "Abbaye du Mont des Cats", "Abertam", "Abondance", "Ackawi",
		"Acorn", "Adelost", "Affidelice au Chablis", "Afuega'l Pitu", "Airag", "Airedale",
		"Aisy Cendre", "Allgauer Emmentaler", "Alverca", "Ambert", "American Cheese",
		"Ami du Chambertin", "Anejo Enchilado", "Anneau du Vic-Bilh", "Anthoriro", "Appenzell",
		"Aragon", "Ardi Gasna", "Ardrahan", "Armenian String", "Aromes au Gene de Marc",
		"Asadero", "Asiago", "Aubisque Pyrenees", "Autun", "Avaxtskyr", "Baby Swiss",
		"Babybel", "Baguette Laonnaise", "Bakers", "Baladi", "Balaton", "Bandal", "Banon",
		"Barry's Bay Cheddar", "Basing", "Basket Cheese", "Bath Cheese", "Bavarian Bergkase",
		"Baylough", "Beaufort", "Beauvoorde", "Beenleigh Blue", "Beer Cheese", "Bel Paese",
		"Bergader", "Bergere Bleue", "Berkswell", "Beyaz Peynir", "Bierkase", "Bishop Kennedy",
		"Blarney", "Bleu d'Auvergne", "Bleu de Gex", "Bleu de Laqueuille",
		"Bleu de Septmoncel", "Bleu Des Causses", "Blue", "Blue Castello", "Blue Rathgore",
		"Blue Vein (Australian)", "Blue Vein Cheeses", "Bocconcini", "Bocconcini (Australian)",
		"Boeren Leidenkaas", "Bonchester", "Bosworth", "Bougon", "Boule Du Roves",
		"Boulette d'Avesnes", "Boursault", "Boursin", "Bouyssou", "Bra", "Braudostur",
		"Breakfast Cheese", "Brebis du Lavort", "Brebis du Lochois", "Brebis du Puyfaucon",
		"Bresse Bleu", "Brick", "Brie", "Brie de Meaux", "Brie de Melun", "Brillat-Savarin",
		"Brin", "Brin d' Amour", "Brin d'Amour", "Brinza (Burduf Brinza)",
		"Briquette de Brebis", "Briquette du Forez", "Broccio", "Broccio Demi-Affine",
		"Brousse du Rove", "Bruder Basil", "Brusselae Kaas (Fromage de Bruxelles)", "Bryndza",
		"Buchette d'Anjou", "Buffalo", "Burgos", "Butte", "Butterkase", "Button (Innes)",
		"Buxton Blue", "Cabecou", "Caboc", "Cabrales", "Cachaille", "Caciocavallo", "Caciotta",
		"Caerphilly", "Cairnsmore", "Calenzana", "Cambazola", "Camembert de Normandie",
		"Canadian Cheddar", "Canestrato", "Cantal", "Caprice des Dieux", "Capricorn Goat",
		"Capriole Banon", "Carre de l'Est", "Casciotta di Urbino", "Cashel Blue", "Castellano",
		"Castelleno", "Castelmagno", "Castelo Branco", "Castigliano", "Cathelain",
		"Celtic Promise", "Cendre d'Olivet", "Cerney", "Chabichou", "Chabichou du Poitou",
		"Chabis de Gatine", "Chaource", "Charolais", "Chaumes", "Cheddar",
		"Cheddar Clothbound", "Cheshire", "Chevres", "Chevrotin des Aravis", "Chontaleno",
		"Civray", "Coeur de Camembert au Calvados", "Coeur de Chevre", "Colby", "Cold Pack",
		"Comte", "Coolea", "Cooleney", "Coquetdale", "Corleggy", "Cornish Pepper",
		"Cotherstone", "Cotija", "Cottage Cheese", "Cottage Cheese (Australian)",
		"Cougar Gold", "Coulommiers", "Coverdale", "Crayeux de Roncq", "Cream Cheese",
		"Cream Havarti", "Crema Agria", "Crema Mexicana", "Creme Fraiche", "Crescenza",
		"Croghan", "Crottin de Chavignol", "Crottin du Chavignol", "Crowdie", "Crowley",
		"Cuajada", "Curd", "Cure Nantais", "Curworthy", "Cwmtawe Pecorino",
		"Cypress Grove Chevre", "Danablu (Danish Blue)", "Danbo", "Danish Fontina",
		"Daralagjazsky", "Dauphin", "Delice des Fiouves", "Denhany Dorset Drum", "Derby",
		"Dessertnyj Belyj", "Devon Blue", "Devon Garland", "Dolcelatte", "Doolin",
		"Doppelrhamstufel", "Dorset Blue Vinney", "Double Gloucester", "Double Worcester",
		"Dreux a la Feuille", "Dry Jack", "Duddleswell", "Dunbarra", "Dunlop", "Dunsyre Blue",
		"Duroblando", "Durrus", "Dutch Mimolette (Commissiekaas)", "Edam", "Edelpilz",
		"Emental Grand Cru", "Emlett", "Emmental", "Epoisses de Bourgogne", "Esbareich",
		"Esrom", "Etorki", "Evansdale Farmhouse Brie", "Evora De L'Alentejo", "Exmoor Blue",
		"Explorateur", "Feta", "Feta (Australian)", "Figue", "Filetta", "Fin-de-Siecle",
		"Finlandia Swiss", "Finn", "Fiore Sardo", "Fleur du Maquis", "Flor de Guia",
		"Flower Marie", "Folded", "Folded cheese with mint", "Fondant de Brebis",
		"Fontainebleau", "Fontal", "Fontina Val d'Aosta", "Formaggio di capra", "Fougerus",
		"Four Herb Gouda", "Fourme d' Ambert", "Fourme de Haute Loire", "Fourme de Montbrison",
		"Fresh Jack", "Fresh Mozzarella", "Fresh Ricotta", "Fresh Truffles", "Fribourgeois",
		"Friesekaas", "Friesian", "Friesla", "Frinault", "Fromage a Raclette", "Fromage Corse",
		"Fromage de Montagne de Savoie", "Fromage Frais", "Fruit Cream Cheese",
		"Frying Cheese", "Fynbo", "Gabriel", "Galette du Paludier", "Galette Lyonnaise",
		"Galloway Goat's Milk Gems", "Gammelost", "Gaperon a l'Ail", "Garrotxa", "Gastanberra",
		"Geitost", "Gippsland Blue", "Gjetost", "Gloucester", "Golden Cross", "Gorgonzola",
		"Gornyaltajski", "Gospel Green", "Gouda", "Goutu", "Gowrie", "Grabetto", "Graddost",
		"Grafton Village Cheddar", "Grana", "Grana Padano", "Grand Vatel",
		"Grataron d' Areches", "Gratte-Paille", "Graviera", "Greuilh", "Greve",
		"Gris de Lille", "Gruyere", "Gubbeen", "Guerbigny", "Halloumi",
		"Halloumy (Australian)", "Haloumi-Style Cheese", "Harbourne Blue", "Havarti",
		"Heidi Gruyere", "Hereford Hop", "Herrgardsost", "Herriot Farmhouse", "Herve",
		"Hipi Iti", "Hubbardston Blue Cow", "Hushallsost", "Iberico", "Idaho Goatster",
		"Idiazabal", "Il Boschetto al Tartufo", "Ile d'Yeu", "Isle of Mull", "Jarlsberg",
		"Jermi Tortes", "Jibneh Arabieh", "Jindi Brie", "Jubilee Blue", "Juustoleipa",
		"Kadchgall", "Kaseri", "Kashta", "Kefalotyri", "Kenafa", "Kernhem", "Kervella Affine",
		"Kikorangi", "King Island Cape Wickham Brie", "King River Gold", "Klosterkaese",
		"Knockalara", "Kugelkase", "L'Aveyronnais", "L'Ecir de l'Aubrac", "La Taupiniere",
		"La Vache Qui Rit", "Laguiole", "Lairobell", "Lajta", "Lanark Blue", "Lancashire",
		"Langres", "Lappi", "Laruns", "Lavistown", "Le Brin", "Le Fium Orbo", "Le Lacandou",
		"Le Roule", "Leafield", "Lebbene", "Leerdammer", "Leicester", "Leyden", "Limburger",
		"Lincolnshire Poacher", "Lingot Saint Bousquet d'Orb", "Liptauer", "Little Rydings",
		"Livarot", "Llanboidy", "Llanglofan Farmhouse", "Loch Arthur Farmhouse",
		"Loddiswell Avondale", "Longhorn", "Lou Palou", "Lou Pevre", "Lyonnais", "Maasdam",
		"Macconais", "Mahoe Aged Gouda", "Mahon", "Malvern", "Mamirolle", "Manchego",
		"Manouri", "Manur", "Marble Cheddar", "Marbled Cheeses", "Maredsous", "Margotin",
		"Maribo", "Maroilles", "Mascares", "Mascarpone", "Mascarpone (Australian)",
		"Mascarpone Torta", "Matocq", "Maytag Blue", "Meira", "Menallack Farmhouse",
		"Menonita", "Meredith Blue", "Mesost", "Metton (Cancoillotte)", "Meyer Vintage Gouda",
		"Mihalic Peynir", "Milleens", "Mimolette", "Mine-Gabhar", "Mini Baby Bells", "Mixte",
		"Molbo", "Monastery Cheeses", "Mondseer", "Mont D'or Lyonnais", "Montasio",
		"Monterey Jack", "Monterey Jack Dry", "Morbier", "Morbier Cru de Montagne",
		"Mothais a la Feuille", "Mozzarella", "Mozzarella (Australian)",
		"Mozzarella di Bufala", "Mozzarella Fresh, in water", "Mozzarella Rolls", "Munster",
		"Murol", "Mycella", "Myzithra", "Naboulsi", "Nantais", "Neufchatel",
		"Neufchatel (Australian)", "Niolo", "Nokkelost", "Northumberland", "Oaxaca",
		"Olde York", "Olivet au Foin", "Olivet Bleu", "Olivet Cendre",
		"Orkney Extra Mature Cheddar", "Orla", "Oschtjepka", "Ossau Fermier", "Ossau-Iraty",
		"Oszczypek", "Oxford Blue", "P'tit Berrichon", "Palet de Babligny", "Paneer", "Panela",
		"Pannerone", "Pant ys Gawn", "Parmesan (Parmigiano)", "Parmigiano Reggiano",
		"Pas de l'Escalette", "Passendale", "Pasteurized Processed", "Pate de Fromage",
		"Patefine Fort", "Pave d'Affinois", "Pave d'Auge", "Pave de Chirac", "Pave du Berry",
		"Pecorino", "Pecorino in Walnut Leaves", "Pecorino Romano", "Peekskill Pyramid",
		"Pelardon des Cevennes", "Pelardon des Corbieres", "Penamellera", "Penbryn",
		"Pencarreg", "Perail de Brebis", "Petit Morin", "Petit Pardou", "Petit-Suisse",
		"Picodon de Chevre", "Picos de Europa", "Piora", "Pithtviers au Foin",
		"Plateau de Herve", "Plymouth Cheese", "Podhalanski", "Poivre d'Ane", "Polkolbin",
		"Pont l'Eveque", "Port Nicholson", "Port-Salut", "Postel", "Pouligny-Saint-Pierre",
		"Pourly", "Prastost", "Pressato", "Prince-Jean", "Processed Cheddar", "Provolone",
		"Provolone (Australian)", "Pyengana Cheddar", "Pyramide", "Quark",
		"Quark (Australian)", "Quartirolo Lombardo", "Quatre-Vents", "Quercy Petit",
		"Queso Blanco", "Queso Blanco con Frutas --Pina y Mango", "Queso de Murcia",
		"Queso del Montsec", "Queso del Tietar", "Queso Fresco", "Queso Fresco (Adobera)",
		"Queso Iberico", "Queso Jalapeno", "Queso Majorero", "Queso Media Luna",
		"Queso Para Frier", "Queso Quesadilla", "Rabacal", "Raclette", "Ragusano", "Raschera",
		"Reblochon", "Red Leicester", "Regal de la Dombes", "Reggianito", "Remedou",
		"Requeson", "Richelieu", "Ricotta", "Ricotta (Australian)", "Ricotta Salata", "Ridder",
		"Rigotte", "Rocamadour", "Rollot", "Romano", "Romans Part Dieu", "Roncal", "Roquefort",
		"Roule", "Rouleau De Beaulieu", "Royalp Tilsit", "Rubens", "Rustinu", "Saaland Pfarr",
		"Saanenkaese", "Saga", "Sage Derby", "Sainte Maure", "Saint-Marcellin",
		"Saint-Nectaire", "Saint-Paulin", "Salers", "Samso", "San Simon", "Sancerre",
		"Sap Sago", "Sardo", "Sardo Egyptian", "Sbrinz", "Scamorza", "Schabzieger", "Schloss",
		"Selles sur Cher", "Selva", "Serat", "Seriously Strong Cheddar", "Serra da Estrela",
		"Sharpam", "Shelburne Cheddar", "Shropshire Blue", "Siraz", "Sirene", "Smoked Gouda",
		"Somerset Brie", "Sonoma Jack", "Sottocenare al Tartufo", "Soumaintrain",
		"Sourire Lozerien", "Spenwood", "Sraffordshire Organic", "St. Agur Blue Cheese",
		"Stilton", "Stinking Bishop", "String", "Sussex Slipcote", "Sveciaost", "Swaledale",
		"Sweet Style Swiss", "Swiss", "Syrian (Armenian String)", "Tala", "Taleggio", "Tamie",
		"Tasmania Highland Chevre Log", "Taupiniere", "Teifi", "Telemea", "Testouri",
		"Tete de Moine", "Tetilla", "Texas Goat Cheese", "Tibet", "Tillamook Cheddar",
		"Tilsit", "Timboon Brie", "Toma", "Tomme Brulee", "Tomme d'Abondance",
		"Tomme de Chevre", "Tomme de Romans", "Tomme de Savoie", "Tomme des Chouans", "Tommes",
		"Torta del Casar", "Toscanello", "Touree de L'Aubier", "Tourmalet",
		"Trappe (Veritable)", "Trois Cornes De Vendee", "Tronchon", "Trou du Cru", "Truffe",
		"Tupi", "Turunmaa", "Tymsboro", "Tyn Grug", "Tyning", "Ubriaco", "Ulloa",
		"Vacherin-Fribourgeois", "Valencay", "Vasterbottenost", "Venaco", "Vendomois",
		"Vieux Corse", "Vignotte", "Vulscombe", "Waimata Farmhouse Blue",
		"Washed Rind Cheese (Australian)", "Waterloo", "Weichkaese", "Wellington",
		"Wensleydale", "White Stilton", "Whitestone Farmhouse", "Wigmore", "Woodside Cabecou",
		"Xanadu", "Xynotyro", "Yarg Cornish", "Yarra Valley Pyramid", "Yorkshire Blue",
		"Zamorano", "Zanetti Grana Padano", "Zanetti Parmigiano Reggiano"
	};


}