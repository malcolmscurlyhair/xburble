package xburble.objects

/**
 * Constructed from the presentation XML file.  Each section can have several children; hence we have a tree
 * structure. Depending on various type fields, and the number of children, we render a section as a table,
 * a text field, a table cell or row, etc.
 *
 * Each Section is build from a 'locator' tag in the presentation file:
 *
 *   <loc xlink:type  = "locator"
 *        xlink:href  = "ck0001527728-20110930.xsd#ck0001527728_CapitalContributed"
 *        xlink:label = "ck0001527728_CapitalContributed"/>
 *
 * The section links to an 'element' tag in the schema document, and (one or more) labels in the label document.  The
 * 'element' tag will in turn define one or more tags in the instance document, each with a unique ID:
 *
 *   <ck0001527728:CapitalContributed contextRef="eol_PE862008--1110-Q0003_STD_183_20110930_0" unitRef="iso4217_USD" decimals="0">
 *     25000
 *   </ck0001527728:CapitalContributed>
 *
 * This dictates what data requires rendering in the section.  The hierarchical structure for sections comes from
 * 'presentationArc' tags in the presentation XML, which link a parent and child:
 *
 *   <presentationArc xlink:type    = "arc"
 *                    xlink:arcrole = "http://www.xbrl.org/2003/arcrole/parent-child"
 *                    xlink:from    = "us-gaap_EquityComponentDomain"
 *                    xlink:to      = "us-gaap_AdditionalPaidInCapitalMember"
 *                    order         = "1.0900"
 *                    priority      = "2"
 *                    use           = "optional"/>
 *
 * Sections can also be annotated, via a 'roleRef' tag in the presentation document:
 *
 *   <roleRef  xlink:type = "simple"
 *             xlink:href = "ck0001527728-20110930.xsd#DocumentDocumentandEntityInformation"
 *             roleURI    = "http://www.0001527728.com/taxonomy/role/DocumentDocumentandEntityInformation"/>
 *
 * This will link through to an annotation in the schema document, which has form:
 *
 *   <annotation>
 *     <appinfo>
 *       <link:roleType roleURI="http://www.0001527728.com/taxonomy/role/DocumentDocumentandEntityInformation" id="DocumentDocumentandEntityInformation">
 *         <link:definition>101 - Document - Document and Entity Information</link:definition>
 *         <link:usedOn>link:calculationLink</link:usedOn>
 *         <link:usedOn>link:presentationLink</link:usedOn>
 *         <link:usedOn>link:definitionLink</link:usedOn>
 *       </link:roleType>
 *
 *       ...
 *
 *     </appInfo>
 *   <annotation>
 */
class Section
{
   String              name
   String              role
   Element             element
   Section             parent
   List<Section>       children = []
   Map<String, String> labels
   String              annotation

   String toString()
   {
      annotation != null ? annotation : (element?.label != null ? element.label : name)
   }

   String returnFullDescription()
   {
      toString(0)
   }

   String toString(int indent)
   {
      if (annotation != null) return "${" " * indent}${annotation}"

      String description = "${" " * indent}${name.padRight(50 - indent)} ${element?.toStringMinusName()}"

      children.each
      {
         Section child -> description += ("\n" + (child != null ? child.toString(indent + 2) : "?"))
      }

      return description
   }
}
