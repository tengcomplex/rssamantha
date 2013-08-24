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
    
    formatter = new SimpleDateFormat(args[1], Locale.FRANCE);
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DATE, -1);
    System.out.println("Test formatter:"+formatter.format(cal.getTime()));
    try
    {
      Date d = formatter.parse(args[0]);
      System.out.println("Success with fr dateformat Date:"+d);
    }
    catch(Exception ex)
    {
      //ex.printStackTrace();
      System.out.println("Failed d with fr dateformat "+ex.getMessage());
    }
        
  }

}