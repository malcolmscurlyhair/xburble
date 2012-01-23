package xburble.objects

/**
 * Represents two or more Filings that the user has chosen to merge together, in order to view them side-by-side.
 */
class Merge
{
    // Separate filing instances.
    List<Filing> filings

    // List of merged filing Sections.
    Map<String, List<Section>> mergeList = [:]

    // Map of (top-level) section to company name.
    Map<Section, Filing> filingsBySection = [:]

    Merge(List<Filing> filings)
    {
       this.filings = filings

       merge()
    }

    /**
     * Merge the Sections supplied with each Filing into a consolidated list.
     *
     * NOTE: this is *very* rough prototype behaviour - the resolution of sections relies on annotations being
     * consistent across filings, which is pretty unreliable.  Also, the eventual aim is to interleave data into the
     * same set of Section objects - right not, they are rendered side-by-side, for each filing.  Merging by top-level
     * Section element does not seem to be the appropriate thing to do here.
     */
    private void merge()
    {
       for (Filing filing : filings)
       {
          for (Section section : filing.sections)
          {
              String annotation = section.annotation.toUpperCase().replaceAll(" +", " ").replaceAll("[0-9]", "")[2..-1].trim()

              section = section.children[0]

              List<Section> toCollapse = mergeList.get(annotation)

              if (toCollapse == null)
              {
                 toCollapse = []
                 mergeList[annotation] = toCollapse
              }

              toCollapse << section

              filingsBySection[ section ] = filing
          }
       }

       // Sort (very roughly) by section type.
       mergeList = mergeList.sort
                   {
                      entry ->

                      String prefix = ""
                      String name   = entry.key

                      if (name.toUpperCase().contains("DOCUMENT"))
                      {
                         prefix = "1"
                      }
                      else if (name.toUpperCase().contains("STATEMENT"))
                      {
                         prefix = "2"
                      }
                      else if (name.toUpperCase().contains("DISCLOSURE"))
                      {
                         prefix = "3"
                      }
                      else
                      {
                         prefix = "4"
                      }

                      return prefix + "  " + name
                   }
    }

    String getDescription()
    {
       filings.collect { Filing filing -> filing.getDescription() }.join(", ")
    }

    String toString()
    {
       getDescription()
    }

    List<String> getSectionNames()
    {
       mergeList.keySet().toList()
    }

    List<Section> getSections(String name)
    {
       mergeList[ name ].toList()
    }

    Filing getFilingFor(Section section)
    {
       filingsBySection[ section ]
    }
}
