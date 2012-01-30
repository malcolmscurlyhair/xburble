package xburble.screen

import javax.swing.JFrame
import javax.swing.JToolBar
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.UIManager

import static java.awt.Color.*
import java.awt.Dimension
import static xburble.renderer.Resources.*
import java.awt.Insets
import java.awt.Cursor
import com.javadocking.dockable.*
import com.javadocking.dock.*
import com.javadocking.DockingManager
import com.javadocking.model.FloatDockModel
import javax.swing.JLabel
import groovy.swing.SwingBuilder
import javax.swing.BorderFactory
import static java.awt.BorderLayout.*
import javax.swing.JOptionPane
import java.awt.Toolkit
import javax.swing.JViewport
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableModel
import javax.swing.JTable
import javax.swing.JScrollPane
import xburble.services.XML
import xburble.services.Status
import xburble.renderer.HTMLRenderer
import javax.swing.JComponent
import javax.swing.JComboBox
import xburble.services.RecentActivity
import ca.odell.glazedlists.GlazedLists
import ca.odell.glazedlists.swing.AutoCompleteSupport
import javax.swing.SwingUtilities
import static xburble.services.RecentActivity.*

import java.awt.Desktop

import xburble.services.Utils
import xburble.renderer.ExcelRenderer
import xburble.objects.search.SearchResults
import xburble.objects.search.FilingList
import xburble.objects.search.RecentFilings
import xburble.objects.Filing
import javax.swing.JCheckBox
import javax.swing.JPanel
import javax.swing.BoxLayout
import java.awt.Font
import java.awt.event.ActionEvent
import xburble.objects.Merge

/**
 * Main screen of the XBurble application.  The toolbar across the top has options to:
 *
 *   o Search for new filings (by company name, or CIK.)
 *   o Re-open recently viewed filings.
 *   o Look up recently submitted filings.
 *   o Merge two or more filings into the same view.
 *   o Split two previously merged filings.
 *   o Export the currently selected filing to Excel.
 *   o Exit the application.
 *
 *  The filings themselves are opened in dockable tabs in the main viewing area.  We also render a status bar at the
 *  bottom of the frame, so print out status messages.
 */
class Frame extends JFrame
{
   // Screen components.
   JToolBar toolBar   = new JToolBar()
   TabDock  dock      = new TabDock()
   JLabel   statusBar = new JLabel(" ")

   // Toolbar buttons.
   JButton searchButton
   JButton openRecentButton
   JButton newFilingsButton
   JButton mergeButton
   JButton splitButton
   JButton exportButton
   JButton exitButton

   // Currently rendered filings, by tab.
   Map<Dockable, Filing> filings = [:]

   // Currently rendered filings, by tab.
   Map<Dockable, Merge> merges = [:]

   /**
    * Initialise the main screen components.
    */
   void layoutComponents()
   {
       setIconImage(LOGO_IMAGE)

       // Set to native look-and-feel.
       UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() )

       setLayout(new BorderLayout())

       this.title = "XBurble - The SEC Filing Viewer"

       this.background = DEFAULT_BACKGROUND
       this.foreground = DEFAULT_FOREGROUND

       this.preferredSize = new Dimension(1200, 900)

       this.defaultCloseOperation = EXIT_ON_CLOSE

       buildTitleBar()
       buildTabFrame()
       buildStatusBar()

       centre(this)
   }

   /**
    * Build the toolbar at the top of the screen, with options to search for filings, reload recently viewed filings,
    * search for newly submitted filings, merge and split filings, export to Excel, and exit the application.
    */
   void buildTitleBar()
   {
       /**
        * Function to add a button to the toolbar.
        */
       def addButton =
       {
           label, text, image, enabled ->

           JButton button = new JButton()

           button.actionCommand = text
           button.text          = "  $label   "
           button.toolTipText   = text
           button.icon          = image
           button.font          = TOOLBAR_FONT
           button.foreground    = TOOLBAR_LABEL
           button.margin        = new Insets(15, 15, 20, 15)

           button.enabled       = enabled

           button.borderPainted = false
           button.opaque        = false
           button.cursor        = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

           button.rolloverEnabled = false

           button.focusable = false

           /**
            * Highlight a toolbar button on mouse-over.
            */
           button.mouseEntered = { e -> if (button.isEnabled()) button.setForeground(TOOLBAR_HIGHLIGHT)  }
           button.mouseExited  = { e -> button.setForeground(TOOLBAR_LABEL)      }

           button.actionPerformed = { e -> Thread.start { performAction(text) } }

           toolBar.add(button)

           return button
       }

       searchButton     = addButton(SEARCH_LABEL,      SEARCH,      SEARCH_IMAGE,      true)
       openRecentButton = addButton(OPEN_RECENT_LABEL, OPEN_RECENT, OPEN_RECENT_IMAGE, true)
       newFilingsButton = addButton(NEW_FILINGS_LABEL, SHOW_NEW,    NEW_FILINGS_IMAGE, true)
       mergeButton      = addButton(MERGE_LABEL,       MERGE,       MERGE_IMAGE,       false)
       splitButton      = addButton(SPLIT_LABEL,       SPLIT,       SPLIT_IMAGE,       false)
       exportButton     = addButton(EXPORT_LABEL,      EXPORT,      EXPORT_IMAGE,      false)
       exitButton       = addButton(EXIT_LABEL,        EXIT,        EXIT_IMAGE,        true)

       toolBar.floatable  = false
       toolBar.background = TOOLBAR_BACKGROUND

       toolBar.rollover = false
       toolBar.focusable = false

       toolBar.border = BorderFactory.createLineBorder(TOOLBAR_FRAME, 1)

       contentPane.add(toolBar, NORTH)
   }

   /**
    * Build the main viewing area, where filings are loaded in as dockable tabs using the Javadocking library.
    */
   void buildTabFrame()
   {
       // Create the dock model for the docks.
       FloatDockModel dockModel = new FloatDockModel()
       dockModel.addOwner("Frame", this)
       DockingManager.setDockModel(dockModel)

       dockModel.addRootDock("dock1", dock, this);

       // Add the root docks to the split panes.
       contentPane.add(dock, CENTER)

       dock.border = BorderFactory.createEmptyBorder(10,0,0,0)

       // Make it invisible, for the moment.
       dock.visible = false
   }

   /**
    * Build the status bar at the bottom of the frame, which will render status messages as they are broadcast across
    * the application.
    */
   void buildStatusBar()
   {
       statusBar.opaque = true

       statusBar.background = STATUS_BAR_BACKGROUND
       statusBar.foreground = DEFAULT_FOREGROUND
       statusBar.font       = STATUS_BAR_FONT

       contentPane.add(statusBar, SOUTH)

       statusBar.border = BorderFactory.createEmptyBorder(8, 5, 10, 5)

       // Register for status messages.
       Closure listener = { message -> setStatus(message) }

       Status.addStatusListener(listener)
   }

   /**
    * Install a status message in the status message.  (Callback from the xburble.services.Status class.)
    *
    * @param message The status message to install.
    */
   void setStatus(String message)
   {
       if (message == null || message.trim().length() == 0)
       {
           statusBar.text = " "  // Need to put in *something* here, or else the status bar is prone
                                 // to collapse to a height of 0.
           return
       }

       statusBar.text = message
   }

   /**
    * Callback triggered when the user clicks on a toolbar button.  Perform an action corresponding to the
    * action command tied to a particular button.
    *
    * @param action The action to perform.
    */
   void performAction(String action)
   {
       setStatus(action)

       switch (action)
       {
           case SEARCH:       openSearchDialog();        return;
           case OPEN_RECENT:  openRecentFilingsDialog(); return;
           case SHOW_NEW:     showNewFilingsDialog();    return;
           case MERGE:        openMergeDialog();         return;
           case SPLIT:        openSplitDialog();         return;
           case EXPORT:       exportFiling();            return;
           case EXIT:         confirmExit();             return;
       }
   }

   /**
    * Open a dialog that will allow the user to search for filing, by company name or CIK.
    */
   private void openSearchDialog()
   {
       SwingBuilder swing = new SwingBuilder()

       JFrame    frame
       JViewport port
       JComboBox search
       JLabel    feedback

       Closure doSearch = {

           String text = search.selectedItem.toString()

           performSearch(frame, text, port, feedback)
       }

       frame = swing.frame(title:      title,
               size:       [700, 600],
               background: WHITE,
               foreground: BLACK,
               iconImage:  LOGO_IMAGE)
               {
                   panel( constraints:    NORTH,
                           border:        BorderFactory.createCompoundBorder(
                                             BorderFactory.createLineBorder(TOOLBAR_FRAME, 1),
                                             BorderFactory.createEmptyBorder(10, 10, 10, 10)),
                           opaque:        true,
                           background:    TOOLBAR_BACKGROUND,
                           preferredSize: [700, 100] )
                           {
                               borderLayout()

                               label(border: BorderFactory.createEmptyBorder(0, 10, 0, 0), icon: SEARCH_IMAGE, constraints: WEST)

                               panel( constraints: CENTER,
                                       border:      BorderFactory.createEmptyBorder(20, 25, 10, 10),
                                       opaque:      true,
                                       background:  TOOLBAR_BACKGROUND )
                                       {
                                           borderLayout()

                                           label(preferredSize: [ 100, 25 ], text: "Enter a company name or CIK to begin your search:", constraints: NORTH)

                                           search = comboBox (editable: true, items: [], preferredSize: [ 100, 14 ], constraints: CENTER)
                                       }

                               panel( constraints: EAST,
                                       border:      BorderFactory.createEmptyBorder(30, 30, 10, 10),
                                       opaque:      true,
                                       background:  TOOLBAR_BACKGROUND )
                                       {
                                           borderLayout()

                                           button(text: "Search", font: TOOLBAR_FONT, constraints: CENTER, actionPerformed: doSearch)
                                       }
                           }

                   port = viewport(constraints: CENTER, preferredSize: [700, 500])

                   panel(
                           constraints: SOUTH,
                           border: BorderFactory.createEmptyBorder(10, 10, 10, 10),
                           opaque:        true,
                           background:    STATUS_BAR_BACKGROUND,
                           preferredSize: [700, 60] )
                           {
                               feedback = label(text: " ", font: STATUS_BAR_FONT, constraints: WEST)
                           }
               }

       /**
        * Use the Glazed Lists library to install auto-complete support - as the user types, a list of recently
        * entered search terms is highlighted.
        */
       SwingUtilities.invokeLater( { ->

           AutoCompleteSupport.install(search, GlazedLists.eventListOf( RecentActivity.getRecentSearches().toArray() ))

       })

       // Perform the search when the user presses 'Enter' on the search box.
       search.editor.editorComponent.actionPerformed = doSearch

       centre(frame)

       frame.show()
   }

   /**
    * Perform a search, either by company name, or CIK.  Display the results inline in the supplied dialog, allowing
    * the user to click on a search result to either view filings for a selected company, or load a particular filing.
    *
    * @param dialog   The dialog in which to display results, and close when a filing is selected.
    * @param pattern  The search text.
    * @param port     The view port in which to install results.
    * @param feedback A mini-status bar, to update with contextual messages.
    */
   private void performSearch(JFrame dialog, String pattern, JViewport port, JLabel feedback)
   {
      pattern = pattern.trim()

      if (pattern.length() == 0) return

      Thread.start { ->

         port.view = new JLabel(LOADING_IMAGE, SwingConstants.CENTER)

         /**
          * Check whether the user is searching for CIK, or a company name.
          */
         boolean isCIK = pattern.matches("[0-9]+")

         SearchResults results

         if (isCIK)
         {
            /**
             * If the user is searching for a CIK, we expect to get back a list of filings...
             */
            feedback.text = "Searching for filings against CIK '$pattern'"

            results = new XML().findFilingsForCompany(pattern)
         }
         else
         {
            feedback.text = "Searching for companies matching pattern '$pattern'"

            results = new XML().findCompany(pattern)
         }

         // Remember this search, if there are any matches.
         if (results.size() > 0)
         {
             RecentActivity.rememberRecentSearch(pattern)
         }

         // Build the results table - the first row back will be the column headings.
         DefaultTableModel model = new DefaultTableModel(results.columnNames[0..-2].toArray(), 0)

         if (results.size() > 0)
         {
            // Install the search results.
            results.data.each
            {
               row ->

               // Wrap the second column in a tag, so it line wraps.
               row[1] = "<html><b>" + row[1] + "</b></html>"

               model.addRow( row[0..-2].toArray() )
            }
         }

         JTable table = new JTable(model)

         table.rowHeight = 50

         table.columnModel.getColumn(0).preferredWidth = 120
         table.columnModel.getColumn(1).preferredWidth = 550

         /**
          * If the user searched for a CIK, we will be showing filings.  We might also have matched one company
          * perfectly in the company search, in which case the SEC site will have returned a list of filings.
          * We need to detect both cases in order to set the appropriate click-through behaviour.
          */
         boolean showingFilings = (results instanceof FilingList)

         /**
          * Install the appropriate click behaviour.  If we are showing filings, clicking a row will load in that
          * filing, and close the dialog.  If we are showing companies, clicking a row will perform a search
          * for filings by the particular company, by calling back with the selected CIK.
          */
         Closure mouseClicked = { e ->

            int row = table.rowAtPoint(e.point)

            if (showingFilings)
            {
               String link         = results.getLink(row)
               String onlineViewer = results.getLinkToOnlineViewer(row)
               dialog.hide()
               dialog.dispose()

               loadFiling(link, onlineViewer)
            }
            else
            {
               performSearch(dialog, results.getCIK(row), port, feedback)
            }
         }

         makeClicky(table, mouseClicked)

         port.view = new JScrollPane( table )

         // Print a relevant contextual message.
         if (isCIK)
         {
            if (results.size() > 1)
            {
                feedback.text = "Found ${ results.size() - 1 } filing(s) for CIK '$pattern', please select one to view detail."
            }
            else
            {
                feedback.text = "No matches found, please try another search."
            }
         }
         else
         {
            if (results.size() > 1)
            {
                feedback.text = "Found ${ results.size() - 1 } result(s) matching '$pattern', please select one to see details."
            }
            else
            {
                feedback.text = "No matches found, please try another search."
            }
         }
      }
   }

   /**
    * Load a filing into a dockable tab in our main viewing area, by parsing the XML at a given URL.
    *
    * @param url          Where to look up the filing data on the SEC website.
    * @param onlineViewer A link to the online viewer for the filing.
    */
   private void loadFiling(String url, String onlineViewer)
   {
      if (!url.startsWith("http"))
      {
          url = "http://www.sec.gov" + url
      }

      Thread.start { ->

         try
         {
             /**
              * Load in the filing from the remote URL, and parse out the data.
              */
             XML xml = new XML()

             setStatus("Loading headline details from $url")

             xml.findHeadlineDetails(new URL(url))

             String title = "$xml.filing.company ${ xml.filing.filingDate.format("ddMMMyyy") }"

             // Add an empty tab with a 'loading' icon.
             Dockable dockable = new DefaultDockable(title, new JLabel(LOADING_IMAGE), title, null, DockingMode.ALL)
             dock.addDockable(dockable, new Position(Position.CENTER))
             dock.visible = true

             setStatus("Loaded filing from $url")

             xml.loadFromURL(new URL(url))

             Filing filing = xml.filing

             // To do: find a better way to update the screen here, rather than tearing out the loading tab and
             // replacing it wholesale.  The Javadocking library does not allow easy replacement of the docked
             // component - if we instead ad the component in a JViewport, we get weird scroll bar behaviour.

             // Removing and re-adding the tab causes an ugly screen-blink, but at least works.
             dock.removeDockable(dockable)

             renderFiling(filing)

             /**
              * Add this filing to the 'recently viewed' list.
              */
             RecentActivity.rememberRecentlyViewFiling(filing.company, filing.cik, filing.filingDate, url, filing.onlineViewer)
         }
         catch (Exception ex)
         {
             ex.printStackTrace()
         }
      }
   }

   /**
    * Render a filing in a new tab.
    */
   private void renderFiling(Filing filing)
   {
      Closure closeTab

      HTMLRenderer renderer = new HTMLRenderer({ closeTab() })
      renderer.render(filing)

      JComponent panel = renderer.view

      Dockable dockable = new DefaultDockable(filing.getDescription(), panel, filing.getDescription(), null, DockingMode.ALL)

      dock.addDockable(dockable, new Position(Position.CENTER))

      filings[ dockable ] = filing

      // Define the 'Close Tab' behaviour - which will have already been bound to the 'Close' button on the
      // rendered tab.
      closeTab =
      {
         dock.removeDockable(dockable)

         disableDocumentSpecificButtons()

         filings.remove(dockable)
      }

      // Enable the Merge and Export buttons, since we have a filing highlighted.
      enableDocumentSpecificButtons()
   }

   /**
    * Render a dialog that allows the user to open a recently viewed filing.
    */
   private void openRecentFilingsDialog()
   {
      SwingBuilder swing = new SwingBuilder()

      JFrame    frame
      JViewport port

      frame = swing.frame(title: title,
                           size: [700, 600],
                     background: WHITE,
                     foreground: BLACK,
                      iconImage: LOGO_IMAGE)
              {
                 panel(constraints: NORTH,
                            border: BorderFactory.createCompoundBorder(
                                      BorderFactory.createLineBorder(TOOLBAR_FRAME, 1),
                                      BorderFactory.createEmptyBorder(10, 10, 10, 10)),
                            opaque: true,
                        background: TOOLBAR_BACKGROUND,
                     preferredSize: [700, 100] )
                         {
                            borderLayout()

                            label(border: BorderFactory.createEmptyBorder(0, 10, 0, 0),
                                    icon: OPEN_RECENT_IMAGE, constraints: WEST)

                            panel( constraints: CENTER,
                                    border:      BorderFactory.createEmptyBorder(20, 25, 10, 10),
                                    opaque:      true,
                                    background:  TOOLBAR_BACKGROUND )
                                    {
                                       borderLayout()

                                       label(preferredSize: [ 100, 25 ],
                                                      text: "Click on a recently viewed filing to re-open it.",
                                               constraints: NORTH)
                                    }
                         }

                 port = viewport(constraints: CENTER, preferredSize: [700, 500])
              }

      List<Map<String,String>> recent = RecentActivity.getRecentlyViewedFilings()

      DefaultTableModel model = new DefaultTableModel(0, 0)
      model.columnIdentifiers = [ "CIK", "Company", "Filing Date" ]

      recent.each
      {
         row ->

         model.addRow( [ row[ CIK ], "<html><b>" + row[ COMPANY ] + "</b></html>", row[ FILING_DATE ].format("ddMMMyyyy") ].toArray() )
      }

      JTable table = new JTable(model)

      table.rowHeight = 50

      table.columnModel.getColumn(0).preferredWidth = 120
      table.columnModel.getColumn(1).preferredWidth = 450

      Closure mouseClicked = { e ->

         int row = table.rowAtPoint(e.point)

         Map<String, String> data = recent[ row ]

         frame.hide()
         frame.dispose()

         loadFiling(data[ "URL" ], data[ "VIEWER" ])
      }

      makeClicky(table, mouseClicked)

      port.view = new JScrollPane( table )

      centre(frame)

      frame.show()
   }

   /**
    * Render a dialog that allows the user to open a newly submitted filing.
    */
   private void showNewFilingsDialog()
   {
      SwingBuilder swing = new SwingBuilder()

      JFrame    frame
      JViewport port

      frame = swing.frame(title: title,
                           size: [700, 600],
                     background: WHITE,
                     foreground: BLACK,
                      iconImage: LOGO_IMAGE)
              {
                 panel(constraints: NORTH,
                            border: BorderFactory.createCompoundBorder(
                                      BorderFactory.createLineBorder(TOOLBAR_FRAME, 1),
                                      BorderFactory.createEmptyBorder(10, 10, 10, 10)),
                            opaque: true,
                        background: TOOLBAR_BACKGROUND,
                     preferredSize: [700, 100] )
                         {
                            borderLayout()

                            label(border: BorderFactory.createEmptyBorder(0, 10, 0, 0),
                                    icon: NEW_FILINGS_IMAGE, constraints: WEST)

                            panel( constraints: CENTER,
                                    border:      BorderFactory.createEmptyBorder(20, 25, 10, 10),
                                    opaque:      true,
                                    background:  TOOLBAR_BACKGROUND )
                                    {
                                       borderLayout()

                                       label(preferredSize: [ 100, 25 ],
                                                      text: "Click on a newly submitted filing to open it.",
                                               constraints: NORTH)
                                    }
                         }

                 port = viewport(constraints: CENTER, preferredSize: [700, 500])
              }

      RecentFilings newSubmissions = new XML().getRecentFilings()

      DefaultTableModel model = new DefaultTableModel(0, 0)
      model.columnIdentifiers = newSubmissions.columnNames

      // Install the search results.
      newSubmissions.data.each
      {
         row ->

         // Wrap the company name column in a tag, so it line wraps.
         row[1] = "<html><b>" + row[1] + "</b></html>"

         model.addRow( row[0..-2].toArray() )
      }

      JTable table = new JTable(model)

      table.rowHeight = 50

      table.columnModel.getColumn(1).preferredWidth = 450

      Closure mouseClicked = { e ->

         int row = table.rowAtPoint(e.point)

         String link = newSubmissions.getLink(row)

         frame.hide()
         frame.dispose()

         loadFiling(link, null)
      }

      makeClicky(table, mouseClicked)

      port.view = new JScrollPane( table )

      centre(frame)

      frame.show()
   }

   /**
    * Render a dialog that allows the user to merge two or more existing filings.
    */
   private void openMergeDialog()
   {
       SwingBuilder swing = new SwingBuilder()

       JFrame frame
       JPanel options
       JButton merge

       Map<Filing, JCheckBox> choices = [:]

       Closure gatherFilings = { ->

          List<Filing> chosen = []

          choices.each
          {
              Filing filing, JCheckBox checkbox ->

              if (checkbox.isSelected())
              {
                 chosen << filing
              }
          }

          return chosen
       }

       Closure doMerge =  { ActionEvent e ->

           List<Filing> chosen = gatherFilings()

           Merge merged = new Merge(chosen)

           frame.hide()
           frame.dispose()

           // Close tabs holding the filings we have just merged.
           List<Dockable> toClose = filings.entrySet().findAll { chosen.contains(it.value) }.collectAll { it.key }

           toClose.each
           {
               Dockable dockable ->

               dock.removeDockable(dockable)

               filings.remove(dockable)
           }

           showMerge(merged)
       }

       frame = swing.frame(title: title,
                            size: [700, 600],
                      background: WHITE,
                      foreground: BLACK,
                       iconImage: LOGO_IMAGE)
                {
                   panel(constraints: NORTH,
                              border: BorderFactory.createCompoundBorder(
                                        BorderFactory.createLineBorder(TOOLBAR_FRAME, 1),
                                        BorderFactory.createEmptyBorder(10, 10, 10, 10)),
                              opaque: true,
                          background: TOOLBAR_BACKGROUND,
                       preferredSize: [700, 100] )
                           {
                              borderLayout()

                              label(border: BorderFactory.createEmptyBorder(0, 10, 0, 0), icon: MERGE_IMAGE, constraints: WEST)

                               panel( constraints: CENTER,
                                       border:      BorderFactory.createEmptyBorder(10, 15, 10, 10),
                                       opaque:      true,
                                       background:  TOOLBAR_BACKGROUND )
                                       {
                                           borderLayout()

                                           label(preferredSize: [ 100, 25 ], text: "Select two or more filings to merge...", constraints: CENTER)
                                       }

                               panel( constraints: EAST,
                                       border:      BorderFactory.createEmptyBorder(20, 20, 10, 10),
                                       opaque:      true,
                                       background:  TOOLBAR_BACKGROUND )
                                       {
                                           borderLayout()

                                           merge = button(text: "Merge", font: TOOLBAR_FONT, constraints: CENTER, actionPerformed: doMerge, enabled: false)
                                       }
                           }

                   scrollPane(constraints: CENTER, preferredSize: [700, 500])
                   {
                      options = panel()
                      {
                         boxLayout(axis: BoxLayout.Y_AXIS)
                      }
                   }
                }

       options.opaque = true

       options.background = DEFAULT_BACKGROUND
       options.foreground = DEFAULT_FOREGROUND

       filings.values().each
       {
          Filing filing ->

          JCheckBox checkbox = swing.checkBox(text: filing.getDescription())

          checkbox.opaque = true

          checkbox.background = DEFAULT_BACKGROUND
          checkbox.foreground = DEFAULT_FOREGROUND

          checkbox.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
          checkbox.font   = checkbox.font.deriveFont(Font.BOLD)

          options.add(checkbox)

          choices[ filing ] = checkbox

          checkbox.actionPerformed = { ActionEvent e -> merge.enabled = gatherFilings().size() > 1 }
       }

       centre(frame)

       frame.show()
   }

   /**
    * Render two or more filings the user has opted to merge together.
    */
   private void showMerge(Merge merge)
   {
      Thread.start { ->

         try
         {
             Closure closeTab

             HTMLRenderer renderer = new HTMLRenderer({ closeTab() })
             renderer.render(merge)

             JComponent panel = renderer.view

             String title = "Merge"

             Dockable dockable = new DefaultDockable(title, panel, title, null, DockingMode.ALL)

             dock.addDockable(dockable, new Position(Position.CENTER))

             merges[ dockable ] = merge

             // Define the 'Close Tab' behaviour - which will have already been bound to the 'Close' button on the
             // rendered tab.
             closeTab =
             {
                dock.removeDockable(dockable)

                disableDocumentSpecificButtons()

                merges.remove(dockable)
             }

             enableDocumentSpecificButtons()
         }
         catch (Exception ex)
         {
             ex.printStackTrace()
         }
      }
   }

   /**
    * Render a dialog that allows the user to split two or more previously merged filings.
    */
   private void openSplitDialog()
   {
       SwingBuilder swing = new SwingBuilder()

       JFrame frame
       JPanel options
       JButton split

       Map<Merge, JCheckBox> choices = [:]

       Closure gatherMerges = { ->

          List<Merge> chosen = []

          choices.each
          {
              Merge merge, JCheckBox checkbox ->

              if (checkbox.isSelected())
              {
                 chosen << merge
              }
          }

          return chosen
       }

       Closure doSplit =  { ActionEvent e ->

           List<Merge> chosen = gatherMerges()

           for (Merge merge : chosen)
           {
               for (Filing filing : merge.filings)
               {
                   renderFiling(filing)
               }
           }

           // Close the merged tab(s) we have just split out.
           List<Dockable> toClose = merges.entrySet().findAll { chosen.contains(it.value) }.collectAll { it.key }

           toClose.each
           {
               Dockable dockable ->

               dock.removeDockable(dockable)

               merges.remove(dockable)
           }

           enableDocumentSpecificButtons()

           frame.hide()
           frame.dispose()
       }

       frame = swing.frame(title: title,
                            size: [700, 600],
                      background: WHITE,
                      foreground: BLACK,
                       iconImage: LOGO_IMAGE)
                {
                   panel(constraints: NORTH,
                              border: BorderFactory.createCompoundBorder(
                                        BorderFactory.createLineBorder(TOOLBAR_FRAME, 1),
                                        BorderFactory.createEmptyBorder(10, 10, 10, 10)),
                              opaque: true,
                          background: TOOLBAR_BACKGROUND,
                       preferredSize: [700, 100] )
                           {
                              borderLayout()

                              label(border: BorderFactory.createEmptyBorder(0, 10, 0, 0), icon: SPLIT_IMAGE, constraints: WEST)

                               panel( constraints: CENTER,
                                       border:      BorderFactory.createEmptyBorder(10, 15, 10, 10),
                                       opaque:      true,
                                       background:  TOOLBAR_BACKGROUND )
                                       {
                                           borderLayout()

                                           label(preferredSize: [ 100, 25 ], text: "Select one or more merges to split...", constraints: CENTER)
                                       }

                               panel( constraints: EAST,
                                       border:      BorderFactory.createEmptyBorder(20, 20, 10, 10),
                                       opaque:      true,
                                       background:  TOOLBAR_BACKGROUND )
                                       {
                                           borderLayout()

                                           split = button(text: "Split", font: TOOLBAR_FONT, constraints: CENTER, actionPerformed: doSplit, enabled: false)
                                       }
                           }

                   scrollPane(constraints: CENTER, preferredSize: [700, 500])
                   {
                      options = panel()
                      {
                         boxLayout(axis: BoxLayout.Y_AXIS)
                      }
                   }
                }

       options.opaque = true

       options.background = DEFAULT_BACKGROUND
       options.foreground = DEFAULT_FOREGROUND

       merges.values().each
       {
          Merge merge ->

          JCheckBox checkbox = swing.checkBox(text: merge.getDescription())

          checkbox.opaque = true

          checkbox.background = DEFAULT_BACKGROUND
          checkbox.foreground = DEFAULT_FOREGROUND

          checkbox.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
          checkbox.font   = checkbox.font.deriveFont(Font.BOLD)

          options.add(checkbox)

          choices[ merge ] = checkbox

          checkbox.actionPerformed = { ActionEvent e -> split.enabled = gatherMerges().size() > 0 }
       }

       centre(frame)

       frame.show()
   }

   /**
    * Export the currently selected filing to Excel.
    */
   private void exportFiling()
   {
      String filingName = dock.selectedDockable.title

      File output = Utils.generateFileName(filingName, "xls")

      Filing filing = getSelectedFiling()
      Merge  merge  = getSelectedMerge()

      ExcelRenderer renderer = new ExcelRenderer(output)

      if (filing != null)
      {
          renderer.render(filing)
      }
      else if (merge != null)
      {
          renderer.render(merge)
      }
      else
      {
          throw new IllegalStateException("Can't find the selected Filing or Merge.")
      }

      Desktop.getDesktop().open(output)
   }

   private Filing getSelectedFiling()
   {
      filings[ dock.getSelectedDockable() ]
   }

   private Merge getSelectedMerge()
   {
      merges[ dock.getSelectedDockable() ]
   }

   private void confirmExit()
   {
      int choice = JOptionPane.showConfirmDialog(
                                  this,
                                  "Are you sure you wish to exit XBurble?",
                                  "Exit the application?",
                                  JOptionPane.YES_NO_OPTION,
                                  JOptionPane.QUESTION_MESSAGE,
                                  EXIT_IMAGE)

      if (choice == JOptionPane.YES_OPTION) System.exit(0)
   }

   private void enableDocumentSpecificButtons()
   {
      mergeButton  .enabled = filings .size() > 1
      splitButton  .enabled = merges  .size() > 0
      exportButton .enabled = true
   }

   private void disableDocumentSpecificButtons()
   {
      mergeButton  .enabled = filings .size() > 1
      splitButton  .enabled = merges  .size() > 0
      exportButton .enabled = false
   }

   private void centre(window)
   {
      window.pack()

      Dimension screenSize = Toolkit.defaultToolkit.screenSize

      int xOffset = (screenSize.width  - window.width)  / 2
      int yOffset = (screenSize.height - window.height) / 2

      window.setLocation(xOffset, yOffset)
   }

   private void makeClicky(JTable table, Closure mouseClicked)
   {
      table.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

      table.selectionBackground = TABLE_HIGHLIGHT

      table.showGrid = false

      table.autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN

      table.mouseMoved = { e ->

         int row = table.rowAtPoint(e.point)

         table.selectionModel.setSelectionInterval(row, row)
      }

      table.mouseExited = { table.selectionModel.clearSelection() }

      table.mouseClicked = mouseClicked
   }
}
