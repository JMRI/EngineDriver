package jmri.enginedriver.enginedriver3;

import com.actionbarsherlock.app.SherlockFragment;
import jmri.enginedriver.enginedriver3.R;
import jmri.enginedriver.enginedriver3.Consts;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ED3Fragment extends SherlockFragment {

//	public ED3Fragment() {
//		super();
//	}
	protected static EngineDriver3Application mainapp; // hold pointer to mainapp
	private int 	_num;  //fragment's index (key)
	private String 	_name; //fragment's title
	private String 	_type; //fragment's type (WEB, LIST, 
	private String 	data; //additional data string 
	protected View		view; //the view object for this fragment
	
	public String getName() {
		return _name;
	}
	public void setName(String name) {
		_name = name;
	}
	public String getType() {
		return _type;
	}
	public void setType(String type) {
		_type = type;
	}
	public int getNum() {
		return _num;
	}
	public void setNum(int num) {
		_num = num;
	}

	/**
	 * Create a new instance of Fragment
	 */
	static ED3Fragment newInstance(int fragNum, String fragType, String fragName) {
		Log.d(Consts.DEBUG_TAG, "in ED3Fragment.newInstance() for " + fragName + " (" + fragNum + ")" + " type " + fragType);
		ED3Fragment f = new ED3Fragment();

		// Store variables for retrieval 
//		setNum(fragNum);
//		setType(fragType);
//		setName(fragName);

		Bundle args = new Bundle();
		args.putInt("fragNum", fragNum);
		args.putString("fragType", fragType);
		args.putString("fragName", fragName);
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

		//fetch the stored data from bundle and copy into member variables
		setNum((getArguments() != null ? getArguments().getInt("fragNum") : 1));
		setType((getArguments() != null ? getArguments().getString("fragType") : "error"));
		setName((getArguments() != null ? getArguments().getString("fragName") : "error"));
		data =  (getArguments() != null ? getArguments().getString("fragData") : "");

		Log.d(Consts.DEBUG_TAG, "in ED3Fragment.onCreate() for " + getName() + " (" + getNum() + ")" + " type " + getType());
	}

	/** inflate the proper xml layout for the fragment type
	 *    runs before activity starts		 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(Consts.DEBUG_TAG, "in ED3Fragment.onCreateView() for " + getName() + " (" + getNum() + ")" + " type " + getType());
		//choose the proper layout xml for this fragment's type
		int rx = R.layout.list_fragment;  //default to list for now
		if (getType() == Consts.WEB) {
			rx = R.layout.web_fragment;
		} else if (getType() == Consts.LIST) {
			rx = R.layout.list_fragment;
		}
		//inflate the proper layout xml and remember it in fragment
		view = inflater.inflate(rx, container, false);  
		View tv = view.findViewById(R.id.title);  //all fragment views currently have title element
		if (tv != null) {
			((TextView)tv).setText(getName() + " (" + getNum() + ")");
		}
		return view;
	}

	/** set up proper data and processing for this fragment type
	 *    runs after activity starts		 */
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.d(Consts.DEBUG_TAG, "in ED3Fragment.onActivityCreated() for " + getName() + " (" + getNum() + ")" + " type " + getType());
		super.onActivityCreated(savedInstanceState);
		mainapp=(EngineDriver3Application)getActivity().getApplication();  //set pointer to app
		
		if (getType() == Consts.LIST) {
			ListView listview = (ListView) view.findViewById(R.id.listview);
			int liid = android.R.layout.simple_list_item_1;  //use default layout and populate with some data
			listview.setAdapter(new ArrayAdapter<String>(getActivity(), liid, sCheeseStrings));
			listview.setOnItemClickListener(new viewlist_item());
			//hide the empty message (since we know we put stuff into the good list above)
			View ev = view.findViewById(android.R.id.empty);
			ev.setVisibility(View.GONE);
		}
	}

	//available click listeners for a viewlist item
	public class viewlist_item implements android.widget.AdapterView.OnItemClickListener	  {

		public void onItemClick(android.widget.AdapterView<?> parent, View v, int position, long id)	    {
			Log.i("FragmentList", "Item clicked: " + id + " on " + getName() + "(" + getNum() + ")");	    
		};
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
		"Wensleydale", "White Stilton", "Whitestone Farmhouse", "Wigmore", "Woodside Cabecou", "Xynotyro", 
		"Yarg Cornish", "Yarra Valley Pyramid", "Yorkshire Blue",
		"Zamorano", "Zanetti Grana Padano", "Zanetti Parmigiano Reggiano"
	};
}
