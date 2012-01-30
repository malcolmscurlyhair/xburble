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

 * Implement Smarter 'Merge' and 'Split' functionality.
   * Make naming of merges smarter - concertina same company + filing date.
 * Use proper error handling (for timeouts, mis-parses.)
 ? Add support for loading from database.
 * Read definition files, show definitions when requested by the user.
 * Read calculation files, use to infer correct rendering.
 * Use calculations to create graphs.
 * Comments everything nicely.
 * Make sure date formats are consistent across:
   * Newest submission.
   * Search + recent dialogs.
   * Rendered documents.
 * Fix rendering of 'clicky' tables - text is still get transformed into
   a white rectangle on mouse-over.


 *************/
