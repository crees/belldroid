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
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.bayofrum.belldroid;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Xml;

public class Method extends SQLiteOpenHelper {

	/**
	 * Simplest methods, too simple for methods.org.
	 * Ordered by stage, then a simple list, name/notation.
	 * Look at how compact you can make even plain hunt!
	 */
	public static final String[][][] methods =
		{	{{}}, {{}}, {{}}, // 0-2 bells (!)
			{	{"Plain Hunt Singles",	"3.1.3 le 1"},
				{"Short Cure for Melancholy", "3.13.1.13.3.13 le 1"}
			}, // Singles
			{{"Plain Hunt Minimus", "x1"}}, // Minimus
			{{"Plain Hunt Doubles",	"5.1.5.1.5 le 1"}}, // Doubles
			{{"Plain Hunt Minor", "x1"}}, // Minor
			{{"Plain Hunt Triples", "7.1.7.1.7.1.7 le 1"}}, // Triples
			{{"Plain Hunt Major", "x1"}}, // Major
			{{"Plain Hunt Caters", "9.1.9.1.9.1.9.1.9 le 1"}}, // Caters
			{{"Plain Hunt Royal", "x1"}}, // Royal
			{{"Plain Hunt Cinques", "E.1.E.1.E.1.E.1.E.1.E le 1"}}, // Cinques
			{{"Plain Hunt Maximus",	"x1"}}, // Maximus
		};
	
	/** List of names of classes, as kept by ringing.org */
/*	private static final String[] CLASS_NAMES = {
		"Principle", "Plain", "Bob", "Place", "Treble%20Dodging",
		"Treble%20Bob", "Surprise", "Delight", "Treble%20Place",
		"Alliance", "Hybrid", "Slow%20Course",
	};
*/	
	public static final String TABLE_METHODS = "methods";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_CLASS = "class";
	public static final String COLUMN_STAGE = "stage";
	public static final String COLUMN_NOTATION = "notation";

	private static final String DATABASE_NAME = "methods.db";
	private static final int DATABASE_VERSION = 2;

	private static final String DATABASE_CREATE = "create table "
	    + TABLE_METHODS + "(" + 
		COLUMN_ID + " integer primary key autoincrement, " +
	    COLUMN_NAME + " text not null, " +
		COLUMN_CLASS + " text not null, " +
	    COLUMN_STAGE + " integer not null, " +
		COLUMN_NOTATION + " text not null, " +
		"dummy text);";

    private final Context context;

	public Method(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }
	
	/**
	 * Creates entire database, and populates with simple methods.
	 * 
	 * @param db provided by the framework.
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_CREATE);
		for (int i = 3; i <= 12; i++) {
			for (String[] m : methods[i]) {
				ContentValues row = new ContentValues();
				row.put(COLUMN_NAME, m[0]);
				row.put(COLUMN_NOTATION, m[1]);
				row.put(COLUMN_STAGE, i);
				row.put(COLUMN_CLASS, "Plain");
				db.insert(TABLE_METHODS, "dummy", row);
			}
		}
	}
	
	/**
	 * Updates the database using ringing.org.
	 * Wrapper around updateDatabaseTask; runs in background.
	 * 
	 * @param numBells	number of Bells selected; if even, also updates the
	 * 					stage just below, e.g. updating Minor also updates Doubles.
	 */
	public void updateDb(int numBells) {
		UpdateDatabaseTask updater = new UpdateDatabaseTask();

		if ((numBells & 1) == 0) {
			updater.execute(numBells - 1, numBells);
		} else {
			updater.execute(numBells);
		}
	}
	
	/**
	 * Checks in the database for relevant method Classes.
	 * 
	 * @param stage	number of Bells; also returns next lower number for even stages;
	 * 				for example requesting Minor Classes also returns Doubles Classes.
	 * @return		Array of Classes, handily appropriate for putting into
	 * 				a ListPreference
	 */
	public ArrayList<CharSequence[]> returnClasses(int stage) {
		ArrayList<CharSequence[]> c = new ArrayList<CharSequence[]>();

		if ((stage & 1) == 0) {
			c.addAll(this.returnClasses(stage - 1));
		}
		
		final SQLiteDatabase db = getReadableDatabase();
		
		final String[] columns = {COLUMN_CLASS};
		
		Cursor cursor = db.query(true, TABLE_METHODS, columns, COLUMN_STAGE + "=" + stage,
				null, null, null, null, null);
		
		if (cursor != null) {
			cursor.moveToFirst();
		} else {
			c.add(new CharSequence[] {"Empty...", "Empty..."});
			return c;
		}
		
		while (!cursor.isAfterLast()) {
			CharSequence row = cursor.getString(0);
			c.add(new CharSequence[] {row, row});
			cursor.moveToNext();
		}
		
		cursor.close();
		db.close();
		
		/* Uniquify, in case stage-1 also has that class */
		ArrayList<CharSequence> u = new ArrayList<CharSequence>();
		for (int i = 0; i < c.size(); i++) {
			if (u.contains(c.get(i)[0])) {
				c.remove(i--);
				continue;
			}
			u.add(c.get(i)[0]);
		}
		return c;
	}
	
	/** Converts the stage as a number into the traditional name. */
	public String stageToString(int stage) {
		final String[] name = {"onebell", "twobell", "Singles",
				"Minimus", "Doubles", "Minor", "Triples", "Major",
				"Caters", "Royal", "Cinques", "Maximus"};
		
		return name[stage - 1];
	}
	
	/**
	 * Extracts from the database all of the Methods required.
	 * @param stage			Number of bells in Method; also returns next lower
	 * 						if even	stage provided, for example stage 8 also
	 * 						returns Doubles methods.
	 * @param methodClass	Returns only Methods with selected class.  If null,
	 * 						all classes returned.
	 * @return				Array of Methods, handily appropriate for putting into
	 * 						a ListPreference
	 */
	public ArrayList<CharSequence[]> returnAll(int stage, String methodClass) {
		ArrayList<CharSequence[]> m = new ArrayList<CharSequence[]>();
		if ((stage & 1) == 0) {
			m.addAll(this.returnAll(stage - 1, methodClass));
		}
		final SQLiteDatabase db = getReadableDatabase();
		
		final String[] columns = {
				COLUMN_NAME,
				COLUMN_NOTATION,
		};
		
		String where = COLUMN_STAGE + "=" + stage;
		
		if (methodClass != null) {
			where += " and " + COLUMN_CLASS + "='" + methodClass + "'";
		}
		
		Cursor cursor = db.query(TABLE_METHODS, columns, where,
				null, null, null, COLUMN_NAME);

		if (cursor != null) {
			if (!cursor.moveToFirst()) {
				m.add(new CharSequence[] {"No results for " + stageToString(stage), "x",});
				db.close();
				return m;
			}
		} else {
			/* Should not happen! */
			m.add(new CharSequence[] {"Failure with cursor for "
					+ stageToString(stage), "x"});
			db.close();
			return m;
		}
		
		while (!cursor.isAfterLast()) {
			CharSequence[] row = new CharSequence[2];
			row[0] = cursor.getString(0);
			row[1] = cursor.getString(1);
			m.add(row);
			cursor.moveToNext();
		}
		
		cursor.close();
		db.close();
		
		return m;
	}
	
	/** Just drop and recreate the table */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_METHODS);
		onCreate(db);
	}
	
	private class UpdateDatabaseTask extends AsyncTask <Integer, Void, Void> {
		private NotificationCompat.Builder mBuilder;
        private NotificationManager mNotificationManager;

        private ArrayList<String[]> methodarray;

        protected void onPreExecute() {
            mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mBuilder = new NotificationCompat.Builder(context);
        }

		@Override
		protected Void doInBackground(Integer... numBells) {
            String numBellsString = "";
            for (int num : numBells) {
                numBellsString += (numBellsString.isEmpty() ?
                        "" : ", ") +
                        String.valueOf(num);
            }
            mBuilder.setContentTitle("Updating methods...")
                    .setContentText("Downloading from methods.ringing.org...")
                    .setSmallIcon(android.R.drawable.stat_sys_download);
            mBuilder.setProgress(0, 0, true);
            mNotificationManager.notify(1, mBuilder.build());
			SQLiteDatabase db = getWritableDatabase();

			for (int num : numBells) {
				methodarray = new ArrayList<String[]>();
				int page = 0;
				boolean morepagescoming = true;
				do {
					try {
						XmlPullParser parser = Xml.newPullParser();
						URL url = new URL(
								"http://methods.ringing.org/cgi-bin/simple.pl?pagesize=1000&fields=name|classes|pn|title&stage="
								+ Integer.toString(num) + "&page=" + Integer.toString(++page));
						InputStream input = url.openStream();
						parser.setInput(input, null);
						// ns_1:rows="8758"
						int event = parser.next();
						int attribute = 0;
						while (! parser.getAttributeName(attribute).equals("rows")) {
							attribute++;
						}
						int numberOfPages = Integer.parseInt(parser.getAttributeValue(attribute)) / 1000;
						if (page > numberOfPages) {
							morepagescoming = false;
						}
						while (event != XmlPullParser.END_DOCUMENT) {
							String method[] = new String[4];
							event = parser.next();
							if (event != XmlPullParser.START_TAG) {
								continue;
							}
							if (!"name".equals(parser.getName())) {
								continue;
							}
							parser.next();
							method[0] = parser.getText();
							// Get class
							do {
								event = parser.next();
								if (event != XmlPullParser.START_TAG) {
									continue;
								}
								if (!"classes".equals(parser.getName())) {
									continue;
								}
								parser.next();
								method[1] = parser.getText();
								break;
							} while (event != XmlPullParser.END_DOCUMENT);
							// Get title
							do {
								event = parser.next();
								if (event != XmlPullParser.START_TAG) {
									continue;
								}
								if (!"title".equals(parser.getName())) {
									continue;
								}
								parser.next();
								method[2] = parser.getText();
								break;
							} while (event != XmlPullParser.END_DOCUMENT);
							Log.d("Updating for... ", method[2]);
							mBuilder.setContentText("Updating for " + method[2]);
							mNotificationManager.notify(1, mBuilder.build());
							/* Get content */
							do {
								event = parser.next();
								if (event != XmlPullParser.START_TAG) {
									continue;
								}
								if ("block".equals(parser.getName())) {
									/* Asymmetric block */
									parser.next();
									method[3] = parser.getText();
									break;
								} else if ("symblock".equals(parser.getName())) {
									/* Symmetrical block */
									parser.next();
									method[3] = parser.getText();
									do {
										event = parser.next();
									} while (event != XmlPullParser.START_TAG);
									
									if ("symblock".equals(parser.getName())) {
										parser.next();
										method[3] += " lh " + parser.getText();
									}
									break;
								}
								/* Hmm, cruft in the way.  Keep skipping! */
							} while (event != XmlPullParser.END_DOCUMENT);
	
							methodarray.add(method);
						}
						input.close();
					} catch (MalformedURLException e) {
						e.printStackTrace();
						break;
					} catch (IOException e) {
						Log.w("Connection failed!", Integer.toString(num));
						mBuilder.setContentText("No Internet connection detected.")
                                .setProgress(0, 0, false)
                                .setSmallIcon(android.R.drawable.stat_sys_warning);
                        mNotificationManager.notify(1, mBuilder.build());
                        return null;
					} catch (XmlPullParserException e) {
						e.printStackTrace();
						break;
					}
				} while (morepagescoming);
				db.delete(TABLE_METHODS,
						COLUMN_STAGE + "=" +
						Integer.toString(num), null);
				
				/* Add plain hunt etc first */
				
				for (String[] m : methods[num]) {
					ContentValues row = new ContentValues();
					row.put(COLUMN_CLASS, "Plain");
					row.put(COLUMN_NAME, m[0]);
					row.put(COLUMN_NOTATION, m[1]);
					row.put(COLUMN_STAGE, Integer.toString(num));
					db.insert(TABLE_METHODS, "dummy", row);
				}
				
				for (String[] m : methodarray) {
					ContentValues row = new ContentValues();
					row.put(COLUMN_NAME, m[2]);
					row.put(COLUMN_CLASS, m[1]);
					row.put(COLUMN_NOTATION, m[3]);
					row.put(COLUMN_STAGE, Integer.toString(num));
					db.insert(TABLE_METHODS, "dummy", row);
				}
			}
			db.close();
            mBuilder.setContentText("Complete!")
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setProgress(0, 0, false);
            mNotificationManager.notify(1, mBuilder.build());
            return null;
		}
	}
}