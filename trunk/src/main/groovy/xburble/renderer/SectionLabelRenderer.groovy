package xburble.renderer

import static xburble.renderer.Resources.*
import java.awt.Component
import javax.swing.ListCellRenderer
import javax.swing.JList
import xburble.objects.*
import javax.swing.JLabel
import java.awt.Color
import javax.swing.BorderFactory
import java.awt.Dimension

/**
 * Renderer used by the section list of the HTMLRenderer view.
 */
class SectionLabelRenderer implements ListCellRenderer
{
   int showingRow = -1

   public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
   {
      String text = value.toString()

      String displayText = text.split("-")[-1]

      if (index == showingRow)
      {
         displayText = "<b>" + displayText + "</b>"
      }

      String colourCode = isSelected ? TABLE_HIGHLIGHT_HTML_CODE : WHITE_COLOUR_HTML_CODE

      JLabel label = new JLabel("""<html><div style="background-color: $colourCode">""" + displayText + "</div></html>")

      if (text.toUpperCase().contains("DOCUMENT"))
      {
         label.setIcon(DOCUMENT_IMAGE)
      }
      else if (text.toUpperCase().contains("DISCLOSURE"))
      {
         label.setIcon(DISCLOSURE_IMAGE)
      }
      else if (text.toUpperCase().contains("STATEMENT"))
      {
         label.setIcon(STATEMENT_IMAGE)
      }
      else if (text.toUpperCase().contains("NOTES"))
      {
         label.setIcon(NOTES_IMAGE)
      }

      if (isSelected)
      {
         label.background = TABLE_HIGHLIGHT
         label.foreground = Color.WHITE
      }
      else
      {
         label.background = DEFAULT_BACKGROUND
         label.foreground = DEFAULT_FOREGROUND
      }

      label.setPreferredSize(new Dimension(200,45))

      label.setEnabled(list.isEnabled());
      label.setFont(list.getFont());
      label.setOpaque(true);

      label.border = BorderFactory.createEmptyBorder(3,3,3,3)

      return label;
   }
}
