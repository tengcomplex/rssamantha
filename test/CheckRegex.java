import java.util.*;
import java.text.*;
import java.util.regex.*;

public class CheckRegex
{

  public static void main(String[] args)
  {
    System.out.println("regex:^(?!ZDF). string:"+args[0]);
    Pattern p = Pattern.compile("^(?!ZDF).");
    Matcher m = p.matcher(args[0]);
    System.out.println(m.matches()?"Match":"No Match");  
  }
}