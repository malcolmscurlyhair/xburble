package xburble.services

/**
 * Utility class for broadcasting status messages across the application.
 */
class Status
{
   private static List<Closure> listeners = []

   static void broadcast(String message)
   {
      for (Closure listener in listeners)
      {
         listener(message)
      }

      // println message
   }

   static void addStatusListener(Closure listener)
   {
      listeners << listener
   }
}
