package xburble.renderer.raw

import javax.swing.tree.TreeCellRenderer
import javax.swing.JLabel
import java.awt.Font
import javax.swing.UIManager
import java.awt.Component
import javax.swing.JTree
import java.awt.Color
import javax.swing.tree.DefaultTreeCellRenderer
import xburble.renderer.raw.ElementNode
import xburble.renderer.raw.FieldNode

/**
 * Custom renderer, for the raw document view.
 */
class ObjectNodeRenderer implements TreeCellRenderer
{
   private JLabel leafRenderer = new JLabel()

   private DefaultTreeCellRenderer nonLeafRenderer = new DefaultTreeCellRenderer()

   Color selectionBorderColor,
         selectionForeground,
         selectionBackground,
         textForeground,
         textBackground

   protected JLabel getLeafRenderer()
   {
      return leafRenderer
   }

   public void setFont(Font font)
   {
      leafRenderer.font = font
   }

   public ObjectNodeRenderer()
   {
      Font fontValue = UIManager.getFont("Tree.font")

      if (fontValue != null)
      {
         leafRenderer.font = fontValue
      }

      leafRenderer.opaque = true

      selectionBorderColor = UIManager.getColor("Tree.selectionBorderColor")
      selectionForeground  = UIManager.getColor("Tree.selectionForeground")
      selectionBackground  = UIManager.getColor("Tree.selectionBackground")
      textForeground       = UIManager.getColor("Tree.textForeground")
      textBackground       = UIManager.getColor("Tree.textBackground")
   }

   public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
   {
      Object type        = value.userObject.getClass().getName().replaceAll("xburble.objects.","").replaceAll("java.lang.","").replaceAll("java.util.","")
      String stringValue = tree.convertValueToText(value, selected, expanded, leaf, row, false).replaceAll(" +"," ")

      String text        = "[$type] $stringValue"

      if (value instanceof FieldNode)
      {
         if (value.userObject instanceof Map || value.userObject instanceof Collection)
         {
            // For a collection or map, the result of calling toString() can get very verbose.
            text = ""
         }

         text = ("<html><b>${value.fieldName}</b> " + text + "</html>")
      }

      if (value instanceof ElementNode)
      {
         // Print out the index, as well as the value.
         text = ("<html><b>${value.index.toString().padRight(2)}<b>" + text + "</html>")
      }

      leafRenderer.text    = text
      leafRenderer.enabled = tree.enabled

      if (selected)
      {
         leafRenderer.foreground = selectionForeground
         leafRenderer.background = selectionBackground
      }
      else
      {
         leafRenderer.foreground = textForeground
         leafRenderer.background = textBackground
      }

      return leafRenderer
   }
}
