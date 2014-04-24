package net.bayofrum.belldroid;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	private TextView debug;

	/* If increased, please check that there is a Layout for the numbers! 
	 * Also necessary will be modification of the regex (BOTH!) in
	 * moveBellsAround and adding cases for the letters.
	 * As well, add a line to methods for each new bell.
	 */
	private static final int maxNumberOfBells = 12;
	
	private int numberOfBells, place = 1, roundTime = 2143 /* three hour peal */,
			change = 0;
	private Boolean standing = false;
	
	private Bell[] bells = new Bell[maxNumberOfBells];
	
	private Handler ringHandler = new Handler();
	private Runnable ringHandlerRun = new Runnable() {
		
		@Override
		public void run() {
			ringHandler.postDelayed(this, roundTime / numberOfBells);
			ringOneBlow();
		}
	};
	
	private SoundPool soundpool = null;
	
	private static final String[][][] methods = {
			{{}}, {{}}, {{}}, // 0-2 bells (!)
			{{"None", ""}}, // Singles
			{{"None", ""}}, // Minimus
			{
				{"Plain Hunt",
					"5.1.5.1.5.1.5.1.5.1"},
			}, // Doubles
			{
				{"Plain Hunt",
					"x1x1x1x1x1x1"},
				{"Plain Hunt Doubles",
					"5.1.5.1.5.1.5.1.5.1"},
				{"Plain Hunt Doubles le",
					"5.1.5.1.5 le 1"},
				{"Grandsire Doubles",
					"3.1.5.1.5.1.5.1.5.1"},
			}, // Minor
			{{"None", ""}}, // Triples
			{{"None", ""}}, // Major
			{{"None", ""}}, // Caters
			{{"None", ""}}, // Royal
			{{"None", ""}}, // Cinques
			{
				{"Cambridge Surprise Maximus", 
					"-3T-14-125T-36-147T-58-169T-70-18-9T-10-ET le 12"}
					// Allegedly equivalent; "x3x4x25x36x47x58x69x70x8x9x0xE le 2"}
			}, // Maximus
	};
	private int methodPosition = 0; /* Used to keep track
									 * of where we are */
	private String methodSelected = ""; /*	Will contain the place 
										 *	notation for the method */
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/* Populate the Spinner with number of bells */
		Spinner bellChooser = (Spinner) findViewById(R.id.numberOfBells);
		ArrayList<Integer> numBellsAdapter = new ArrayList<Integer>();
		for (int i = 4; i <= maxNumberOfBells; i++) {
			numBellsAdapter.add(i);
		}
		
		bellChooser.setAdapter(new ArrayAdapter<Integer> (this,
				android.R.layout.simple_spinner_item, numBellsAdapter));
		bellChooser.setOnItemSelectedListener(
				new AdapterView.OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> arg0, View arg1,
							int arg2, long arg3) {
						numberOfBells = (Integer)arg0.getSelectedItem();
						showMethodsInSpinner();
						draw_bells();
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
						// DO_NADA-- can't happen
					}
		});
		debug = (TextView)findViewById(R.id.debug);

	}

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
					{1, -1, -2, 0, -3, 6},
					{0, 9, 0, 0, 7, 0},
					{0, 0, 0, 8, 0, 0},
				},	// Nine (Caters)
				{	{0, 0, 3, 4, 0, 0},
					{0, 2, 0, 0, 5, 0},
					{1, -1, -2, 0, -3, 6},
					{0, 10, 0, 0, 7, 0},
					{0, 0, 9, 8, 0, 0},
				},	// Ten (Caters, Royal)
				{
					{0, 0, 3, 4, 0, 0},
					{0, 2, 0, 0, 5, 0},
					{1, -1, -2, 0, -3, 6},
					{11, 0, 0, 0, 0, 7},
					{0, 10, 0, 0, 8, 0},
					{0, 0, 0, 9, 0, 0},
				},	// Eleven (Cinques)
				{
					{0, 0, 3, 4, 0, 0},
					{0, 2, 0, 0, 5, 0},
					{1, -1, -2, 0, -3, 6},
					{12, 0, 0, 0, 0, 7},
					{0, 11, 0, 0, 8, 0},
					{0, 0, 10, 9, 0, 0},
				},	// Twelve (Cinques/maximus)
		};
		
		TableLayout table = (TableLayout)findViewById(R.id.mainTable);
		
		table.removeAllViews();
		ringHandler.removeCallbacks(ringHandlerRun);
		if (soundpool != null)
			soundpool.release();
		soundpool = new SoundPool(12, AudioManager.STREAM_MUSIC, 0);
		
		place = 1;
		change = 0;
		
		for (int counter = 0; counter < maxNumberOfBells; counter++) {
			bells[counter] = new Bell(this, counter+1, maxNumberOfBells, numberOfBells, soundpool);
		}

		for (int[] r: Layouts[numberOfBells]) {
			TableRow row = new TableRow(this);
			row.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.MATCH_PARENT, 1.0f));
			for (int col : r) {
				switch (col) {
				case -1:
					Button lookto = new Button(this);
					lookto.setText("Look to!");
					lookto.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							ringHandler.removeCallbacks(ringHandlerRun);
							ringHandler.postDelayed(ringHandlerRun, roundTime / numberOfBells);
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
	
	private void showMethodsInSpinner() {
		/* Populate the Spinner with methods */
		Spinner methodChooser = (Spinner) findViewById(R.id.methodChooser);
		ArrayList<String> methodAdapter = new ArrayList<String>();
		for (String[] method : methods[numberOfBells]) {
			methodAdapter.add(method[0]);
		}
		
		methodChooser.setAdapter(new ArrayAdapter<String> (this,
				android.R.layout.simple_spinner_item, methodAdapter));
		methodChooser.setOnItemSelectedListener(
				new AdapterView.OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> arg0, View arg1,
							int arg2, long arg3) {
						methodSelected = methods[numberOfBells]
								[arg0.getSelectedItemPosition()][1];
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
						// DO_NADA-- can't happen
					}
		});

	}

	private void ringOneBlow() {
		/* Open handstroke lead-- all bells will be
		 * at handstroke, so just check the treble.
		 */
		if (place == 0) {
			place++;
			moveBellsAround();
			if (bells[0].atHandStroke()) {
				if (standing == true) {
					standing = false;
					ringHandler.removeCallbacks(ringHandlerRun);
					onPause();
				}
				return;
			}
		}

		for (Bell bell : bells)
			if (bell.getPlace() == place) {
				bell.ring();
			}
		if (++place > numberOfBells)
			place = 0;
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
			for (int p = 1; p < numberOfBells + 1; p++) {
				if (bells[p-1].getPlace() != p)
					rounds = false;
			}
			if (rounds) {
				/* That's all! */
				change = 0;
				return;
			}
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
				return;
			default:
				/* Houston, we have a problem...? */
				methodPosition = 0;
				String order = new String();
				for (int i = 0; i < numberOfBells; i++) {
					for (Bell b : bells) {
						if (i+1 == b.getPlace())
							order += String.valueOf(b.getNumber());
					}
				}
				debug.setText("Uh, run out, no lead end????" + order);
//				ringHandler.removeCallbacks(ringHandlerRun);
				return;
			}
		}
		
		String order = new String();
		for (int i = 0; i < numberOfBells; i++) {
			for (Bell b : bells) {
				if (i+1 == b.getPlace())
					order += String.valueOf(b.getNumber()) + " ";
			}
		}
		debug.setText(order);
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
			debug.setText(methodSelected);
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
//		debug.setText(methodSelected);
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
		for (int p = 1; p <= numberOfBells; p++)
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
	
	public void onPause() {
		super.onPause();
		ringHandler.removeCallbacks(ringHandlerRun);
		place = 1;
		change = 0;
		methodPosition = 0;
		for (Bell b: bells)
			b.setPlace(b.getNumber());
	}
}