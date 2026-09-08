package com.example.queuefree;

import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.Locale;

/**
 * LanguageHelper - Manages multi-language support
 * Supports English and Kannada with Text-to-Speech
 */
public class LanguageHelper {

    private static final String PREFS_NAME = "QueueFreePrefs";
    private static final String KEY_LANGUAGE = "selected_language";

    public static final String LANG_ENGLISH = "en";
    public static final String LANG_KANNADA = "kn";

    private Context context;
    private TextToSpeech tts;
    private String currentLanguage;

    public LanguageHelper(Context context) {
        this.context = context;
        loadLanguagePreference();
        initializeTTS();
    }

    /**
     * Initialize Text-to-Speech engine
     */
    private void initializeTTS() {
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    setTTSLanguage(currentLanguage);
                }
            }
        });
    }

    /**
     * Set TTS language
     */
    private void setTTSLanguage(String languageCode) {
        Locale locale;
        if (LANG_KANNADA.equals(languageCode)) {
            locale = new Locale("kn", "IN"); // Kannada
        } else {
            locale = Locale.ENGLISH;
        }

        int result = tts.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(context, "Language not supported", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Speak text in selected language
     */
    public void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    /**
     * Get token notification text in selected language
     */
    public String getTokenNotification(int tokenNumber) {
        if (LANG_KANNADA.equals(currentLanguage)) {
            return "ನಿಮ್ಮ ಟೋಕನ್ ಸಂಖ್ಯೆ " + tokenNumber;
        } else {
            return "Your token number is " + tokenNumber;
        }
    }

    /**
     * Get turn notification in selected language
     */
    public String getTurnNotification() {
        if (LANG_KANNADA.equals(currentLanguage)) {
            return "ನಿಮ್ಮ ಸರದಿ ಬಂದಿದೆ. ದಯವಿಟ್ಟು ಕೌಂಟರ್‌ಗೆ ಬನ್ನಿ";
        } else {
            return "Your turn has arrived. Please proceed to the counter";
        }
    }

    /**
     * Save language preference
     */
    public void setLanguage(String languageCode) {
        currentLanguage = languageCode;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
        setTTSLanguage(languageCode);
    }

    /**
     * Load language preference
     */
    private void loadLanguagePreference() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentLanguage = prefs.getString(KEY_LANGUAGE, LANG_ENGLISH);
    }

    /**
     * Get current language
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * Shutdown TTS engine
     */
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
