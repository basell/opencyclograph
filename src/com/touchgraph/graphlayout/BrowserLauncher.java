package com.touchgraph.graphlayout;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * <p>
 * Launch web browser to display URL.
 * </p>
 * <p>
 * <b>This class has been modified from the original source for use in J-Bird. See Version info for J-Bird modifications.</b>
 * </p>
 * <p>
 * BrowserLauncher is a class that provides one static method, openURL, which opens the default
 * web browser for the current user of the system to the given URL.  It may support other
 * protocols depending on the system -- mailto, ftp, etc. -- but that has not been rigorously
 * tested and is not guaranteed to work.
 * </p>
 * <p>
 * Yes, this is platform-specific code, and yes, it may rely on classes on certain platforms
 * that are not part of the standard JDK.  What we're trying to do, though, is to take something
 * that's frequently desirable but inherently platform-specific -- opening a default browser --
 * and allow programmers (you, for example) to do so without worrying about dropping into native
 * code or doing anything else similarly evil.
 * </p>
 * <p>
 * Anyway, this code is completely in Java and will run on all JDK 1.1-compliant systems without
 * modification or a need for additional libraries.  All classes that are required on certain
 * platforms to allow this to run are dynamically loaded at runtime via reflection and, if not
 * found, will not cause this to do anything other than returning an error when opening the
 * browser.
 * </p>
 * <p>
 * There are certain system requirements for this class, as it's running through Runtime.exec(),
 * which is Java's way of making a native system call.  Currently, this requires that a Macintosh
 * have a Finder which supports the GURL event, which is true for Mac OS 8.0 and 8.1 systems that
 * have the Internet Scripting AppleScript dictionary installed in the Scripting Additions folder
 * in the Extensions folder (which is installed by default as far as I know under Mac OS 8.0 and
 * 8.1), and for all Mac OS 8.5 and later systems.  On Windows, it only runs under Win32 systems
 * (Windows 95, 98, and NT 4.0, as well as later versions of all).  On other systems, this drops
 * back from the inherently platform-sensitive concept of a default browser and simply attempts
 * to launch Netscape via a shell command.
 * </p>
 * <p>
 * This code is Copyright 1999-2002 by Eric Albert (ejalbert@cs.stanford.edu) and may be
 * redistributed or modified in any form without restrictions as long as the portion of this
 * comment from this paragraph through the end of the comment is not removed.  The author
 * requests that he be notified of any application, applet, or other binary that makes use of
 * this code, but that's more out of curiosity than anything and is not required.  This software
 * includes no warranty.  The author is not repsonsible for any loss of data or functionality
 * or any adverse or unexpected effects of using this software.
 * </p>
 * <p>
 * Credits:
 * <br>Steven Spencer, JavaWorld magazine (<a href="http://www.javaworld.com/javaworld/javatips/jw-javatip66.html">Java Tip 66</a>)
 * <br>Thanks also to Ron B. Yeh, Eric Shapiro, Ben Engber, Paul Teitlebaum, Andrea Cantatore,
 * Larry Barowski, Trevor Bedzek, Frank Miedrich, Ron Rabakukk, and Glenn Vanderburg.
<br> J-Bird mods by Dick Repasky.
 *
 * @author Eric Albert (<a href="mailto:ejalbert@cs.stanford.edu">ejalbert@cs.stanford.edu</a>)
 * @version 1.4b2
 * </p>
 * <p>
 * <b>Modifications to Version 1.4b2 source for J-Bird:</b> 
 * </p>
 * <ol>
 * <li>fixed Linux quote problem as described on a bug report on the 
 *     sourceforge project page;</li>
 * <li>  guarantee three slashes on file urls on Mac OS X MRJ 1_3_1;</li>
 * <li>add support for com.apple.eio.FileManager.openURL in Mac OS X 
 *       MRJ 1_4_1. Unfortunately, com.apple.eio.FileManager.openURL was
 *       broken in Java(TM) 2 Runtime Environment, Standard Edition (build 1.4.1_01-39)
 *       Java HotSpot(TM) Client VM (build 1.4.1_01-14, mixed mode). Provided kludge
 *       as the method launchMRJ41Bad. The kludge launches
 *       Internet Explorer unless the BROWSER environment variable is
 *	 set to the name of the browser.  This doesn't work for Safari.
 *	Note that the kludge can be used for other versions of the MRJ
 *	by setting the mrj to MRJ_BAD in the initialization block of code.
 * 	This will probably work until IE is no longer shipped with Mac OS X.
 *	</li>
 * <li>Expanded Unix horizons</li>
 * 	<ul>
 * 	<li>Look for firefox, opera, dillo, konqueror, mozilla and netscape
 * 	    on the path specified by the PATH environment variable.</li>
 * 	<li>If the PATH appears to be empty, check for browsers in
 * 	    /usr/bin and /usr/local/bin.</li>
 * 	<li>Launch other browsers differently than netscape.</li>
 * 	</ul>
 * <li>KDE 2.0 kfmclient recognized (J-Bird 0.4.6)</li>
 *	<ul>
 *	<li>KDE 2.0 sets BROWSER environment variable to "kfmclient openProfile webbrowsing" at least on Mandrake 10.0.
	<li>If the browser contains the string "kfmclient openProfile",
		launchKfmclient is called.</li>
	<li>This is a hack. I am reluctant to parse into arguments any
		browser value that contains spaces because somebody is sure
		to eventually create a browser binary that has a name that
		contains spaces. Reconsider this later.</li>
 *	</ul>
 * <li>Added methods openFile(String) and open(File) which use 
 *		File.absolutePath and escapes
 * 		spaces in the path. (J-Bird 0.5.1)</li>
 * </ol>
 */
public class BrowserLauncher {

	/**
	 * The Java virtual machine that we are running on.  Actually, in most cases we only care
	 * about the operating system, but some operating systems require us to switch on the VM. */
	private static int jvm;

	/** The browser for the system */
	private static Object browser;

	/**
	 * Caches whether any classes, methods, and fields that are not part of the JDK and need to
	 * be dynamically loaded at runtime loaded successfully.
	 * <p>
	 * Note that if this is <code>false</code>, <code>openURL()</code> will always return an
	 * IOException.
	 */
	private static boolean loadedWithoutErrors;

	/** The com.apple.mrj.MRJFileUtils class */
	private static Class mrjFileUtilsClass;

	/** The com.apple.mrj.MRJOSType class */
	private static Class mrjOSTypeClass;

	/** The com.apple.MacOS.AEDesc class */
	private static Class aeDescClass;
	
	/** The <init>(int) method of com.apple.MacOS.AETarget */
	private static Constructor aeTargetConstructor;
	
	/** The <init>(int, int, int) method of com.apple.MacOS.AppleEvent */
	private static Constructor appleEventConstructor;
	
	/** The <init>(String) method of com.apple.MacOS.AEDesc */
	private static Constructor aeDescConstructor;
	
	/** The findFolder method of com.apple.mrj.MRJFileUtils */
	private static Method findFolder;

	/** The getFileCreator method of com.apple.mrj.MRJFileUtils */
	private static Method getFileCreator;
	
	/** The getFileType method of com.apple.mrj.MRJFileUtils */
	private static Method getFileType;
	
	/** The openURL method of com.apple.mrj.MRJFileUtils */
	private static Method openURL;
	
	/** The makeOSType method of com.apple.MacOS.OSUtils */
	private static Method makeOSType;
	
	/** The putParameter method of com.apple.MacOS.AppleEvent */
	private static Method putParameter;
	
	/** The sendNoReply method of com.apple.MacOS.AppleEvent */
	private static Method sendNoReply;
	
	/** Actually an MRJOSType pointing to the System Folder on a Macintosh */
	private static Object kSystemFolderType;

	/** The keyDirectObject AppleEvent parameter type */
	private static Integer keyDirectObject;

	/** The kAutoGenerateReturnID AppleEvent code */
	private static Integer kAutoGenerateReturnID;
	
	/** The kAnyTransactionID AppleEvent code */
	private static Integer kAnyTransactionID;

	/** The linkage object required for JDirect 3 on Mac OS X */
	@SuppressWarnings("unused")
	private static Object linkage;
	
	/** The framework to reference on Mac OS X 10.0.x */
	@SuppressWarnings("unused")
	private static final String JDirect_MacOSX = "/System/Library/Frameworks/Carbon.framework/Frameworks/HIToolbox.framework/HIToolbox";

	/** JVM constant for MRJ 2.0 */
	private static final int MRJ_2_0 = 0;
	
	/** JVM constant for MRJ 2.1.x and 2.2.x */
	private static final int MRJ_2_1 = 1;

	/** JVM constant for Java on Mac OS X 10.0 (MRJ 3.0) */
	private static final int MRJ_3_0 = 3;
	
	/** JVM constant for Java on Mac OS X 10.1 and later (MRJ 3.1) */
	private static final int MRJ_3_1 = 4;

	/** JVM constant for any Windows NT JVM */
	private static final int WINDOWS_NT = 5;
	
	/** JVM constant for any Windows 9x JVM */
	private static final int WINDOWS_9x = 6;

	/** JVM constant for Java on Mac OS X 10.1 and later (MRJ 4.1) */
	private static final int MRJ_4_1 = 7;

	/** JVM constant for any other platform */
	private static final int OTHER = -1;

	/** JVM constant for macs that can be used to load IE using some 
		javascript magic. */
	private static final int MRJ_BAD = -2;

	/**
	 * The file type of the Finder on a Macintosh.  Hardcoding "Finder" would keep non-U.S. English
	 * systems from working properly.
	 */
	private static final String FINDER_TYPE = "FNDR";

	/**
	 * The creator code of the Finder on a Macintosh, which is needed to send AppleEvents to the
	 * application.
	 */
	private static final String FINDER_CREATOR = "MACS";

	/** The name for the AppleEvent type corresponding to a GetURL event. */
	private static final String GURL_EVENT = "GURL";

	/**
	 * The first parameter that needs to be passed into Runtime.exec() to open the default web
	 * browser on Windows.
	 */
    private static final String FIRST_WINDOWS_PARAMETER = "/c";
    
    /** The second parameter for Runtime.exec() on Windows. */
    private static final String SECOND_WINDOWS_PARAMETER = "start";
    
    /**
     * The third parameter for Runtime.exec() on Windows.  This is a "title"
     * parameter that the command line expects.  Setting this parameter allows
     * URLs containing spaces to work.
     */
    private static final String THIRD_WINDOWS_PARAMETER = "\"\"";
	
	/**
	 * The shell parameters for Netscape that opens a given URL in an already-open copy of Netscape
	 * on many command-line systems.
	 */
	private static final String NETSCAPE_REMOTE_PARAMETER = "-remote";
	private static final String NETSCAPE_OPEN_PARAMETER_START = "openURL(";
	private static final String NETSCAPE_OPEN_PARAMETER_END = ")";
	
	/**
	 * The message from any exception thrown throughout the initialization process.
	 */
	private static String errorMessage;

		/** @since J-Bird 0.4.3 */
	private static final String[] UNIX_PATHS = new String[] {
				"/usr/bin",
				"/usr/local/bin"
	};

		/** @since J-Bird 0.4.3 */
	private static final String[] UNIX_BROWSERS = new String[] {
				"firefox",
				"opera",
				"dillo",
				"konqueror",
				"mozilla",
				"netscape"
	};

	/**
	 * An initialization block that determines the operating system and loads the necessary
	 * runtime data.
	 */
	static {
		loadedWithoutErrors = true;
		String osName = System.getProperty("os.name");
		if (osName.startsWith("Mac OS")) {
			String mrjVersion = System.getProperty("mrj.version");
			String majorMRJVersion = mrjVersion.substring(0, 3);
			System.out.println("System property mrj.version = "
				+ mrjVersion);
			System.out.println("clipped value = " + majorMRJVersion);
			try {
				double version = Double.valueOf(majorMRJVersion).doubleValue();
				if (version == 2) {
					jvm = MRJ_2_0;
				} else if (version >= 2.1 && version < 3) {
					// Assume that all post-2.1 2.x versions of MRJ work the same. MRJ 2.1 actually
					// works via Runtime.exec() and 2.2 supports that but has an openURL() method
					// as well that we don't use because the Runtime.exec() method works fine.
					jvm = MRJ_2_1;
				} else if (version == 3.0) {
					jvm = MRJ_3_0;
				} else if (version >= 3.1 && version < 4.0) {
					// Assume that all 3.1 and later versions of MRJ work the same.
					jvm = MRJ_3_1;
				} else if (version >= 69.) {// crazy but true
					jvm = MRJ_4_1;
				} else if (version >= 4.0 && version < 5.0) {
					// some releases here don't work
					// have to be conservative and 
					// use IE for all of them
					jvm = MRJ_BAD;
				// Uncomment the following and use MRJ_BAD 
				// for jvms for which you want to try 
				// applescript magic to launch IE.
				/*
				} else if (version == something bad ) {
					jvm = MRJ_BAD;
				*/
				} else {
					loadedWithoutErrors = false;
					errorMessage = "Unsupported MRJ version: " + version;
				}
			} catch (NumberFormatException nfe) {
				loadedWithoutErrors = false;
				errorMessage = "Invalid MRJ version: " + mrjVersion;
			}
		} else if (osName.startsWith("Windows")) {
			if (osName.indexOf("9") != -1 || osName.indexOf("Me") != -1) {
				jvm = WINDOWS_9x;
			} else {
				jvm = WINDOWS_NT;
			}
		} else {
			jvm = OTHER;
		}
		
		if (loadedWithoutErrors) {	// if we haven't hit any errors yet
			loadedWithoutErrors = loadClasses();
		}
		System.out.println("BrowserLauncher jvm = " + jvm);
	}

	/**
	 * This class should be never be instantiated; this just ensures so.
	 */
	private BrowserLauncher() { }
	
	/**
	 * Called by a static initializer to load any classes, fields, and methods required at runtime
	 * to locate the user's web browser.
	 * @return <code>true</code> if all intialization succeeded
	 *		   <code>false</code> if any portion of the initialization failed
	 */
	private static boolean loadClasses() {
		switch (jvm) {
			case MRJ_2_0:
				return loadMRJ20Classes();
			case MRJ_2_1:
				try {
					mrjFileUtilsClass = Class.forName("com.apple.mrj.MRJFileUtils");
					mrjOSTypeClass = Class.forName("com.apple.mrj.MRJOSType");
					Field systemFolderField = mrjFileUtilsClass.getDeclaredField("kSystemFolderType");
					kSystemFolderType = systemFolderField.get(null);
					findFolder = mrjFileUtilsClass.getDeclaredMethod("findFolder", new Class[] { mrjOSTypeClass });
					getFileCreator = mrjFileUtilsClass.getDeclaredMethod("getFileCreator", new Class[] { File.class });
					getFileType = mrjFileUtilsClass.getDeclaredMethod("getFileType", new Class[] { File.class });
				} catch (ClassNotFoundException cnfe) {
					errorMessage = cnfe.getMessage();
					return false;
				} catch (NoSuchFieldException nsfe) {
					errorMessage = nsfe.getMessage();
					return false;
				} catch (NoSuchMethodException nsme) {
					errorMessage = nsme.getMessage();
					return false;
				} catch (SecurityException se) {
					errorMessage = se.getMessage();
					return false;
				} catch (IllegalAccessException iae) {
					errorMessage = iae.getMessage();
					return false;
				}
				break;
			case MRJ_3_0:
			    try {
					Class linker = Class.forName("com.apple.mrj.jdirect.Linker");
					Constructor constructor = linker.getConstructor(new Class[]{ Class.class });
					linkage = constructor.newInstance(new Object[] { BrowserLauncher.class });
				} catch (ClassNotFoundException cnfe) {
					errorMessage = cnfe.getMessage();
					return false;
				} catch (NoSuchMethodException nsme) {
					errorMessage = nsme.getMessage();
					return false;
				} catch (InvocationTargetException ite) {
					errorMessage = ite.getMessage();
					return false;
				} catch (InstantiationException ie) {
					errorMessage = ie.getMessage();
					return false;
				} catch (IllegalAccessException iae) {
					errorMessage = iae.getMessage();
					return false;
				}
				break;
			case MRJ_3_1:
				try {
					mrjFileUtilsClass = Class.forName("com.apple.mrj.MRJFileUtils");
					openURL = mrjFileUtilsClass.getDeclaredMethod("openURL", new Class[] { String.class });
				} catch (ClassNotFoundException cnfe) {
					errorMessage = cnfe.getMessage();
					return false;
				} catch (NoSuchMethodException nsme) {
					errorMessage = nsme.getMessage();
					return false;
				}
				break;
			case MRJ_4_1: // useless until apple fixes bug in FileManager.openURL
				try {
					mrjFileUtilsClass = Class.forName("com.apple.eio.FileManager");
					openURL = mrjFileUtilsClass.getDeclaredMethod("openURL", new Class[] { String.class });
				} catch (ClassNotFoundException cnfe) {
					errorMessage = cnfe.getMessage();
					return false;
				} catch (NoSuchMethodException nsme) {
					errorMessage = nsme.getMessage();
					return false;
				}
				break;
			default:
			    break;
		}
		return true;
	}

	/**
	 * Loads the classes, fields, and methods needed when running under MRJ 2.0. Sets
	 * <code>errorMessage</code> if it fails.
	 * @return <code>true</code> if all operations succeeded; <code>false</code> otherwise
	 */
	private static boolean loadMRJ20Classes() {
		try {
			Class aeTargetClass = Class.forName("com.apple.MacOS.AETarget");
			Class osUtilsClass = Class.forName("com.apple.MacOS.OSUtils");
			Class appleEventClass = Class.forName("com.apple.MacOS.AppleEvent");
			Class aeClass = Class.forName("com.apple.MacOS.ae");
			aeDescClass = Class.forName("com.apple.MacOS.AEDesc");

			aeTargetConstructor = aeTargetClass.getDeclaredConstructor(new Class [] { int.class });
			appleEventConstructor = appleEventClass.getDeclaredConstructor(new Class[] { int.class, int.class, aeTargetClass, int.class, int.class });
			aeDescConstructor = aeDescClass.getDeclaredConstructor(new Class[] { String.class });

			makeOSType = osUtilsClass.getDeclaredMethod("makeOSType", new Class [] { String.class });
			putParameter = appleEventClass.getDeclaredMethod("putParameter", new Class[] { int.class, aeDescClass });
			sendNoReply = appleEventClass.getDeclaredMethod("sendNoReply", new Class[] { });

			Field keyDirectObjectField = aeClass.getDeclaredField("keyDirectObject");
			keyDirectObject = (Integer) keyDirectObjectField.get(null);
			Field autoGenerateReturnIDField = appleEventClass.getDeclaredField("kAutoGenerateReturnID");
			kAutoGenerateReturnID = (Integer) autoGenerateReturnIDField.get(null);
			Field anyTransactionIDField = appleEventClass.getDeclaredField("kAnyTransactionID");
			kAnyTransactionID = (Integer) anyTransactionIDField.get(null);
		} catch (ClassNotFoundException cnfe) {
			errorMessage = cnfe.getMessage();
			return false;
		} catch (NoSuchMethodException nsme) {
			errorMessage = nsme.getMessage();
			return false;
		} catch (NoSuchFieldException nsfe) {
			errorMessage = nsfe.getMessage();
			return false;
		} catch (IllegalAccessException iae) {
			errorMessage = iae.getMessage();
			return false;
		}
		return true;
	}
	
	/**
	 * Attempts to locate the default web browser on the local system.  Caches the result so it
	 * only locates the browser once for each use of this class per JVM instance.
	 * @return The browser for the system.  Note that this may not be what you would consider
	 *			to be a standard web browser; instead, it's the application that gets called to
	 *			open the default web browser.  In some cases this will be a non-String object
	 *			that provides the means of calling the default browser.
	 */
	private static Object locateBrowser() {
		if (browser != null) {
			return browser;
		}
		switch (jvm) {
			case MRJ_2_0:
				try {
					Integer finderCreatorCode = (Integer) makeOSType.invoke(null, new Object[] { FINDER_CREATOR });
					Object aeTarget = aeTargetConstructor.newInstance(new Object[] { finderCreatorCode });
					Integer gurlType = (Integer) makeOSType.invoke(null, new Object[] { GURL_EVENT });
					Object appleEvent = appleEventConstructor.newInstance(new Object[] { gurlType, gurlType, aeTarget, kAutoGenerateReturnID, kAnyTransactionID });
					// Don't set browser = appleEvent because then the next time we call
					// locateBrowser(), we'll get the same AppleEvent, to which we'll already have
					// added the relevant parameter. Instead, regenerate the AppleEvent every time.
					// There's probably a way to do this better; if any has any ideas, please let
					// me know.
					return appleEvent;
				} catch (IllegalAccessException iae) {
					browser = null;
					errorMessage = iae.getMessage();
					return browser;
				} catch (InstantiationException ie) {
					browser = null;
					errorMessage = ie.getMessage();
					return browser;
				} catch (InvocationTargetException ite) {
					browser = null;
					errorMessage = ite.getMessage();
					return browser;
				}
			case MRJ_2_1:
				File systemFolder;
				try {
					systemFolder = (File) findFolder.invoke(null, new Object[] { kSystemFolderType });
				} catch (IllegalArgumentException iare) {
					browser = null;
					errorMessage = iare.getMessage();
					return browser;
				} catch (IllegalAccessException iae) {
					browser = null;
					errorMessage = iae.getMessage();
					return browser;
				} catch (InvocationTargetException ite) {
					browser = null;
					errorMessage = ite.getTargetException().getClass() + ": " + ite.getTargetException().getMessage();
					return browser;
				}
				String[] systemFolderFiles = systemFolder.list();
				// Avoid a FilenameFilter because that can't be stopped mid-list
				for(int i = 0; i < systemFolderFiles.length; i++) {
					try {
						File file = new File(systemFolder, systemFolderFiles[i]);
						if (!file.isFile()) {
							continue;
						}
						// We're looking for a file with a creator code of 'MACS' and
						// a type of 'FNDR'.  Only requiring the type results in non-Finder
						// applications being picked up on certain Mac OS 9 systems,
						// especially German ones, and sending a GURL event to those
						// applications results in a logout under Multiple Users.
						Object fileType = getFileType.invoke(null, new Object[] { file });
						if (FINDER_TYPE.equals(fileType.toString())) {
							Object fileCreator = getFileCreator.invoke(null, new Object[] { file });
							if (FINDER_CREATOR.equals(fileCreator.toString())) {
								browser = file.toString();	// Actually the Finder, but that's OK
								return browser;
							}
						}
					} catch (IllegalArgumentException iare) {
						//browser = browser;
						errorMessage = iare.getMessage();
						return null;
					} catch (IllegalAccessException iae) {
						browser = null;
						errorMessage = iae.getMessage();
						return browser;
					} catch (InvocationTargetException ite) {
						browser = null;
						errorMessage = ite.getTargetException().getClass() + ": " + ite.getTargetException().getMessage();
						return browser;
					}
				}
				browser = null;
				break;
			case MRJ_3_0:
			case MRJ_3_1:
			case MRJ_4_1:		// does not work
				browser = "";	// Return something non-null
				break;
				// Kludge for bad jvms where the best strategy
				// is to launch IE using some applescript magic.
			case MRJ_BAD:
				browser = getEnvironmentBrowser();
				if (browser == null 
					|| ((String)browser).length() == 0) {
					browser = "Internet Explorer";
				}
				break;
			case WINDOWS_NT:
				browser = "cmd.exe";
				break;
			case WINDOWS_9x:
				browser = "command.com";
				break;
			case OTHER:
			default:
				// On systems other than Windows and the Mac, we try via a rather Unix-
				// specific hack to read the BROWSER environment variable
				// <http://tuxedo.org/~esr/BROWSER/>. If we can't read that variable or
				// it isn't set, we use Netscape.
				// Note: This is commented out for now. It'll work soon.
				browser = getEnvironmentBrowser();
				if (browser == null 
					|| ((String)browser).length() == 0) {
					browser = locateUnixBrowser();
				}
				break;
		}
		System.out.println("locateBrowser returning " + browser);
		return browser;
	}

	/**
	  * Open a file as a URL. Space in the name are escaped with 
	  * %20 (Needed at least on Mac OS X 10.2 and 10.3).
	  * @since J-Bird 0.5.1
	  *
	*/

	public static void openFile(String filename) throws IOException {
		open(new File(filename));
	}

	/**
	  * Open a file as a URL. Space in the name are escaped with 
	  * %20 (neeeded on at least Mac OS X 10.2 and 10.3).
	  * @since J-Bird 0.5.1
	  *
	*/

	public static void open(File file) throws IOException {
		String url = file.toURL().toString();
		if (url.indexOf(' ') > -1) {
			url = url.replaceAll(" ", "%20");
		}
		openURL(url);
	}

	/**
	 * Attempts to open the default web browser to the given URL.
	 * @param url The URL to open
	 * @throws IOException If the web browser could not be located or does not run
	 */
	public static void openURL(String url) throws IOException {
		if (!loadedWithoutErrors) {
			throw new IOException("Exception in finding browser: " + errorMessage);
		}
		Object browser = locateBrowser();
		if (browser == null) {
			throw new IOException("Unable to locate browser: " + errorMessage);
		}
		
		switch (jvm) {
			case MRJ_2_0:
				Object aeDesc = null;
				try {
					aeDesc = aeDescConstructor.newInstance(new Object[] { url });
					putParameter.invoke(browser, new Object[] { keyDirectObject, aeDesc });
					sendNoReply.invoke(browser, new Object[] { });
				} catch (InvocationTargetException ite) {
					throw new IOException("InvocationTargetException while creating AEDesc: " + ite.getMessage());
				} catch (IllegalAccessException iae) {
					throw new IOException("IllegalAccessException while building AppleEvent: " + iae.getMessage());
				} catch (InstantiationException ie) {
					throw new IOException("InstantiationException while creating AEDesc: " + ie.getMessage());
				} finally {
					aeDesc = null;	// Encourage it to get disposed if it was created
					browser = null;	// Ditto
				}
				break;
			case MRJ_2_1:
				Runtime.getRuntime().exec(new String[] { (String) browser, url } );
				break;
			case MRJ_3_0:
				int[] instance = new int[1];
				int result = ICStart(instance, 0);
				if (result == 0) {
					int[] selectionStart = new int[] { 0 };
					byte[] urlBytes = url.getBytes();
					int[] selectionEnd = new int[] { urlBytes.length };
					result = ICLaunchURL(instance[0], new byte[] { 0 }, urlBytes,
											urlBytes.length, selectionStart,
											selectionEnd);
					if (result == 0) {
						// Ignore the return value; the URL was launched successfully
						// regardless of what happens here.
						ICStop(instance);
					} else {
						throw new IOException("Unable to launch URL: " + result);
					}
				} else {
					throw new IOException("Unable to create an Internet Config instance: " + result);
				}
				break;
			case MRJ_3_1:
			case MRJ_4_1:
				launchMRJ31Plus(url);

				break;
			case MRJ_BAD:	// for problem mrjs for which want 
					// to try applescript magic
				launchMRJBad(browser, url);
				break;
		    case WINDOWS_NT:
		    case WINDOWS_9x:
		    	// Add quotes around the URL to allow ampersands and other special
		    	// characters to work.
		    	String[] arguments;
		    	if (jvm == WINDOWS_9x) {
		    		arguments = new String[] { (String) browser,
		    							  FIRST_WINDOWS_PARAMETER,
		    							  SECOND_WINDOWS_PARAMETER,
		    							  null };
		    	} else {
		    		arguments = new String[] { (String) browser,
		    							  FIRST_WINDOWS_PARAMETER,
		    							  SECOND_WINDOWS_PARAMETER,
		    							  THIRD_WINDOWS_PARAMETER,
		    							  null };
		    	}
		    	arguments[arguments.length - 1] = '"' + url + '"';
				Process process = Runtime.getRuntime().exec(arguments);
				// This avoids a memory leak on some versions of Java on Windows.
				// That's hinted at in <http://developer.java.sun.com/developer/qow/archive/68/>.
				try {
					process.waitFor();
					process.exitValue();
				} catch (InterruptedException ie) {
					throw new IOException("InterruptedException while launching browser: " + ie.getMessage());
				}
				break;
			case OTHER:
				launchUnixBrowser(browser, url);
				break;
			default:
				// This should never occur, but if it does, we'll try the simplest thing possible
				launchUnixVanillaBrowser((String) browser, url);
				break;
		}
	}

	/**
	 * Tries to read the BROWSER environment variable, which should be set to the absolute
	 * path of the user's preferred web browser as proposed at <http://tuxedo.org/~esr/BROWSER/>.
	 * @return The value of the BROWSER environment variable, or null if the variable does
	 *		   not exist or can't be read.
	 */
	private static String getEnvironmentBrowser() {
		return getEnvUnix("BROWSER");
	}

		/**
		  * @return the value of a Unix environment variable.
		  * The value will be an empty string if the environment
		  * variable is undefined.
		  * @since J-Bird 0.4.3 (pulled out of getEnvironmentBrowser)
		  *
		*/
	private static String getEnvUnix(String envvar) {
		String value = null;
		try {
			String[] echoParams = { "/bin/sh", 
						"-c", 
						"echo $" + envvar };
			Process echoProcess = Runtime.getRuntime().exec(echoParams);
			InputStream echoStream = echoProcess.getInputStream();
			BufferedReader echoReader = new BufferedReader(
					new InputStreamReader(echoStream));
			value = echoReader.readLine();
			echoReader.close();
		} catch (Throwable t) {
			// If anything goes wrong, we'll return null.
		}
		return value;
	}

		/**
		  * @return environment variable PATH or default UNIX_PATHS
		  * if there is no path or if an error occurs.
		  * @since J-Bird 0.4.3
		  *
		*/
	public static String[] getUnixPath() {
		String path = getEnvUnix("PATH");
		return (path == null || path.length() == 0) 
			? UNIX_PATHS
			: path.split(System.getProperty("path.separator"));
	}

	/**
	 * Ensure that a file url has three slashes in it.  This is necessary on Mac OS X
	 * to get a browser to open instead of a file browser.
	 * @return file url with 3 slashes in it.
	*/

	private static final String ensureThreeSlashes(String url) {
			// file: urls need 3 leading slashes
		if (url.startsWith("file:") || url.startsWith("FILE:")) {
		   String slashes = url.substring(5, 8);
		   if (! slashes.startsWith("///")) {		// less than 3 slashes
			int offset;
			if (slashes.startsWith("//")) {		// two starting slashes
				offset = 7;
			} else if (slashes.startsWith("/")) {	// one starting slash
				offset = 6;
			} else {				// no starting slash (bad url)
				offset = 5;
			}
			url = "file:///" + url.substring(offset);
		   }
		}
		return url;
 	}
	
	/**
	  * Launch browser on Mac OS X MRJ 1_3_1 or greater. Doesn't 
	  * work for MRJ_1_4_1 because of bug. See kludge.
	  *
	*/

	private static final void launchMRJ31Plus(String url) 
			throws IOException {
		url = ensureThreeSlashes(url);
		try {
			System.out.println("openURL - invoking MAC openURL");
			openURL.invoke(null, new Object[] { url });
			System.out.println("openURL - MAC openURL returned!");
		} catch (InvocationTargetException ite) {
			System.out.println("openURL exception "
				+ite.getMessage());
			throw new IOException(
			   "InvocationTargetException while calling openURL: "
				+ ite.getMessage());
		} catch (IllegalAccessException iae) {
			System.out.println("openURL exception "
				+iae.getMessage());
			throw new IOException(
			   "IllegalAccessException while calling openURL: "
				 + iae.getMessage());
		}
	}

	/**
	  * The method com.apple.eio.FileManager.openURL is broken in build 1.4.1_01-39.
	  * This method fills the gap with some AppleScript magic.
	  *
	*/
	private static final void launchMRJBad(Object browser, String url) 
							throws IOException {
	   url = ensureThreeSlashes(url);
		System.out.println("launchMRJ41Bad " + browser + "\t" + url);
	   Runtime.getRuntime().exec(new String[] { "osascript", 
				"-l", "AppleScript",
				"-e", "Tell application \"" + (String)browser 
					+ "\" to getURL \"" + url + "\"" });
	}

		/**
		  * Path for common browsers
		  * Returns null if nothing on the list is found.
		  * @since J-Bird 0.4.3
		  *
		*/
	private static final String locateUnixBrowser() {
		String pathsep = System.getProperty("file.separator");
		String[] pathdirs = getUnixPath();
		for (int pathidx = 0; pathidx < pathdirs.length; pathidx++) {
			String apath = pathdirs[pathidx];
			if (! apath.endsWith(pathsep)) {
				apath = apath + pathsep;
			}
			for (int bridx = 0; bridx < UNIX_BROWSERS.length; 
							bridx++) { 
				String browser = apath + UNIX_BROWSERS[bridx];
				File bfile = new File(browser);
				if (bfile.exists()) {
					return browser; //pathdirs[bridx];
				}
			}
		}
		return null;
	}

		/**
		  * Launch a browser under Unix.
		  * Separeation of netscape from other browsers is new.
		  * @since J-Bird 0.4.3
		  *
		*/

	private static final void launchUnixBrowser(Object browser, 
						String url) 
					throws IOException {
		String sbrowser = (String) browser;
		if (sbrowser.endsWith("netscape")) {
			launchUnixNetscape(sbrowser, url);
		} else if (sbrowser.indexOf("kfmclient") > -1) {
			launchKfmclient(url);
		} else {
			launchUnixVanillaBrowser(sbrowser, url);
		}
	}

		/**
		  * Launch netscape under Unix.
		  * @since J-Bird 0.4.3 (taken form method openURL
		  *
		*/
	private static final void launchUnixNetscape(String browser, 
							String url) 
						throws IOException {
		// Assume that we're on Unix and that Netscape is installed
		// Attempt to open the URL in a currently running session of Netscape
		Process process = Runtime.getRuntime().exec(new String[] { 
			browser,
			NETSCAPE_REMOTE_PARAMETER,
			NETSCAPE_OPEN_PARAMETER_START +
			url +
			NETSCAPE_OPEN_PARAMETER_END });
		try {
			int exitCode = process.waitFor();
			if (exitCode != 0) {	// if Netscape was not open
				launchUnixVanillaBrowser(browser, url);
			}
		} catch (InterruptedException ie) {
			throw new IOException(
				"InterruptedException while launching browser: "
					+ ie.getMessage());
		}
	}

		/**
		  * A hack for opening a web browser in KDE 2.0.
		  * It seems at least on Mandrake 10.0, KDE 2.0 is configured
		  * to set the BROWSER enivonment variable to 
		  * "kfmclient openProfile webbrowsing", which is not 
		  * the simple name of a binary. Syntax for kfmclient 
		  * from <a href="http://www.developer.kde.org/documentation/other/kfmclient.html">
		  * http://www.developer.kde.org/documentation/other/kfmclient.html</a> is "kfmclient openProfile webbrowsing &lt;URL&gt;.
		  * since J-Bird 0.4.6
		  *
		*/

	private static final void launchKfmclient(String url)
							throws IOException{
		Runtime.getRuntime().exec(new String[] { "kfmclient",
				"openProfile", "webbrowsing", url });
	}

		/**
		  * Launch using command name and url
		  * @since J-Bird 0.4.3 (taken from method openURL)
		  *
		*/
	private static final void launchUnixVanillaBrowser(String browser,
							String url) 
						throws IOException {
		Runtime.getRuntime().exec(new String[] { browser, url });
	}


	/**
	 * Methods required for Mac OS X 10.0.x. The presence of native methods does not cause
	 * any problems on other platforms.
	 */
	private native static int ICStart(int[] instance, int signature);
	private native static int ICStop(int[] instance);
	private native static int ICLaunchURL(int instance, byte[] hint, byte[] data, int len,
											int[] selectionStart, int[] selectionEnd);
}
