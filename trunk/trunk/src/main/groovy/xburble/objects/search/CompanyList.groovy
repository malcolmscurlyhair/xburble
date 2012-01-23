package xburble.objects.search

/**
 * List of companies coming back from an EDGAR search.
 */
class CompanyList extends SearchResults
{
   String[] getColumnNames()
   {
      [ "CIK", "Company", "State", "Link" ]
   }

   String getCIK(int index)
   {
      return data[index][0]
   }
}
