package mobi.boilr.boilr.views.fragments;

import java.io.IOException;

import mobi.boilr.boilr.R;
import mobi.boilr.boilr.domain.AndroidNotify;
import mobi.boilr.boilr.utils.Conversions;
import mobi.boilr.libdynticker.core.Exchange;
import mobi.boilr.libdynticker.core.Pair;
import mobi.boilr.libpricealarm.UpperLimitSmallerOrEqualLowerLimitException;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;

public class PriceHitAlarmCreationFragment extends AlarmCreationFragment {

	private class OnPriceHitSettingsPreferenceChangeListener extends
	OnAlarmSettingsPreferenceChangeListener {

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			String key = preference.getKey();
			if(key.equals(PREF_KEY_UPPER_VALUE) || key.equals(PREF_KEY_LOWER_VALUE)) {
				preference.setSummary(newValue + " " + pairs.get(pairIndex).getExchange());
			} else {
				return super.onPreferenceChange(preference, newValue);
			}
			return true;
		}
	}

	@Override
	protected void updateDependentOnPair() {
		EditTextPreference[] edits = { upperLimitPref, lowerLimitPref };
		if(!recoverSavedInstance && lastValue != Double.POSITIVE_INFINITY) {
			for(EditTextPreference edit : edits) {
				edit.setText(Conversions.formatMaxDecimalPlaces(lastValue));
			}
		}
		String text;
		for(EditTextPreference edit : edits) {
			edit.setEnabled(true);
			text = edit.getText();
			if(text != null && !text.equals(""))
				edit.setSummary(text + " " + pairs.get(pairIndex).getExchange());
		}
	}

	@Override
	protected void disableDependentOnPair() {
		disableDependentOnPairHitAlarm();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		listener = new OnPriceHitSettingsPreferenceChangeListener();
		super.onCreate(savedInstanceState);

		removePrefs(hitAlarmPrefsToKeep);
		if(savedInstanceState == null) {
			EditTextPreference[] prefs = { upperLimitPref, lowerLimitPref, updateIntervalPref };
			for(EditTextPreference p : prefs) {
				p.setText(null);
			}
			updateIntervalPref.setSummary(sharedPrefs.getString(SettingsFragment.PREF_KEY_DEFAULT_UPDATE_INTERVAL, "") + " s");
		} else {
			// Upper and lower limit prefs summary will be updated by updateDependentOnPair()
			String updateInterval = updateIntervalPref.getText();
			if(updateInterval == null || updateInterval.equals("")) {
				updateIntervalPref.setSummary(sharedPrefs.getString(SettingsFragment.PREF_KEY_DEFAULT_UPDATE_INTERVAL, "") + " s");
			} else {
				updateIntervalPref.setSummary(updateInterval + " s");
			}
		}
		alarmTypePref.setValueIndex(0);
		specificCat.setTitle(alarmTypePref.getEntry());
		alarmTypePref.setSummary(alarmTypePref.getEntry());
	}

	@Override
	public void makeAlarm(int id, Exchange exchange, Pair pair, AndroidNotify notify)
			throws UpperLimitSmallerOrEqualLowerLimitException, IOException {
		String updateInterval = updateIntervalPref.getText();
		// Time is in seconds, convert to milliseconds
		long period = 1000 * Long.parseLong(updateInterval != null ? updateInterval :
			sharedPrefs.getString(SettingsFragment.PREF_KEY_DEFAULT_UPDATE_INTERVAL, ""));
		String upperLimitString = upperLimitPref.getText();
		double upperLimit;
		if(upperLimitString == null || upperLimitString.equals(""))
			upperLimit = Double.POSITIVE_INFINITY;
		else
			upperLimit = Double.parseDouble(upperLimitString);
		String lowerLimitString = lowerLimitPref.getText();
		double lowerLimit;
		if(lowerLimitString == null || lowerLimitString.equals(""))
			lowerLimit = Double.NEGATIVE_INFINITY;
		else
			lowerLimit = Double.parseDouble(lowerLimitString);
		if(mBound) {
			mStorageAndControlService.createAlarm(id, exchange, pair, period, notify, upperLimit, lowerLimit);
		} else {
			throw new IOException(enclosingActivity.getString(R.string.not_bound, "PriceHitAlarmCreationFragment"));
		}
	}
}
