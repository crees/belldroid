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
	
//	private TextView debug;

	/* If increased, please check that there is a Layout for the numbers! */
	private static final int maxNumberOfBells = 12;
	
	private int numberOfBells, place = 1, roundTime = 1500,
			change = 0, methodSelected = 0;
	
	private Bell[] bells = new Bell[maxNumberOfBells];
	
	private Handler ringHandler = new Handler();
	private Runnable ringHandlerRun = new Runnable() {
		
		@Override
		public void run() {
			ringOneBlow();
			ringHandler.postDelayed(this, roundTime / numberOfBells);
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
					"x16x16x16x16x16x16"},
				{"Plain Hunt Doubles",
					"56.16.56.16.56.16.56.16.56.16"},
			}, // Minor
			{{"None", ""}}, // Triples
			{{"None", ""}}, // Major
			{{"None", ""}}, // Caters
			{{"None", ""}}, // Royal
			{{"None", ""}}, // Cinques
			{{"None", ""}}, // Maximus
	};
	private int methodPosition = 0; /* Used to keep track
									 * of where we are */

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
//		debug = (TextView)findViewById(R.id.debug);

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
							ringHandler.removeCallbacks(ringHandlerRun);
							for (Bell b : bells) {
								if (b.getStroke() != Bell.Stroke.hand)
									b.ring();
							}
							place = 1;
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
						methodSelected = arg0.getSelectedItemPosition();
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
			if (bells[0].getStroke() == Bell.Stroke.hand)
				return;
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
		if (change == 0)
			return; /* Still in rounds */
		
		change++;
		
		/* OK, so now we need to iterate through the
		 * method.  Changes are delimited by dots 
		 * if necessary.  If an "x" or "-" is found,
		 * that is a change.  If a dot is found, that
		 * moves onto the next change.  Keep a permanent
		 * iterator to move across the line.
		 */
		
		String method = methods[numberOfBells][methodSelected][1];
	
		ArrayList<Integer> exceptions = new ArrayList<Integer>();
		
		if (methodPosition >= method.length()) {
			/* That's all!  Sure hope we're at rounds.... */
			change = 0;
			return;
		}
		
		switch (method.charAt(methodPosition)) {
		case '-':
		case 'x':
			/* OK, swap all bells over */
			methodPosition++;
			swapAllBellsExcept(exceptions);
			return;
		case '.':
			/* OK, we're done switching and holding, time to move on! */
			methodPosition++;
			break;
		}
		/* We have obviously been given a number.
		 * Add this to the collection.
		 */
		Pattern expression = Pattern.compile("^[0-9]+");
		Matcher matcher = expression.matcher(method.substring(methodPosition));
		matcher.find();
//		debug.setText(String.valueOf(matcher.end()));
		methodPosition += matcher.end();
		for (char c : matcher.group().toCharArray())
				exceptions.add(c - '0');					
		swapAllBellsExcept(exceptions);
	}
	
	private void swapAllBellsExcept(ArrayList<Integer> exceptions) {

		int up = 1;
		
		/* Get array of bells in place order */
		ArrayList<Bell> bellsInPlaceOrder = new ArrayList<Bell>();
		for (int p = 1; p <= numberOfBells; p++)
			for (Bell b : bells)
				if (b.getPlace() == p)
					bellsInPlaceOrder.add(b);

		for (Bell b : bellsInPlaceOrder) {
			if (exceptions.contains(b.getPlace()))
				continue;
			switch (up) {
			case 0:
				up = 1;
				b.moveDown();
				break;
			case 1:
				up = 0;
				b.moveUp();
				break;
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
	}
}