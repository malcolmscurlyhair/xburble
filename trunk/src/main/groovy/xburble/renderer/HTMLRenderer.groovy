package xburble.renderer

import groovy.swing.SwingBuilder

import static xburble.renderer.Resources.*

import static javax.swing.JSplitPane.*
import javax.swing.JList

import xburble.objects.Section

import javax.swing.BorderFactory
import javax.swing.JScrollPane
import xburble.objects.Context
import xburble.objects.Datapoint
import javax.swing.JEditorPane
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet
import javax.swing.text.Document
import javax.swing.JComponent
import static java.awt.BorderLayout.*
import java.awt.Cursor
import java.awt.event.MouseEvent
import java.awt.Dimension
import java.awt.Point
import javax.swing.JFrame
import java.awt.Desktop
import javax.swing.JButton
import xburble.objects.Filing
import java.text.NumberFormat
import java.text.DecimalFormat
import xburble.objects.Merge

/**
 * Renders a JPanel containing a JSplitPane: the left hand side will be a list of Section elements in a Filing,
 * the right hand side shows a given Section, when selected, as HTML.
 */
class HTMLRenderer extends AbstractRenderer
{
    // Top level components.
    JList       sections
    JScrollPane detailView
    JScrollPane structureView

    // Builder instance.
    SwingBuilder swing = new SwingBuilder()

    // Filing data to be rendered.
    Filing filing

    // Merge data to be rendered.
    Merge merge

    // Pre-processed data, ready for rendering.
    RenderingInfo info

    // Main view.
    JComponent view

    // HTML being written out write now.
    StringBuilder html

    // Function to execute when the 'Close' icon is clicked
    Closure close

    HTMLRenderer(Closure close)
    {
        this.close = close
    }

    /**
     * Build the main viewing area, a JSplitPane with Sections listed along the left, and a detail area on the right,
     * to be populated as a Section is highlighted.
     */
    void render(Filing filing)
    {
        this.filing = filing

        view = swing.panel(border: BorderFactory.createEtchedBorder())
                {
                    borderLayout()

                    SectionLabelRenderer sectionRenderer = new SectionLabelRenderer()

                    JButton rawButton

                    panel(constraints: NORTH, border: BorderFactory.createEmptyBorder(5,15,8,5))
                            {
                                borderLayout()

                                label(constraints: CENTER, font: TITLE_FONT, text: filing.company + " (" + filing.cik + ") " + filing.filingDate.format("ddMMMyyyy"))
                                panel(constraints: EAST)
                                        {
                                            flowLayout()

                                            button(text: "View Online",
                                                    enabled: filing.onlineViewer != null,
                                                    actionPerformed: { Desktop.getDesktop().browse(new URI(filing.onlineViewer)) })

                                            rawButton = button(text: "Show Raw Document Structure",
                                                    enabled: false,
                                                    actionPerformed: {

                                                        Section section = sections.model.getElementAt(sectionRenderer.showingRow)

                                                        DocumentStructureRenderer doc = new DocumentStructureRenderer()
                                                        doc.render(section)

                                                        JFrame frame = new JFrame()

                                                        frame.add(new JScrollPane(doc.tree))
                                                        frame.setPreferredSize(new Dimension(800,600))
                                                        frame.pack()
                                                        frame.show()
                                                    })


                                            label(  icon: CLOSE_IMAGE,
                                                    enabled: false,
                                                    mouseEntered: { MouseEvent e -> e.component.enabled = true  },
                                                    mouseExited: { MouseEvent e -> e.component.enabled = false },
                                                    mouseClicked: close,
                                                    cursor: Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
                                        }
                            }

                    swing.splitPane(
                            border:          BorderFactory.createEtchedBorder(),
                            constraints:     CENTER,
                            dividerLocation: 250,
                            orientation:     HORIZONTAL_SPLIT,
                            leftComponent:   swing.scrollPane(border: BorderFactory.createEmptyBorder())
                                    {
                                        sections = list(
                                                font: SECTION_FONT,
                                                listData: filing.sections,
                                                cellRenderer: sectionRenderer,
                                                border: BorderFactory.createEmptyBorder(10, 10, 10, 10),
                                                cursor: Cursor.getPredefinedCursor(Cursor.HAND_CURSOR),
                                                mouseMoved: { MouseEvent e ->

                                                    Point p = new Point(e.getX(), e.getY())
                                                    int index = sections.locationToIndex(p)

                                                    sections.setSelectedIndex(index)

                                                    e.component.repaint()
                                                },
                                                mouseExited:  { MouseEvent e ->

                                                    sections.clearSelection()

                                                    e.component.repaint()
                                                },
                                                mouseClicked: { MouseEvent e ->

                                                    Point p = new Point(e.getX(), e.getY())
                                                    int index = sections.locationToIndex(p)

                                                    sectionRenderer.showingRow = index

                                                    rawButton.enabled = true

                                                    Section section = sections.model.getElementAt(index)

                                                    render(section)
                                                })
                                    },
                            rightComponent:  detailView = swing.scrollPane(name: "Document", border: BorderFactory.createEmptyBorder())
                    )
                }
    }

    /**
     * Render a Merge.
     */
    public void render(Merge merge)
    {
        this.merge = merge

        view = swing.panel(border: BorderFactory.createEtchedBorder())
                {
                    borderLayout()

                    SectionLabelRenderer sectionRenderer = new SectionLabelRenderer()

                    JButton rawButton

                    panel(constraints: NORTH, border: BorderFactory.createEmptyBorder(5,15,8,5))
                            {
                                borderLayout()

                                label(constraints: CENTER, font: TITLE_FONT, text: merge.getDescription())
                                panel(constraints: EAST)
                                        {
                                            flowLayout()

                                            label(  icon: CLOSE_IMAGE,
                                                    enabled: false,
                                                    mouseEntered: { MouseEvent e -> e.component.enabled = true  },
                                                    mouseExited: { MouseEvent e -> e.component.enabled = false },
                                                    mouseClicked: close,
                                                    cursor: Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
                                        }
                            }

                    swing.splitPane(
                            border:          BorderFactory.createEtchedBorder(),
                            constraints:     CENTER,
                            dividerLocation: 250,
                            orientation:     HORIZONTAL_SPLIT,
                            leftComponent:   swing.scrollPane(border: BorderFactory.createEmptyBorder())
                                    {
                                        sections = list(
                                                font: SECTION_FONT,
                                                listData: merge.sectionNames,
                                                cellRenderer: sectionRenderer,
                                                border: BorderFactory.createEmptyBorder(10, 10, 10, 10),
                                                cursor: Cursor.getPredefinedCursor(Cursor.HAND_CURSOR),
                                                mouseMoved: { MouseEvent e ->

                                                    Point p = new Point(e.getX(), e.getY())
                                                    int index = sections.locationToIndex(p)

                                                    sections.setSelectedIndex(index)

                                                    e.component.repaint()
                                                },
                                                mouseExited:  { MouseEvent e ->

                                                    sections.clearSelection()

                                                    e.component.repaint()
                                                },
                                                mouseClicked: { MouseEvent e ->

                                                    Point p = new Point(e.getX(), e.getY())
                                                    int index = sections.locationToIndex(p)

                                                    sectionRenderer.showingRow = index

                                                    String sectionName = sections.model.getElementAt(index)

                                                    render(merge.getSections(sectionName))
                                                })
                                    },
                            rightComponent:  detailView = swing.scrollPane(name: "Document", border: BorderFactory.createEmptyBorder())
                    )
                }
    }

    /**
     * Show detail on a particular Section of the filing - render as HTML, and inject it into the detail view.
     */
    public void render(Section section)
    {
        try
        {
            html = new StringBuilder()

            info = filing != null ? new RenderingInfo(filing, section) : new RenderingInfo(merge.filings, section)

            if (info.contexts.size() > 0)
            {
                renderDetail()
            }
            else
            {
                html << "<i>No data to show.</i>"
            }

            detailView.viewport.view = buildLayoutViewArea("<html><body>$html</body></html>", section)
        }
        catch (Throwable ex)
        {
            ex.printStackTrace()
        }
    }

    public void render(List<Section> sections)
    {
        try
        {
            html = new StringBuilder()

            boolean firstRow = false

            for (Section section : sections)
            {
                info = filing != null ? new RenderingInfo(filing, section) : new RenderingInfo(merge.filings, section)

                if (info.contexts.size() > 0)
                {
                    Filing filing = merge.getFilingFor(section)

                    if (firstRow)
                    {
                       firstRow = false
                    }
                    else
                    {
                       html << "<br>"
                    }

                    html << "<h1>" + filing.company + "&nbsp;&nbsp;&nbsp;(" + filing.filingDate.format("ddMMMyyyy") + ")</h1>"

                    renderDetail()
                }
            }

            detailView.viewport.view = buildLayoutViewArea("<html><body>$html</body></html>", sections[0])
        }
        catch (Throwable ex)
        {
            ex.printStackTrace()
        }

    }

    protected void startRow()
    {
        html << "<tr>"
    }

    protected void startRow(int depth)
    {
        html << """    <tr class="depth-$depth">  \n"""
    }

    protected void endRow()
    {
        html << "</tr>"
    }

    protected void nextColumn()
    {
        html << "<td></td>"
    }

    protected void renderHeading(String heading, int width)
    {
        html << """<th colspan="$width">$heading</th>"""
    }

    protected void renderLabel(String label)
    {
       if (label == null)
       {
          label = ""
       }

       html << """      <th>${ label.split("\\[")[0].trim() }</th>\n"""
    }

    NumberFormat format = new DecimalFormat("###,###,###,###,##0.######")

    protected void renderDatapoint(Datapoint data)
    {
        if (data != null)
        {
            String type = data?.element?.typeName
            String text = data.value

            if (type != null && text != null && text != "")
            {
                type = type.split(":")[-1]

                if (type == "monetaryItemType")
                {
                   BigDecimal decimal = new BigDecimal(text)

                   if (data.decimals != null && data.decimals < 0)
                   {
                      // Make sure we are rounding to thousands or millions.  Anything else doesn't really make sense.
                      int multiplier = data.decimals

                      if (multiplier >= -3)
                      {
                         multiplier = -3
                      }
                      else
                      {
                         multiplier = -6
                      }

                      decimal = decimal * Math.pow(10, multiplier)
                   }

                   text = format.format(decimal)
                }
                else if (type == "sharesItemType")
                {
                   BigDecimal decimal = new BigDecimal(text)

                   text = format.format(decimal)
                }
                else if (type == "dateItemType")
                {

                }
            }

            html << ("<td>" + prettify(text) + "</td")
        }
        else
        {
            html << "<td></td>"
        }
    }

    private void renderDetail()
    {
        html << "<table>         \n"
        html << "  <thead>       \n"

        renderHeadings(info)

        html << "  </thead> \n"
        html << "  <tbody>  \n"

        renderBody(info)

        html << "  </tbody>  \n"
        html << "</table>   \n"


        // Temporary debug output: render a list of contexts here.
        /**
          html << "<br>"
          html << "<table>"
          html << "  <thead><tr><th>ID</th><th>Start</th><th>End</th><th>Instant</th><th>Segments</th></tr></thead>"
          html << "  <tbody>"

          info.contexts.unique().each
          {
              Context ctx ->

              html << """<tr><td>${ ctx.id }</td><td>${ ctx.period.startDate?.format("ddMMMyyyy") }</td><td>${ ctx.period.endDate?.format("ddMMMyyyy") }</td><td>${ ctx.period.instant?.format("ddMMMyyyy") }</td><td>${ ctx.segments }</td></tr>"""
          }
        */

        html << "  </tbody>"
        html << "</table>"
    }

    private JEditorPane buildLayoutViewArea(String html, Section section)
    {
        JEditorPane area = new JEditorPane()

        area.setEditable(false)

        HTMLEditorKit kit = new HTMLEditorKit();
        area.setEditorKit(kit)

        StyleSheet styleSheet = kit.getStyleSheet();

        styleSheet.addRule("body { color: black; background-color: white; font-family: calibri, helvetica; font-size: 11px; margin: 10px; }")
        styleSheet.addRule("th   { background-color: #9f9f9f; color: #e0e0e0; padding-top: 5px; padding-bottom: 5px; font-weight: bold; border-width: 2px; border-style: solid; border-color: #f8f8ff  }")

        // Right-align data points for balance sheets.
        String name = section.toString().toUpperCase()

        if (name.contains("BALANCE") || name.contains("STATEMENT"))
        {
            styleSheet.addRule("td      { padding: 3px; padding-left: 10px; text-align: right; border-width: 2px; border-style: solid; background-color: #f8f8ff; border-color: #ffffff }")
        }
        else
        {
            styleSheet.addRule("td      { padding: 3px; padding-left: 10px; text-align: left; border-width: 2px; border-style: solid; background-color: #f8f8ff; border-color: #ffffff  }")
        }

        // Calculate the amount of indentation on the label as a function of the
        // the max depth, and depth of the label in the hierarchy.
        Closure padding =
        {
            int depth ->

            int maxDepth = info.depth

            return 5 * (maxDepth - (4 - depth))
        }

        styleSheet.addRule(".depth-1    { background-color: #c0c0c0; color: #e0e0e0; }")
        styleSheet.addRule(".depth-1 td { background-color: #ffffff; color: #000000; }")

        styleSheet.addRule(".depth-1 th { padding: 5px; padding-top:  10px; text-align: left; font-style: normal; color: #000000  }")
        styleSheet.addRule(".depth-2 th { padding: 3px; padding-left: ${ padding(2) }px; text-align: left; background-color: #dddddd; color: #000000; max-width: 100px; font-weight: bold; font-style: normal                    }")
        styleSheet.addRule(".depth-3 th { padding: 3px; padding-left: ${ padding(3) }px; text-align: left; background-color: #e0e0e0; color: #000000; max-width: 100px; font-weight: normal; font-style: italic                  }")
        styleSheet.addRule(".depth-4 th { padding: 3px; padding-left: ${ padding(4) }px; text-align: left; background-color: #efefef; color: #000000; max-width: 100px; font-weight: notmal; font-style: normal; font-size: 10px }")

        Document doc = kit.createDefaultDocument()

        area.setDocument(doc)
        area.setText(html)
        area.setOpaque(true)

        return area
    }

    /**
     * Spot embedded HTML data, give it a bit of a face lift.
     */
    private String prettify(String string)
    {
        string.replaceAll("(?i)Times New Roman", "Calibri").replaceAll("(?i)font-size: ?10pt", "font-size: 12pt")
    }
}
