package jmri.enginedriver3;
/**
 Collected constants of general utility.
<P>All members of this class are immutable. 
 <P>(This is an example of 
 <a href='http://www.javapractices.com/Topic2.cjp'>class for constants</a>.)
 */
public final class Consts  {

	public static final String DEBUG_TAG = "EngineDriver3";

	public static final String WEB = "web";
	public static final String LIST = "list";
    public static final String THROTTLE = "throttle";
    public static final String TURNOUT = "turnout";
	public static final String CONNECT = "connect";
	public static final String PREFS = "prefs";
	
	/** Opposite of {@link #FAILS}.  */
	public static final boolean PASSES = true;
	/** Opposite of {@link #PASSES}.  */
	public static final boolean FAILS = false;

	/** Opposite of {@link #FAILURE}.  */
	public static final boolean SUCCESS = true;
	/** Opposite of {@link #SUCCESS}.  */
	public static final boolean FAILURE = false;

	/** 
   Useful for {@link String} operations, which return an index of <tt>-1</tt> when 
   an item is not found. 
	 */
    public static final int NOT_FOUND = -1;

    public static final int INITIAL_HEARTBEAT = 5000;

	/** System property - <tt>line.separator</tt>*/
	public static final String NEW_LINE = System.getProperty("line.separator");
	/** System property - <tt>file.separator</tt>*/
	public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	/** System property - <tt>path.separator</tt>*/
	public static final String PATH_SEPARATOR = System.getProperty("path.separator");

	public static final String EMPTY_STRING = "";
	public static final String SPACE = " ";
	public static final String TAB = "\t";
	public static final String SINGLE_QUOTE = "'";
	public static final String PERIOD = ".";
	public static final String DOUBLE_QUOTE = "\"";

	// PRIVATE //

	/**
   The caller references the constants using <tt>Consts.EMPTY_STRING</tt>, 
   and so on. Thus, the caller should be prevented from constructing objects of 
   this class, by declaring this private constructor. 
	 */
	private Consts(){
		//this prevents even the native class from 
		//calling this ctor as well :
		throw new AssertionError();
	}
}

