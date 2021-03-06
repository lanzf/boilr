package mobi.boilr.boilr.views.fragments;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import mobi.boilr.boilr.R;
import mobi.boilr.boilr.domain.AndroidNotify;
import mobi.boilr.boilr.utils.Conversions;
import mobi.boilr.libdynticker.core.Exchange;
import mobi.boilr.libdynticker.core.Pair;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;

public class PriceChangeAlarmCreationFragment extends AlarmCreationFragment {
	private class OnPriceChangeSettingsPreferenceChangeListener extends
	OnAlarmSettingsPreferenceChangeListener {

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			String key = preference.getKey();
			if(key.equals(PREF_KEY_SPIKE_ALERT)) {
				isSpikeAlert = (Boolean) newValue;
			} else if(key.equals(PREF_KEY_CHANGE_IN_PERCENTAGE)) {
				isPercentage = (Boolean) newValue;
				updateChangeValueSummary();
			} else if(key.equals(PREF_KEY_CHANGE_VALUE)) {
				preference.setSummary(getChangeValueSummary((String) newValue));
			} else if(key.equals(PREF_KEY_TIME_FRAME)) {
				preference.setSummary(Conversions.buildMinToDaysSummary((String) newValue, enclosingActivity));
			} else {
				return super.onPreferenceChange(preference, newValue);
			}
			return true;
		}
	}

	@Override
	protected void updateDependentOnPair() {
		updateDependentOnPairChangeAlarm();
	}

	@Override
	protected void disableDependentOnPair() {
		disableDependentOnPairChangeAlarm();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		listener = new OnPriceChangeSettingsPreferenceChangeListener();
		super.onCreate(savedInstanceState);

		removePrefs(changeAlarmPrefsToKeep);
		isSpikeAlert = spikeAlertPref.isChecked();
		isPercentage = isPercentPref.isChecked();
		updateIntervalPref.setDependency(PREF_KEY_SPIKE_ALERT);
		if(savedInstanceState == null) {
			changePref.setText(null);
			timeFramePref.setText(null);
			timeFramePref.setSummary(Conversions.buildMinToDaysSummary(sharedPrefs.getString(SettingsFragment.PREF_KEY_DEFAULT_TIME_FRAME, ""),
					enclosingActivity));
			if(isSpikeAlert) {
				updateIntervalPref.setText(null);
				updateIntervalPref.setSummary(sharedPrefs.getString(SettingsFragment.PREF_KEY_DEFAULT_UPDATE_INTERVAL, "") + " s");
			}
		} else {
			// Change value pref summary will be updated by updateDependentOnPair()

			String timeFrame = timeFramePref.getText();
			if(timeFrame == null || timeFrame.equals("")) {
				timeFramePref.setSummary(Conversions.buildMinToDaysSummary(
						sharedPrefs.getString(SettingsFragment.PREF_KEY_DEFAULT_TIME_FRAME, ""), enclosingActivity));
			} else {
				timeFramePref.setSummary(Conversions.buildMinToDaysSummary(timeFrame, enclosingActivity));
			}
			if(isSpikeAlert) {
				String updateInterval = updateIntervalPref.getText();
				if(updateInterval == null || updateInterval.equals("")) {
					updateIntervalPref.setSummary(sharedPrefs.getString(SettingsFragment.PREF_KEY_DEFAULT_UPDATE_INTERVAL, "") + " s");
				} else {
					updateIntervalPref.setSummary(updateInterval + " s");
				}
			}
		}
		alarmTypePref.setValueIndex(1);
		specificCat.setTitle(alarmTypePref.getEntry());
		alarmTypePref.setSummary(alarmTypePref.getEntry());
	}

	@Override
	public void makeAlarm(int id, Exchange exchange, Pair pair, AndroidNotify notify)
			throws InterruptedException, ExecutionException, IOException {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(enclosingActivity);
		String timeFrameString = ((EditTextPreference) findPreference(PREF_KEY_TIME_FRAME)).getText();
		// Time is in minutes, convert to milliseconds
		long timeFrame = Conversions.MILIS_IN_MINUTE
			* Long.parseLong(timeFrameString != null ? timeFrameString :
				sharedPreferences.getString(SettingsFragment.PREF_KEY_DEFAULT_TIME_FRAME, ""));
		String changeValueString = ((EditTextPreference) findPreference(PREF_KEY_CHANGE_VALUE)).getText();
		double change;
		if(changeValueString == null || changeValueString.equals(""))
			change = Double.POSITIVE_INFINITY;
		else
			change = Double.parseDouble(changeValueString);
		long updateInterval = 3000;
		if(isSpikeAlert) {
			String updateIntervalString = updateIntervalPref.getText();
			// Time is in seconds, convert to milliseconds
			updateInterval = 1000 * Long.parseLong(updateIntervalString != null ? updateIntervalString : sharedPrefs.getString(
					SettingsFragment.PREF_KEY_DEFAULT_UPDATE_INTERVAL, ""));
		}
		if(mBound) {
			if(isPercentage) {
				float percent = (float) change;
				if(isSpikeAlert) {
					mStorageAndControlService.createAlarm(id, exchange, pair, updateInterval, notify, percent, timeFrame);
				} else {
					mStorageAndControlService.createAlarm(id, exchange, pair, timeFrame, notify, percent);
				}
			} else {
				if(isSpikeAlert) {
					mStorageAndControlService.createAlarm(id, exchange, pair, updateInterval, notify, change, timeFrame);
				} else {
					mStorageAndControlService.createAlarm(id, exchange, pair, timeFrame, notify, change);
				}
			}
		} else {
			throw new IOException(enclosingActivity.getString(R.string.not_bound, "PriceChangeAlarmCreationFragment"));
		}
	}
}
