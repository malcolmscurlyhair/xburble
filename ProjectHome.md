**XBurble** is open-source desktop application written in <a href='http://groovy.codehaus.org'>Groovy</a>, that allows a user to search across company earnings reports submitted to the <a href='http://www.sec.gov/edgar.shtml'>EDGAR</a> site in <a href='http://en.wikipedia.org/wiki/XBRL'>XBRL</a> format.  Once found, filings can be be viewed within the application, exported to Excel, and merged together for comparison purposes.

The application was originally developed for the <a href='http://xbrl.us/research/Pages/challenge.aspx'>XBRL Challenge</a>.

### Requirements ###

To run, the application requires the <a href='http://java.com/en/download/index.jsp'>Java 6</a> runtime.

<br>

<h2>How It Works</h2>

Rather than using a database, XBurble plugs into the existing functionality of the EDGAR website, and uses <a href='http://en.wikipedia.org/wiki/Web_scraping'>web scraping</a> to mimic the actions of browser.  By doing this, it can reuse the <a href='http://www.sec.gov/edgar/searchedgar/companysearch.html'>search page</a>, and connect to the <a href='http://www.sec.gov/Archives/edgar/usgaap.rss.xml'>RSS feed</a> showing the latest filing submissions.  It can also pull down and render filings stored online.<br>
<br>
<br>

<h2>Functionality</h2>

<h3>Screencast</h3>
A brief demonstration of the application is available <a href='http://v5.tinypic.com/player.swf?file=nqtk6g&s=5'>here</a>.<br>
<br>
<br>
<h3>Screenshots</h3>

<i>The Start Screen</i> - what the user sees when they first open the application.  Notice the options to search filngs, load recently viewed filngs, view new filings, and to exit the application.<br>
<br>
<img src='http://oi42.tinypic.com/2429wqs.jpg' width='550'>

<i>Searching for a Filing</i> - search can be done by company name or CIK.<br>
<br>
<img src='http://oi44.tinypic.com/11ujayg.jpg' width='550'>

<img src='http://oi42.tinypic.com/kec208.jpg' width='550'>

<i>A Rendered Filing</i> - different sections of the filing can be highlighted, and they will be rendered in the detail pane.  Note the link to view the filing on the EDGAR website.<br>
<br>
<img src='http://oi43.tinypic.com/dcd4kj.jpg' width='550'>

<img src='http://oi44.tinypic.com/veoo02.jpg' width='550'>

<i>Reopening Recently Viewed Filings</i> - a list of recently viewed filings ia stored for easy access.  This list persists between restarts of the application.<br>
<br>
<img src='http://oi43.tinypic.com/11w3l8w.jpg' width='550'>

<i>New Submissions</i> - this list is drawn from the SEC RSS feed.<br>
<br>
<img src='http://oi42.tinypic.com/vmzko2.jpg' width='550'>

<i>Exporting to Excel</i> - the export format is fairly authentic to how the filing is rendered within the application.<br>
<br>
<img src='http://oi44.tinypic.com/alp01l.jpg' width='550'>

<i>Merging Filings</i> - filings from different companies can be merged. in order to view reported figures alongside each other.<br>
<br>
<img src='http://oi41.tinypic.com/2n1fdw1.jpg' width='550'>

<img src='http://oi41.tinypic.com/2m2xcnm.jpg' width='550'>

<img src='http://oi44.tinypic.com/2uiaypg.jpg' width='550'>

<i>Viewing the Raw Document Structure</i>

<img src='http://oi43.tinypic.com/1zow1sn.jpg' width='550'>

<br>


<h2>The Codebase</h2>

<h3>Building the Code From Scratch</h3>

To access the code, you will need a <a href='http://subversion.tigris.org/'>Subversion</a> client installed.  To build it, you will require a local installation of <a href='http://maven.apache.org/'>Maven</a>.<br>
<br>
To check out the code, using the following command<br>
<br>
<pre><code>   svn checkout http://xburble.googlecode.com/svn/trunk/ xburble-read-only<br>
</code></pre>

This will create a local copy of the source code.  To compile and package the code, run:<br>
<br>
<pre><code>   mvn install<br>
</code></pre>

...in the directory containing the <code>pom.xml</code> file.  This will generate a file in the <code>/target</code> directory, called <code>xburble.jar</code>.  This jar file contains all the project dependencies, and is an executable jar - meaning that on most systems, you need only double-click on the jar to start the application.  Failing this, you can use the following syntax to start from the command line:<br>
<br>
<pre><code>   java -classpath target/xburble.jar xburble.XBurble<br>
</code></pre>

<br>

<h3>Libraries Used</h3>

XBurble is built using the following libraries, released under various open source license:<br>
<br>
<br>
<ul><li><a href='http://ccil.org/~cowan/XML/tagsoup/'> Tagsoup </a> - A library for parsing HTML, allowing for non-standard and incomplete syntax.  Used to connect to the EDGAR search screens, and scrape the contents.</li></ul>

<ul><li><a href='http://groovy.codehaus.org'>Groovy</a> - A dynamic language for the <b>Java Virtual Machine</b>, that has support for functional programming.  Has powerful libraries to assist in building Swing GUIs, and parsing XML.</li></ul>

<ul><li><a href='http://poi.apache.org/'>Apache POI</a> - Permits creation of Excel files within the application.</li></ul>

<ul><li><a href='http://www.javadocking.com/'>Java Swing Docking Framework</a> - Used to create dynamic tabs in the main application screen.</li></ul>

<ul><li><a href='http://www.glazedlists.com/'>Glazed Lists</a> - Used to add auto-complete support in various places in the application.</li></ul>

<br>

<h2>Future Work</h2>

There a number of limitations and shortcomings to the project in it's current state.  The key areas for improvement are:<br>
<br>
<ul><li><b>Simplistic Merges</b> - The original intention of the application was to allow close comparison between data points on filings submitted by different companies.  As is, sections of each filing are simply rendered on top of each other - these type of data tables should be interleaved to make comparison easier.<br>
</li><li><b>Rendering Issues</b> - There is room for improvement around the HTML rendering - it has not been extensively tested on non-standard filings, and is prone to break when certain datapoints are missing.  It also tends to get confused when too many contexts are supplied in the instance document<br>
</li><li><b>Improvements to the Excel Export Functionality</b> - Large text blocks are not handled well by the export functionality, and the export format for merged filings is a little messy.<br>
</li><li><b>Shortcomings in the XML Parsing</b> - there are number of short cuts taken in the treatment of namespaces, that allow for ambiguous behaviour during parsing.<br>
</li><li><b>Use of the Calculation and Definition Files</b> - Currently, the calculation and definition files are not used.  Both contain data that could inform the rendering of tables, and enrich the display of data.<br>
</li><li><b>Charting Requirements</b> - Balance sheets in particular lend themselves well to being rendered in charts, which would make cross company comparisons easier.<br>
</li><li><b>Historical Analysis</b> -  There is currently no easy way to pull up multiple flings for the same company over historical dates, and observe changes year-on-year.<br>
</li><li><b>Non-English Languages</b> -  Although XBRL has support for languages in non-English languages, the application currently only supports American English.<br>
</li><li><b>Installer</b> -  An installer is required to make the application more user-friendly.</li></ul>
