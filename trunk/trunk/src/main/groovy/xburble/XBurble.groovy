package xburble

import xburble.screen.Frame
import xburble.services.Cache
import xburble.services.RecentActivity
import xburble.services.XML

/**
 * Startup class for the XBurble application.
 */
class XBurble
{
   static void main(String... args)
   {
      // XML xml = new XML()
      // xml.loadFromDirectory(new File("src/main/resources/filings/Putnam Hills Corp"))

      Frame screen = new Frame();
      screen.layoutComponents()
      screen.pack()
      screen.visible = true

      Cache.init()
      RecentActivity.init()
   }
}

/************

 To do:

 x Cache schemas in working directory.
 x Cache type definitions in memory.
 x Remember recently viewed filings.
 x Remember recent searches (auto-suggest).
 x For filings in search tab, treat HTML nicely.
 x Make tabs have title: COMPANY_NAME (CIK) FILING_DATE.
 x Allow tabs to be closed.
 x Enable/disable buttons as tabs are selected/closed.
 x Add backdoor to view raw XML structure.
 x Fix spacing of panel when docked.
 x Add title to panel, and each section.
 x Make titles for each section tidier.
 x Fix rendering of filings.
 x Find 'Split' icon.
 x Handle 'direct to filings' behaviour on exact search match.
 x Indicate loading status.
 x Make a note of (and ignore) deprecated elements.
 x Fix rendering of list items.
 x Adjust font size for text blocks.
 x Find better waiting icon.
 x Find better export icon.
 x Pull rendering mechanisms into common methodology.
 x Reorder recent, to put most recent at top.
 x Number formats.
 x Implement 'Export to Excel' functionality.
 x Add Maven build file.
 x Hard coded list of elements to ignore.
 x Fix missing images when running from double-click.
 x Find a decent logo.
 x Fix segmented data.

 * Fix rendering of balance sheets.
 * Implement 'Merge' and 'Split' functionality.

 * Use proper error handling (for timeouts, mis-parses.)
 ? Add support for loading from database.
 * Read definition files, show definitions when requested by the user.
 * Read calculation files, use to infer correct rendering.
 * Use calculations to create graphs.
 * Comments everything nicely.

 *************/
