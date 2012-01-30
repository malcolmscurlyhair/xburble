package xburble.objects

/**
 * A filing parsed from XBRL data.
 */
class Filing
{
    // Legal entity.
   String company
   String cik

   // When the filing was submitted.
   Date filingDate

   // Link to the online viewer.
   String onlineViewer

   // The final representation of the parsed data.
   List<Section> sections = []

   String getDescription()
   {
       "$company ${ filingDate.format("ddMMMyyy") }"
   }

   String toString()
   {
       return getDescription()
   }
}
