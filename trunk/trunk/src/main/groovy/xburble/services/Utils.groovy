package xburble.services

/**
 * Common utility functions used across the application.
 */
class Utils
{
   static File getWorkingDirectory()
   {
      // Working directory will vary by operating system.
      String os   = System.getProperty("os.name")
      String home = System.getProperty("user.home")

      File workingDirectory

      if (os == "Mac OS X")
      {
         workingDirectory = new File(home + "/Library/Application Support/XBurble")

         workingDirectory.mkdirs()
      }
      else
      {
         workingDirectory = new File(home + "/.xburble")

         workingDirectory.mkdirs()
      }

      return workingDirectory
   }

   static File getDesktop()
   {
      String os   = System.getProperty("os.name")
      String home = System.getProperty("user.home")

      File desktop

      if (os == "Mac OS X")
      {
         desktop = new File(home + "/Desktop")
      }
      else
      {
         desktop = new File(home + "/Desktop")
      }

      return desktop
   }

   static File generateFileName(String name, String extension)
   {
      name = name.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_")

      String path = getDesktop().absolutePath

      int count = 0

      File output = new File(path + "/" + name + "." + extension)

      while (output.exists())
      {
         output = new File(path + "/" + name + "_(" + count + ")." + extension)

         count++
      }

      return output
   }

}
