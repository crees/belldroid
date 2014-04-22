package net.bayofrum.belldroid;

import java.util.ArrayList;

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

	/* If increased, please check that there is a Layout for the numbers! */
	private static final int maxNumberOfBells = 12;
	
	private int numberOfBells, place = 1, roundTime = 1500;
	
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
						draw_bells();
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
						// DO_NADA-- can't happen
					}
		});
	}

	protected void draw_bells() {
		// All bells starting in top left corner
		
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
					{0, 2, 3, 0},
					{1, -1, 0, 4},
					{0, 0, 0, 5},
					{0, 7, 6, 0},
				},	// Seven (Triples)
				{	{0, 2, 3, 0}, 
					{1, -1, 0, 4},
					{8, 0, 0, 5},
					{0, 7, 6, 0},
				},	// Eight (Triples/major)
				{
					{0, 0, 3, 4, 0, 0},
					{0, 2, 0, 0, 5, 0},
					{1, 0, -1, 0, 0, 6},
					{0, 9, 0, 0, 7, 0},
					{0, 0, 0, 8, 0, 0},
				},	// Nine (Caters)
				{	{0, 0, 3, 4, 0, 0},
					{0, 2, 0, 0, 5, 0},
					{1, 0, -1, 0, 0, 6},
					{0, 10, 0, 0, 7, 0},
					{0, 0, 9, 8, 0, 0},
				},	// Ten (Caters, Royal)
				{
					{0, 0, 3, 4, 0, 0},
					{0, 2, 0, 0, 5, 0},
					{1, 0, -1, 0, 0, 6},
					{11, 0, 0, 0, 0, 7},
					{0, 10, 0, 0, 8, 0},
					{0, 0, 0, 9, 0, 0},
				},	// Eleven (Cinques)
				{
					{0, 0, 3, 4, 0, 0},
					{0, 2, 0, 0, 5, 0},
					{1, 0, -1, 0, 0, 6},
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

	private void ringOneBlow() {
		for (Bell bell : bells)
			if (bell.getPlace() == place) {
				bell.ring();
			}
		if (++place > numberOfBells)
			place = 1;
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