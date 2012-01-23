package xburble.services

/**
 * On-disk caching functionality - caches XML schemas and the like by URL, to prevent too-frequent access to the
 * EDGAR site.
 */
class Cache
{
   private static File workingDirectory
   private static File cacheDirectory

   static void init()
   {
      workingDirectory = Utils.getWorkingDirectory()

      if (workingDirectory.exists())
      {
         Status.broadcast("Using working directory $workingDirectory.absolutePath")

         cacheDirectory = new File(workingDirectory.absolutePath + "/Cache")

         cacheDirectory.mkdirs()

         scheduleCleanup()
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

   public static String get(URL url)
   {
       if (!initialised())
       {
          return url.text
       }

       Status.broadcast("Searching for $url in cache")

       String path = url.toString()

       if (path.startsWith("http://"))  path = path[7..-1]
       if (path.startsWith("https://")) path = path[8..-1]

       path = path.toLowerCase()

       File cache = new File(cacheDirectory.absolutePath + "/" + path.replaceAll("[^a-z0-9/]", "_"))

       if (cache.exists())
       {
          Status.broadcast("Found $url in cache")

          cache.setLastModified(System.currentTimeMillis())

          return cache.text
       }
       else
       {
          Status.broadcast("Caching data for $url")

          String text = url.text

          cache.parentFile.mkdirs()
          cache.createNewFile()

          cache << text

          return text
       }
   }

   private static void scheduleCleanup()
   {
      int delay  = 30 * 1000      // Delay first invocation 30 sec.
      int period = 5 * 60 * 1000  // Repeat every five minutes.

      Timer timer = new Timer()
      timer.scheduleAtFixedRate(new Cleanup(dir: cacheDirectory), delay, period)
   }
}

/**
 * Scheduled task to clean up files older than a week in the cache directory.
 */
class Cleanup extends TimerTask
{
   File dir

   void run()
   {
       try
       {
          long oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)

          dir.eachFileRecurse
          {
             File file ->

             if (!file.isDirectory() && file.lastModified() < oneWeekAgo)
             {
                 file.delete()
             }
          }
       }
       catch (Exception ex)
       {
          // Not fatal.
          Status.broadcast("Encountered error cleaning up caches: " + ex.message)
       }
   }
}
