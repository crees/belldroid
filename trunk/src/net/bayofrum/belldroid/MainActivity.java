package net.bayofrum.belldroid;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private TextView debug;

	/* If increased, please check that there is a Layout for the numbers! 
	 * Also necessary will be modification of the regex (BOTH!) in
	 * moveBellsAround and adding cases for the letters.
	 * As well, add a line to methods for each new bell, and in Preferences
	 * array too.
	 */
	private static final int maxNumberOfBells = 12;
	private static final int delayBeforeStriking = 1000;
	private int place = 0, change = 0, blowTime, userBellPlace = 0;
	private int scoreTotal = 0, scoreOverRounds = 0;
	/**
	 * elapsedTime-- used with System.nanoTime() to 
	 * 				 measure time between manual blows
	 */
	private long elapsedTime;
	private boolean standing = false;
	
	private Bell[] bells = new Bell[maxNumberOfBells];
	
	private Handler ringHandler = new Handler();
	private Runnable ringHandlerRun = new Runnable() {
		
		@Override
		public void run() {
			ringOneBlow();
		}
	};
	private OnClickListener ringMe = new OnClickListener() {
		
		@Override
		public void onClick(View arg0) {
			Bell bell = (Bell) arg0;
			if (bell.getNumber() != getUserBell())
				return;
			int msec = (int)(System.nanoTime() - elapsedTime) / 1000000;
			bell.ring(ringHandler, sprefs.getBoolean("simulator", false) ?
					delayBeforeStriking : 0);
			/* Are we on time? Check first for ringing way out, before the
			 * previous blow or after the next blow */
			if (bell.getPlace() > place) {
				debug.setText("Way too early! %score: " +
						scoreTotal / ++scoreOverRounds);
				return;
			} else if (bell.getPlace() + 1 < place) {
				debug.setText("Way too wide! %score: " +
						scoreTotal / ++scoreOverRounds);
				return;
			}

			int blowTime = getBlowTime();
			int percentOff = ((msec - blowTime) * 100) / blowTime;
			scoreTotal += 100 - Math.abs(percentOff);
			if (Math.abs(percentOff) < 10)
				debug.setText("Very good! %score:" +
						scoreTotal / ++scoreOverRounds);
			else if (percentOff > 0)
				debug.setText("Try a little earlier, %score: " +
						scoreTotal / ++scoreOverRounds);
			else
				debug.setText("Try a little wider, %score: " +
						scoreTotal / ++scoreOverRounds);
		}
	};
	
	private SoundPool soundpool = null;
	
	public static final String[][][] methods =
		{
			{{}}, {{}}, {{}}, // 0-2 bells (!)
			{
				{"Plain Hunt Singles",
					"3.1.3.1.3.1"},
			}, // Singles
			{
				{"Plain Hunt Minimus",
					"x1x1x1x1"},
				{"Plain Hunt Singles",
					"3.1.3.1.3.1"},
				{"Short Cure for Melancholy",
					"3.13.1.13.3.13.1.13.3.13.1"}
			}, // Minimus
			{
				{"Plain Hunt Doubles",
					"5.1.5.1.5.1.5.1.5.1"},
			}, // Doubles
			{
				{"Plain Hunt Minor",
					"x1x1x1x1x1x1"},
				{"Plain Hunt Doubles",
					"5.1.5.1.5 le 1"},
				{"Grandsire Doubles",
					"3.1.5.1.5.1.5.1.5.1"},
			}, // Minor
			{
				{"Plain Hunt Triples", 
					"7.1.7.1.7.1.7.1.7.1.7.1.7.1"},
			}, // Triples
			{
				{"Plain Hunt Major",
					"x1x1x1x1x1x1x1x1"},
				{"Plain Hunt Triples",
					"7.1.7.1.7.1.7.1.7.1.7.1.7.1"},
			}, // Major
			{
				{"Plain Hunt Caters",
					"9.1.9.1.9.1.9.1.9.1.9.1.9.1.9.1.9.1"},
			}, // Caters
			{
				{"Plain Hunt Royal",
					"x1x1x1x1x1x1x1x1x1"},
				{"Plain Hunt Caters",
					"9.1.9.1.9.1.9.1.9.1.9.1.9.1.9.1.9.1"},
			}, // Royal
			{
				{"Plain Hunt Cinques", 
					"E.1.E.1.E.1.E.1.E.1.E.1.E.1.E.1.E.1.E.1.E.1"},
			}, // Cinques
			{
				{"Plain Hunt Maximus",
					"x1x1x1x1x1x1x1x1x1x1x1x1"},
				{"Plain Hunt Cinques",
					"E.1.E.1.E.1.E.1.E.1.E.1.E.1.E.1.E.1.E.1.E.1"},
				{"Cambridge Surprise Maximus", 
					"-3T-14-125T-36-147T-58-169T-70-18-9T-10-ET le 12"}
					// Allegedly equivalent; "x3x4x25x36x47x58x69x70x8x9x0xE le 2"}
			}, // Maximus
		};
	private int methodPosition = 0; /* Used to keep track
									 * of where we are */
	private String methodSelected = ""; /*	Will contain the place 
										 *	notation for the method */
	
	private SharedPreferences sprefs;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		sprefs = PreferenceManager.getDefaultSharedPreferences(this);
		debug = (TextView)findViewById(R.id.debug);

	}
	
	public int getNumberOfBells() {
		return Integer.valueOf(sprefs.getString("number_of_bells", "6"));
	}

	@SuppressWarnings("deprecation") /* FILL_PARENT */
	@SuppressLint("InlinedApi") /* FILL_PARENT */
	protected void draw_bells() {
		/* Should be straightforward to understand
		 * layout concept.  -1 for Look to! button,
		 * -2 for Go! button, -3 for Stand!
		 */
		
		final int Layouts[][][] = {
				{}, {}, {}, {},  // No layouts for 0-3 bells
				{
					{1, 0, 2},
					{-1, -2, -3},
					{4, 0, 3},
				},	// Four bells (Singles/minumus)
				{
					{1, 2, 3},
					{-1, -2, -3},
					{5, 0, 4},
				},	// Five (Doubles)
				{
					{0, 0, 2, 3, 0, 0}, 
					{1, -1, -2, -3, 4},
					{0, 0, 6, 5, 0, 0},
				},	// Six (Doubles/minor)
				{
					{0, 2, 0, 3, 0},
					{1, -1, -2, -3, 4},
					{0, 0, 0, 0, 5},
					{0, 7, 0, 6, 0},
				},	// Seven (Triples)
				{	{0, 2, 0, 3, 0}, 
					{1, -1, -2, -3, 4},
					{8, 0, 0, 0, 5},
					{0, 7, 0, 6, 0},
				},	// Eight (Triples/major)
				{
					{0, 0, 3, 4, 0, 0},
					{0, 2, 0, 0, 5, 0},
					{1, -1, -2, 0, 0, 6},
					{0, 9, 0, -3, 7, 0},
					{0, 0, 0, 8, 0, 0},
				},	// Nine (Caters)
				{	{0, 0, 3, 4, 0, 0},
					{0, 2, 0, 0, 5, 0},
					{1, -1, -2, 0, 0, 6},
					{0, 10, -3, 0, 7, 0},
					{0, 0, 9, 8, 0, 0},
				},	// Ten (Caters, Royal)
				{
					{0, 0, 3, 4, 0, 0},
					{0, 2, 0, 0, 5, 0},
					{1, -1, -2, 0, 0, 6},
					{11, 0, -3, 0, 0, 7},
					{0, 10, 0, 0, 8, 0},
					{0, 0, 0, 9, 0, 0},
				},	// Eleven (Cinques)
				{
					{0, 0, 3, 4, 0, 0},
					{0, 2, -1, 0, 5, 0},
					{1, 0, -2, 0, 0, 6},
					{12, 0, -3, 0, 0, 7},
					{0, 11, 0, 0, 8, 0},
					{0, 0, 10, 9, 0, 0},
				},	// Twelve (Cinques/maximus)
		};
		
		TableLayout table = (TableLayout)findViewById(R.id.mainTable);
		
		table.removeAllViews();
		ringHandler.removeCallbacks(ringHandlerRun);
		if (soundpool != null)
			soundpool.release();
		soundpool = new SoundPool(getNumberOfBells(),
				AudioManager.STREAM_MUSIC, 0);
		
		place = 0;
		change = 0;
		methodPosition = 0;
		
		for (int counter = 0; counter < maxNumberOfBells; counter++) {
			bells[counter] = new Bell(this, counter+1, maxNumberOfBells, getNumberOfBells(), soundpool);
			bells[counter].setOnClickListener(ringMe);
		}

		for (int[] r: Layouts[getNumberOfBells()]) {
			TableRow row = new TableRow(this);
			row.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.FILL_PARENT,
                    TableLayout.LayoutParams.FILL_PARENT, 1.0f));
			for (int col : r) {
				switch (col) {
				case -1:
					Button lookto = new Button(this);
					lookto.setText("Look to!");
					lookto.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							ringHandler.removeCallbacks(ringHandlerRun);
							blowTime = getBlowTime();
							ringHandler.postDelayed(ringHandlerRun,
									blowTime);
						}
					});
					row.addView(lookto);
					break;
				case -2:
					Button go = new Button(this);
					go.setText("Go!");
					go.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							methodSelected = 
									sprefs.getString("selected_method", "");
							methodPosition = 0;
							change = 1;
						}
					});
					row.addView(go);
					break;
				case -3:
					Button stand = new Button(this);
					stand.setText("Stand!");
					stand.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							standing = true;
						}
					});
					row.addView(stand);
					break;
				case 0:
					row.addView(new TextView(this));
					break;
				default:
					row.addView(bells[col-1]);
					break;
				}
			}
			table.addView(row);
		}
	}
	
	private void ringOneBlow() {
		ringHandler.postDelayed(ringHandlerRun, blowTime);

		/* Open handstroke lead-- all bells will be
		 * at handstroke, so just check the treble.
		 */

		if (place == 0) {
			place++;
			moveBellsAround();
			if (getUserBell() > 0 && getUserBell() < getNumberOfBells()) {
				userBellPlace = bells[getUserBell()-1].getPlace();
				if (userBellPlace == 1) {
					/* The user's bell is leading.
					 * If we are getting the handstroke gap, we
					 * set the timer for the next blow.
					 * Otherwise, if we're going for backstroke, set the
					 * timer behind by one blow to score correctly.
					 * Don't trust the Treble's stroke here, pick third!
					 */
					elapsedTime = System.nanoTime() - 
							(bells[2].atBackStroke() ? blowTime * 1000000 : 0);
				}
			} else
				userBellPlace = 0;
			if (bells[0].atHandStroke()) {
				/* The Treble is Always Right! (even if the user
				 * controls it....
				 */
				if (standing == true) {
					standing = false;
					onPause();
				}
				return;
			}
		}
		
		if (place == userBellPlace) {
			/* Don't ring the user's bell! */
		} else {
			for (Bell bell : bells)
				if (bell.getPlace() == place)
					bell.ring(ringHandler,
							sprefs.getBoolean("simulator", false) ?
							delayBeforeStriking : 0);			
		}
		
		if (place == userBellPlace - 1) {
			/* Next stroke will be the user's bell!  Start the timer... 
			 * Leading is dealt with earlier. */
			elapsedTime = System.nanoTime();
		}

		if (++place > getNumberOfBells())
			place = 0;
	}
	
	private int getUserBell() {
		String userBellString = sprefs.getString("my_bell", "None");
		
		if (userBellString.equals("None"))
			return 0;
		else
			return Integer.parseInt(userBellString);
	}
	
	private int getBlowTime() {
		String peal_string = sprefs.getString("peal_time", "180");
		int peal_minutes;
		try {
			peal_minutes = Integer.parseInt(peal_string);
		} catch (NumberFormatException e) {
			Toast.makeText(this,
					"Ah... no text allowed in peal time! Defaulting to 180",
					Toast.LENGTH_LONG).show();
			peal_minutes = 180;
		}
		
		/* We do not want to overflow... just in case! */
		long peal_milliseconds = (long)peal_minutes * 60000;
		
		int blow_milliseconds = (int)(peal_milliseconds / 5040);
		
		/* Because of open handstroke lead, we have an extra
		 * beat every two rounds.
		 */
		blow_milliseconds *= 2;
		blow_milliseconds /= (getNumberOfBells() * 2 + 1);
		
		return blow_milliseconds; /* Int preference??? */
	}
	
	private void moveBellsAround() {
		/* No error checking!!! Please make sure
		 * your method is correct...
		 */
		switch (change) {
		case 0:
			return; /* Still in rounds */
		case 1:
			/* We need to check that we start at handstroke. */
			if (bells[0].atBackStroke())
				return;
		}
		change++;
		
		/* OK, so now we need to iterate through the
		 * method.  Changes are delimited by dots 
		 * if necessary.  If an "x" or "-" is found,
		 * that is a change.  If a dot is found, that
		 * moves onto the next change.  Keep a permanent
		 * iterator to move across the line.
		 */
		
		ArrayList<Integer> exceptions = new ArrayList<Integer>();
		
		if (methodPosition >= methodSelected.length()) {
			/* Instructions finished.  Are we in rounds? */
			Boolean rounds = true;
			for (int p = 1; p < getNumberOfBells() + 1; p++) {
				if (bells[p-1].getPlace() != p)
					rounds = false;
			}
			if (rounds) {
				/* That's all! */
				change = 0;
				return;
			} else
			/* In this case, we need to know if a full lead was given.
			 * If the lead is symmetrical, then we need to reverse
			 * the instructions to get treble back to lead.
			 */
			switch (bells[0].getPlace()) {
			case 1:
				/* Complete instructions, we simply restart the
				 * method until we get back to rounds.
				 */
				methodPosition = 0;
				break; /* Will now continue to change */
			default:
				/* Houston, we have a problem...? */
				methodPosition = 0;
				String order = new String();
				for (int i = 0; i < getNumberOfBells(); i++) {
					for (Bell b : bells) {
						if (i+1 == b.getPlace())
							order += String.valueOf(b.getNumber());
					}
				}
				debug.setText("Uh, run out, no lead end????" + order);
				ringHandler.removeCallbacks(ringHandlerRun);
				return;
			}
		}
		
/*		Debug section to show the order of bells.
 * 		String order = new String();
		for (int i = 0; i < getNumberOfBells(); i++) {
			for (Bell b : bells) {
				if (i+1 == b.getPlace())
					order += String.valueOf(b.getNumber()) + " ";
			}
		}
		debug.setText(order);
*/
		switch (methodSelected.charAt(methodPosition)) {
		case ' ':
		case 'l':
			/* Symmetrical instructions, only the first half
			 * is given plus lead end.  We must invert them, strip
			 * the first instruction and then append them to the method.
			 */
			Pattern e = Pattern.compile("^((.*)[-x]|(.*[-x.])[0-9ET]+) ?l[he] ?([0-9ET]+)$");
			Matcher m = e.matcher(methodSelected);
			m.find();
			String methodBody = m.group(m.group(2) != null ? 2 : 3);
			/* We don't really need to worry about extra dots */
			methodSelected = m.group(1) +
					new StringBuilder(methodBody).reverse().toString() +
					"." + m.group(4); /* Lead end */
			moveBellsAround(); /* Recurse */
			return;
		case '-':
		case 'x':
			/* OK, swap all bells over */
			methodPosition++;
			swapAllBellsExcept(exceptions);
			return;
		case '.':
			/* OK, we're done switching and holding, time to move on! */
			while (methodSelected.charAt(++methodPosition) == '.')
				/* Skip past all dots */;
			break;
		}
		/* We have obviously been given a number.
		 * Add this to the collection.
		 */
		Pattern expression = Pattern.compile("^[0-9ET]+");
		Matcher matcher = expression.matcher(methodSelected.substring(methodPosition));
		matcher.find();
		methodPosition += matcher.end();
		for (char c : matcher.group().toCharArray()) {
			/* What a faff! We must first try bells 10-12 */	
			switch (c) {
			case '0':
				exceptions.add(10);
				break;
			case 'E':
				exceptions.add(11);
				break;
			case 'T':
				exceptions.add(12);
				break;
			default:
				exceptions.add(c - '0');
				break;
			}
		}
		swapAllBellsExcept(exceptions);
	}
	
	private void swapAllBellsExcept(ArrayList<Integer> exceptions) {

		/* Get array of bells in place order */
		ArrayList<Bell> bellsInPlaceOrder = new ArrayList<Bell>();
		for (int p = 1; p <= getNumberOfBells(); p++)
			for (Bell b : bells)
				if (b.getPlace() == p && !exceptions.contains(b.getPlace()))
					bellsInPlaceOrder.add(b);
		
		/* Find adjacent pairs of bells, and swap */
		for (int p = 0; p < bellsInPlaceOrder.size() - 1; p += 2) {
			Bell first = bellsInPlaceOrder.get(p);
			Bell second = bellsInPlaceOrder.get(p + 1);
			if (second.getPlace() - first.getPlace() == 1) {
				first.moveUp();
				second.moveDown();
			}
		}
	}
		
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.action_settings:
	        startActivity(new Intent(this, PreferencesActivity.class));
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	public void onPause() {
		super.onPause();
		ringHandler.removeCallbacks(ringHandlerRun);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		draw_bells();
	}
}