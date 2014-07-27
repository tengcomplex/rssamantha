import java.util.*;
import java.text.*;


public class ParseDateTime
{

  public static void main(String[] args)
  {
    System.out.println("dt:"+args[0]+" format:"+args[1]);
    SimpleDateFormat formatter = new SimpleDateFormat(args[1]);
    
    try
    {
      Date d = formatter.parse(args[0]);
      SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.S Z");
      System.out.println("Success with given dateformat Date:"+sdf.format(d));
    }
    catch(Exception ex)
    {
      //ex.printStackTrace();
      System.out.println("Failed d with default dateformat "+ex.getMessage());
    }
    // test other locales
    Locale[] otherLocales = {Locale.FRANCE, Locale.GERMAN};
    for(Locale lo : otherLocales)
    {
        formatter = new SimpleDateFormat(args[1], lo);
        Calendar cal = Calendar.getInstance();
        System.out.println("Show formatter:"+formatter.format(cal.getTime()));
        System.out.print("Test locale "+lo.toString()+" ");
        try
        {
          Date d = formatter.parse(args[0]);
          System.out.println("Success, Date:"+d);
        }
        catch(Exception ex)
        {
            System.out.println("Fail, "+ex.getMessage());
        }
    }   
  }

}