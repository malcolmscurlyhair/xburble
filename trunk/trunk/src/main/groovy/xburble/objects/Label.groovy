package xburble.objects

/**
 * A label described in the 'label' XML file.  Used to describe Datapoints when rendering the filing.  Will look like:
 *
 *   <label xlink:type  = "resource"
 *          xlink:role  = "http://www.xbrl.org/2003/role/label"
 *          xlink:label = "us-gaap_AccountsPayableCurrent_lbl"
 *          xml:lang    = "en-US">
 *
 *      Accounts payable
 *
 *   </label>
 *
 */
class Label
{
   String id
   String language
   String text      // The text of the label.
   String role      // Probably redundant.

   String toString()
   {
      text
   }

   Label clone()
   {
      new Label([ id: id, language: language, text: text, role: role ])
   }
}
