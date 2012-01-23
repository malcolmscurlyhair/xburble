package xburble.renderer

import javax.swing.tree.TreePath
import javax.swing.tree.TreeNode
import javax.swing.JTree
import xburble.objects.Label
import xburble.renderer.raw.FieldNode
import java.lang.reflect.Field
import javax.swing.tree.DefaultMutableTreeNode
import xburble.renderer.raw.ElementNode
import xburble.objects.Section
import xburble.renderer.raw.ObjectNodeRenderer
import java.awt.Font
import static java.awt.Font.*
import xburble.objects.Filing
import xburble.objects.Merge

/**
 * Renders individual Section instances as a JTree widget, showing the raw, internal structure of a filing.  Uses
 * the reflection APIs to walk the object tree, an output which will look a little like the inspection window of
 * a debugger window in an IDE.
 */
class DocumentStructureRenderer implements Renderer
{
   // Rendering font.
   Font font = new Font("Consolas",  PLAIN,  11)

   // Generated JTree component.
   JTree tree

   /**
    * Rendering of whole Filing instances is not currently supported.
    */
   public void render(Filing filing)
   {
      throw new IllegalStateException("Rendering of whole Filing instances is not currently supported.")
   }

   /**
    * Rendering of Merges not currently supported.
    */
   public void render(Merge merge)
   {
      throw new IllegalStateException("Rendering of whole Merge instances is not currently supported.")
   }

   /**
    * Render a Section of a Filing, as a JTree widget.
    */
   public void render(Section section)
   {
      DefaultMutableTreeNode root = new DefaultMutableTreeNode(section)

      tree = new JTree(root)
      tree.font  = font
      renderDetailNode(section, root)

      tree.cellRenderer = new ObjectNodeRenderer()

      expandAll(tree)
   }

   private void renderStructureNode(Section section, DefaultMutableTreeNode node)
   {
      if (section == null)
      {
         return
      }

      section.children.each
      {
         Section subSection ->

         DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(subSection)

         node.add(subNode)

         renderStructureNode(subSection, subNode)
      }
   }

   private void renderDetailNode(Object object, DefaultMutableTreeNode node)
   {
      renderDetailNode(object, node, [])
   }

   private void renderDetailNode(Object object, DefaultMutableTreeNode node, Collection alreadyEncountered)
   {
      if (object == null)
      {
         return
      }

      if (object instanceof Collection)
      {
         object.eachWithIndex { Object value, int i ->

            ElementNode child = new ElementNode(value)

            child.index = i

            node.add(child)

            if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Label)
            {
               return
            }

            renderDetailNode(value, child, alreadyEncountered)
         }

         return
      }

      if (object instanceof Map)
      {
         object.each { Object key, Object value ->

            FieldNode child = new FieldNode(value)

            child.fieldName = key

            node.add(child)

            if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Label)
            {
               return
            }
         }

         return
      }

      object.getClass().getDeclaredFields().each
      {
         Field field ->

         if (field.name == "name")          return
         if (field.name == "parent")        return
         if (field.name.contains('$'))      return
         if (field.name == "__timeStamp")   return
         if (field.name == "label")         return

         field.accessible = true

         Object value = field.get(object)

         if (value == null) return

         if (alreadyEncountered.contains(value))
         {
            return
         }

         alreadyEncountered << value

         if (value instanceof MetaClass || value instanceof java.lang.ref.SoftReference || value instanceof Class )
         {
            return
         }

         def subNode

         if (field.name == "children" || field.name == "element")
         {
            subNode = node
         }
         else
         {
            subNode = new FieldNode(value)

            subNode.fieldName = field.name

            node.add(subNode)
         }

         if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Label)
         {
            return
         }

         renderDetailNode(value, subNode, alreadyEncountered)
      }
   }

   private void expandAll(JTree tree)
   {
      expandAll(tree, true);
   }

   private void expandAll(JTree tree, boolean expand)
   {
      expandAll(tree, new TreePath(tree.model.root), expand);
   }

   private void expandAll(JTree tree, TreePath parent, boolean expand)
   {
      // Traverse children
      TreeNode node = (TreeNode)parent.getLastPathComponent();

      if (node.getChildCount() >= 0)
      {
         for (Enumeration e=node.children(); e.hasMoreElements();)
         {
            TreeNode n = (TreeNode)e.nextElement();
            TreePath path = parent.pathByAddingChild(n);
            expandAll(tree, path, expand);
         }
      }

      // Expansion or collapse must be done bottom-up
      if (expand)
      {
         tree.expandPath(parent);
      }
      else
      {
         tree.collapsePath(parent);
      }
   }
}
