package com.igel.expenses.tracker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public final class CalendarUtils {	

	private static DateFormat mDateFormat = new SimpleDateFormat("EEE, dd.MM.yyyy");

	public static String getDateString(Calendar date) {
		return mDateFormat.format(date.getTime());
	}
	
	public static Calendar getFirstDayOfMonth(Calendar date) {
        Calendar calendar = (Calendar)date.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.clear(Calendar.HOUR);
        calendar.clear(Calendar.HOUR_OF_DAY);
        calendar.clear(Calendar.MINUTE);
        calendar.clear(Calendar.SECOND);
        calendar.clear(Calendar.MILLISECOND);
        return calendar;
	}

	public static Calendar getFirstDayOfNextMonth(Calendar date) {
        Calendar calendar = (Calendar)date.clone();
		removeTimeFields(calendar);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MONTH, 1);
        return calendar;
	}

	public static Calendar getLastDayOfMonth(Calendar date) {
        Calendar calendar = getFirstDayOfMonth(date);
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        return calendar;
	}
	
	public static Calendar getEndOfDay(Calendar date) {
		Calendar calendar = (Calendar)date.clone();
		removeTimeFields(calendar);
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		return calendar;
	}
	
	private static void removeTimeFields(Calendar date) {
		date.clear(Calendar.HOUR);
		date.clear(Calendar.HOUR_OF_DAY);
		date.clear(Calendar.MINUTE);
		date.clear(Calendar.SECOND);
		date.clear(Calendar.MILLISECOND);		
	}
}
