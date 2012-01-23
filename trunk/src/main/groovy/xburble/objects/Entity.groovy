package xburble.objects

/**
 * The legal entity issuing the filing, (or a subsidiary, I guess), described as part of a Context.
 */
class Entity implements Comparable
{
   String cik

   String toString()
   {
      cik
   }

   int compareTo(Object e)
   {
      return cik.compareTo(e.cik)
   }

   Entity clone()
   {
      new Entity([cik: cik])
   }
}
