package com.anysoftkeyboard.dictionaries;

import com.anysoftkeyboard.AnyKeyboardContextProvider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.UserDictionary.Words;
import android.text.TextUtils;
import android.util.Log;

class AndroidUserDictionary extends UserDictionaryBase {

	private static final String[] PROJECTION = {
        Words._ID,
        Words.WORD,
        Words.FREQUENCY
    };

	private static final int INDEX_WORD = 1;
    private static final int INDEX_FREQUENCY = 2;

	private ContentObserver mObserver;
	private final String mLocale;
	
    public AndroidUserDictionary(AnyKeyboardContextProvider context, String locale)
    {
    	super("AndroidUserDictionary", context);
    	mLocale = locale;
    }

	protected void closeAllResources() {
		if (mObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mObserver);
            mObserver = null;
        }
	}
	
	@Override
	public void loadDictionary() {
		loadDictionaryAsync();
	}

	protected void loadDictionaryAsync() {

		Cursor cursor = mContext.getContentResolver().query(Words.CONTENT_URI, PROJECTION,
			"("+Words.LOCALE+" IS NULL) or ("+Words.LOCALE+"=?)",
        	new String[] { mLocale }, null);
		if (cursor == null)
			throw new RuntimeException("No built-in Android dictionary!");
		
		addWords(cursor);
		
		// Perform a managed query. The Activity will handle closing and requerying the cursor
        // when needed.
        ContentResolver cres = mContext.getContentResolver();

        cres.registerContentObserver(Words.CONTENT_URI, true, mObserver = new ContentObserver(null) {
            @Override
            public void onChange(boolean self) {
                mRequiresReload = true;
            }
        });
	}

	private void addWords(Cursor cursor) {
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String word = cursor.getString(INDEX_WORD);
                int frequency = cursor.getInt(INDEX_FREQUENCY);
                // Safeguard against adding really long words. Stack may overflow due
                // to recursion
                if (word.length() < MAX_WORD_LENGTH) {
                	addWordFromStorage(word, frequency);
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        
    }

	protected void AddWordToStorage(String word, int frequency) {
		if (TextUtils.isEmpty(word)) {
			return;
		}
		
		if (frequency < 0) frequency = 0;
		if (frequency > 255) frequency = 255;
		
		ContentValues values = new ContentValues(4);
		values.put(Words.WORD, word);
		values.put(Words.FREQUENCY, frequency);
		values.put(Words.LOCALE, mLocale);
		values.put(Words.APP_ID, 0); // TODO: Get App UID
		
		Uri result = mContext.getContentResolver().insert(Words.CONTENT_URI, values );
		Log.i(TAG, "Added the word '"+word+"' into Android's user dictionary. Result "+result);
		//Words.addWord(mContext, word, frequency, Words.LOCALE_TYPE_CURRENT);
	}
}