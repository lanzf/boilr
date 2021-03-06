package mobi.boilr.boilr.utils;

import mobi.boilr.boilr.R;
import mobi.boilr.boilr.activities.AlarmListActivity;
import mobi.boilr.boilr.activities.NotificationActivity;
import mobi.boilr.boilr.activities.SettingsActivity;
import mobi.boilr.libdynticker.core.Pair;
import mobi.boilr.libpricealarm.Alarm;
import mobi.boilr.libpricealarm.Alarm.Direction;
import mobi.boilr.libpricealarm.PriceChangeAlarm;
import mobi.boilr.libpricealarm.PriceHitAlarm;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/* Based on Android DeskClock AlarmNotifications. */
public final class Notifications {

	private static final int noInternetNotificationID = 432191926;
	private static Notification.Builder noInternetNotification = null;
	public static boolean allowNoInternetNotification = true;
	private static final Bitmap upArrowBitmap = textAsBitmap("▲", 100, Color.GREEN);
	private static final Bitmap downArrowBitmap = textAsBitmap("▼", 100, Color.RED);

	private static Notification.Builder setCommonNotificationProps(Context context, Alarm alarm,
			String firingReason) {
		Notification.Builder notification = new Notification.Builder(context)
			.setContentTitle(context.getString(R.string.boilr_alarm))
			.setContentText(firingReason)
			.setSmallIcon(R.drawable.ic_action_alarms)
			.setLights(0xFFFF0000, 333, 333) // Blink in red ~3 times per second.
			.setOngoing(false)
			.setAutoCancel(true);
		if(isDirectionUp(alarm)) {
			notification.setLargeIcon(upArrowBitmap);
		} else {
			notification.setLargeIcon(downArrowBitmap);
		}
		Intent viewAlarmsIntent = new Intent(context, AlarmListActivity.class);
		viewAlarmsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		notification.setContentIntent(PendingIntent.getActivity(context, alarm.getId(), viewAlarmsIntent, PendingIntent.FLAG_UPDATE_CURRENT));
		return notification;
	}

	public static void showLowPriorityNotification(Context context, Alarm alarm) {
		String firingReason = getFiringReason(context, alarm);
		Notification.Builder notification = setCommonNotificationProps(context, alarm, firingReason);
		notification.setPriority(Notification.PRIORITY_DEFAULT);
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(alarm.hashCode());
		nm.notify(alarm.hashCode(), notification.build());
	}

	public static void showAlarmNotification(Context context, Alarm alarm) {
		int alarmID = alarm.getId();
		// Close dialogs and window shade, so this will display
		context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
		String firingReason = getFiringReason(context, alarm);
		Notification.Builder notification = setCommonNotificationProps(context, alarm, firingReason);
		notification.setPriority(Notification.PRIORITY_MAX);

		// Setup fullscreen intent
		Intent fullScreenIntent = new Intent(context, NotificationActivity.class);
		fullScreenIntent.putExtra("alarmID", alarmID);
		fullScreenIntent.putExtra("firingReason", firingReason);
		fullScreenIntent.putExtra("canKeepMonitoring", canKeepMonitoring(alarm));
		if(isDirectionUp(alarm)) {
			fullScreenIntent.putExtra("arrow", "▲");
			fullScreenIntent.putExtra("colour", Color.GREEN);
		} else {
			fullScreenIntent.putExtra("arrow", "▼");
			fullScreenIntent.putExtra("colour", Color.RED);
		}
		fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
		notification.setFullScreenIntent(PendingIntent.getActivity(context, alarmID, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT), true);

		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(alarm.hashCode());
		nm.notify(alarm.hashCode(), notification.build());
	}

	private static boolean isDirectionUp(Alarm alarm) {
		boolean isDirectionUp;
		if(alarm instanceof PriceHitAlarm) {
			/*
			 * PriceHitAlarm has no valid direction if it triggers on the first
			 * time it fetches last price.
			 */
			PriceHitAlarm hitAlarm = (PriceHitAlarm) alarm;
			isDirectionUp = hitAlarm.wasUpperLimitHit();
		} else {
			isDirectionUp = alarm.getDirection() == Direction.UP;
		}
		return isDirectionUp;
	}

	/*
	 * By Ted Hopp https://stackoverflow.com/a/8799344
	 */
	private static Bitmap textAsBitmap(String text, float textSize, int textColor) {
		Paint paint = new Paint();
		paint.setTextSize(textSize);
		paint.setColor(textColor);
		paint.setTextAlign(Paint.Align.LEFT);
		int width = (int) (paint.measureText(text) + 0.5f); // round
		float baseline = (int) (-paint.ascent() + 0.5f); // ascent() is negative
		int height = (int) (baseline + paint.descent() + 0.5f);
		Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(image);
		canvas.drawText(text, 0, baseline, paint);
		return image;
	}

	private static boolean canKeepMonitoring(Alarm alarm) {
		if(alarm instanceof PriceChangeAlarm)
			return true;
		else
			return false;
	}

	public static void showNoInternetNotification(Context context) {
		if(allowNoInternetNotification) {
			if(noInternetNotification == null) {
				Intent changeSettingsIntent = new Intent(context, SettingsActivity.class);
				changeSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				noInternetNotification = new Notification.Builder(context)
						.setContentTitle(context.getString(R.string.no_internet))
						.setContentText(context.getString(R.string.no_updates))
						.setSmallIcon(R.drawable.ic_action_warning)
						.setOngoing(false)
						.setAutoCancel(true)
						.setPriority(Notification.PRIORITY_DEFAULT)
						.setWhen(0)
						.setContentIntent(PendingIntent.getActivity(context, noInternetNotificationID, changeSettingsIntent, PendingIntent.FLAG_UPDATE_CURRENT));
			}
			NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			nm.notify(noInternetNotificationID, noInternetNotification.build());
		}
	}

	private static String getFiringReason(Context context, Alarm alarm) {
		Pair pair = alarm.getPair();
		if(alarm instanceof PriceHitAlarm) {
			return pair.getCoin() + " " + context.getString(R.string.at) + " " + Conversions.formatMaxDecimalPlaces(alarm.getLastValue()) + " "
					+ pair.getExchange() + "\n" + context.getString(R.string.in) + " " + alarm.getExchange().getName();
		} else if(alarm instanceof PriceChangeAlarm) {
			PriceChangeAlarm changeAlarm = (PriceChangeAlarm) alarm;
			String reason = pair.getCoin() + "/" + pair.getExchange() + " " + context.getString(R.string.had) + "\n";
			if(changeAlarm.isPercent())
				reason += Conversions.format2DecimalPlaces(changeAlarm.getLastChange()) + "%";
			else
				reason += Conversions.formatMaxDecimalPlaces(changeAlarm.getLastChange()) + " " + pair.getExchange();
			reason += " " + context.getString(R.string.change) + "\n" + context.getString(R.string.in) + " " + alarm.getExchange().getName() + "\n" + context.getString(R.string.during) + " "
					+ Conversions.formatMilis(changeAlarm.getElapsedMilis());
			return reason;
		}
		return "Could not retrieve firing reason.";
	}

	public static void clearNotification(Context context, int alarmID) {
		Log.d("Clearing notifications for alarm instance: " + alarmID);
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(alarmID);
	}

	public static void clearNoInternetNotification(Context context) {
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(noInternetNotificationID);
	}

	public static void rebuildNoInternetNotification() {
		noInternetNotification = null;
	}
}
