package xburble.renderer

import java.awt.Color
import static java.awt.Color.*

import java.awt.Font
import static java.awt.Font.*
import javax.swing.ImageIcon
import javax.imageio.ImageIO
import java.awt.Image

/**
 * Common rendering resources.
 */
class Resources
{
    // Action descriptions (double as tooltips and command constants).
    static String SEARCH      = "Search for a filing"
    static String OPEN_RECENT = "Open a recently viewed filing"
    static String SHOW_NEW    = "Show recently submitted filings"
    static String MERGE       = "Merge tow or more filings togetherr"
    static String SPLIT       = "Split previously merged filings"
    static String EXPORT      = "Export this filing to Excel"
    static String EXIT        = "Exit the application"

    // Labels.
    static String SEARCH_LABEL      = "Search"
    static String OPEN_RECENT_LABEL = "Recent"
    static String NEW_FILINGS_LABEL = "Newest"
    static String MERGE_LABEL       = "Merge"
    static String SPLIT_LABEL       = "Split"
    static String EXPORT_LABEL      = "Export"
    static String EXIT_LABEL        = "Exit"

    // Images.
    static ImageIcon SEARCH_IMAGE      = findIcon("images/search.png")
    static ImageIcon OPEN_RECENT_IMAGE = findIcon("images/recent.png")
    static ImageIcon NEW_FILINGS_IMAGE = findIcon("images/new.png")
    static ImageIcon MERGE_IMAGE       = findIcon("images/merge.png")
    static ImageIcon SPLIT_IMAGE       = findIcon("images/split.png")
    static ImageIcon EXPORT_IMAGE      = findIcon("images/export.png")
    static ImageIcon EXIT_IMAGE        = findIcon("images/exit.png")
    static ImageIcon CLOSE_IMAGE       = findIcon("images/close.png")
    static ImageIcon LOADING_IMAGE     = findIcon("images/loading.gif")
    static ImageIcon DISCLOSURE_IMAGE  = findIcon("images/disclosure.png")
    static ImageIcon DOCUMENT_IMAGE    = findIcon("images/document.png")
    static ImageIcon STATEMENT_IMAGE   = findIcon("images/statement.png")
    static ImageIcon NOTES_IMAGE       = findIcon("images/notes.gif")

    static Image LOGO_IMAGE            = findImage("images/logo.gif")

    static ImageIcon findIcon(String path)
    {
        File local = new File("src/main/resources/" + path)

        if (local.exists())
        {
            return new ImageIcon(local.absolutePath)
        }
        else
        {
            InputStream stream = Resources.class.getClassLoader().getResourceAsStream(path)

            Image image = ImageIO.read(stream)

            return new ImageIcon(image)
        }
    }

    static Image findImage(String path)
    {
        File local = new File("src/main/resources/" + path)

        if (local.exists())
        {
            return ImageIO.read(local)
        }
        else
        {
            InputStream stream = Resources.class.getClassLoader().getResourceAsStream(path)

            return ImageIO.read(stream)
        }
    }

    // Fonts.
    static Font TOOLBAR_FONT    = new Font("Gill Sans MT", BOLD,  14)
    static Font TITLE_FONT      = new Font("Gill Sans MT", BOLD,  14)
    static Font STATUS_BAR_FONT = new Font("Consolas",     PLAIN, 10)
    static Font SECTION_FONT    = new Font("Helvetica",    PLAIN, 10)

    // Colours.
    static Color DEFAULT_BACKGROUND    = WHITE
    static Color DEFAULT_FOREGROUND    = BLACK
    static Color TOOLBAR_BACKGROUND    = new Color(230,230,235)
    static Color TOOLBAR_FRAME         = new Color(170,170,220)
    static Color TOOLBAR_LABEL         = new Color(30,30,35)
    static Color TOOLBAR_HIGHLIGHT     = new Color(130,130,235)
    static Color STATUS_BAR_BACKGROUND = new Color(240,240,245)
    static Color TABLE_HIGHLIGHT       = new Color(200, 200, 255)

    static String TABLE_HIGHLIGHT_HTML_CODE = "#c8c8ff"
    static String WHITE_COLOUR_HTML_CODE    = "#ffffff"

}
