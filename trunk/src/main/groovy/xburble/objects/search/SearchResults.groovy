package xburble.objects.search

/**
 * Results which come back from a company or filing search on the EDGAR website.
 */
abstract class SearchResults
{
   abstract String[] getColumnNames()

   List data = []

   public void add(List row)
   {
      data << row
   }

   int size()
   {
      return data.size()
   }
}
