package xburble.services

import groovy.util.slurpersupport.NodeChild
import xburble.objects.*

import org.ccil.cowan.tagsoup.Parser
import xburble.objects.search.CompanyList
import xburble.objects.search.SearchResults
import xburble.objects.search.FilingList
import xburble.objects.search.RecentFilings

/**
 * Functionality for reading in filings from their raw XML, and locating filings on the SEC website, via web-scraping.
 */
class XML extends DataProvider
{
   // The HTML of the web page we are scraping.
   String html

   // Interim collections, used during extraction.
   Map<String, Context>   contexts                  = [:]
   Map<String, Unit>      units                     = [:]
   Map<String, Element>   elementDescriptionsByID   = [:]
   Map<String, Element>   elementDescriptionsByName = [:]
   Map<String, Map>       datapointsByContext       = [:]
   Map<String, String>    referencedSchemas         = [:]
   Map<String, String>    namespaces                = [:]
   Map<String, String>    annotations               = [:]

   // In-memory cache of type definitions, keyed by namespace.
   Map<String, List<Element>> definitions = [:]

   // The parsed data.
   Filing filing

   /**
    * Find a list of companies matching the supplied pattern.
    */
   SearchResults findCompany(String pattern)
   {
      NodeChild html = parseHTML("http://www.sec.gov/cgi-bin/browse-edgar?company=${ encode(pattern) }&match=&CIK=&filenum=&State=&Country=&SIC=&owner=exclude&Find=Find+Companies&action=getcompany")

      // Double check we haven't matched exactly one company, and been posted through to the list of filings.
      if (!html.toString().contains('Companies with names matching'))
      {
         Status.broadcast("Found one match, showing filings...")

         return extractFilingsFromHTML(html)
      }

      CompanyList results = new CompanyList()

      // Find the 'Results' table, (a <table> tag with a 'summary' attribute of 'Results'), and extract the
      // contents into a CompanyList object.
      html.body.'**'.findAll
                     {
                        tag -> tag.name() == 'table' && tag.@summary == 'Results'
                     }
                    .each
                     {
                        table ->

                        html.body.'**'.findAll { tag -> tag.name() == 'tr' }.each
                        {
                           tr ->

                           def row_data = []

                           tr.td.each
                           {
                              td ->

                              row_data << td.text()
                           }

                           if (row_data.isEmpty()) return

                           // Construct company-specific link if applicable.
                           row_data << "http://www.sec.gov/cgi-bin/browse-edgar?action=getcompany&CIK=${ row_data[0] }&type=&dateb=&owner=exclude&count=100"

                           results.add( row_data )
                        }
                     }

      return results
   }

   /**
    * Search for filings for the company with the supplied CIK.
    */
   FilingList findFilingsForCompany(String cik)
   {
      loadFilingsFromURL( "http://www.sec.gov/cgi-bin/browse-edgar?action=getcompany&CIK=${ encode(cik) }&type=&dateb=&owner=exclude&count=100" )
   }

   /**
    * Scrape a list of filings from the supplied URL.
    */
   private FilingList loadFilingsFromURL(String url)
   {
      NodeChild html = parseHTML( url )

      extractFilingsFromHTML(html)
   }

   /**
    * Scrape a list of filings from the supplied (parsed) HTML.
    */
   private FilingList extractFilingsFromHTML(NodeChild html)
   {
      FilingList results = new FilingList()

      // Find all the relevant details.
      html.body.'**'.findAll
                     {
                        tag -> tag.name() == 'table' && tag.@summary == 'Results'
                     }
                    .each
                     {
                        table ->

                        html.body.'**'.findAll { tag -> tag.name() == 'tr' }.each
                        {
                           tr ->

                           String filingType = tr.td[0].text()

                           if (filingType != '10-Q') return

                           def link = tr.td[1].a

                           String href        = link[0].@href
                           String linkText    = tr.td[1].text().trim()
                           String description = tr.td[2][0].children().findAll { it instanceof String }.join('<br>')
                           String filingDate  = tr.td[3].text().trim()

                           if (!linkText.contains('Interactive')) return

                           String onlineViewer = tr.td[1][0].children()[2].attributes['href']

                           results.add( [ filingDate, description, href, onlineViewer ] )
                        }
                     }

      return results
   }

   /**
    * Retrieve a list of recently submitted filings from the RSS feed on the EDGAR site.
    */
   RecentFilings getRecentFilings()
   {
       RecentFilings recent = new RecentFilings()

       NodeChild rss =  new XmlSlurper().parseText( new URL("http://www.sec.gov/Archives/edgar/usgaap.rss.xml").text )

       rss.channel.item.each
       {
           NodeChild item ->

           recent.add([ item.xbrlFiling.cikNumber.text(),
                        item.xbrlFiling.companyName.text(),
                        item.xbrlFiling.filingDate.text(),
                        item.link.text() ])
       }

       return recent
   }

   /**
    * Parse a web-page into tags.
    */
   private NodeChild parseHTML(String url)
   {
      Status.broadcast "Parsing HTML at $url"

      new XmlSlurper(new Parser()).parseText( new URL(url).text )
   }

   /**
    * Load in a filing from a specified directory - which should contain the instance document, and companion documents.
    */
   Filing loadFromDirectory(File dir)
   {
      if (filing == null)
      {
         this.filing = new Filing()
      }

      File instance
      File schema
      File presentation
      File label
      File definition
      File calculation

      dir.eachFile
      {
         file ->

         if (file.name.toLowerCase().endsWith("xsd")) schema = file
         else
         {
            if (file.name.toLowerCase().matches(".*cal.*xml"))      calculation  = file
            else if (file.name.toLowerCase().matches(".*pre.*xml")) presentation = file
            else if (file.name.toLowerCase().matches(".*lab.*xml")) label        = file
            else if (file.name.toLowerCase().matches(".*def.*xml")) definition   = file
            else if (file.name.toLowerCase().matches(".*xml"))      instance     = file
         }
      }

      load(instance, schema, presentation, label, definition)

      return filing
   }

   /**
    * Find the top-level details (company name, CIK, filing date, link the online viewer) from a filing URL.
    */
   void findHeadlineDetails(URL url)
   {
      if (filing == null)
      {
         this.filing = new Filing()
      }

      if (html == null)
      {
         html = url.text.replaceAll('\r', ' ').replaceAll('\n', ' ')
      }

      NodeChild parsed = new XmlSlurper(new Parser()).parseText(html)


      parsed.'**'.findAll { NodeChild tag -> tag.name() == 'a' && tag.@id == 'interactiveDataBtn'  }
                 .each    { NodeChild a   -> filing.onlineViewer = "http://www.sec.gov" + a.@href  }

      parsed.'**'.findAll { NodeChild tag  -> tag.name() == 'span' && tag.@class == 'companyName'  }
                 .each    { NodeChild span -> filing.company = span.text().split("\\(")[0]         }

      parsed.'**'.findAll { NodeChild tag  -> tag.name() == 'div' && tag.text() == 'Filing Date'   }
                 .each    {

                      NodeChild div ->

                      String glob = div.parent().text().replaceAll("[^0-9\\-]", "")

                      filing.filingDate = Date.parse("yyyy-MM-dd", glob[0..9])
                   }

      filing.cik = filing.onlineViewer.split("cik=")[1].split("&")[0]
   }

   /**
    * Load in a filing from a URL on the SEC website.
    */
   Filing loadFromURL(URL url)
   {
      if (filing == null)
      {
         this.filing = new Filing()
      }

      URL instance
      URL schema
      URL presentation
      URL label
      URL definition
      URL calculation

      if (html == null)
      {
         html = url.text.replaceAll('\r', ' ').replaceAll('\n', ' ')
      }

      html.split("href").each
      {
         String fragment ->

         fragment = fragment.split('"')[1]

         String prefix = "http://www.sec.gov"

         if      (fragment.endsWith(".xsd"))     schema       = new URL(prefix + fragment)
         else if (fragment.endsWith("_cal.xml")) calculation  = new URL(prefix + fragment)
         else if (fragment.endsWith("_def.xml")) definition   = new URL(prefix + fragment)
         else if (fragment.endsWith("_lab.xml")) label        = new URL(prefix + fragment)
         else if (fragment.endsWith("_pre.xml")) presentation = new URL(prefix + fragment)
         else if (fragment.endsWith(".xml"))     instance     = new URL(prefix + fragment)
      }

      load(instance, schema, presentation, label, definition)

      return filing
   }

   /**
    * Load in a filing.
    *
    * The instance document holds all the data points - numbers, descriptive text, dates -
    * but all jumbled up and without any idea of what each data point means, and how it is
    * to be presented.  It also contain a list of 'contexts' - typically time periods, or
    * time periods for some subject matter - that serve to group the data points together,
    * in a vague, touchy-feely manner.
    *
    * To make any sense of this, we have reach out to the companion XML documents, and figure
    * out how to present the information according to the meta-data.
    */
   private void load(instanceXML, schemaXSD, presentationXML, labelXML, definitionXML)
   {
      // Parse the main XML document...
      NodeChild instance = parse(instanceXML)

      // ... and its schema.
      NodeChild schema = parse(schemaXSD)

      // Find any namespaces mentioned in the instance document...
      Map namespaces = getNamespaces(instance)

      namespaces.each {

         prefix, namespace ->

         Status.broadcast "  Found that prefix '$prefix' corresponds to namespace $namespace"
      }

      // ...then resolve their location using the schema.
      Map namespaceLocations = [:]

      // Keys will be the attribute name, values will be the something like 'http://xbrl.org/2005/xbrldt'.  Resolve these
      // values via <import> tags found in the schema.
      schema.'import'.each
      {
         NodeChild importTag ->

         String namespace      = importTag.@namespace
         String schemaLocation = importTag.@schemaLocation

         Status.broadcast "  Resolved namespace '$namespace' to URL $schemaLocation"

         namespaceLocations[ namespace ] = schemaLocation
      }

      // Make sure we actually do have all the commonly used namespaces.
      if (namespaceLocations[ 'http://xbrl.sec.gov/dei/2011-01-31' ] == null)
      {
         namespaceLocations [ 'http://xbrl.sec.gov/dei/2011-01-31' ] = 'http://xbrl.sec.gov/dei/2011/dei-2011-01-31.xsd'
      }

      if (namespaceLocations[ 'http://fasb.org/us-gaap/2011-01-31' ] == null)
      {
         namespaceLocations [ 'http://fasb.org/us-gaap/2011-01-31' ] = 'http://xbrl.fasb.org/us-gaap/2011/elts/us-gaap-2011-01-31.xsd'
      }

      // Now, what of the actual data points?  We have to look up <element> tags in the schema file,
      // so we know what elements to expect.
      loadElementsFromSchema(schema, schemaXSD)

      // Look up element definitions in linked schemas too.
      namespaceLocations.each
      {
          namespace, xsd ->

          Status.broadcast "Loading type definitions for namespace $namespace"

          List<Element> typeDefinitions = definitions[namespace]

          if (typeDefinitions != null)
          {
             Status.broadcast("Found type definitions for $namespace in memory cache")

             typeDefinitions.each
             {
                Element e ->

                elementDescriptionsByID   [ e.id   ] = e
                elementDescriptionsByName [ e.name ] = e
             }
          }
          else
          {
             typeDefinitions = loadElementsFromSchema(parseSchema(xsd), xsd)

             definitions[namespace] = typeDefinitions
          }
      }

      // Now, remember all the various contexts, filing them away by unique ID.
      instance.context.each
      {
         NodeChild context ->

         String id  = context.@id.text()
         String cik = context.entity.identifier.text()

         Map segments = [:]

         context.entity.segment.explicitMember.each
         {
            NodeChild dimension ->

            String dimensionID = dimension.@dimension.text().replaceAll(':', '_')

            Element segmentDefinition =  elementDescriptionsByID.get(dimensionID)

            segments[ segmentDefinition ] = dimension.text().replaceAll('[\n\r]','').trim()
         }

         Entity entity = new Entity(cik: cik)
         Period period = new Period(
                                context.period.startDate?.text(),
                                context.period.endDate?.text(),
                                context.period.instant?.text()
                             )

         contexts[ id ] = new Context(id: id, entity: entity, period: period, segments: segments)
      }

      Status.broadcast "Found ${ contexts.size() } individual contexts."

      // Remember the units.
      instance.unit.each
      {
          NodeChild node ->

          Unit unit

          if (node.measure.any())
          {
              unit = new Unit(measure: node.measure.text())
          }
          else
          {
              unit = new Unit(
                           numerator:   node.divide.unitNumerator.measure.text(),
                           denominator: node.divide.unitDenominator.measure.text()
                     )
          }

          units[ node.@id.text() ] = unit
      }

      /**
       * Okay, we now what data points to expect. Read each element out of the instance document,
       * and file them away against the linked contexts.
       */

      // (Actually, before we to that, we need to do some shuffling around of namespaces.  The schema
      // document specifies a namespace prefix for each element, but Groovy XML parsing appears to throw
      // away the prefix when parsing the instance document; and instead only store the namespace name,
      // something like 'http://xbrls.org/2008/xbrls/metapattern/hierarchy'.  To allow for this, we dig
      // around the available namespaces, and create a map of namespace name to prefix, in order to do a
      // reverse lookup when the time comes.

      // Yes, I have no idea what I am doing.)

      Map namespacesByPrefix = getNamespaces(instance)

      // Invert the map, so keys becomes values and values keys.
      namespacesByPrefix.each { prefix, name -> namespaces[name] = prefix }

      int datapointCount = 0

      elementDescriptionsByID.each
      {
         String id, Element element ->

         // The ID tag in the schema will look something like:
         //
         //    id="ck0001527728_CapitalContributed"
         //
         // We are looking for a tag in the instance document like:
         //
         //    <ck0001527728:CapitalContributed>
         //

         if (!id.contains('_')) return

         def (prefix, name) = id.split('_')

         def nodes = instance.children().findAll()
         {
            NodeChild node ->

            node.name() == name && prefix == namespaces[ node.namespaceURI() ]
         }

         nodes.each
         {
            NodeChild datapoint ->

            def contextID = datapoint.@contextRef.text()

            def datapointsUnderThisContext = datapointsByContext[ contextID ]

            if (datapointsUnderThisContext == null)
            {
               datapointsUnderThisContext = [:]
               datapointsByContext[ contextID ] = datapointsUnderThisContext
            }

            String tagName = datapoint.name()

            String decimals = datapoint.@decimals.text()

            Datapoint data = new Datapoint(
                                            name:     tagName,
                                            value:    datapoint.text(),
                                            element:  element,
                                            unit:     units [ datapoint.@unitRef.text() ],
                                            decimals: decimals != null && decimals.length() > 0 && decimals.toUpperCase() != "INF"
                                                         ? new Integer(decimals) : null
                                          )

            datapointCount++

            datapointsUnderThisContext[ tagName ] = data

            // Record the datapoint against the element definition, keyed by the context.
            Context ctx = contexts[ contextID ]

            element.datapoints[ ctx ] = data

            data.context = ctx
         }
      }

      Status.broadcast "Found $datapointCount individual data items."


      // Finally, pull in the filing date - this will dictate which datapoints are relevant to render in the final output.
      if (instance.DocumentPeriodEndDate != null && instance.DocumentPeriodEndDate.text().length() > 0)
      {
         filing.filingDate = Date.parse(Period.INPUT_FORMAT, instance.DocumentPeriodEndDate.text())
      }

      if (instance.EntityRegistrantName != null && instance.EntityRegistrantName.text().length() > 0)
      {
         filing.company = instance.EntityRegistrantName.text()
      }

      if (instance.EntityCentralIndexKey != null && instance.EntityCentralIndexKey.text().length() > 0)
      {
         filing.cik = instance.EntityCentralIndexKey.text()
      }

      /**
       * Right, we now have all the data points, the units, and the context objects.  Now we have to figure out how to
       * present all of this stuff, and label it.  The presentation is described in the presentation XML document,
       * and the labels in the label document.  (There's definitions and calculations, too, but we'll get to them in
       * a moment.)
       */

      /**
       * First load in the <roleRef> tags in the presentation document.  They will look like:
       *
       *   <roleRef xlink:type = "simple"
       *            xlink:href = "Hierarchy.xsd#AccountingPolicies"
       *            roleURI    = "http://xbrls.org/2008/xbrls/metapattern/hierarchy/AccountingPolicies" />
       *
       * ...and serve to link <presentationLink> tags with a matching role attribute back to an element
       * in the schema.
       *
       * (If that sounds painful to read, is was twice as painful to write.)
       */

      Map roleReferences = [:]

      NodeChild presentation = parse(presentationXML)

      presentation.roleRef.each
      {
         NodeChild roleReference ->

         String link = snip(roleReference.@href.text(), '#')   // e.g. And is value of 'Hierarchy.xsd#AccountingPolicies' gets shortened to 'AccountingPolicies'.
         String uri  = roleReference.@roleURI.text()           // e.g. We read in a value of 'http://xbrls.org/2008/xbrls/metapattern/hierarchy/AccountingPolicies'.

         roleReferences[ uri ] = link
      }

      /**
       * Now construct the presentation hierarchy.
       */
      presentation.presentationLink.each
      {
         NodeChild section ->

         // println "Loading presentation heirarchy for ${ section.@role } "

         String role     = section.@role.text()
         String uri      = role                    // We will find a role URI of e.g. 'http://xbrls.org/2008/xbrls/metapattern/hierarchy/AccountingPolicies'...
         String link     = roleReferences[uri]     // ...and look up a value of "Hierarchy.xsd#AccountingPolicies"

         Section root = new Section(name: snip(link, '#'), role: role)

         filing.sections << root

         /**
          * Pull out out the <loc> tags in this section, which will look something like:
          *
          *   <loc xlink:type  = "locator"
          *        xlink:label = "pattern_BankBorrowingsPolicy"
          *        xlink:href  = "Hierarchy.xsd#pattern_BankBorrowingsPolicy" />
          *
          * These link an element in the instance file, to a label.  (To be discovered later.)  They also imply that
          * the element is to rendered in a particular section.
          */
         Map<String, String> elementLabels = [:]

         section.loc.each
         {
            NodeChild loc ->

            if (loc.@type.text() != "locator")
            {
               throw new IllegalStateException("Expected type attribute to have value 'locator', instead found value ${loc.@type.text()}")
            }

            String elementReference = snip(loc.@href.text(), '#')
            String labelReference   = loc.@label.text()

            elementLabels[ labelReference ] = elementReference
         }

         /**
          * Now pull out all the <presentationArc> tags in this section, which will look something like:
          *
          *   <presentationArc xlink:type    = "arc"
          *                    xlink:arcrole = "http://www.xbrl.org/2003/arcrole/parent-child"
          *                    xlink:from    = "pattern_FinancialInstrumentsPolicyTextBlock"
          *                    xlink:to      = "pattern_BankBorrowingsPolicy"
          *                    order         = "3"
          *                    use           = "optional" />
          *
          * These link different <loc> tags into a tree structure.  (You might think it easier if we had just
          * embedded the <loc> tags within each other, in order to define the hierarchy; but once again, the
          * data format farts in your general direction.)
          *
          * The 'from' and 'to' elements define the parent-child relationship; the order defines where in the ordered
          * list of children the child belongs, on the parent.
          */
         Map<String, Section> subSections = [:]

         Map<Section, TreeMap<Double, Section>> orderedChildren = [:]

         section.presentationArc.each
         {
            NodeChild presentationArc ->

            if (presentationArc.@type.text() != "arc")
            {
               throw new IllegalStateException("Expected type attribute to have value 'arc', instead found value ${presentationArc.@type.text()}")
            }

            if (presentationArc.@arcrole.text() != "http://www.xbrl.org/2003/arcrole/parent-child")
            {
               throw new IllegalStateException("Expected arcrole attribute to refer to parent-child relationship, instead found value ${presentationArc.@arcrole.text()}")
            }

            String from  = snip( elementLabels[ presentationArc.@from.text() ], '_')
            String to    = snip( elementLabels[ presentationArc.@to.text()   ], '_')
            double order = Double.parseDouble(presentationArc.@order.text())

            Section parent = subSections[ from ]
            Section child  = subSections[ to   ]

            if (parent == null)
            {
               Element fromElement = elementDescriptionsByName[from]

               if (fromElement == null)
               {
                  // println " Can't find data point for $from"
               }

               parent = new Section(name: from, element: fromElement)
               subSections[from] = parent
            }

            if (child == null)
            {
               Element toElement = elementDescriptionsByName[to]

               if (toElement == null)
               {
                  // println "   Can't find data point for $to"
               }

               child = new Section(name: to, element: toElement)
               subSections[to] = child
            }

            if (orderedChildren[ parent ] == null)
            {
               orderedChildren[ parent ] = new TreeMap<Double, Section>()
            }

            orderedChildren[ parent ].put( order, child )
         }

         orderedChildren.each
         {
            parent, children ->

            int order = 0

            children.values().each
            {
               child ->

               parent.children[ order ] = child
               child.parent = parent

               order++
            }
         }

         // Find any and all roots.
         subSections.each
         {
            String name, Section subSection ->

            if (subSection.parent == null)
            {
                root.children << subSection
            }
         }
      }

      // Inject any annotations we have found into the sections.
      filing.sections.each
      {
         Section section ->

         section.annotation = annotations[ section.name ]
      }

      // Sort the top-level sections in alphabetical order.
      filing.sections = filing.sections.sort { it.toString().split(" ")[0] }

      // Load in the labels from the label file.
      NodeChild labels = parse(labelXML)

      /**
       * We have three types of node we are interested in:
       *
       *  o Locators   - these pick out elements in the schema (and hence tags in the instance document).
       *  o Labels     - supply some label to print, for a given language.  (Typically only US English is specified.)
       *  o Label Arcs - link locators to labels.
       *
       * First we pick out the locators, which will look a bit like:
       *
       *   <loc xlink:type  = "locator"
       *        xlink:href  = "http://xbrl.sec.gov/dei/2011/dei-2011-01-31.xsd#dei_DocumentType"
       *        xlink:label = "element756" />
       */

      Map<String, String> locators = [:]

      labels.labelLink.loc.each
      {
         NodeChild loc ->

         if (loc.@type.text() != "locator")
         {
            throw new IllegalStateException("Expected type attribute to have value 'locator', instead found value ${label.@type.text()}")
         }

         String href  = snip(loc.@href.text(), '#')
         String label = loc.@label.text()

         locators[ label ] = href
      }

      /**
       * Next, we pick out the labels.  These look a bit like:
       *
       *   <label xlink:type  = "resource"
       *          xlink:label = "label756"
       *          xlink:role  = "http://www.xbrl.org/2003/role/label"
       *          xml:lang    = "en-US"
       *          id          = "label_dei_DocumentType_en-US">Document Type</label>
       *
       */
      Map<String, Map<String, Label>> labelsByNameThenLanguage = [:]

      labels.labelLink.label.each
      {
         NodeChild label ->

         if (label.@type.text() != "resource")
         {
            throw new IllegalStateException("Expected type attribute to have value 'resource', instead found value ${label.@type.text()}")
         }

         if (!label.@role.text().contains('label'))
         {
            // Not a label, ignore.
            return
         }

         String id   = label.@id.text()
         String name = label.@label.text()
         String lang = label.@lang.text()
         String text = label.text()

         Label l = new Label(
                              id:       id,
                              language: lang,
                              text:     text
                            )

         Map<String, Label> labelsByLanguage = labelsByNameThenLanguage[ name ]

         if (labelsByLanguage == null)
         {
            labelsByLanguage = [:]
            labelsByNameThenLanguage[ name ] = labelsByLanguage
         }

         labelsByLanguage[ lang ] = l
      }

      /**
       * Finally, we pick out the arcs, which look like:
       *
       *    <labelArc xlink:type    = "arc"
       *              xlink:arcrole = "http://www.xbrl.org/2003/arcrole/concept-label"
       *              xlink:from    = "element756"
       *              xlink:to      = "label756" />
       */
      labels.labelLink.labelArc.each
      {
         NodeChild arc ->

         if (arc.@type.text() != "arc")
         {
            throw new IllegalStateException("Expected type attribute to have value 'arc', instead found value ${label.@type.text()}")
         }

         String from = snip( locators[ arc.@from.text() ], '_')
         String to   = arc.@to.text()

         Element element = elementDescriptionsByName[ from ]

         if (element == null)
         {
            Status.broadcast "Missing element for $from, referenced in label arc"
            return
         }

         Map<String, Label> labelsByLanguage = labelsByNameThenLanguage[to]

         if (labelsByLanguage != null)
         {
            element.label = element.label = labelsByLanguage[ 'en-US' ]
         }
      }

      /**
       * Resolve labels for the segments defined in each context.
       */
      contexts.each
      {
         String id, Context context ->

         if (context.segments.size() > 0)
         {
             Map<Element, String> resolved = [:]

             context.segments.each
             {
                 Element segment, String domain ->

                 // println "Trying to resolve label for domain $domain"

                 Element e = elementDescriptionsByName.get(domain.split(":")[-1])

                 if (e?.label != null)
                 {
                    resolved[ segment ] = e.label.text

                    // println "Resolved label to '${ e.label.text }'"
                 }
                 else
                 {
                    resolved[ segment ] = domain
                 }
             }

             context.segments = resolved
         }
      }

      /**
       * Filter out redundant elements that seem to be supplied with most filings.
       */
      Closure filter

      filter =
      {
         Section section, Closure criteria ->

         section.children = section.children.findAll { Section child -> !criteria(child) }

         section.children.each { Section child -> filter(child, criteria) }
      }

      for (Section section : filing.sections)
      {
         if (section.annotation != null)
         {
            if (section.annotation.matches("(?i).*Statement of Cash Flows"))
            {
               filter(section,
                      {
                         Section checking ->

                         return checking?.element?.id == "us-gaap_Cash"
                      })
            }

            if (section.annotation.matches("(?i).*Statements of Cash Flows.*"))
            {
               filter(section,
                      {
                         Section checking ->

                         return checking?.element?.id?.matches("us-gaap_Cash.*")
                      })
            }
         }
      }
   }

   /**
    * Find a schema (either at a remote location, or in the local resources, if we have a cached copy),
    * and parse the XML.
    */
   private NodeChild parseSchema(String url)
   {
      File file = new File("resources/schemas/" + url[7..-1])

      String text

      if (file.exists())
      {
         Status.broadcast "    ...found local definition for $url."

         return parse(file)
      }
      else
      {
         return parse( new URL(url) )
      }
   }

   /**
    * Load in all the 'element' tags from a schema, build up corresponding 'Element' objects, and file them away by 'id
    * and 'name' in the following instance variables:
    *
    *   o elementDescriptionsByID
    *   o elementDescriptionsByName
    *
    * Load in any annotations while we are at it.
    *
    * Return the list of Elements extracted, for caching.
    *
    * @param xsd   The (parsed) XML schema.
    */
   private List<Element> loadElementsFromSchema(NodeChild xsd, path)
   {
      List<Element> elements = []

      xsd.element.each
      {
         NodeChild element ->

         String id   = element.@id.text()
         String name = element.@name.text()

         Element description = new Element (
                                             id:         id,
                                             name:       name,
                                             typeName:   element.@type.text(),
                                             groupName:  element.@substitutionGroup.text(),
                                             periodType: element.@periodType.text(),
                                             isAbstract: element.@abstract.text() == "true",
                                             deprecated: element.@deprecatedDate.text() != ""
                                           )

         elements << description

         elementDescriptionsByID   [ id   ] = description
         elementDescriptionsByName [ name ] = description
      }

      xsd.annotation.appinfo.roleType.each
      {
         NodeChild annotation ->

         annotations[ annotation.@id.text() ] = annotation.definition.text()
      }

      // println "Loaded the following elements from $path:"

      // elements.each { Element element -> println ("   " + element) }

      return elements
   }

   /**
    * Extract the namespaces from a parsed XML object, using some reflective obfuscation.
    */
   private Map getNamespaces(xml)
   {
      def namespaceTagHints = xml.getClass().getSuperclass().getDeclaredField("namespaceTagHints")
      namespaceTagHints.setAccessible(true)

      return new LinkedHashMap(namespaceTagHints.get(xml))
   }

   /**
    * Parse an XML document, supplied either as a File, a URL, or raw text.
    * @param data
    * @return
    */
   private NodeChild parse(data)
   {
      if (data instanceof File)
      {
         Status.broadcast "Parsing $data.absolutePath"

         return new XmlSlurper().parse(data)
      }

      if (data instanceof String)
      {
         return new XmlSlurper().parseText(data)
      }

      if (data instanceof URL)
      {
         return new XmlSlurper().parseText( Cache.get(data) )
      }
   }

   /**
    * Split on the provided delimited, return anything after the first break.
    */
   private String snip(String arg, String divider)
   {
      return arg.substring(arg.indexOf(divider) + 1)
   }

   /**
    * Encode some text in an URL-friendly manner.
    */
   private String encode(txt)
   {
      URLEncoder.encode(txt, "ISO-8859-1")
   }
}
