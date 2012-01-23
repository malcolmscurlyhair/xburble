package xburble.renderer.raw

import javax.swing.tree.DefaultMutableTreeNode

/**
 * Custom node type, to display (indexed) objects in a collection, for the (temporary) 'Structure' view.
 */
class ElementNode extends DefaultMutableTreeNode
{
   def ElementNode(Object o)
   {
      super(o)
   }

   int index
}
