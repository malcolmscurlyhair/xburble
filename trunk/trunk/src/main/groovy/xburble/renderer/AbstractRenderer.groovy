package xburble.renderer

import xburble.objects.*

/**
 * Abstraction of various rendering functionality, used by cell-based renderers such as HTMLRenderer, and ExcelRenderer.
 */
abstract class AbstractRenderer implements Renderer
{
    /**
     * Render the headings for this Section.
     */
    public void renderHeadings(RenderingInfo info)
    {
        startRow()

        if (info.partitions.size() > 1)
        {
            // Filter out any contexts which don't match any of the selectors.
            info.partitions.remove(null)

            nextColumn()

            info.partitions.each
            {
                Map partitionKey, List<Context> contexts ->

                // We expect a single domain here.
                String label = partitionKey.values().toList()[0]

                renderHeading(label, contexts.size())
            }

            endRow()
            startRow()

            nextColumn()

            // Reset the contexts so they correspond to the columns we are actually displaying.
            info.contexts = []

            info.partitions.each
            {
                Map partitionKey, List<Context> contexts ->

                contexts.each
                {
                    Context ctx ->

                    renderHeading(ctx.period.toString(), 1)

                    info.contexts << ctx
                }
            }
        }
        else
        {
            nextColumn()

            // Filter out all the contexts which belong to particular domains.
            info.contexts = info.contexts.findAll { Context ctx -> ctx.segments.isEmpty() }

            List<Period> periods = info.contexts.collectAll { Context ctx -> ctx.period }.unique()

            periods.each
            {
                Period period ->

                renderHeading(period.toString(), 1)
            }
        }

        endRow()
    }

    /**
     * Render the body of this Section.
     */
    protected void renderBody(RenderingInfo info)
    {
        def renderRow

        renderRow =
            {
                items, depth = 1 ->

                if (items.children == null) return

                items.children.each
                {
                    Section row ->

                    if (row.element?.deprecated)
                    {
                        return
                    }

                    startRow(depth)

                    renderLabel(row.element?.label?.toString())

                    info.contexts.each
                    {
                        Context ctx ->

                        if (row.element == null) return

                        Datapoint data = row.element.datapoints[ ctx ]

                        renderDatapoint(data)
                    }

                    endRow()

                    renderRow(row, depth + 1)
                }
            }

        // Find the maximum depth of embedding, to inform our rendering styles..
        Closure findDepth

        findDepth =
        {
            Section section, int depth = 0 ->

            if (section.children != null)
            {
                int maxDepthSoFar = depth

                section.children.each
                {
                    Section child ->

                    int newDepth = findDepth(child, depth + 1)

                    maxDepthSoFar = Math.max(maxDepthSoFar, newDepth)
                }

                depth = maxDepthSoFar
            }

            return depth
        }

        int depth = findDepth(info.lineItems)

        info.depth = depth

        renderRow(info.lineItems, 5 - depth)
    }

    // --- These methods to be defined on the concrete implementation. --- //

    protected abstract void startRow()

    protected abstract void startRow(int depth)

    protected abstract void endRow()

    protected abstract void nextColumn()

    protected abstract void renderHeading(String heading, int width)

    protected abstract void renderLabel(String label)

    protected abstract void renderDatapoint(Datapoint data)
}
