/* ========================================================================
 * JCommon : a free general purpose class library for the Java(tm) platform
 * ========================================================================
 *
 * (C) Copyright 2000-2006, by Object Refinery Limited and Contributors.
 *
 * Project Info:  http://www.jfree.org/jcommon/index.html
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc.
 * in the United States and other countries.]
 *
 * ---------------
 * SerialDate.java
 * ---------------
 * (C) Copyright 2001-2006, by Object Refinery Limited.
 *
 * Original Author:  David Gilbert (for Object Refinery Limited);
 * Contributor(s):   -;
 *
 * $Id: SerialDate.java,v 1.9 2011/10/17 20:08:22 mungady Exp $
 *
 * Changes (from 11-Oct-2001)
 */
package main;

import java.io.Serializable;
import java.text.*;
import java.util.*;
/**
 *  An abstract class that defines our requirements for manipulating dates,
 *  without tying down a particular implementation.
 *  <pre>
 *      Requirement 1 : match at least what Excel does for dates;
 *      Requirement 2 : the date represented by the class is immutable;
 *  </pre>
 *  <pre>
 *      Why not just use java.util.DayDate?  We will, when it makes sense.  At times,
 *      java.util.DayDate can be *too* precise - it represents an instant in time,
 *      accurate to 1/1000th of a second (with the date itself depending on the
 *      time-zone).  Sometimes we just want to represent a particular day (e.g. 21
 *      January 2015) without concerning ourselves about the time of day, or the
 *      time-zone, or anything else.  That's what we've defined DayDate for.
 *  </pre>
 *  You can call getInstance() to get a concrete subclass of DayDate,
 *  without worrying about the exact implementation.
 *
 * @author David Gilbert
 */
public abstract class DayDate implements Comparable,
                                    Serializable {
    public static enum Month {
        JANUARY(1),
        FEBRUARY(2),
        MARCH(3),
        APRIL(4),
        MAY(5),
        JUNE(6),
        JULY(7),
        AUGUST(8),
        SEPTEMBER(9),
        OCTOBER(10),
        NOVEMBER(11),
        DECEMBER(12);

        public final int index;

        Month(int index) {
            this.index = index;
        }

        public static Month make(int monthIndex) {
            for (Month m : Month.values()) {
                if (m.index == monthIndex)
                    return m;
            }
            throw new IllegalArgumentException(
                    "Invalid month index " + monthIndex);
        }
    }

    public static final DateFormatSymbols
        DATE_FORMAT_SYMBOLS = new SimpleDateFormat("", Locale.ENGLISH)
            .getDateFormatSymbols();

    public static enum Day {
        MONDAY(Calendar.MONDAY),
        TUESDAY(Calendar.TUESDAY),
        WEDNESDAY(Calendar.WEDNESDAY),
        THURSDAY(Calendar.THURSDAY),
        FRIDAY(Calendar.FRIDAY),
        SATURDAY(Calendar.SATURDAY),
        SUNDAY(Calendar.SUNDAY);

        private final int index;
        private  static DateFormatSymbols dateFormatSymbols
                = new DateFormatSymbols(Locale.ENGLISH);
        Day(int day) {
            index = day;
        }

        public static Day fromInt(int index) throws IllegalArgumentException {
            for (Day d : Day.values()) {
                if (d.index == index)
                    return d;
            }
            throw new IllegalArgumentException(
                    String.format("Illegal day index: %d", index));
        }

        public int toInt() {
            return index;
        }
    }

    static final int[] LAST_DAY_OF_MONTH =
            {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    static final int[] AGGREGATE_DAYS_TO_END_OF_MONTH =
            {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365};

    static final int[] AGGREGATE_DAYS_TO_END_OF_PRECEDIND_MONTH =
            {0, 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365};

    static final int[] LEAP_YEAR_AGGREGATE_DAYS_TO_END_OF_MONTH =
            {0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335, 366};

    static final int[] LEAP_YEAR_AGGREGATE_DAYS_TO_END_OF_PRECEDIND_MONTH =
            {0, 0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335, 366};

    public static final int FIRST_WEEK_IN_MONTH = 1;

    public static final int SECOND_WEEK_IN_MONTH = 2;

    public static final int THIRD_WEEK_IN_MONTH = 3;

    public static final int FOURTH_WEEK_IN_MONTH = 4;

    public static final int LAST_WEEK_IN_MONTH = 0;

    public static final int INCLUDE_NONE = 0;

    public static final int INCLUDE_FIRST = 1;

    public static final int INCLUDE_SECOND = 2;

    public static final int INCLUDE_BOTH = 3;

    public static final int PRECEDING = -1;

    public static final int NEAREST = 0;

    public static final int FOLLOWING = 1;

    private String description;

    /** Default Constructor */
    protected DayDate() {}

    /**
     * Converts the supplied string to a day of the week
     * @param s a string representing the day of the week
     * @return <code>-1</code> if the string is not convertable, the day of
     *          the week otherwise.
     */
    public static int stringToWeekdayCode(String s) {
        final String[] shortWeekdayNames =
                DATE_FORMAT_SYMBOLS.getShortWeekdays();
        final String[] weekDayNames = DATE_FORMAT_SYMBOLS.getWeekdays();

        int result = -1;
        s = s.trim();
        for (int i = 0; i < weekDayNames.length; i++) {
            if (s.equalsIgnoreCase(shortWeekdayNames[i])) {
                result = i;
                break;
            }
            if (s.equalsIgnoreCase(weekDayNames[i])) {
                result = i;
                break;
            }
        }
        return result;
    }

    /**
     * Returns a string representing the supplied day-of-the-week
     * Need to find a better approach
     * @param weekday the day of the week
     * @return a string representing the supplied day-of-the-week
     */
    public static String weekdayCodeToString(final int weekday) {
        final String[] weekdays = DATE_FORMAT_SYMBOLS.getWeekdays();
        return weekdays[weekday];
    }

    /**
     * Returns an array of month names
     * @return an array of month names
     */
    public static String[] getMonths() {
        return getMonths(false);
    }

    /**
     * Returns an array of month names
     * @param shortened a flag indicating that shortened month names should be returned
     * @return an array of month names
     */
    public static String[] getMonths(final boolean shortened) {

        if (shortened) {
            return DATE_FORMAT_SYMBOLS.getShortMonths();
        }
        else {
            return DATE_FORMAT_SYMBOLS.getMonths();
        }

    }

    /**
     * Returns the quarter for the specified month
     * @param code the month code (1-12).
     * @return the quarter that the month belongs to
     * @throws java.lang.IllegalArgumentException
     */
    public static int monthCodeToQuarter(final int code) {

        switch (Month.make(code)) {
            case JANUARY:
            case FEBRUARY:
            case MARCH: return 1;
            case APRIL:
            case MAY:
            case JUNE: return 2;
            case JULY:
            case AUGUST:
            case SEPTEMBER: return 3;
            case OCTOBER:
            case NOVEMBER:
            case DECEMBER: return 4;
            default: return -1;
        }

    }

    /**
     * Returns a string representing the supplied month.
     *
     * The string returned is the long form of the month name taken from the
     * default locale
     * @param month the month
     * @return a string representing the supplied month
     */
    public static String monthCodeToString(final Month month) {
        return monthCodeToString(month, false);
    }

    /**
     * Returns a string representing the supplied month.
     *
     * The string returned is the long or short form of the month name taken
     * from the default locale
     * @param month the month
     * @param shortened shorten if <code>true</code> return the abbreviation of the month.
     * @return a string representing the supplied month.
     * @throws java.lang.IllegalArgumentException
     */
    public static String monthCodeToString(final Month month,
                                            final boolean shortened) {
        final String[] months;

        if (shortened) {
            months = DATE_FORMAT_SYMBOLS.getShortMonths();
        }
        else {
            months = DATE_FORMAT_SYMBOLS.getMonths();
        }

        return months[month.index - 1];

    }

    /**
     * Converts a string to a month code.
     * <P>
     *     This method will return one of the constants JANUARY, FEBRUARY, ...,
     *     DECEMBER that corresponds to the string. If the string is not recognised
     *     , this method returns -1
     * </P>
     *
     * @param s the string tor parse.
     * @return <code>-1</code> if the string is not parseable, the month of the
     *          year otherwise.
     */
    public static int stringToMonthCode(String s) {

        final String[] shortMonthNames = DATE_FORMAT_SYMBOLS.getShortMonths();
        final String[] monthNames = DATE_FORMAT_SYMBOLS.getMonths();

        int result = -1;
        s = s.trim();

        // first try parsing the string as an integer (1-12)...
        try {
            result = Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            // suppress
        }

        // now search through the names...
        if ((result < 1) || (result > 12)) {
            result = -1;
            for (int i = 0; i < monthNames.length; i++) {
                if (s.equalsIgnoreCase(shortMonthNames[i])) {
                    result = i + 1;
                }
                if (s.equalsIgnoreCase(monthNames[i])) {
                    result = i + 1;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Returns true if the supplied integer code represents a valid
     * week-in-the-month, and false otherwise.
     *
     * @param code the code being checked for validity
     * @return <code>true</code> if the supplied integer code represents a
     *          valid week-in-the-month.
     */
    public static boolean isValidWeekInMonthCode(final int code) {

        switch (code) {
            case FIRST_WEEK_IN_MONTH:
            case SECOND_WEEK_IN_MONTH:
            case THIRD_WEEK_IN_MONTH:
            case FOURTH_WEEK_IN_MONTH: return true;
            default : return false;
        }

    }

    /**
     * Determines whether or not the specified year is a leap year.
     * @param yyyy the year (in the range of 1900 to 9999).
     * @return <code>true</code> if the specified year is a leap year
     */
    public static boolean isLeapYear(final int yyyy) {

        if ((yyyy % 4) != 0) {
            return false;
        }
        else if ((yyyy % 400) == 0) {
            return true;
        }
        else if ((yyyy % 100) == 0) {
            return false;
        }
        else {
            return true;
        }

    }

    /**
     * Returns the number of leap years from 1900 to the specified year INCLUSIVE
     * <P>
     *     Note that 1900 is not a leap year
     * </P>
     * @param yyyy
     * @return
     */
    public static int leapYearCount(final int yyyy) {

        final int leap4 = (yyyy - 1896) / 4;
        final int leap100 = (yyyy - 1800) / 100;
        final int leap400 = (yyyy - 1600) / 400;
        return leap4 - leap100 + leap400;

    }

    /**
     * Returns the number of the last day of the month, taking into account
     * leap year
     * @param month the month.
     * @param yyyy the year (in the range 1900 to 9999).
     * @return the number of the last day of the month.
     */
    public static int lastDayOfMonth(final Month month, final int yyyy) {

        final int result = LAST_DAY_OF_MONTH[month.index];
        if (month != Month.FEBRUARY) {
            return result;
        }
        else if (isLeapYear(yyyy)) {
            return result + 1;
        }
        else {
           return result;
        }
    }

    /**
     * Creates a new date by adding the specified number of days to the base date
     * @param days the number of days to add (can be negative)
     * @param base the base date
     * @return a new date
     */
    public static DayDate addDays(final int days, final DayDate base) {

        final int serialDayNumber = base.toSerial() + days;
        return DayDate.createInstance(serialDayNumber);
    }

    /**
     * Creates a new date by adding the specified number of months to the base
     * date.
     * <P>
     *     If the base date is close to the  end of the month, the day on the result
     *     may be adjusted slightly: 31 May + 1 month = 30 June.
     * </P>
     * @param months the number of months to add (can be negative).
     * @param base the base date
     * @return a new date
     */
    public static DayDate addMonths(final int months,
                                    final DayDate base) {

        final int yy = (12 * base.getYYYY() + base.getMonth() + months - 1) / 12;

        final int mm = (12 * base.getYYYY() + base.getMonth() + months - 1) % 12 + 1;

        final int dd = Math.min(
                base.getDayOfMonth(), DayDate.lastDayOfMonth(Month.make(mm), yy)
        );
        return DayDate.createInstance(dd, mm, yy);

    }

    /**
     * Creates a new date by adding the specified number of years to the base
     * date.
     * @param years years the number of years to add (can be negative).
     * @param base the base date
     * @return a new date
     */
    public static DayDate addYears(final int years, final DayDate base) {

        final int baseY = base.getYYYY();
        final int baseM = base.getMonth();
        final int baseD = base.getDayOfMonth();

        final int targetY = baseY + years;
        final int targetD = Math.min(
                baseD, DayDate.lastDayOfMonth(Month.make(baseM), targetY)
        );

        return DayDate.createInstance(targetD, baseM, targetY);

    }

    /**
     * Returns the latest date that falls on the specified day-of-the-week and
     * is BEFORE the base date
     * @param targetWeekday a code for the target day-of-the-week
     * @param base the base date.
     * @return the latest date that falls on the specified day-of-the-week and
     *          is BEFORE the base date
     */
    public static DayDate getPreviousDayOfWeek(final Day targetWeekday,
                                               final DayDate base) {
        // find the date...
        final int adjust;
        final int baseDOW = base.getDayOfWeek();
        if (baseDOW > targetWeekday.toInt()) {
            adjust = Math.min(0, targetWeekday.toInt() - baseDOW);
        }
        else {
            adjust = -7 + Math.max(0, targetWeekday.toInt() - baseDOW);
        }

        return DayDate.addDays(adjust, base);
    }

    /**
     * Returns the earliest date that falls on the specified day-of-the-week
     * and is AFTER the base date.
     * @param targetWeekday a code for the target day-of-the-week
     * @param base the base date
     * @return the earliest date that falls on the specified day-of-the-week
     *          and is AFTER the base date.
     */
    public static DayDate getFollowingDayOfWeek(final Day targetWeekday,
                                                final DayDate base) {

        // find the date...
        final int adjust;
        final int baseDOW = base.getDayOfWeek();

        if (baseDOW >= targetWeekday.toInt()) {
            adjust = 7 + Math.min(0, targetWeekday.toInt() - baseDOW);
        }
        else {
            adjust = Math.max(0, targetWeekday.toInt() - baseDOW);
        }

        return DayDate.addDays(adjust, base);

    }

    /**
     * Returns the date that falls on the specified day-of-the-week and is
     * CLOSET to the base date.
     * @param targetDOW a code for the target day-of-the-week
     * @param base the base date.
     * @return the date that falls on the specified day-of-the-week and is
     *          CLOSET to the base date.
     */
    public static DayDate getNearestDayOfWeek(final Day targetDOW,
                                              final DayDate base) {
        // find the date...
        int delta = targetDOW.toInt() - base.getDayOfWeek();
        int positiveDelta = delta + 7;
        int adjust = positiveDelta % 7;
        if (adjust > 3)
            adjust -= 7;

        return DayDate.addDays(adjust, base);

    }

    /**
     * Rolls the date forward to the last day of the month
     * @param base the base date
     * @return a new serial date
     */
    public DayDate getEndOfCurrentMonth(final DayDate base) {
        final int last = DayDate.lastDayOfMonth(
                Month.make(base.getMonth()), base.getYYYY()
        );
        return DayDate.createInstance(last, base.getMonth(), base.getYYYY());
    }

    /**
     * Returns a string corresponding to the week-in-the-month code.
     * <P>
     *     Need to find a better approach
     * </P>
     * @param count an integer code representing the week-in-the-month-code.
     * @return a string corresponding to the week-in-the-month code.
     */
    public static String weekInMonthToString(final int count) {

        switch (count) {
            case DayDate.FIRST_WEEK_IN_MONTH: return "First";
            case DayDate.SECOND_WEEK_IN_MONTH : return "Second";
            case DayDate.THIRD_WEEK_IN_MONTH : return "Third";
            case DayDate.FOURTH_WEEK_IN_MONTH : return "Fourth";
            case DayDate.LAST_WEEK_IN_MONTH : return "Last";
            default:
                return "DayDate.weekInMonthToString(): invalid code.";
        }

    }

    /**
     * Returns a string representing the supplied 'relative'
     * <P>
     *     Need to find ad better approach
     * </P>
     * @param relative a constant representing the 'relative'
     * @return a string representing the supplied 'relative'
     */
    public static String relativeToString(final int relative) {

        switch (relative) {
            case DayDate.PRECEDING : return "Preceding";
            case DayDate.NEAREST: return "Nearest";
            case DayDate.FOLLOWING : return "Following";
            default: return "ERROR: Relative To String";
        }

    }

    /**
     * Factory method that returns a instance of some concrete subclass of
     * {@link DayDate}
     * @param day the day (1-31).
     * @param month the month (1-12).
     * @param yyyy the year (in the range 1900 to 9999).
     * @return An instance of {@link DayDate}
     */
    public static DayDate createInstance(final int day, final int month,
                                         final int yyyy) {
        return DayDateFactory.makeDate(day, month, yyyy);
    }

    /**
     * Factory method that returns a instance of some concrete subclass of
     * {@link DayDate}
     * @param day the day (1-31).
     * @param month An instance of {@link Month} (JANUARY-DECEMBER).
     * @param yyyy the year (in the range 1900 to 9999).
     * @return An instance of {@link DayDate}
     */
    public static DayDate createInstance(final int day, final Month month,
                                         final int yyyy) {
        return DayDateFactory.makeDate(day, month, yyyy);
    }

    /**
     * Factory method that returns a instance of some concrete subclass of
     * {@link DayDate}
     * @param serial date A Java date object
     * @return An instance of {@link DayDate}
     */
    public static DayDate createInstance(final int serial) {
        return DayDateFactory.makeDate(serial);
    }

    /**
     * Factory method that returns an instance of a subclass of DayDate
     * @param date A Java date object
     * @return a instance of DayDate
     */
    public static DayDate createInstance(final java.util.Date date) {
        return DayDateFactory.makeDate(date);
    }

    /**
     * Returns the serial number for the date, where 1 January 1900 = 2 (this
     * corresponds, almost, to the numbering system used in Microsoft Excel for
     * Windows and Lotus 1-2-3).
     * @return the serial number for the date
     */
    public abstract int toSerial();

    /**
     * Returns a java.util.DayDate. Since java.util.DayDate has more precision than
     * DayDate, we need to define a convention for the 'time of day'.
     * @return this as <code>java.util.DayDate</code>
     */
    public abstract java.util.Date toDate();


    /**
     * Returns a description of the date
     * @return a description of the date
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Sets the description for the date
     * @param description the new description for the date
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * Converts the date to a string.
     * @return a string representation of the date.
     */
    public String toString() {
        return getDayOfMonth() + "-" + DayDate.monthCodeToString(Month.make(getMonth()))
                                + "-" + getYYYY();
    }

    /**
     * Returns the year (assume a valid range of 1900 to 9999).
     * @return the year.
     */
    public abstract int getYYYY();

    /**
     * Returns the month (January = 1, February = 2, March = 3).
     * @return the month of the year.
     */
    public abstract int getMonth();

    /**
     * Returns the day of the month.
     * @return the day of the month.
     */
    public abstract int getDayOfMonth();

    /**
     * Returns the day of the week.
     * @return the day of the week.
     */
    public abstract int getDayOfWeek();

    /**
     * Returns the difference (in days) between this date and the specified
     * 'other' date
     * <P>
     *     The result is positive if this date is after the 'other' date and
     *     negative if it is before the 'other' date.
     * </P>
     * @param other the date being compared to.
     * @return the difference between this and the other date.
     */
    public abstract int compare(DayDate other);

    /**
     * Returns true if this Serial represents the same date as the specified DayDate
     * @param other the date being compared to
     * @return <code>true</code> if this DayDate represents the same date as the specified
     *      DayDate.
     */
    public abstract boolean isOn(DayDate other);

    /**
     * Returns true if this DayDate represents an earlier date compared to
     * the specified DayDate.
     * @param other other The dat being compared to.
     * @return <code>true</code> if this DayDate represents an earlier adate
     *          compared to the specified DayDate.
     */
    public abstract boolean isBefore(DayDate other);

    /**
     * Returns true if this DayDate represents the same date as the
     * specified DayDate.
     * @param other the date being compared to.
     * @return <code>true</code> if this DayDate represents the same date
     *          as the specified DayDate.
     */
    public abstract boolean isOnOrBefore(DayDate other);

    /**
     * Returns true if this DayDate represents the same date as the specified
     * DayDate
     * @param other the date being compared to
     * @return <code>true</code> if this DayDate represents the same date
     *          as the specified DayDate
     */
    public abstract boolean isAfter(DayDate other);

    /**
     * Returns true if this DayDate represents the same date as the
     * specified DayDate
     * @param other the date being compared to
     * @return <code>true</code> if this DayDate represents the same date
     *          as the specified DayDate
     */
    public abstract boolean isOnOrAfter(DayDate other);

    /**
     * Returns <code>true</code> if this {@link DayDate} represents the same date as the
     * specified range (INCLUSIVE). The date order of d1 and d2 is not important.
     * @param d1 a boundary date for the range.
     * @param d2 the other boundary date for the range
     * @return A boolean
     */
    public abstract boolean isInRange(DayDate d1, DayDate d2);

    /**
     * Returns <code>true</code> if this {@link DayDate} represents the same date as the
     * specified range (caller specifies whether or not the end-points are included). The
     * date order of d1 and d2 is not important.
     * @param d1 a boundary date for the range.
     * @param d2 the other boundary date for the range.
     * @param include a code that controls whether or not the start and end
     *                dates are included in the range.
     * @return A boolean
     */
    public abstract boolean isInRange(DayDate d1, DayDate d2,
                                      int include);

    /**
     * Returns the latest date that falls on the specified day-of-the-week and
     * is BEFORE this date
     * @param targetDOW a code for the target day-of-the-week
     * @return the latest date that falls on the specified day-of-the-week and
     *          is BEFORE this date
     */
    public DayDate getPreviousDayOfWeek(final int targetDOW) {
        return getPreviousDayOfWeek(Day.fromInt(targetDOW), this);
    }

    /**
     * Returns the earliest date that falls on the specified day-of-the-week
     * and is AFTER this date.
     * @param targetDOW a code for the target day-of-the-week.
     * @return the earliest date that falls on the specified day-of-the-week
     *          and is AFTER this date.
     */
    public DayDate getFollowingDayOfWeek(final int targetDOW) {
        return getFollowingDayOfWeek(Day.fromInt(targetDOW), this);
    }

    /**
     * Returns the nearest date that falls on the specified day-of-the-week.
     *
     * @param targetDOW a code for the target day-of-the-week.
     * @return the nearest date that falls on the specified day-of-the-week
     */
    public DayDate getNearestDayOfWeek(final int targetDOW) {
        return getNearestDayOfWeek(Day.fromInt(targetDOW), this);
    }
}