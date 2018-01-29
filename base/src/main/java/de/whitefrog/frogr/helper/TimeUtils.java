package de.whitefrog.frogr.helper;

import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

/**
 * Abstract helper class for time based calculations.
 */
public abstract class TimeUtils {
  /**
   * Format a time interval in milliseconds into a human readable form.
   * Automatically detects the required format to use.
   * @param i Time in milliseconds
   * @return The formatted time interval
   */
  public static String formatInterval(long i) {
    return formatInterval(i, TimeUnit.MILLISECONDS);
  }
  /**
   * Format a time interval into a human readable form.
   * Automatically detects the required format to use.
   * @param i Time
   * @param timeUnit TimeUnit to use for the calculation 
   * @return The formatted time interval
   */
  public static String formatInterval(long i, TimeUnit timeUnit) {
    if(!timeUnit.equals(TimeUnit.NANOSECONDS)) i = timeUnit.toNanos(i);
    final long days = TimeUnit.NANOSECONDS.toDays(i);
    final long hr = TimeUnit.NANOSECONDS.toHours(i - TimeUnit.DAYS.toNanos(days));
    final long min = TimeUnit.NANOSECONDS.toMinutes(i - TimeUnit.DAYS.toNanos(days) - TimeUnit.HOURS.toNanos(hr));
    final long sec = TimeUnit.NANOSECONDS.toSeconds(i - TimeUnit.DAYS.toNanos(days) - TimeUnit.HOURS.toNanos(hr) - TimeUnit.MINUTES.toNanos(min));
    final long ms = TimeUnit.NANOSECONDS.toMillis(i - TimeUnit.DAYS.toNanos(days) - TimeUnit.HOURS.toNanos(hr) - TimeUnit.MINUTES.toNanos(min) - TimeUnit.SECONDS.toNanos(sec));
    final long micro = TimeUnit.NANOSECONDS.toMicros(i - TimeUnit.DAYS.toNanos(days) - TimeUnit.HOURS.toNanos(hr) - TimeUnit.MINUTES.toNanos(min) - TimeUnit.SECONDS.toNanos(sec) - TimeUnit.MILLISECONDS.toNanos(ms));
    final long ns = i - TimeUnit.DAYS.toNanos(days) - TimeUnit.HOURS.toNanos(hr) - TimeUnit.MINUTES.toNanos(min) - TimeUnit.SECONDS.toNanos(sec) - TimeUnit.MILLISECONDS.toNanos(ms) - TimeUnit.MICROSECONDS.toNanos(micro);

    if(days > 0) {
      return MessageFormat.format("{0} {0,choice,0#days|1#day|1<days} {1} {1,choice,0#hours|1#hour|1<hours}", days, hr);
    }
    else if(hr > 0) {
      return MessageFormat.format("{0} {0,choice,0#hours|1#hour|1<hours} {1} {1,choice,0#minutes|1#minute|1<minutes}", hr, min);
    }
    else if(min > 0) {
      return MessageFormat.format("{0} {0,choice,0#minutes|1#minute|1<minutes} {1} {1,choice,0#seconds|1#second|1<seconds}", min, sec);
    }
    else if(sec > 0) {
      return MessageFormat.format("{0} {0,choice,0#seconds|1#second|1<seconds} {1}ms", sec, ms);
    }
    else if(ms > 10) {
      return MessageFormat.format("{0}ms", ms);
    }
    else if(ms > 0) {
      return MessageFormat.format("{0}.{1}ms", ms, micro);
    }
    else if(micro > 10) {
      return MessageFormat.format("{0}µs", micro);
    }
    else if(micro > 0) {
      return MessageFormat.format("{0}.{1}µs", micro, ns);
    }
    else {
      return MessageFormat.format("{0}ns", ns);
    }
  }

  /**
   * Calculates how many operations where done per second.
   * @param time Time in milliseconds
   * @param count Operation count
   * @return Operations per second as formatted string
   */
  public static String perSecond(long time, long count) {
    if(count == 0) return "0/s";
    return perSecond(time / count);
  }
  /**
   * Calculates how many operations where done per second.
   * The calculation is done in double precision.
   * @param divisor Time divided by operation count
   * @return Operations per second as formatted string
   */
  public static String perSecond(double divisor) {
    final double after = TimeUnit.SECONDS.toNanos(1) / divisor;
    return String.format("%.2f/s", after);
  }
}
