/*-
 * Copyright (c) 2014, Chris Rees
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *    
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.bayofrum.belldroid;

import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
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
	
	/**
	 * Used for several purposes; call changes until done, method scoring;
	 * can also be used to debug Methods showing the line.
	 */
	private TextView notifyArea;

	/**
	 * If increased, please check that there is a Layout for the numbers! 
	 * Also necessary will be modification of the regex (BOTH!) in
	 * moveBellsAround and adding cases for the letters.
	 * As well, add a line to methods for each new bell, and in Preferences
	 * array too, and Methods class for the names of the stages.
	 */
	private static final int BELLS_MAX = 12;
	
	/** Number of milliseconds to delay, when Delay is set. */
	private static final int delayBeforeStriking = 1000;

    /** Values for the "My bell" integer */
    private static final int BELL_CALL_CHANGES = 0;
    private static final int BELL_POOR_STRIKING = -1;

    private int bell_poor_striking = 0;
    private int bell_poor_striking_difference = 0;
    private int bell_poor_striking_percentage = 0;

	/**
	 * Keeps track.  Holds the place, special value 0 for before lead,
	 * which is when places change and the handstroke gap is inserted.
	 */ 
	private int place = 0;
	
	/** Keeps track. */
	private int change = 0, userBellPlace = 0, scoreTotal = 0, scoreOverRounds = 0;
	
	/** Used with System.nanoTime() to measure time between manual blows. */
	private long elapsedTime;
	/** When set to true, stops the bells at next handstroke in ringOneBlow(). */
	private boolean standing = false;
	
	/** Array of Bells, starting at 0 for treble. */
	private Bell[] bells = new Bell[BELLS_MAX];
	
	/** For call changes. */
	private Bell toMoveDown = null, toMoveUp = null;
	
	/**
	 * Shows the original state before method starts;
	 * not necessarily rounds.
	 */
	private String originalState = new String();
	
	/** Attaches ringHandlerRun to schedule blows. */
	private Handler ringHandler = new Handler();
	
	/** Attaches to ringHandler to schedule blows. */
	private Runnable ringHandlerRun = new Runnable() {
		@Override
		public void run() {
			ringOneBlow();
		}
	};
	
	/** Keeps track of the SoundPool, used to ring the Bells. */
	private SoundPool soundpool = null;
	
	/**
	 * Keeps track of how far along the Method is, used as an index
	 * to the String methodSelected
	 */
	private int methodPosition = 0;
	
	/** Contains the place notation for the Method. */
	private String methodSelected = "";
	
	/** Accesses Preferences */
	private SharedPreferences sprefs;
	
	/**
	 * Sets each Bell such that on a click, the user's Bell is checked that
	 * it is selected, and then if no Bell is selected, call a change, to be
	 * processed in moveBellsAround().
	 */
	private OnClickListener ringMe = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
			Bell bell = (Bell) arg0;

			if (bell.getNumber() == getUserBell()) {
				final int msec = (int)(System.nanoTime() - elapsedTime) / 1000000;
				bell.ring(ringHandler, sprefs.getBoolean("simulator", false) ?
						delayBeforeStriking : 0);
				
				/* Are we on time? Check first for ringing way out, before the
				 * previous blow or after the next blow */
				if (bell.getPlace() > place) {
					notifyArea.setText("Way too early! %score: " +
							scoreTotal / ++scoreOverRounds);
					return;
				} else if (bell.getPlace() + 1 < place) {
					notifyArea.setText("Way too wide! %score: " +
							scoreTotal / ++scoreOverRounds);
					return;
				}
	
				int blowTime = getBlowTime();
				int percentOff = ((msec - blowTime) * 100) / blowTime;
				scoreTotal += 100 - Math.abs(percentOff);
				
				if (Math.abs(percentOff) < 10) {
					notifyArea.setText("Very good! %score:" +
							scoreTotal / ++scoreOverRounds);
				} else if (percentOff > 0) {
					notifyArea.setText("Try a little earlier, %score: " +
							scoreTotal / ++scoreOverRounds);
				} else {
					notifyArea.setText("Try a little wider, %score: " +
							scoreTotal / ++scoreOverRounds);
				}
			} else if (getUserBell() == BELL_CALL_CHANGES) {
				/* Just calling! */
				if (callingUp()) {
					/* Last bell?? Can't move that up... */
					if (bell.getPlace() == getNumberOfBells()) {
						return;
					}
					for (int i = 0; i < getNumberOfBells(); i++) {
						if (bell.getPlace() + 1 == bells[i].getPlace()) {
							toMoveDown = bells[i];
							toMoveUp = bell;
							notifyArea.setText(toMoveUp.getName()
									+ " to " + toMoveDown.getName() + "!");
							break;
						}
					}
				} else {
					/* Leading bell?? Can't move that down... */
					if (bell.getPlace() == 1) {
						return;
					}
					Bell toFollow = null;
					for (int i = 0; i< getNumberOfBells(); i++) {
						if (bell.getPlace() == bells[i].getPlace() + 2) {
							toFollow = bells[i];
						} else if (bell.getPlace() - 1 == bells[i].getPlace()) {
							toMoveUp = bells[i];
							toMoveDown = bell;
							break;
						}
					}
					/* Calling down is annoying-- special calls to lead. */
					if (bell.getPlace() == 2) {
						notifyArea.setText(bell.getName() + " lead!");
					} else if (toFollow != null) {
						notifyArea.setText(bell.getName() + " to " +
								toFollow.getName() + "!");
					}
				}
			} else if (getUserBell() == BELL_POOR_STRIKING) {
                if (bell.getNumber() == bell_poor_striking) {
                    notifyArea.setText("Bell number " + bell.getName() + ", please " +
                            (bell_poor_striking_percentage > 0 ? "pull in!" : "hold up!"));
                    set_bell_poor_striking();
                } else {
                    notifyArea.setText("Actually, " + bell.getName() + " is striking correctly.");
                }
            }
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		sprefs = PreferenceManager.getDefaultSharedPreferences(this);
		notifyArea = (TextView)findViewById(R.id.notify_area);
		draw_bells();
	}
	
	/** Gets number of Bells, from Preferences as int */
	public int getNumberOfBells() {
		return Integer.valueOf(sprefs.getString("number_of_bells", "6"));
	}

	/**
	 * Draws Bells, sets up sound, resets everything.
	 */
	@SuppressWarnings("deprecation") /* FILL_PARENT */
	@SuppressLint("InlinedApi") /* FILL_PARENT */
	protected void draw_bells() {
		/**
		 * Should be straightforward to understand
		 * layout concept.  -1 for Look to! button,
		 * -2 for Go! button, -3 for Stand!
		 */
		final int Layouts[][][] = {
				{}, {}, {}, {},  // No layouts for 0-3 bells
				{	{1, 0, 2},
					{-1, -2, -3},
					{4, 0, 3},
				},	// Four bells (Singles/minumus)
				{	{1, 2, 3},
					{-1, -2, -3},
					{5, 0, 4},
				},	// Five (Doubles)
				{	{0, 0, 2, 3, 0, 0}, 
					{1, -1, -2, -3, 4},
					{0, 0, 6, 5, 0, 0},
				},	// Six (Doubles/minor)
				{	{0, 2, 0, 3, 0},
					{1, -1, -2, -3, 4},
					{0, 0, 0, 0, 5},
					{0, 7, 0, 6, 0},
				},	// Seven (Triples)
				{	{0, 2, 0, 3, 0}, 
					{1, -1, -2, -3, 4},
					{8, 0, 0, 0, 5},
					{0, 7, 0, 6, 0},
				},	// Eight (Triples/major)
				{	{0, 0, 3, 4, 0, 0},
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
				{	{0, 0, 3, 4, 0, 0},
					{0, 2, 0, 0, 5, 0},
					{1, -1, -2, 0, 0, 6},
					{11, 0, -3, 0, 0, 7},
					{0, 10, 0, 0, 8, 0},
					{0, 0, 0, 9, 0, 0},
				},	// Eleven (Cinques)
				{	{0, 0, 3, 4, 0, 0},
					{0, 2, -1, 0, 5, 0},
					{1, 0, -2, 0, 0, 6},
					{12, 0, -3, 0, 0, 7},
					{0, 11, 0, 0, 8, 0},
					{0, 0, 10, 9, 0, 0},
				},	// Twelve (Cinques/Maximus)
		};
		
		TableLayout table = (TableLayout)findViewById(R.id.mainTable);
		
		table.removeAllViews();
		ringHandler.removeCallbacks(ringHandlerRun);
		if (soundpool != null) {
			soundpool.release();
		}
		soundpool = new SoundPool(getNumberOfBells(),
				AudioManager.STREAM_MUSIC, 0);
		
		place = change = methodPosition = 0;
		
		for (int counter = 0; counter < BELLS_MAX; counter++) {
			bells[counter] = new Bell(this, counter + 1, BELLS_MAX,
					getNumberOfBells(), soundpool);
			bells[counter].setOnClickListener(ringMe);
		}

		for (int[] layoutRow: Layouts[getNumberOfBells()]) {
			TableRow row = new TableRow(this);
			row.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.FILL_PARENT,
                    TableLayout.LayoutParams.FILL_PARENT, 1.0f));
			for (int cell : layoutRow) {
				switch (cell) {
				case -1:
					Button lookto = new Button(this);
					lookto.setText("Look to!");
					lookto.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							ringHandler.removeCallbacks(ringHandlerRun);
                            notifyArea.setText("");
                            set_bell_poor_striking();
							ringHandler.postDelayed(ringHandlerRun,
									getBlowTime());
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
					/** Empty cell, put "something" in there */
					row.addView(new TextView(this));
					break;
				default:
					row.addView(bells[cell-1]);
					break;
				}
			}
			table.addView(row);
		}
	}
	
	/**
	 * Schedules the next blow, decides on whether to leave a handstroke gap,
	 * checks for standing.
	 */
	private void ringOneBlow() {
		ringHandler.postDelayed(ringHandlerRun, getBlowTime());

		/**
		 * Open handstroke lead-- all bells will be
		 * at handstroke, so just check the treble.
		 */
		if (place == 0) {
			place++;
			moveBellsAround();
			if (getUserBell() > 0 && getUserBell() <= getNumberOfBells()) {
				userBellPlace = bells[getUserBell()-1].getPlace();
				if (userBellPlace == 1) {
					/* The user's bell is leading.
					 * If we are getting the handstroke gap, we
					 * set the timer for the next blow.
					 * Otherwise, if we're going for backstroke, set the
					 * timer behind by one blow to score correctly.
					 * Don't trust the Treble's stroke here, pick third!
					 */
					if (bells[2].atHandStroke() && handstrokeGapEnabled()) {
						elapsedTime = System.nanoTime();
					} else {
						elapsedTime = System.nanoTime() - getBlowTime() * 1000000;
					}
				}
			} else {
				userBellPlace = 0;
			}
			/* Check the Treble's stroke, unless the user controls it,
			 * in which case check the Two
			 */
			if ((getUserBell() == 1 ? bells[1] : bells[0]).atHandStroke()) {
				if (standing == true) {
					standing = false;
					methodPosition = 0;
					change = 0;
					/* Might as well go back to rounds */
					for (int i = 0; i < getNumberOfBells(); i++) {
						bells[i].setPlace(i + 1);
					}
					onPause();
				}
				if (handstrokeGapEnabled()) {
					return;
				}
			}
		}
		
		switch(place - userBellPlace) {
		case 0:
			/* Don't ring the user's bell! */
			break;
		case -1:
			/* Next stroke will be the user's bell!  Start the timer... 
			 * Leading is dealt with earlier. */
			elapsedTime = System.nanoTime();
			/* continue */
		default:
			for (Bell bell : bells) {
				if (bell.getPlace() == place) {
					bell.ring(ringHandler,
							sprefs.getBoolean("simulator", false) ?
							delayBeforeStriking : 0);
				}
			}
			break;
		}
		
		if (++place > getNumberOfBells()) {
			place = 0;
		}
	}
	
	/**
	 * Returns user's Bell from preferences.  If unset, returns 0, which
	 * is taken to mean call change mode.
	 */
	private int getUserBell() {
		String userBellString = sprefs.getString("my_bell", "None-- call changes");
		
		if (userBellString.equals("None-- call changes") || 
				userBellString.equals("None" /* Compat for before 0.3 */))
			return BELL_CALL_CHANGES;
        else if (userBellString.equals("Choose the wrongly striking bell"))
            return BELL_POOR_STRIKING;
		else
			return Integer.parseInt(userBellString);
	}
	
	/**
	 * Calculates the time for one blow.
	 * 
	 * With the handstroke gap, of course it must be worked out over
	 * MAX_BELLS + 0.5, since in one whole pull there are 2 * MAX_BELLS + 1
	 * beats.
	 * 
	 * Sure hope this is optimised out, we call this a lot!
	 * 
	 * @return time for one blow in milliseconds
	 */
	private int getBlowTime() {
		String peal_string = sprefs.getString("peal_time", "180");
		int peal_minutes;
		try {
			peal_minutes = Integer.parseInt(peal_string);
		} catch (NumberFormatException e) {
			/* Should NEVER happen; peal_time is set as integer type */
			Toast.makeText(this,
					"Ah... no text allowed in peal time! Defaulting to 180",
					Toast.LENGTH_LONG).show();
			peal_minutes = 180;
		}
		
		/* We do not want to overflow... just in case! */
		long peal_milliseconds = (long) peal_minutes * 60000;
		
		int blow_milliseconds = (int) (peal_milliseconds / 5040);
		
		/* Because of open handstroke lead, we have an extra
		 * beat every two rounds.
		 */
		blow_milliseconds *= 2;
		blow_milliseconds /= (getNumberOfBells() * 2 + 1);

        /* Last bell was held up too long/pulled in too quickly */
        blow_milliseconds -= bell_poor_striking_difference;
        bell_poor_striking_difference = 0;

        /* Check to see if the next ringer is quick/slow */
        if (getUserBell() == BELL_POOR_STRIKING) {
            for (Bell b : bells) {
                if (b.getPlace() == place + 1 && b.getNumber() == bell_poor_striking) {
                    bell_poor_striking_difference = (int)(blow_milliseconds *
                            0.01 * bell_poor_striking_percentage);
                    blow_milliseconds += bell_poor_striking_difference;
                    break;
                }
            }
        }
		return blow_milliseconds;
	}

    private void set_bell_poor_striking() {
        bell_poor_striking =
                (int)(Math.random() * (getNumberOfBells() - 2)) + 1;
        bell_poor_striking_percentage = (Math.random() < 0.5 ? -1 : 1) *
                Integer.parseInt(sprefs.getString("how_poor_striking", "0"));
    }
	
	/**
	 * Returns true if calling up is selected in preferences,
	 * false for down.
	 */
	private boolean callingUp() {
		return sprefs.getBoolean("calling_up", true);
	}
	
	private boolean handstrokeGapEnabled() {
		return sprefs.getBoolean("handstroke_gap", true);
	}
	
	/**
	 * First performs call changes if necessary, then checks for methods.
	 * The method notation is trusted to be correct-- what else is there
	 * to do??  Hopefully wouldn't be an issue in production, unless the
	 * user is later allowed to add his/her own methods.
	 */
	private void moveBellsAround() {

		/* Let's do call changes first */
		if (toMoveDown != null && toMoveUp != null && bells[0].atHandStroke()) {
			toMoveDown.moveDown();
			toMoveUp.moveUp();
			notifyArea.setText("");
			toMoveDown = toMoveUp = null;
		}
		
		switch (change) {
		case 0:
			return; /* Still in original state (usually rounds) */
		case 1:
			/* 
			 * We need to check that we start at handstroke.
			 * N.B. Trusting the Treble could be a bad move,
			 * in case the user controls it, but almost certainly
			 * won't be an issue.
			 */
			if (bells[0].atBackStroke())
				return;
			
			/* Now we record the original state, probably rounds */
			originalState = "";
			for (int i = 0; i < getNumberOfBells(); i++) {
				originalState += String.valueOf(bells[i].getPlaceAsChar());
			}
		}
		change++;
		
		/*
		 * OK, so now we need to iterate through the
		 * method.  Changes are delimited by dots 
		 * if necessary.  If an "x" or "-" is found,
		 * that is a change.  If a dot is found, that
		 * moves onto the next change.  Keep a permanent
		 * iterator to move across the line.
		 */
		
		/** Stores <i>place values</i> (NOT numbers) of Bells to make places. */
		SortedSet<Integer> exceptions = new TreeSet<Integer>();
		
		if (methodPosition >= methodSelected.length()) {
			/* Instructions finished.  Are we in the original state? */
			boolean origState = true;
			for (int i = 0; i < getNumberOfBells(); i++) {
				/* Parse the original state */
				if (bells[i].getPlaceAsChar() != originalState.charAt(i)) {
					origState = false;
				}
			}
			if (origState) {
				/* That's all! */
				change = 0;
				return;
			} else {
				/* Not in rounds, so we restart the method. */
				methodPosition = 0;
			}
		}
		
		/* Debug section to show the order of bells. */
/* 		@SuppressWarnings("unused")
		String order = new String();
		for (int i = 0; i < getNumberOfBells(); i++) {
			for (Bell b : bells) {
				if (i+1 == b.getPlace())
					order += String.valueOf(b.getNumber()) + " ";
			}
		}
		notifyArea.setText(methodSelected.substring(0, methodPosition+1) + ">" +
				methodSelected.substring(methodPosition+1, methodSelected.length())
				+ " | " + order);
*/
		switch (methodSelected.charAt(methodPosition)) {
		case ' ':
		case 'l':
			/* 
			 * Symmetrical instructions, only the first half
			 * is given plus lead end.  We must invert them, strip
			 * the first instruction and then append them to the method.
			 * Could definitely use some error checking....
			 */
			Pattern e = Pattern.compile("^([^ l]+) ?l[he] ?([-x.0-9ET]+)$");
			Matcher m = e.matcher(methodSelected);
			m.find();
			/* We don't really need to worry about extra dots */
			methodSelected = m.group(1) +
					allButLastChangeAndReverse(m.group(1)) +
					"." + getLeadHead(m.group(2));
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
			do {
				methodPosition++;
			} while (methodSelected.charAt(methodPosition) == '.');
			break;
		}
		/* 
		 * We have obviously been given a number.
		 * Add this to the collection.
		 */
		Pattern expression = Pattern.compile("^[0-9ET]+");
		Matcher matcher = expression.matcher(methodSelected.substring(methodPosition));
		if (!matcher.find()) {
			/* Ah... this is actually a big problem.  Complain loudly and stand. */
			Toast.makeText(this,
					"Invalid characters in method.  Please report to developer, including method name.",
					Toast.LENGTH_LONG).show();
			standing = true;
			return;
		}
		methodPosition += matcher.end();
		for (char c : matcher.group().toCharArray()) {
			/* What a faff! We must first try bells 10-12 */	
			exceptions.add(Bell.charToInt(c));
		}
		swapAllBellsExcept(exceptions);
	}
	
	/**
	 * Take a section of a method, and strips off just the last change.
	 * If it is simply one change, return nothing.
	 * 
	 * @param m method from which to strip the final change
	 * @return the method missing the last change
	 */
	
	private String allButLastChangeAndReverse(String m) {
		String ret = new String();
		if (m.matches(".*[-.x].*")) {
			Matcher stripper = Pattern.compile("^(.*[-x.])[0-9ET]+|(.*)[-x]$").matcher(m);			
			stripper.find();
			ret = stripper.group(stripper.group(1) != null ? 1 : 2);
		} else {
			/* Just one change, no stripping to do here */
			ret = "";
		}
		
		return new StringBuilder(ret).reverse().toString();
	}

	/**
	 * Sometimes the lead head isn't just one change... we
	 * must process it as with the method body.  If it's one
	 * change, just return it as-is
	 * 
	 * @param lh lead head to process
	 * @return a processed lead head
	 */
	private String getLeadHead(String lh) {
		String ret = new String();
		if (lh.matches(".*[-.x].*")) {
			/* Multiple changes... we should return the whole thing plus
			 * reverse and stripped */
			ret = lh + "." + allButLastChangeAndReverse(lh);
		} else {
			ret = lh;
		}
		return ret;
	}
	
	/**
	 * Given a set of Bell places, swap every Bell except the excepted ones.
	 *
	 * @param exceptions <i>place value</i> of Bells to make places.
	 */
	private void swapAllBellsExcept(SortedSet<Integer> exceptions) {

		/** Minimum place notation is really annoying.
		 * One has to work outwards, using the stationary
		 * bells as guides.  For example;
		 * .4. means that the lead stays still, but if
		 * we don't add 1 here, the code will make the thirds
		 * and fourths stay still.
		 * We can almost cheat... if the lowest exception is
		 * even, we must keep the lead still.
		 */

		if (!exceptions.isEmpty()) {
			if ((exceptions.first() & 1) == 0) {
				exceptions.add(1);
			}
		}
		
		/* Get array of bells in place order */
		ArrayList<Bell> bellsInPlaceOrder = new ArrayList<Bell>();
		for (int p = 1; p <= getNumberOfBells(); p++) {
			for (Bell b : bells) {
				if (b.getPlace() == p && !exceptions.contains(b.getPlace())) {
					bellsInPlaceOrder.add(b);
				}
			}
		}
		
		/* Find adjacent pairs of bells, and swap */
		for (int p = 0; p < bellsInPlaceOrder.size() - 1; p++) {
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
	    case R.id.updatedb:
	    	Method methods = new Method(this);
	    	methods.updateDb(getNumberOfBells());
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
		/* Only need to reset because of changes in bells preferences */
		draw_bells();
	}
}