package nl.mtc.plugin;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.PendingIntent;
import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;

import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.ReaderCallback;
import android.nfc.tech.NfcF;
import android.nfc.Tag;


public class NFCSuppressor extends CordovaPlugin {

	// declare the context objects
	private			Activity activity;
	private			Context context;
	
    // declare the nfc adapter device
    private 		NfcAdapter 		nfc;

    // declare an intent and intentfilters to be used (solution 2)
    private 		Intent 			my_intent;
    private 		PendingIntent 	pendingIntent;

    private final 	IntentFilter 	ndef;
    private final 	IntentFilter[] 	intentFiltersArray;
    private final 	String[][] 		techListsArray;

	{
		// initialize intent filters to handle only the intents created by NFC that we intercept
		ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
			try {
				ndef.addDataType("*/*");    /* Handles all MIME based dispatches.
											   You should specify only the ones that you need. */
			}
			catch (MalformedMimeTypeException e) {
				throw new RuntimeException("fail", e);
			}
		intentFiltersArray = new IntentFilter[] {ndef, };

		// initialize the tech lists
		techListsArray = new String[][] { new String[] { NfcF.class.getName() } };
		
	}

	@Override
	protected void pluginInitialize() {
		super.pluginInitialize();
		
		// get the currect activity and application context
		this.activity = this.cordova.getActivity();
		
		if (this.activity != null) {
			this.context = activity.getApplicationContext();
			
			// get the nfcAdapter
			this.nfc = NfcAdapter.getDefaultAdapter(this.context);
		}

        // enable reader mode with empty callback (solution 1)
		if (this.nfc != null) {
			this.nfc.enableReaderMode(this.activity, new NfcAdapter.ReaderCallback() {
				public void onTagDiscovered(Tag tag) { }
				}, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
				null
			);
		}

        // setup an intent that can be used by the foreground dispatch system (solution 2)
		this.my_intent      = new Intent(this.context, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        this.pendingIntent 	= PendingIntent.getActivity(this.context, 0, this.my_intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
	}

	@Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
		if (this.nfc != null) {
			this.nfc.disableForegroundDispatch(this.activity);
		}
    }

	@Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
		if (this.nfc != null) {
			this.nfc.enableForegroundDispatch(this.activity, this.pendingIntent, this.intentFiltersArray, this.techListsArray);
		}
    }

	@Override
    public void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            // drop NFC events
        } else {
            super.onNewIntent(intent);
        }
    }
	
	public String checkNFC() {
		return String.valueOf(this.nfc != null);
	}
	
    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

        if (action.equals("checknfc")) {

            String message = "NFC available: " + this.checkNFC();
            callbackContext.success(message);

            return true;

        } else {
            
            return false;

        }
    }
}
