package xburble.objects

/**
 * A tag in one of the schema files, describing a datapoint that can appear in the instance document.  Looks like:
 *
 *   <element id                = "pattern_AccountingPoliciesHierarchy"
 *            name              = "AccountingPoliciesHierarchy"
 *            type              = "xbrli:stringItemType"
 *            substitutionGroup = "xbrls:hierarchyGroup"
 *            xbrli:periodType  = "duration"
 *            abstract          = "true"
 *            nillable          = "true" />
 *
 * The an element in the schema document will corresponds to several tags in the instance document, each in a different
 * context.
 */
class Element
{
   String id
   String name
   String typeName
   String groupName
   String periodType
   boolean deprecated

   boolean isAbstract

   Label label

   Map<Context, Datapoint> datapoints = [:]

   String toString()
   {
      return id
   }

   String toStringMinusName()
   {
      "of type ${typeName} [${groupName}] ${isAbstract ? "<ABSTRACT>" : ""}"
   }
}
