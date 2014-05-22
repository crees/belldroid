package net.bayofrum.belldroid;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Xml;

public class Method extends SQLiteOpenHelper {

	public static final String[][][] methods =
		{
			{{}}, {{}}, {{}}, // 0-2 bells (!)
			{
				{"Plain Hunt Singles",
					"3.1.3.1.3.1"},
				{"Short Cure for Melancholy",
					"3.13.1.13.3.13.1.13.3.13.1"}
			}, // Singles
			{
				{"Plain Hunt Minimus",
					"x1x1x1x1"},
			}, // Minimus
			{
				{"Plain Hunt Doubles",
					"5.1.5.1.5 le 1"},
				{"Grandsire Doubles",
					"3.1.5.1.5.1.5.1.5.1"},
			}, // Doubles
			{
				{"Plain Hunt Minor",
					"x1x1x1x1x1x1"},
			}, // Minor
			{
				{"Plain Hunt Triples", 
					"7.1.7.1.7.1.7.1.7.1.7.1.7.1"},
			}, // Triples
			{
				{"Plain Hunt Major",
					"x1x1x1x1x1x1x1x1"},
			}, // Major
			{
				{"Plain Hunt Caters",
					"9.1.9.1.9.1.9.1.9.1.9.1.9.1.9.1.9.1"},
			}, // Caters
			{
				{"Plain Hunt Royal",
					"x1x1x1x1x1x1x1x1x1"},
			}, // Royal
			{
				{"Plain Hunt Cinques", 
					"E.1.E.1.E.1.E.1.E.1.E.1.E.1.E.1.E.1.E.1.E.1"},
			}, // Cinques
			{
				{"Plain Hunt Maximus",
					"x1x1x1x1x1x1x1x1x1x1x1x1"},
				{"Cambridge Surprise Maximus",
					"x3x4x25x36x47x58x69x70x8x9x0xE le 2"},
			}, // Maximus
		};
	
	public static final String TABLE_METHODS = "methods";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_CLASS = "class";
	public static final String COLUMN_STAGE = "stage";
	public static final String COLUMN_NOTATION = "notation";

	public final Context c;
	
	private static final String DATABASE_NAME = "methods.db";
	private static final int DATABASE_VERSION = 1;

	private static final String DATABASE_CREATE = "create table "
	    + TABLE_METHODS + "(" + 
		COLUMN_ID + " integer primary key autoincrement, " +
	    COLUMN_NAME + " text not null, " +
		COLUMN_CLASS + " text not null, " +
	    COLUMN_STAGE + " integer not null, " +
		COLUMN_NOTATION + " text not null, " +
		"dummy text);";
	
	public Method(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		c = context;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_CREATE);
		for (int i = 3; i <= 12; i++) {
			for (String[] m : methods[i]) {
				ContentValues row = new ContentValues();
				row.put(COLUMN_NAME, m[0]);
				row.put(COLUMN_NOTATION, m[1]);
				row.put(COLUMN_STAGE, i);
				row.put(COLUMN_CLASS, "plain");
				db.insert(TABLE_METHODS, "dummy", row);
			}
		}
	}
	
	public void updateDb(int numBells) {
		UpdateDatabaseTask updater = new UpdateDatabaseTask();

		if ((numBells & 1) == 0 && numBells != 4)
			updater.execute(numBells - 1, numBells);
		else
			updater.execute(numBells);
	}
	
	public ArrayList<CharSequence[]> returnAll(int stage) {
		ArrayList<CharSequence[]> m = new ArrayList<CharSequence[]>();

		if ((stage & 1) == 0)
			m.addAll(this.returnAll(stage - 1));
		
		SQLiteDatabase db = getReadableDatabase();
		
		String[] columns = {
				COLUMN_NAME,
				COLUMN_NOTATION,
				COLUMN_STAGE,
				COLUMN_CLASS,
		};
		Cursor cursor = db.query(TABLE_METHODS, columns,
				COLUMN_STAGE + "=" + stage,
				null, null, null, null);

		if (cursor != null)
			cursor.moveToFirst();
		else {
			/* Should not happen! */
			m.add(new CharSequence[] {"Database empty for this number???", ""});
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
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO, nothing new planned, so no need for this yet
	}
	
	private class UpdateDatabaseTask extends AsyncTask <Integer, Void, Void> {

		private ArrayList<String[]> methodarray;
		
		@Override
		protected Void doInBackground(Integer... numBells) {
			XmlPullParser parser = Xml.newPullParser();
			
			SQLiteDatabase db = getWritableDatabase();
			
			for (int num : numBells) {
				try {
					methodarray = new ArrayList<String[]>();
					URL url = new URL("http://methods.ringing.org/cgi-bin/simple.pl?performances/firsttower/date%3E1000&fields=title|pn&stage="
							+ Integer.toString(num));
					InputStream input = url.openStream();
					parser.setInput(input, null);
					int event = parser.next();
					while (event != XmlPullParser.END_DOCUMENT) {
						String method[] = new String[2];
						event = parser.next();
						if (event != XmlPullParser.START_TAG)
							continue;
						if (!"title".equals(parser.getName()))
							continue;
						parser.next();
						method[0] = parser.getText();
						do {
							event = parser.next();
							if (event != XmlPullParser.START_TAG)
								continue;
							if (!"symblock".equals(parser.getName()))
								continue;
							parser.next();
							method[1] = parser.getText();
							while (parser.next() != XmlPullParser.START_TAG)
								;
							if ("symblock".equals(parser.getName())) {
								parser.next();
								String second = parser.getText();
								if (second.length() < method[1].length())
									method[1] += " lh " + second;
								else
									method[1] = second + " lh " + method[1];
							}
							break;
						} while (event != XmlPullParser.END_DOCUMENT);

						methodarray.add(method);
					}
					input.close();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					// Not connected?  Just silently fail for now.
					return null;
				} catch (XmlPullParserException e) {
					e.printStackTrace();
				}
				
				db.delete(TABLE_METHODS,
						COLUMN_STAGE + "=" +
						Integer.toString(num), null);
				
				for (String[] m : methodarray) {
					ContentValues row = new ContentValues();
					row.put(COLUMN_NAME, m[0]);
					row.put(COLUMN_NOTATION, m[1]);
					row.put(COLUMN_STAGE, Integer.toString(num));
					row.put(COLUMN_CLASS, "plain");
					db.insert(TABLE_METHODS, "dummy", row);
				}
			}
			db.close();
			return null;
		}
	}
}
