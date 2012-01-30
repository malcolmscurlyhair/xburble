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

      if (startDate != null && endDate != null)
      {
         description = gap() + " ending "+ description
      }

      return description
   }

   String gap()
   {
      if (startDate != null && endDate != null)
      {
          return "" + monthsBetween(startDate, endDate) + "M"
      }

      return null
   }

   int monthsBetween(from, to)
   {
      def first  = new GregorianCalendar(time: from)
      def second = new GregorianCalendar(time: to)

      int a = (second.get(Calendar.YEAR)  - first.get(Calendar.YEAR)) * 12
      int b = (second.get(Calendar.MONTH) - first.get(Calendar.MONTH))
      int c = Math.round((second.get(Calendar.DAY_OF_MONTH) - first.get(Calendar.DAY_OF_MONTH)) / 30)

      // println "${ startDate.format("ddMMMyyyy") } - ${ endDate.format("ddMMMyyyy") } =  $a + $b + $c = ${ a + b + c}"

      return a + b + c
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

      if (startDate != null && period.startDate == null) return -1
      if (startDate == null && period.startDate != null) return 1

      // Check gap between start and end dates.
      if (endDate != null && startDate != null && period.endDate != null && period.startDate != null)
      {
         int gap_1 = endDate - startDate
         int gap_2 = period.endDate - period.startDate

         if (gap_1 != gap_2)
         {
            return gap_1 > gap_2 ? 1 : -1
         }
      }

      if (endDate != null && period.endDate != null && endDate != period.endDate)
      {
         return -endDate.compareTo(period.endDate)
      }

      if (startDate != null && period.startDate != null && startDate != period.startDate)
      {
         return -startDate.compareTo(period.startDate)
      }
   }
}
