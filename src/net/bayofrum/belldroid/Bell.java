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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.media.SoundPool;
import android.os.Handler;
import android.view.Gravity;
import android.widget.Button;

@SuppressLint("ViewConstructor")
public class Bell extends Button {

	private final int bellNumber;
    private final boolean PIANO_SOUNDS = false;

    /** Starting on B natural, centihertz */
    private final int BELL_FREQUENCIES[] = {
            /* b    c#      d# */
            49388,  55437,  62225,
            /* e    f#      g# */
            65925,  73999,  83061,
            /* a#   b       c# */
            93233,  98777,  110873,
            /* d#   e       f# */
            124451, 131851, 147998,
    };
	
	private int place;
    private float rate = 1.0f;
	private Stroke stroke = Stroke.HAND;

    public enum Stroke { HAND, BACK };
	public final int soundId;

	private final SoundPool soundPool;
	
	public static int charToInt(char c) {
		switch (c) {
		case '0':
			return 10;
		case 'E':
			return 11;
		case 'T':
			return 12;
		default:
			return c - '0';
		}
	}
	
	/**
	 * Returns a Bell object, based on TextView.  Best
	 * stored as a ring of bells; e.g. Bell[] bells.
	 * 
	 * @param context	context
	 * @param bellNumber	The number for this Bell object
	 * @param maxNumberOfBells	Maximum number of Bell objects -- used
	 * 							to find the sound file
	 * @param numberOfBells		Also used to find the sound file
	 * @param soundPool			a SoundPool to which the Bell sound is added
	 */
	public Bell(Context context, int bellNumber, int maxNumberOfBells,
			int numberOfBells, SoundPool soundPool) {
		super(context);

		AssetManager assets = context.getAssets();
		int soundId = 0;

		this.bellNumber = bellNumber;
		this.soundPool = soundPool;
		place = this.bellNumber;
		this.setText(Integer.toString(this.bellNumber));
		this.setGravity(
				Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);		
		this.setClickable(true);
		if (PIANO_SOUNDS == true) {
            try {
                AssetFileDescriptor soundfile = assets.openFd("piano-based/" +
                        String.valueOf(
                                bellNumber + maxNumberOfBells - numberOfBells
                        ) + ".ogg");
                soundId = soundPool.load(soundfile, 1);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } else {
            try {
                AssetFileDescriptor soundfile = assets.openFd("tower-bells/towerbell.wav");
                soundId = soundPool.load(soundfile, 1);
                rate = (float)BELL_FREQUENCIES[Math.abs(numberOfBells - bellNumber)] /
                        (float)BELL_FREQUENCIES[6];
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
		this.soundId = soundId;
	}
	
	/**
	 * Returns the number of this Bell as int.
	 * 
	 * @return the number of this Bell, starting with 1.
	 */
	public int getNumber() {
		return this.bellNumber;
	}
	
	/**
	 * Returns the name of this Bell as a String;
	 * capitalised as a proper noun.
	 * 
	 * @return the name of this Bell.
	 */
	public String getName() {
		final String[] bellNames = {
				".", "One", "Two", "Three", "Four", "Five", "Six",
				"Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve",
		};

		return bellNames[this.bellNumber];
	}
	
	/**
	 * Returns the place value of this Bell
	 * as an int.
	 * 
	 * @return the place value, starting with 1.
	 */
	public int getPlace() {
		return this.place;
	}

	
	/**
	 * Returns the place value of this Bell
	 * as a character, with single digits, 10->0, 11->E, 12->T
	 */
	public char getPlaceAsChar() {
		int p = this.getPlace();
		switch(p) {
		case 12:
			return 'T';
		case 11:
			return 'E';
		case 10:
			return '0';
		default:
			return Integer.toString(p).charAt(0);
		}
	}

	/**
	 * Sets the place value of this Bell
	 * as int.
	 * 
	 * @param p the place value of this Bell, starting with 1.
	 */
	public void setPlace(int p) {
		this.place = p;
	}
	
	/**
	 * Returns true if this Bell is at handstroke.
	 * 
	 * @return true at handstroke, false at backstroke.
	 */
	public boolean atHandStroke() {
		return this.stroke == Stroke.HAND;
	}
	
	/**
	 * Returns true if this Bell is at backstroke.
	 * 
	 * @return true at backstroke, false at handstroke.
	 */
	public boolean atBackStroke() {
		return this.stroke == Stroke.BACK;
	}
	
	/**
	 * Moves this Bell's place up by one.
	 * 
	 * The programmer is responsible for finding
	 * the previous Bell to moveDown().
	 */
	public void moveUp() {
		this.place++;
	}
	
	/**
	 * Moves this Bell's place down by one.
	 * 
	 * The programmer is responsible for finding
	 * the next Bell to moveUp().
	 */
	public void moveDown() {
		this.place--;
	}
	
	/**
	 * Rings this Bell, resulting in a switch of Stroke,
	 * and a change in colour to represent that.
	 * 
	 * @param bonger	Handler for scheduling the sound.  Pass null for silence.
	 * @param delay		Delay between Stroke and sound.
	 * @return			the place value of the Bell.
	 */
	public int ring(Handler bonger, int delay) {
		switch (stroke) {
		case HAND:
			this.setTextColor(Color.RED);
			stroke = Stroke.BACK;
			break;
		case BACK:
			this.setTextColor(Color.BLACK);
			stroke = Stroke.HAND;
			break;
		}
		if (bonger != null) {
			bonger.postDelayed(new Runnable() {
				@Override
				public void run() {
					soundPool.play(soundId, 0.99f, 0.99f, 1, 0, rate);
				}
			}, delay);
		}
		return place;
	}
}
