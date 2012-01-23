package xburble.renderer

import xburble.objects.Section
import xburble.objects.Filing
import xburble.objects.Merge

/**
 * Something which renders Filings or Sections.
 */
interface Renderer
{
    /**
     * Render a Filing.
     */
    public abstract void render(Filing filing)

    /**
     * Render a Merge.
     */
    public abstract void render(Merge merge);

    /**
     * Render a Section.
     */
    public abstract void render(Section section)
}
