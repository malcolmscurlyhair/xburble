package xburble.services

/**
 * Functionality to load and save recent searches, and details on recently viewed filings, from temporary files
 * under the user's home directory.
 */
class RecentActivity
{
   private static File workingDirectory
   private static File recentActivityDirectory

   public static String COMPANY     = "COMPANY"
   public static String CIK         = "CIK"
   public static String FILING_DATE = "FILINGDATE"
   public static String URL         = "URL"
   public static String VIEWER      = "VIEWER"

   private static NO_OF_SEARCHES_TO_REMEMBER = 1000
   private static NO_OF_FILINGS_TO_REMEMBER  = 1000

   private static List<String> recentSearches
   private static List<String> recentlyViewFilings

   private static File recentSearchesCacheFile
   private static File recentlyViewFilingsCacheFile
   private static File recentSearchesCacheTempFile
   private static File recentlyViewFilingsCacheTempFile

   private static String SEPARATOR = "#"

   static void init()
   {
      // Working directory will vary by operating system.
      workingDirectory = Utils.getWorkingDirectory()

      if (workingDirectory.exists())
      {
          recentActivityDirectory = new File(workingDirectory.absolutePath + "/Recent")

          recentActivityDirectory.mkdirs()
      }
      else
      {
         Status.broadcast("Unable to access working directory $workingDirectory.absolutePath, no on-disk caches will be created")

         workingDirectory = null
      }
   }

   private static boolean initialised()
   {
      return workingDirectory != null
   }

   static synchronized List<String> getRecentSearches()
   {
      if (!initialised()) return []

      if (recentSearches == null)
      {
         File recent = getRecentSearchesCacheFile()

         if (recent.exists())
         {
            recentSearches = []

            recent.eachLine
            {
               String line ->

               recentSearches << line
            }
         }
         else
         {
            recentSearches = []
         }
      }

      return recentSearches
   }

   static synchronized List<String> getRecentSearchesMatching(String pattern)
   {
      if (!initialised()) return []

      return getRecentSearches().findAll { String search ->

                                    search.toUpperCase().contains(pattern.toUpperCase())

                                 }.sort()
   }

   public synchronized static void rememberRecentSearch(String search)
   {
      if (!initialised()) return

      List<String> searches = getRecentSearches()

      if (searches.contains(search)) return

      searches << search

      while (searches.size() > NO_OF_SEARCHES_TO_REMEMBER)
      {
         searches.remove(0)
      }

      File cache = getRecentSearchesCacheFile()
      File temp  = recentSearchesCacheTempFile

      if (temp.exists())
      {
         temp.delete()
      }

      temp.createNewFile()

      searches.each
      {
         String recentSearch ->

         temp.append(recentSearch + "\n")
      }

      if (cache.exists())
      {
         cache.delete()
      }

      temp.renameTo(cache)

      temp.delete()
   }

   public synchronized static List<Map<String, String>> getRecentlyViewedFilings()
   {
      if (!initialised()) return [:]

      if (recentlyViewFilings == null)
      {
         File recent = getRecentlyViewFilingsCacheFile()

         if (recent.exists())
         {
            recentlyViewFilings = []

            recent.eachLine
            {
               String line ->

               String[] items = line.split(SEPARATOR)

               recentlyViewFilings <<  [     (COMPANY): items[0],
                                                 (CIK): items[1],
                                         (FILING_DATE): Date.parse("ddMMMyyyy", items[2]),
                                                 (URL): items[3],
                                              (VIEWER): items[4] ]
            }
         }
         else
         {
            recentlyViewFilings = []
         }
      }

      return recentlyViewFilings
   }

   public synchronized static void rememberRecentlyViewFiling(String company, String cik, Date filingDate, String url, String viewerURL)
   {
      if (!initialised()) return

      List<Map<String, String>> filings = getRecentlyViewedFilings()

      def data = [ (COMPANY): company, (CIK): cik, (FILING_DATE): filingDate, (URL): url, (VIEWER): viewerURL ]

      def match = filings.find
      {
         Map<String, String> filing ->

         return filing[ COMPANY     ] == data[ COMPANY     ] &&
                filing[ CIK         ] == data[ CIK         ] &&
                filing[ FILING_DATE ] == data[ FILING_DATE ]
      }

      if (match != null)
      {
          // Remove it, and reinsert, so it rises to the top of the list.
          filings.remove(match)
      }

      filings.add(0, data)

      while (filings.size() > NO_OF_FILINGS_TO_REMEMBER)
      {
         filings.remove(filings.size() - 1)
      }

      File cache = getRecentlyViewFilingsCacheFile()
      File temp  = recentlyViewFilingsCacheTempFile

      if (temp.exists())
      {
         temp.delete()
      }

      temp.createNewFile()

      filings.each
      {
         Map filing ->

         String companyEntry    = filing[ COMPANY     ].replaceAll(SEPARATOR, "")
         String cikEntry        = filing[ CIK         ].replaceAll(SEPARATOR, "")
         String filingDateEntry = filing[ FILING_DATE ].format("ddMMMyyyyy").replaceAll(SEPARATOR, "")
         String urlEntry        = filing[ URL         ].replaceAll(SEPARATOR, "")
         String viewer          = filing[ VIEWER      ].replaceAll(SEPARATOR, "")

         temp.append("$companyEntry$SEPARATOR$cikEntry$SEPARATOR$filingDateEntry$SEPARATOR$urlEntry$SEPARATOR$viewer\n")
      }

      if (cache.exists())
      {
         cache.delete()
      }

      temp.renameTo(cache)

      temp.delete()
   }

   private synchronized static File getRecentSearchesCacheFile()
   {
      if (recentSearchesCacheFile == null)
      {
         recentSearchesCacheFile     = new File(recentActivityDirectory.absolutePath + "/recent.searches")
         recentSearchesCacheTempFile = new File(recentSearchesCacheFile.absolutePath + ".tmp")
      }

      return recentSearchesCacheFile
   }

   private synchronized static File getRecentlyViewFilingsCacheFile()
   {
      if (recentlyViewFilingsCacheFile == null)
      {
         recentlyViewFilingsCacheFile     = new File(recentActivityDirectory.absolutePath + "/recently.viewed.filings")
         recentlyViewFilingsCacheTempFile = new File(recentlyViewFilingsCacheFile.absolutePath + ".tmp")
      }

      return recentlyViewFilingsCacheFile
   }
}
