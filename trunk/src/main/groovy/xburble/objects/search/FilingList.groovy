package xburble.objects.search

/**
 * A list of filings come back from an EDGAR search.
 */
class FilingList extends SearchResults
{
   String[] getColumnNames()
   {
      [ "Filing Date", "Description", "Link" ]
   }

   String getLink(int index)
   {
      return data[index][2]
   }

   String getLinkToOnlineViewer(int index)
   {
      return data[index][3]
   }
}
