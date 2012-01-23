package xburble.renderer

import xburble.objects.Section
import xburble.objects.Context
import xburble.objects.Element
import xburble.objects.Datapoint
import xburble.services.DataProvider
import xburble.objects.Filing

/**
 * Before being rendered, Section information requires a little pre-processing, to decide what the key datapoints
 * are, and how they should be displayed.
 */
class RenderingInfo
{
   // Filing (or filings) we are rendering.
   List<Filing> filings

   // Section we are rendering.
   Section section

   // Data extracted during pre-processing.
   Map           partitions
   Section       lineItems
   List<Context> contexts

   // How deeply nested are the sections?
   int depth

   RenderingInfo(Filing filing, Section section)
   {
      this([ filing ], section)
   }

   RenderingInfo(List<Filing> filings, Section section)
   {
      this.filings = filings
      this.section = section

      if (section.element == null)
      {
         this.section = section.children[0]
      }

      init()
   }

   private void init()
   {
      // Top level section will have one child, and no-pertinent information.
      Section table = section.children[0]

      List<Section> axes = table.children.findAll { Section axis -> axis.element?.groupName == "xbrldt:dimensionItem" }

      lineItems = table.children.find { Section data -> data.element?.groupName == "xbrli:item" }

      if (axes.isEmpty()) lineItems = section

      // If there is only one axis, it will typically be the legal entity axis.  The filing is specific for one
      // particular legal entity, so this is kind of redundant.  Any further axes are more interesting - they will
      // define sub-categories, to group columns under.

      // Currently only cope with 1 or 2 axes.
      Map domains = [:]

      axes.each
      {
         Section axis ->

         assert axis.element?.groupName == "xbrldt:dimensionItem"

         if (axis.element.id.endsWith("LegalEntityAxis"))
         {
             // Not much to be gleaned here.
             return
         }

         List domainsForThisAxis = []

         domains[ axis.name ] = domainsForThisAxis

         // Axes will define a number of domains.
         axis.children.each
         {
            Section domain ->

            domain.children.each
            {
               Section member ->

               domainsForThisAxis << member
            }
         }
      }

      // That's it for the axes.  Now we have to walk the data points, and pick out the various contexts - then
      // decide what (if any) domains they fall under.
      contexts = []

      def walkData

      walkData =
      {
         Section data ->

         if (data.element != null)
         {
             data.element.datapoints.each
             {
                 Context ctx, Datapoint point ->

                 if (ctx == null) return

                 // Reject this context, if the periodType is set inappropriately.
                 if (data.element.periodType != null)
                 {
                     if (data.element.periodType == 'duration' && ctx.period.instant != null) return
                     if (data.element.periodType == 'instant'  && ctx.period.instant == null) return
                 }

                 if (!contexts.contains(ctx))
                 {
                     contexts << ctx
                 }
             }
         }

         if (data.children != null)
         {
             data.children.each
             {
                 Section child ->

                 walkData(child)
             }
         }
      }

      walkData(lineItems)

      // We should have *some* contexts by this point.
      if (contexts.size() == 0) return

      // Sort the contexts into their natural order.
      contexts = contexts.sort()

      // Filter out contexts which we are not interested in.
      contexts = contexts.findAll {
                                      Context ctx ->

                                      // Retain any instants occurring two years before the earliest filing date.
                                      if (ctx.period.instant != null)
                                      {
                                          return filings.any { Filing filing -> filing.filingDate.year - ctx.period.instant.year < 2 }
                                      }

                                      // Retain any periods that satisfy the same condition, providing they
                                      // fall on the same month as the filing date.
                                      if (filings.any { Filing filing -> ctx.period.endDate.format("MMM") == filing.filingDate.format("MMM") })
                                      {
                                          return filings.any { Filing filing -> filing.filingDate.year - ctx.period.endDate.year < 2 }
                                      }

                                      return false
                                   }

      // Partition the contexts by domain - the table we will eventually render will a little like:
      //    __________________________________________________________________________
      //   |              |                             |                             |
      //   |              |  DOMAIN 1                   |  DOMAIN 2                   |
      //   |______________|_____________________________|_____________________________|
      //   |              |              |              |              |              |
      //   |              |  CONTEXT 1   |  CONTEXT 2   |  CONTEXT 3   |  CONTEXT 4   |
      //   |______________|______________|______________|______________|______________|
      //   |              |              |              |              |              |
      //   |  Label       |  Data        |  Data        |  Data        |  Data        |
      //   |______________|______________|______________|______________|______________|

      partitions = contexts.groupBy
      {
          Context ctx ->

          if (domains.isEmpty())      return null
          if (ctx.segments.isEmpty()) return null

          def locator = [:]

          ctx.segments.each
          {
             Element segment, String name ->

             if (domains.containsKey(segment.name))
             {
                locator[ segment.name ] = name
             }
          }

          if (locator.isEmpty()) return null

          return locator
      }
   }
}
