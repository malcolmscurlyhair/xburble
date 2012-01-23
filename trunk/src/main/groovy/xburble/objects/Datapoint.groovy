package xburble.objects

/**
 * A datapoint in the instance document, which defines a single atomic data value.  (Though in many instances, this
 * data value can be HTML-formatted text.)
 *
 * Datapoints link to a unique context via a 'contextRef' attribute, and will look something like this in the XML:
 *
 *   <ck0001527728:ExpensePaidByRelatedPartyOnBehalfOfTheCompany
 *                  contextRef="eol_PE862008--1110-Q0003_STD_256_20110930_0"
 *                     unitRef="iso4217_USD"
 *                    decimals="0">
 *
 *      13735
 *
 *   </ck0001527728:ExpensePaidByRelatedPartyOnBehalfOfTheCompany>
 *
 * The datapoint will have a corresponding definition in a linked schema, which we load in and attached as an 'Element'
 * object. These definitions in the schema(s) will look something like:
 *
 *   <element name              = "ExpensePaidByRelatedPartyOnBehalfOfTheCompany"
 *            id                = "ck0001527728_ExpensePaidByRelatedPartyOnBehalfOfTheCompany"
 *            type              = "xbrli:monetaryItemType"
 *            abstract          = "false"
 *            xbrli:periodType  = "duration"
 *            xbrli:balance     = "debit"
 *            nillable          = "true"
 *            substitutionGroup = "xbrli:item"/>
 *
 * These define formatting rules for the data (I guess?).
 */
class Datapoint
{
   String  name     // Tag name.
   String  value    // Text within the tag.
   Element element  // Element definition, pulled from the xsd.
   Context context  // Back link to corresponding context this datapoint sits within.

   Unit unit        // Units for this datapoints.

   String toString()
   {
      "${name} ${element.toStringMinusName()} \n\n       ${value.trim()}\n"
   }

   Datapoint clone()
   {
      new Datapoint([ name: name, value: value, element: element?.clone(), context: context?.clone(), unit: unit?.clone() ])
   }

   Datapoint clone(Context context, Element element)
   {
      new Datapoint([ name: name, value: value, element: element, context: context, unit: unit?.clone() ])
   }
}
