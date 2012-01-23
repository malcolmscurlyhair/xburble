package xburble.objects.search

/**
 * Recent filings, pulled from the SEC RSS feed.
 */
class RecentFilings extends SearchResults
{
   String[] getColumnNames()
   {
      [ "Company", "CIK", "Filing Date" ]
   }

   String getLink(int index)
   {
      return data[index][3]
   }
}
