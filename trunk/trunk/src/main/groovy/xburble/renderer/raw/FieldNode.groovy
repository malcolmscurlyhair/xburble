package xburble.renderer.raw

import javax.swing.tree.DefaultMutableTreeNode

/**
 * Custom node type, to display (named) fields on an object, for the (temporary) 'Structure' view.
 */
class FieldNode extends DefaultMutableTreeNode
{
   def FieldNode(Object o)
   {
      super(o)
   }

   String fieldName
}
