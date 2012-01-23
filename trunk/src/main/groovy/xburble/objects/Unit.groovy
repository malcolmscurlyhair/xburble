package xburble.objects

/**
 * A Unit for a datapoint - e.g. for example a currency value, or no. of shares.  Simple values (such as those two
 * mentioned) will just have the 'measure' variable populated.  Complex value (such as dollars per share) will
 * instead have the numerator and denominator.
 */
class Unit
{
   String measure      // e.g. "iso4217:USD"

   String numerator    // e.g. "iso4217:USD"
   String denominator  // e.g. "shares"

   Unit clone()
   {
       new Unit([ measure: measure, numerator: numerator, denominator: denominator ])
   }
}
