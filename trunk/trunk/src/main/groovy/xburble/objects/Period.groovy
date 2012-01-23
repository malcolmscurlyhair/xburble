package xburble.objects

/**
 * An instant, or duration, described as part of a Context.
 */
class Period implements Comparable
{
   static String INPUT_FORMAT  = "yyyy-MM-dd"
   static String OUTPUT_FORMAT = "MMM. dd, yyyy"

   Period(String start, String end, pointInTime)
   {
       if (start       != null && start       != "") startDate = Date.parse(INPUT_FORMAT, start       .replaceAll("[^0-9\\-]", ""))
       if (end         != null && end         != "") endDate   = Date.parse(INPUT_FORMAT, end         .replaceAll("[^0-9\\-]", ""))
       if (pointInTime != null && pointInTime != "") instant   = Date.parse(INPUT_FORMAT, pointInTime .replaceAll("[^0-9\\-]", ""))
   }

   Period(Date startDate, Date endDate, Date instant)
   {
       this.startDate = startDate
       this.endDate   = endDate
       this.instant   = instant
   }

   Date startDate
   Date endDate
   Date instant

   String toString()
   {
      if (instant != null && instant != "")
      {
         return instant.format(OUTPUT_FORMAT)
      }

      String description = endDate.format(OUTPUT_FORMAT)

      if (startDate != null)
      {
         description = ((Math.round((endDate - startDate) / 30).intValue())  + "M ending ") + description
      }

      return description
   }

   boolean equals(Period p)
   {
      return this.compareTo(p) == 0
   }

   int compareTo(Object period)
   {
      if (instant != null && period.instant == null) return 1
      if (instant == null && period.instant != null) return -1

      if (instant != null && period.instant != null && instant != period.instant)
      {
         return -instant.compareTo(period.instant)
      }

      if (endDate != null && period.endDate == null) return -1
      if (endDate == null && period.endDate != null) return 1

      if (endDate != null && period.endDate != null && endDate != period.endDate)
      {
         return -endDate.compareTo(period.endDate)
      }

      if (startDate != null && period.startDate == null) return -1
      if (startDate == null && period.startDate != null) return 1

      if (startDate != null && period.startDate != null && startDate != period.startDate)
      {
         return -startDate.compareTo(period.startDate)
      }
   }

   Period clone()
   {
      new Period(startDate, endDate, instant)
   }
}
