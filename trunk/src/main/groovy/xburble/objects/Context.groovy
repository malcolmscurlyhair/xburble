package xburble.objects

/**
 * Datapoints belong to a single context, which links the data point to:
 *
 *   o A single legal entity (via a CIK).
 *   o A period in time (either a duration, or a point in time) .
 *   o Various 'segments' - which serve as categorisations of the datapoint(s) along various 'axes'.
 *
 * In instance the XML, they will look something like:
 *
 *    <context id='I-2007'>
 *      <entity>
 *        <identifier scheme='http://www.sec.gov/CIK'>1234567890</identifier>
 *        <segment>
 *          <xbrldi:explicitMember dimension="gaap:EntityAxis">          gaap:ABCCompanyDomain              </xbrldi:explicitMember>
 *          <xbrldi:explicitMember dimension="gaap:BusinessSegmentAxis"> gaap:ConsolidatedGroupDomain       </xbrldi:explicitMember>
 *          <xbrldi:explicitMember dimension="gaap:VerificationAxis">    gaap:UnqualifiedOpinionMember      </xbrldi:explicitMember>
 *          <xbrldi:explicitMember dimension="gaap:PremiseAxis">         gaap:ActualMember                  </xbrldi:explicitMember>
 *          <xbrldi:explicitMember dimension="gaap:ReportDateAxis">      gaap:ReportedAsOfMarch182008Member </xbrldi:explicitMember>
 *        </segment>
 *      </entity>
 *      <period>
 *        <instant>2007-12-31</instant>
 *      </period>
 *    </context>
 *
 * The ID is unique in the instance document.  Individual tags containing datapoints will reference this ID via their
 * 'contextRef' attribute.
 */
class Context implements Comparable
{
   String id       // The unique identifier for the tag, pulled from the 'id' attribute.
   Entity entity   // The entity this context applies to (defined by a CIK).
   Period period   // The period of time (either a duration, or a point in time) this context covers.

   Map<Element, String> segments = [:] // Where this context sits of various axes.

   String toString()
   {
      "[Context] ${period} ${id}"
   }

   boolean equals(Context ctx)
   {
       return this.compareTo(ctx) == 0
   }

   /**
    * Define a natural ordering on the Context.
    */
   int compareTo(Object ctx)
   {
       int result = entity.compareTo(ctx.entity)

       if (result != 0) return result

       result = period.compareTo(ctx.period)

       if (result != 0) return result

       if (segments.equals(ctx.segments)) return 0

       return id.compareTo(ctx.id)
   }

   Context clone()
   {
       Map<Element, String> clonedSegments = [:]

       segments.each
       {
           Element element, String domain ->

           clonedSegments [ element?.clone() ] = domain
       }

       new Context([ id: id, entity: entity?.clone(), period: period?.clone(), segments: clonedSegments ])
   }
}
