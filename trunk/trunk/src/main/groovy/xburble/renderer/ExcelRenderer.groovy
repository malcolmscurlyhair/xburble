package xburble.renderer

import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.WorkbookUtil
import xburble.objects.Section
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.hssf.usermodel.HSSFCellStyle
import xburble.objects.Datapoint
import xburble.objects.Filing
import org.apache.poi.ss.util.CellRangeAddress
import xburble.objects.Merge

/**
 * Exports a Filing to Excel format, attempting to render headings and labels with a similar look-and-feel to the
 * HTMLRenderer.  Uses the Apache POI library.
 *
 * This object is single-use only.
 */
class ExcelRenderer extends AbstractRenderer
{
    // The Filing being rendered.
    Filing filing

    // The Merge being rendered.
    Merge merge

    // The output location.
    File output

    // Pre-processed rendering info.
    RenderingInfo info

    // Current workbook.
    Workbook workbook

    // Current worksheet.
    Sheet sheet

    // Current row.
    Row row

    // Current row and column positions.
    short rowNo
    short colNo

    // Label depth (affects rendering style).
    int depth

    // Styles.
    CellStyle title
    CellStyle heading
    CellStyle label1
    CellStyle label2
    CellStyle label3
    CellStyle label4
    CellStyle company
    CellStyle data

    ExcelRenderer(File output)
    {
        this.output = output
    }

    /**
     * Render a Filing as an Excel workbook.
     */
    void render(Filing filing)
    {
        if (output == null) throw new IllegalStateException("Output location must be set before calling render(Filing)")

        this.filing = filing

        // Generate the work book in memory.
        workbook = new HSSFWorkbook()

        initialiseStyles()

        // Render each (top-level) Section as a separate worksheet.
        filing.sections.each
        {
            Section section ->

            String tab = section.toString()

            // Make sure to escape illegal characters in the work sheet name.
            String safeName = WorkbookUtil.createSafeSheetName(tab)

            sheet = workbook.createSheet(safeName)

            info = new RenderingInfo(filing, section)

            render(section)
        }

        // Write out the completed workbook
        FileOutputStream out = new FileOutputStream(output)
        workbook.write(out)
        out.close()
    }

    /**
     * Render a Merge as an Excel workbook.
     */
    public void render(Merge merge)
    {
        if (output == null) throw new IllegalStateException("Output location must be set before calling render(Filing)")

        this.merge = merge

        // Generate the work book in memory.
        workbook = new HSSFWorkbook()

        initialiseStyles()

        int sheetNo = 1

        // Render each (top-level) Section as a separate worksheet.
        merge.mergeList.each
        {
            String label, List<Section> sections ->

            sheet = workbook.createSheet("Sheet " + sheetNo)

            sheetNo++

            boolean firstSection = true

            reset()

            for (Section section : sections)
            {
               Row companyRow = sheet.createRow(rowNo - 2)

               Font companyFont = workbook.createFont()

               companyFont.fontHeightInPoints = COMPANY_FONT_HEIGHT
               companyFont.fontName           = COMPANY_FONT
               companyFont.boldweight         = COMPANY_FONT_WEIGHT
               companyFont.color              = COMPANY_FONT_COLOUR

               CellStyle company = workbook.createCellStyle()
               company.setFont(companyFont)

               Filing filing = merge.getFilingFor(section)

               Cell companyCell = companyRow.createCell(1)
               companyCell.setCellStyle(company)
               companyCell.setCellValue(filing.company + " (" + filing.filingDate.format("ddMMMyyyy") + ")")

               info = new RenderingInfo(filing, section)

               render(section, firstSection)

               this.rowNo += 5
               this.colNo = 1

               firstSection = false
            }
        }

        // Write out the completed workbook
        FileOutputStream out = new FileOutputStream(output)
        workbook.write(out)
        out.close()
    }

    private void initialiseStyles()
    {
        // To do: refactor this mess.
        Font titleFont = workbook.createFont()

        titleFont.fontHeightInPoints = TITLE_FONT_HEIGHT
        titleFont.fontName           = TITLE_FONT
        titleFont.boldweight         = TITLE_FONT_WEIGHT
        titleFont.color              = TITLE_FONT_COLOUR

        title = workbook.createCellStyle()
        title.setFont(titleFont)


        Font headingFont = workbook.createFont()

        headingFont.fontHeightInPoints = HEADING_FONT_HEIGHT
        headingFont.fontName           = HEADING_FONT
        headingFont.boldweight         = HEADING_FONT_WEIGHT
        headingFont.color              = HEADING_FONT_COLOUR

        heading = workbook.createCellStyle()
        heading.setFont(headingFont)
        heading.setFillForegroundColor(HEADING_BACKGROUND)
        heading.setFillBackgroundColor(HEADING_BACKGROUND)
        heading.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND)

        Font labelFont1 = workbook.createFont()

        labelFont1.fontHeightInPoints = ROW_LABEL_DEPTH_1_FONT_HEIGHT
        labelFont1.fontName           = ROW_LABEL_DEPTH_1_FONT
        labelFont1.boldweight         = ROW_LABEL_DEPTH_1_FONT_WEIGHT
        labelFont1.color              = ROW_LABEL_DEPTH_1_FONT_COLOUR

        label1 = workbook.createCellStyle()
        label1.setFont(labelFont1)


        Font labelFont2 = workbook.createFont()

        labelFont2.fontHeightInPoints = ROW_LABEL_DEPTH_2_FONT_HEIGHT
        labelFont2.fontName           = ROW_LABEL_DEPTH_2_FONT
        labelFont2.boldweight         = ROW_LABEL_DEPTH_2_FONT_WEIGHT
        labelFont2.color              = ROW_LABEL_DEPTH_2_FONT_COLOUR

        label2 = workbook.createCellStyle()
        label2.setFont(labelFont2)

        label2.setFillForegroundColor(ROW_LABEL_DEPTH_1_BACKGROUND)
        label2.setFillBackgroundColor(ROW_LABEL_DEPTH_1_BACKGROUND)
        label2.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND)


        Font labelFont3 = workbook.createFont()

        labelFont3.fontHeightInPoints = ROW_LABEL_DEPTH_3_FONT_HEIGHT
        labelFont3.fontName           = ROW_LABEL_DEPTH_3_FONT
        labelFont3.boldweight         = ROW_LABEL_DEPTH_3_FONT_WEIGHT
        labelFont3.color              = ROW_LABEL_DEPTH_3_FONT_COLOUR
        labelFont3.italic             = true

        label3 = workbook.createCellStyle()
        label3.setFont(labelFont3)
        label3.setWrapText(true)
        label3.setIndention((short) 2)


        Font labelFont4 = workbook.createFont()

        labelFont4.fontHeightInPoints = ROW_LABEL_DEPTH_4_FONT_HEIGHT
        labelFont4.fontName           = ROW_LABEL_DEPTH_4_FONT
        labelFont4.boldweight         = ROW_LABEL_DEPTH_4_FONT_WEIGHT
        labelFont4.color              = ROW_LABEL_DEPTH_4_FONT_COLOUR

        label4 = workbook.createCellStyle()
        label4.setFont(labelFont4)
        label4.setWrapText(true)
        label4.setIndention((short) 2)


        Font dataFont = workbook.createFont()

        dataFont.fontHeightInPoints = DATA_FONT_HEIGHT
        dataFont.fontName           = DATA_FONT
        dataFont.boldweight         = DATA_FONT_WEIGHT
        dataFont.color              = DATA_FONT_COLOUR

        data = workbook.createCellStyle()
        data.setFont(dataFont)
    }

    private void reset()
    {
        this.rowNo = merge != null ? 5 : 3
        this.colNo = 1
    }

    /**
     * Render a Section of a Filing, as a Worksheet in the Workbook currently being generated.
     */
    public void render(Section section)
    {
        render(section, true)
    }

    public void render(Section section, boolean renderTitle)
    {
        if (workbook == null) throw new IllegalStateException("Workbook must be initialised before calling render(Section section)")
        if (sheet    == null) throw new IllegalStateException("Worksheet must be initialised before calling render(Section section)")
        if (info     == null) throw new IllegalStateException("Rendering info must be initialised before calling render(Section section)")

        if (renderTitle)
        {
            // Write out the title in big text.
            Row titleRow = sheet.createRow(1)

            Cell titleCell = titleRow.createCell(1)
            titleCell.setCellStyle(title)
            titleCell.setCellValue(section.toString().split("-")[-1].trim())

            reset()
        }

        renderHeadings(info)

        renderBody(info)

        sheet.setColumnWidth(1, 10000)
    }

    protected void startRow()
    {
        this.colNo = 1

        row = sheet.createRow(rowNo)
    }

    protected void startRow(int depth)
    {
        this.colNo = 1
        this.depth = depth

        row = sheet.createRow(rowNo)
    }

    protected void endRow()
    {
        this.colNo = 1
        this.rowNo ++
    }

    protected void nextColumn()
    {
        this.colNo ++
    }

    protected void renderHeading(String label, int width)
    {
        Cell headingCell = row.createCell(colNo)
        headingCell.setCellValue(label)
        headingCell.setCellStyle(heading)

        sheet.autoSizeColumn(colNo);

        sheet.addMergedRegion(new CellRangeAddress(
           rowNo,            // First row    (0-based)
           rowNo,            // Last row     (0-based)
           colNo,            // First column (0-based)
           colNo + width - 1 // Last column  (0-based)
        ));

        this.colNo += width
    }

    protected void renderLabel(String label)
    {
        Cell labelCell = row.createCell(colNo)

        if      (depth == 1) labelCell.setCellStyle(label1)
        else if (depth == 1) labelCell.setCellStyle(label2)
        else if (depth == 1) labelCell.setCellStyle(label3)
        else                 labelCell.setCellStyle(label4)

        labelCell.setCellValue(label)
    }

    protected void renderDatapoint(Datapoint datum)
    {
        this.colNo ++

        if (datum?.value == null) return

        Cell dataCell = row.createCell(colNo)

        String type  = datum?.element?.typeName
        Object value = datum.value

        if (type != null && value != "")
        {
            type = type.split(":")[-1]

            if (type == "monetaryItemType" || type == "sharesItemType")
            {
               value = new BigDecimal(value)
            }
            else if (type == "dateItemType")
            {
               // value = Date.parse("yyyy-MM-dd", value)
            }
        }

        // Truncate very long block of text.  To do: cope better with HTML.
        if (value != null && value instanceof String && value.length() > 32000)
        {
            value = value[0..32000]
        }

        dataCell.setCellStyle(data)
        dataCell.setCellValue(value)
    }

    // Big, ugly list of rendering constants.  To do: make this an enum.
    private String TITLE_FONT = "Calibri"
    private short  TITLE_FONT_HEIGHT = 18
    private short  TITLE_FONT_WEIGHT = Font.BOLDWEIGHT_BOLD
    private short  TITLE_FONT_COLOUR = IndexedColors.GREY_40_PERCENT.getIndex()

    private String COMPANY_FONT = "Calibri"
    private short  COMPANY_FONT_HEIGHT = 13
    private short  COMPANY_FONT_WEIGHT = Font.BOLDWEIGHT_BOLD
    private short  COMPANY_FONT_COLOUR = IndexedColors.GREY_80_PERCENT.getIndex()

    private String HEADING_FONT = "Calibri"
    private short  HEADING_FONT_HEIGHT = 11
    private short  HEADING_FONT_WEIGHT = Font.BOLDWEIGHT_BOLD
    private short  HEADING_FONT_COLOUR = IndexedColors.GREY_25_PERCENT.getIndex()
    private short  HEADING_BACKGROUND  = IndexedColors.GREY_50_PERCENT.getIndex()

    private String ROW_LABEL_DEPTH_1_FONT = "Calibri"
    private short  ROW_LABEL_DEPTH_1_FONT_HEIGHT = 10
    private short  ROW_LABEL_DEPTH_1_FONT_WEIGHT = Font.BOLDWEIGHT_BOLD
    private short  ROW_LABEL_DEPTH_1_FONT_COLOUR = IndexedColors.BLACK.getIndex()
    private short  ROW_LABEL_DEPTH_1_BACKGROUND  = IndexedColors.GREY_25_PERCENT.getIndex()

    private String ROW_LABEL_DEPTH_2_FONT = "Calibri"
    private short  ROW_LABEL_DEPTH_2_FONT_HEIGHT = 10
    private short  ROW_LABEL_DEPTH_2_FONT_WEIGHT = Font.BOLDWEIGHT_BOLD
    private short  ROW_LABEL_DEPTH_2_FONT_COLOUR = IndexedColors.BLACK.getIndex()
    private short  ROW_LABEL_DEPTH_2_BACKGROUND  = IndexedColors.WHITE.getIndex()

    private String ROW_LABEL_DEPTH_3_FONT = "Calibri"
    private short  ROW_LABEL_DEPTH_3_FONT_HEIGHT = 10
    private short  ROW_LABEL_DEPTH_3_FONT_WEIGHT = Font.BOLDWEIGHT_NORMAL
    private short  ROW_LABEL_DEPTH_3_FONT_COLOUR = IndexedColors.BLACK.getIndex()
    private short  ROW_LABEL_DEPTH_3_BACKGROUND  = IndexedColors.WHITE.getIndex()

    private String ROW_LABEL_DEPTH_4_FONT = "Calibri"
    private short  ROW_LABEL_DEPTH_4_FONT_HEIGHT = 10
    private short  ROW_LABEL_DEPTH_4_FONT_WEIGHT = Font.BOLDWEIGHT_NORMAL
    private short  ROW_LABEL_DEPTH_4_FONT_COLOUR = IndexedColors.BLACK.getIndex()
    private short  ROW_LABEL_DEPTH_4_BACKGROUND  = IndexedColors.WHITE.getIndex()

    private String DATA_FONT = "Calibri"
    private short  DATA_FONT_HEIGHT = 10
    private short  DATA_FONT_WEIGHT = Font.BOLDWEIGHT_NORMAL
    private short  DATA_FONT_COLOUR = IndexedColors.BLACK.getIndex()
    private short  DATA_BACKGROUND  = IndexedColors.WHITE.getIndex()

}
