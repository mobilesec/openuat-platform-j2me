/* Copyright Rene Mayrhofer and Iulia Ion
 * File created 2008-06-25
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */
package org.openuat.apps.j2me;

import java.io.IOException;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.midlet.MIDlet;

import net.sf.microlog.Level;
import net.sf.microlog.appender.FileAppender;
import net.sf.microlog.appender.FormAppender;
import net.sf.microlog.ui.LogForm;

import org.openbandy.service.LogService;
import org.openuat.authentication.AuthenticationProgressHandler;
import org.openuat.authentication.HostProtocolHandler;
import org.openuat.authentication.exceptions.InternalApplicationException;
import org.openuat.channel.main.RemoteConnection;
import org.openuat.channel.main.bluetooth.BluetoothSupport;
import org.openuat.channel.main.bluetooth.jsr82.BluetoothPeerManager;
import org.openuat.channel.main.bluetooth.jsr82.BluetoothRFCOMMChannel;
import org.openuat.channel.main.bluetooth.jsr82.BluetoothRFCOMMServer;
import org.openuat.log.Log;
import org.openuat.log.LogFactory;
import org.openuat.log.j2me.MicrologFactory;
import org.openuat.log.j2me.MicrologLogger;



/** This MIDlet demonstrates four out-of-band peer
 * verification: visual, audio, madlib, slowcodec.
 * 
 * @author Rene Mayrhofer
 * @author Iulia Ion
 * @version 1.0
 *
 */
public class OpenUATmidlet extends MIDlet implements CommandListener,
		BluetoothPeerManager.PeerEventsListener, AuthenticationProgressHandler{
	public final static UUID serviceUUID = new UUID("e8341b89f78d4a45a8c8313a7a8f3d9a", false);

	/** The key resulting from the message exchange during the authentication process */
	protected byte [] authKey;
	
	//preloaded images
	protected Image search = null;
	protected Image vCard = null;
	protected Image print = null;
	
	//authentication options
	protected Image visual = null;
	/**Hapadep */
	protected Image audio = null;
	/** melodic sound */
	protected Image slowcodec = null;
	/** Compare sentence */
	protected Image madlib = null;
	/**Hash comparison, manual*/
	protected Image manualcom = null;
	
	protected Image running = null;
	
	protected Image buttonok = null;
	protected Image buttoncancel = null;	
	protected Image replay = null;	
	protected Image secure = null;
	protected Image error = null;	
	
	/** Initial screen with options */
	protected List home_screen;

	/** screen that shows what Bluetooth devices have been found */
	protected List dev_list;

	/** The list of Bluetooth services offered by a device */
	protected List serv_list;

	/** displays the available verification methods: visual, audio, madlib, slowcodec */
	protected List verify_method;
	/** The result of the verification method */
	protected Form success_form;
	protected Form failure_form;

	protected Displayable logScreen;
	
	/** Chosen by the user to indicate the verification outcome in manual verification cases */
	protected Command back;
	protected Command exit;
	protected Command log;
	protected Command successCmd;
	protected Command failureCmd;
	protected Command increaseVolume;
	protected Command decreaseVolume;
	
	private Image warnIcon;
	
	protected Display display;

	protected BluetoothRFCOMMServer rfcommServer;
	protected BluetoothPeerManager peerManager;

	/** Discovered Bluetooth services */
	protected Vector services;
	
	/* The microlog LogForm */
	private LogForm logForm;
	
	/* Logger instance to log statistics data */
	private Log statisticsLogger;
	
	/** Play volume, how loud the phone should play the tunes */
	public int volume = 75;

	

	private Log logger;
	
	protected Gauge bluetoothProgressGauge;
	protected ProgressScreen progressScreen;

	/** Tells whether this device initiated the set-up or not. If it did, it will be the one coordinating the verification process.	 */
	protected boolean initiator = false;

	/** the screen to which to change to when te BAck method is invoked */
	private Displayable previousScreen;
	/** the screen that is current being showed */
	private Displayable currentScreen;
	
	/** Client connection to the bluetooth server */
	private BluetoothRFCOMMChannel c; 
	
	/** starting of verification time */
	public long startTime;
	

	public long startTimeMadLib;
	
	private void loadImages(){
		try {
			search = Image.createImage("/search_sm.png");
		} catch (IOException e) {
			LogService.warn(this, "could not load image search_sm.png");
		}
		try {
			vCard = Image.createImage("/vcard_sm.png");
		} catch (IOException e) {
			LogService.warn(this, "could not load image vCard_sm.png");
		}
		try {
			print = Image.createImage("/print_sm.png");

		} catch (IOException e) {
			LogService.warn(this, "could not load image print_sm.png");
		}
		try {
			visual = Image.createImage("/visual_sm.png");
		} catch (IOException e) {
			LogService.warn(this, "could not load image");
		}
		try {
			audio = Image.createImage("/audio_sm.png");
		} catch (IOException e) {
			LogService.warn(this, "could not load image");
		}
		try {
			slowcodec = Image.createImage("/slowcodec_sm.png");
		} catch (IOException e) {
			LogService.warn(this, "could not load image");
		}
		try {
			madlib = Image.createImage("/madlib_sm.png");

		} catch (IOException e) {
			LogService.warn(this, "could not load image /madlib_sm.png");
		}
		try {
			manualcom = Image.createImage("/madlib_sm.png");

		} catch (IOException e) {
			LogService.warn(this, "could not load image /madlib_sm.png");
		}
		try {
			running = Image.createImage("/running_sm.png");

		} catch (IOException e) {
			LogService.warn(this, "could not load image");
		}
		try {
			buttonok = Image.createImage("/button_ok_sm.png");
		} catch (IOException e) {
			LogService.warn(this, "could not load image");
		}
		try {
			buttoncancel = Image.createImage("/button_cancel_sm.png");
		} catch (IOException e) {
			LogService.warn(this, "could not load image button_cancel_sm.png");
		}
		try {
			replay = Image.createImage("/replay_sm.png");
		} catch (IOException e) {
			LogService.warn(this, "could not load image replay_sm.png");
		}
		try {
			secure = Image.createImage("/secure_sm.png");
		} catch (IOException e) {
			LogService.warn(this, "could not load image secure_sm.png");
		}
		try {
			error = Image.createImage("/error_sm.png");
		} catch (IOException e) {
			LogService.warn(this, "could not load image error_sm.png");
		}
	}

	
	public OpenUATmidlet() {
		initLogger();
		initGui();
		initBluetooth();
		
	}

	private void initLogger() {
		// Initialize the logger. Use a wrapper around the microlog framework.
		LogFactory.init(new MicrologFactory());
		logger = LogFactory.getLogger("org.openuat.apps.j2me.BedaMIDlet");
		statisticsLogger = LogFactory.getLogger("statistics");
		
		// TODO: should configure microlog in its properties file
		FileAppender fileAppender = new FileAppender();
		fileAppender.setDirectory("Memory card/Others/");
		logForm = new LogForm();
		logForm.setDisplay(display);
		logForm.setPreviousScreen(previousScreen);
		FormAppender formAppender = new FormAppender(logForm);
		net.sf.microlog.Logger nativeLogger = ((MicrologLogger)logger).getNativeLogger();
		nativeLogger.addAppender(fileAppender);
		nativeLogger.addAppender(formAppender);
		nativeLogger = ((MicrologLogger)statisticsLogger).getNativeLogger();
		nativeLogger.addAppender(fileAppender);
		nativeLogger.addAppender(formAppender);
		nativeLogger.setLogLevel(Level.TRACE);
	}
	private void initBluetooth() {
		if (! BluetoothSupport.init()) {
			do_alert("Could not initialize Bluetooth API", Alert.FOREVER);
			return;
		}

		try {

			rfcommServer = new BluetoothRFCOMMServer(null, serviceUUID, "OpenUAT- Exchange vCard", 
					-1, true, false);
			rfcommServer.addAuthenticationProgressHandler(this);
			rfcommServer.start();

//			LogService.debug(this, "Finished starting SDP service at " + rfcommServer.getRegisteredServiceURL());
		} catch (IOException e) {
			LogService.error(this, "Error initializing BlutoothRFCOMMServer: ", e);
		}

		try {
			peerManager = new BluetoothPeerManager();
			peerManager.addListener(this);
		} catch (IOException e) {
			LogService.error(this, "Error initializing BlutoothPeerManager",  e);
			
		}
	}

	protected void initGui() {
		loadImages();
		LocalDevice local_device;
		String deviceName= "";
		try {
			local_device = LocalDevice.getLocalDevice();
			deviceName = local_device.getFriendlyName();
			
		} catch (BluetoothStateException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		
		//initialize screens
		home_screen = new List("OpenUAT: "+deviceName, Choice.IMPLICIT); //the main menu
		dev_list = new List("Select Device", Choice.IMPLICIT); //the list of devices
		serv_list = new List("Available Services", Choice.IMPLICIT); //the list of services
		
		//initialize commands
		exit = new Command("Exit", Command.EXIT, 1);
		back = new Command("Back", Command.BACK, 1);
		log = new Command("Log", Command.ITEM, 2);

		successCmd = new Command("Success", Command.SCREEN, 1);
		failureCmd = new Command("Failure", Command.SCREEN, 1);
		
		increaseVolume = new Command("Increase volume", Command.SCREEN, 2);
		decreaseVolume = new Command("Decrease volume", Command.SCREEN, 2);
		
		home_screen.append("Find devices", search);
		//home_screen.append("Send vCard", vCard);
		home_screen.append("Print document", print);
		
		home_screen.addCommand(exit);
		home_screen.addCommand(log);
		home_screen.addCommand(increaseVolume);
		home_screen.addCommand(decreaseVolume);
		

		try {
			warnIcon = Image.createImage("/Button_Icon_Yellow_32.png");
		} catch (IOException ioe) {
			warnIcon = null;
			logger.warning("Could not create warn icon", ioe);
		}
		dev_list.addCommand(exit);
		dev_list.addCommand(log);
		dev_list.addCommand(back);

		serv_list.addCommand(exit);
		dev_list.addCommand(log);
		serv_list.addCommand(back);
		
		logScreen = LogService.getLog();
		logScreen.addCommand(back);
		
		//Image[]	images = new Image[]{visual, audio, slowcodec, madlib, manualcom};
		Image[]	images = new Image[]{visual, audio, slowcodec, madlib};
		
		//verify_method = new List("Method", Choice.IMPLICIT, new String[]{"Visual channel","Audio channel","Compare tunes","Compare text", "Compare numbers"}, images);
		
		verify_method = new List("Method", Choice.IMPLICIT, new String[]{"Visual channel","Audio channel","Compare tunes","Compare text"}, images);
		verify_method.addCommand(back);
		verify_method.addCommand(log);
		verify_method.addCommand(exit);
		verify_method.addCommand(increaseVolume);
		verify_method.addCommand(decreaseVolume);
		
		success_form =  new Form ("OpenUAT");
		failure_form =  new Form ("OpenUAT");
		
		success_form.append(buttonok);
		success_form.append("Congratulations! Authentication successful!\n");
		success_form.append("Your connection is now secure!\n");
		success_form.append(secure);
		
		failure_form.append("Error. Authentication failed.\n");
//		failure_form.append("Retry?");
//		failure_form.append(error);
//		failure_form.append("Please try again.\n");

		success_form.addCommand(exit);
		success_form.addCommand(log);
		success_form.addCommand(back);

		failure_form.addCommand(exit);
		failure_form.addCommand(log);
		failure_form.addCommand(back);
		failure_form.addCommand(increaseVolume);
		failure_form.addCommand(decreaseVolume);
		
		dev_list.setCommandListener(this);
		home_screen.setCommandListener(this);
		serv_list.setCommandListener(this);
		success_form.setCommandListener(this);
		failure_form.setCommandListener(this);
		logScreen.setCommandListener(this);
		
		display = Display.getDisplay(this);
		//what to do about the other options? recall the thing
	}

	public void startApp() {
		display.setCurrent(home_screen);
		currentScreen = home_screen;
		
	}

	public void commandAction(Command com, Displayable dis) {
		if (com == exit) { //exit triggered from the main form
			if (rfcommServer != null)
				try {
					rfcommServer.stop();
				} catch (InternalApplicationException e) {
					do_alert("Could not de-register SDP service: " + e, Alert.FOREVER);
				}
				destroyApp(false);
				notifyDestroyed();

		}
		else if (com == List.SELECT_COMMAND) {
			if (dis == getHomeScreen()) { //select triggered from the main from
				if (home_screen.getSelectedIndex() == 0) { //find devices
					//show the cached devices and search again option
					if(dev_list!=null && dev_list.size()>0){
						display.setCurrent(dev_list);
						previousScreen = currentScreen;
						currentScreen = dev_list;
						return;
					} 

					if (!peerManager.startInquiry(false)) {
						this.do_alert("Error in initiating search", 4000);
					}
					alertWait("Searching for authenticaton service...", false);
					
//				}if (getMain_list().getSelectedIndex() == 1) { //demo - N95
//					String btAdd = "001C9AF755EB";
//					connectTo(btAdd, 5);
//				}else if (getHomeScreen().getSelectedIndex() == 1) { //demo N82
//					String btAdd = "001DFD71C3C3";
//					connectTo(btAdd, 5);
				}else if(getHomeScreen().getSelectedIndex() == 1) { //demo computer
					//print document 
					//String btAdd = "001F5B7B16F7";
					String btAdd = "001EC28E3024"; //iulias laptop
					
					connectTo(btAdd, 1);
					

				}
			}
			if (dis == dev_list) { //select triggered from the device list
				if (dev_list.getSelectedIndex() >= 0) { //find services
					RemoteDevice[] devices = peerManager.getPeers();
					
					serv_list.deleteAll(); //empty the list of services in case user has pressed back
					if (!peerManager.startServiceSearch(devices[dev_list.getSelectedIndex()], serviceUUID)) {
						this.do_alert("Error in initiating search", 4000);
					}
					
//					progressScreen = new ProgressScreen("/bluetooth_icon_sm.png", 15);
//					progressScreen.showActionAtStartupGauge("Inquiring device for services...");
//					display.setCurrent(progressScreen);
//					currentScreen = progressScreen;
//					previousScreen = dev_list;
				}
			}
			if (dis == serv_list){
				if (serv_list.getSelectedIndex() >= 0) { //find services
					ServiceRecord service = (ServiceRecord) services.elementAt(serv_list.getSelectedIndex());
					DataElement ser_de = service.getAttributeValue(0x100);
					String service_name = (String) ser_de.getValue();
					String connectionURL = service.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
					
					do_alert(service_name+":"+connectionURL, 5000);
					connectTo(connectionURL);
				}
			}
		}
		else if (com == back) {
			
			if(currentScreen.equals(success_form))
				previousScreen = home_screen;

			display.setCurrent(previousScreen);
			previousScreen = home_screen;
			//at most two steps for back
//			currentScreen = previousScreen;
//			previousScreen = currentScreen;
		}
		else if (com == log) {
			display.setCurrent(logScreen);
			previousScreen = currentScreen;
			currentScreen = logScreen;
		} else if (com == increaseVolume){
			if (volume <= 90)
				volume += 10;
			do_alert("Volume increased to " + volume, 1000);
		} else if (com == decreaseVolume){
			if (volume >= 10)
				volume -= 10;
			do_alert("Volume decreased to " + volume, 1000);
		}
	}

	/**
	 * Starts the connection and authentication process to the given Bluetooth address.
	 * @param btAdd The MAC address to which to connect via Bluetooth
	 * @param channel The channel number to which to connect to (and on which the OpenUAT service is running on the remote device)
	 */
	protected void connectTo(String btAdd, int channel) {
	
		boolean keepConnected = true;
		String optionalParam = null; 
		
//		LogService.debug(this, "starting authentication ");
		try {
			LogService.info(this, "trying to reconnect");
			LogService.info(this, "c: "+c);

			if (c!=null) LogService.info(this, "c.isopen: "+c.isOpen());
			initiator = true;
			c = new BluetoothRFCOMMChannel(btAdd, channel);
			c.open();
			HostProtocolHandler.startAuthenticationWith(c, this, -1, keepConnected, optionalParam, true);

		} catch (IOException e) {
			LogService.error(this, "Could not conenct to "+btAdd, e);
			LogService.info(this, "msg:"+e.getMessage());
			do_alert("Try again", Alert.FOREVER);
		}
	}


	/**
	 * Starts the connection and authentication process to the given Bluetooth address.
	 * @param btAdd
	 */
	protected void connectTo(String connectionURL) {
		boolean keepConnected = true;
		String optionalParam = null;  
//		LogService.debug(this, "starting authentication ");
		try {
			initiator = true;
			BluetoothRFCOMMChannel chanel = new BluetoothRFCOMMChannel(connectionURL);
			chanel.open();
			HostProtocolHandler.startAuthenticationWith(chanel, this, -1, keepConnected, optionalParam, true);

		} catch (IOException e) {
			LogService.error(this, "could not connect to "+connectionURL, e);
			do_alert("Try again", Alert.FOREVER);
		}
	}

	/** 
	 * Display an alert to inform the user.
	 * @param msg
	 * @param time_out
	 */
	public void do_alert(String msg, int time_out) {
		if (display.getCurrent() instanceof Alert) {
			((Alert) display.getCurrent()).setString(msg);
			((Alert) display.getCurrent()).setTimeout(time_out);
		} else {
			Alert alert = new Alert("Bluetooth");
			alert.setString(msg);
			alert.setTimeout(time_out);
			display.setCurrent(alert);
		}
	}
	/* Places a "Please wait..." message on screen.
	 * Launch it while some background processing is done. */
	private void alertWait(String msg, boolean returnToHome) {
		Alert a = new Alert("Please wait...", msg, warnIcon, AlertType.INFO);
		a.setTimeout(Alert.FOREVER);
		Gauge gauge = new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING);
		a.setIndicator(gauge);
		if (returnToHome) {
			display.setCurrent(a, currentScreen);
		}
		else {
			display.setCurrent(a);
		}
	}

	/**
	 * Display an alert which uses a progress bar.
	 * @param title The title of the alert.
	 * @param msg The message to be displayed
	 * @param img Image to be shown on the gauge
	 * @param max_time The maximum steps time on the gauge.
	 */
	public void do_alert_gauge(String title, String msg, String img, int max_time) {
		Image icon = null;
		try {
			icon = Image.createImage(img);
		}
		catch (IOException ioe) {
			LogService.error(this, "could not load image bluetooth_icon.png",ioe);
		}
		Gauge gauge = new Gauge("Progress", false, max_time, 0);
		Alert alert = new Alert(title);
		alert.setString(msg);
		alert.setImage(icon);
		alert.setTimeout(Alert.FOREVER);
		alert.setIndicator(gauge);

		display.setCurrent(alert);
	}

	// TODO: activate me again when J2ME polish can deal with Java5 sources!
	//@Override
	public void pauseApp() {
		// nothing to do when the app is paused, leave the background actions running
	}

	// TODO: activate me again when J2ME polish can deal with Java5 sources!
	//@Override
	public void destroyApp(boolean unconditional) {
		// just try to close all channels to shutdown quickly, all other resources should be freed automatically
		BluetoothRFCOMMChannel.shutdownAllChannels();
	}

	/**
	 * Called when the Bluetooth device inquire has finished. 
	 * It displays the list of devices so the user can select which to connect to.
	 */
	public void inquiryCompleted(Vector newDevices) {
		for (int i=0; i<newDevices.size(); i++) {
			String device_name = BluetoothPeerManager.resolveName((RemoteDevice) newDevices.elementAt(i));
			this.dev_list.append(device_name, null);
//			logForm.setPreviousScreen(dev_list);
			display.setCurrent(dev_list);
			currentScreen = dev_list;
			previousScreen = home_screen;
		}
	}


	public void AuthenticationFailure(Object sender, Object remote, Exception e, String msg) {
		// just ignore for this demo application 
	}

	public void AuthenticationProgress(Object sender, Object remote, int cur, int max, String msg) {
		// just ignore for this demo application 
	}
	
	public boolean AuthenticationStarted(Object sender, Object remote) {
		Alert alert = new Alert("Incoming connection", "Pairing with" + remote.toString(), null, AlertType.CONFIRMATION);
		alert.setTimeout(Alert.FOREVER);
		display.setCurrent(home_screen);
		display.setCurrent(alert);
		return true;
	}

	
	public void AuthenticationSuccess(Object sender, Object remote, Object result) {
//		LogService.debug(this, "Successful authentication");
		Object[] res = (Object[]) result;
		// remember the secret key shared with the other device
		//byte[] sharedKey = (byte[]) res[0];
		// and extract the shared authentication key for phase 2
		authKey = (byte[]) res[1];
		// then extract the optional parameter
		String param = (String) res[2];
//		LogService.debug(this, "Extracted session key of length " + sharedKey.length +
//				", authentication key of length " + authKey.length + 
//				" and optional parameter '" + param + "'");
		
		RemoteConnection connectionToRemote = (RemoteConnection) res[3];
		LogService.info(this, "Start con with: "+connectionToRemote.getRemoteName());
		if(initiator){
			verify_method.setCommandListener(new KeyVerifier(authKey, connectionToRemote, this));
			previousScreen = currentScreen;
			currentScreen = verify_method;
			display.setCurrent(verify_method);
			//reset the initial value
			initiator = false;
		}else{
			boolean success = new KeyVerifier(authKey, connectionToRemote, this).verify();
			informSuccess(success);

		}

	}

	/**
	 * Called when the Bluetooth service search for the selected device has completed. The user can then select the OpenUAT service and start the authentication process.
	 */
	public void serviceSearchCompleted(RemoteDevice remoteDevice, Vector serv, int errorReason) {
		this.services = serv;
		do_alert("Service search on " + remoteDevice + " finished with " + services.size() + " entries", 3000);
		if (errorReason == BluetoothPeerManager.PeerEventsListener.SEARCH_COMPLETE) {
			if (services.size() > 0) {
				for (int x = 0; x < services.size(); x++) {
					try {
						ServiceRecord rec = (ServiceRecord) services.elementAt(x);
						
						DataElement ser_de = rec.getAttributeValue(0x100);
						String service_name = (String) ser_de.getValue();
						if(service_name==null) service_name="[empty]";
						serv_list.append(service_name, null);
						 
					} catch (Exception e) {
						do_alert("Error in adding services ", 1000);
						serv_list.append("--", null);
					}
				}
				display.setCurrent(serv_list);
				currentScreen = serv_list;
				previousScreen = dev_list;
			}
			else
				do_alert("No OpenUAT service found on this device", 5000);
		}
		else {
			String errorMsg = "unknown error code!";
			switch (errorReason) {
				case BluetoothPeerManager.PeerEventsListener.DEVICE_NOT_REACHABLE:
					errorMsg = "Device " + remoteDevice + " not reachable";
					break;
				case BluetoothPeerManager.PeerEventsListener.SEARCH_FAILED:
					errorMsg = "Service search on device " + remoteDevice + " failed";
					break;
				case BluetoothPeerManager.PeerEventsListener.SEARCH_ABORTED:
					errorMsg = "Service search on device " + remoteDevice + " was aborted";
					break;
			}
			do_alert(errorMsg, Alert.FOREVER);
		}
	}

	/**
	 * Displays a screen to the user telling whether the exchanged key was successfully verified.
	 * @param success
	 */
	public void informSuccess(boolean success) {
		previousScreen = currentScreen;
		long endTime = System.currentTimeMillis();
		LogService.info(this, "completion time: "+(endTime-startTime));
		if(success){
			display.setCurrent(success_form);
			currentScreen = success_form;
			LogService.info(this, "SUCCESS");
			
		}
		else{
			display.setCurrent(failure_form);
			currentScreen = failure_form;
			LogService.info(this, "FAILURE");
		}
		try {
			c.close();
		} catch (Exception e) {
			LogService.error(this,"could not close con", e);
		}
		
	}

	public List getHomeScreen() {
		return home_screen;
	}

	public Command getBack() {
		return back;
	}

	public Command getFailure() {
		return failureCmd;
	}


	public Command getSuccessCmd() {
		return successCmd;
	}
}

