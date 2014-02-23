/*
 * Copyright (C) 2014 Kemin Zhou, Pól Ua Laoínecháin. 
 *
 * The original and  main author is Kemin Zhou and the original code is 
 * to be found here: http://orpara.com/acedbdriver/index.html. 
 * 
 * Pól Ua Laoínecháin has actually used the AceDB server system recently, 
 * so may also be of assistance.
 *
 * 
 * Kemin Zhou can be contacted here: kmzhou4@yahoo.com
 * and 
 * Pól Ua Laoínecháin can be contacted here: linehanp@tcd.ie.
 *
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published here: http://www.gnu.org/licenses/agpl-3.0.html and
 * explained here: https://www.gnu.org/licenses/why-affero-gpl.html
 * as published by the Free Software Foundation. 
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.relvar.ACeDB2Java;



import java.util.ArrayList;

public class ACeDB2JavaResult implements IACeDB2JavaConstants
{
  private String result = null;
  private ACeDB2JavaConnect DB = null;
  private int begin = -1;  // begin index of one line
  private int end = -1;   // end index of one line
  private int count = 0;  // the position of the line
  private boolean more = false; // more results following
  private ArrayList typeInfo = new ArrayList();   // type of each column
  // needs to encore to server

  /* For storing split result for the current pointer.  NULL field
   * from AQL will be represented as null String ""
   */
  private String[] rowArray;

  /**
   * used if Result properity is random access.
   */
  private ArrayList resultVec = null;

  /**
   * to mark the type of result, currently RESULT_FORWARD, RESULT_RANDOM.
   */
  protected int resultProperty = RESULT_FORWARD;
  private int totalRowCount = 0;
  private int currentRow = 0;  // index starting from 0, for internal use

	//public static int INT = 1;
  //public static int ACEOBJ = 2;
  /**
   * construct an empty Result object if 0 objects are returned.
   */
  protected ACeDB2JavaResult()
  {
  }

  /**
   * the String r should be the whole message body obtained from the Ace or
   * Aceclient class Should not be used outside the package. Only execQuery()
   * methods from the Ace class produces Result objects.
   */
  protected ACeDB2JavaResult(String r)
  {
    result = r;
    begin = 0;
    end = result.indexOf('\n');
    if (end == -1)
    {
      System.err.println("Nothing in result: " + result);
      result = null;
      return;
    }

    String rInfo = result.substring(begin, end);  // result info 
    //System.err.println(rInfo); //debug
    if (!rInfo.startsWith("Format"))
    {
      System.err.println("Should start with Format");
      System.err.println("the result of the query is:\n" + result);
      System.exit(1);
    }
    /* this is the first line of the AQL output
     * the following line splits it into tokens
     *  Format  Sequence  Int  Int $ */
    int b = 6;  // jump after Format
    int e;
    while (true)
    {
      /* b<rInfo.length() should be first tested */
      while (b < rInfo.length() && (rInfo.charAt(b) == ' '
              || rInfo.charAt(b) == '\t'))
      {
        b++; // skip space
      }
      if (b == rInfo.length())
      {
        break;
      }
      e = b + 1;
      while (e < rInfo.length() && rInfo.charAt(e) != ' '
              && rInfo.charAt(e) != '\t')
      {
        e++;
      }
      typeInfo.add(rInfo.substring(b, e));
      if (e == rInfo.length())
      {
        break;
      }
      b = e + 1;
    }
    /* the Species:"xxxx" is still there */
    String tmpStr = (String) typeInfo.get(0);
    int tmpInt = result.indexOf(tmpStr, end);
    if (tmpInt != -1)
    {
      end += (tmpStr.length() + 1);
      //System.err.println("got the bad thing");
    }
    rowArray = new String[typeInfo.size()];
  }

  /**
   * Construct a new Result object from String r.
   *
   * @param db indicates whether more encore is needed. when the db argument is
   * given, we assume more segments of results are needed to be obtained from
   * the database.
   */
  protected ACeDB2JavaResult(String r, ACeDB2JavaConnect db)
  {
    this(r);
    DB = db;
    more = true;
  }

  /**
   * Construct a random access result from a complete query result. private
   * member Random == true. resultProperty set to RESULT_RANDOM.
   */
  protected ACeDB2JavaResult(String r, boolean random)
  {
    this(r);
    if (random)
    {
      resultProperty = RESULT_RANDOM;
    }
    resultVec = new ArrayList();
    begin = end + 1;
    while (begin < result.length())
    {
      if (result.charAt(begin) == '/'
              && result.charAt(begin + 1) == '/')
      {  // end mark by ACE
        begin += 3;
        end = begin + 1;
        while (result.charAt(end) != ' ')
        {
          end++;
        }
        int total = Integer.parseInt(result.substring(begin, end));
        if (resultVec.size() != total)
        {
          System.err.println("resultVec has " + resultVec.size());
          System.err.println("Ace reported total:" + total);
          System.err.println("they look different");
          System.exit(1);
        }
        totalRowCount = resultVec.size();
        break;
      }
      end = result.indexOf('\n', begin);
      resultVec.add(result.substring(begin, end));
      begin = end + 1;
    }
    //System.out.println(resultVec.size() + " rows retrieved"); //debug
    result = null;  // discard the string version of the result
  }

  /**
   * if there is more results, after calling next you can retrieve the result
   * with getXXXX methods. If returned objects are returned, then next() will return false.
   */
  public boolean next() throws ACeDB2JavaException
  {
    if (resultProperty == RESULT_FORWARD)
    {
      return nextForward();
    }
    else if (resultProperty == RESULT_RANDOM)
    {
      if (currentRow < totalRowCount)
      {
        split((String) resultVec.get(currentRow++));
        return true;
      }
      else
      {
        return false;
      }
    }
    else
    {
      System.err.println(resultProperty + " not implemented yet");
      System.exit(1);
      return false;
    }
  }

  /**
   * Move pointer to the next item in result; this is a FORWARD_ONLY iterator.
   * Cannot go backward. After calling this method, you can retrieve results
   * with one of those getxxx() methods. If the result is empty, it simply
   * returns false.
   *
   * @return true if has more items in the result
   */
  public boolean nextForward() throws ACeDB2JavaException
  {
    if (result == null)
    {
      return false;
    }
    /* needs to close off DB after finished with all the item
     * in the String object
     */
    begin = end + 1;
    if (begin < result.length())
    {
      if (result.charAt(begin) == '/'
              && result.charAt(begin + 1) == '/')
      {  // end mark by ACE
        begin += 3;
        end = begin + 1;
        while (result.charAt(end) != ' ')
        {
          end++;
        }
        int total = Integer.parseInt(result.substring(begin, end));
        if (currentRow != total)
        {
          System.err.println("count from nextForward() differ"
                  + " from ace\ncount=" + currentRow
                  + "total from Ace=" + total
                  + "This is a bug in ace, ignored right now");
          //System.exit(1);
        }
        if (DB != null)
        {
          DB.close();
          result = null;
        }
        totalRowCount = currentRow;
        return false;  // Saceserver uses // as end mark
      }
    }
    else
    {
      if (!more)
      {
        if (DB != null)
        {
          DB.close();
        }
        return false;  // end of the message
      }
      else
      {
        System.err.println("doing encore inside next()"); //debug
        int dbStatus = DB.execCommand("encore");
        if (dbStatus == ACESERV_MSGFAIL)
        {
          throw new ACeDB2JavaException("failed to encore in Result");
        }
        else
        {
          if (dbStatus == ACESERV_MSGENCORE)
          {
            more = true;
          }
          else if (dbStatus == ACESERV_MSGOK)
          {
            more = false;
          }
          else
          {
            System.err.println(dbStatus + "What could be wrong here");
            System.exit(1);
          }
          result = DB.getMessage();
          begin = 0;
        }
      }
    }
    end = result.indexOf('\n', begin);
    currentRow++;
    split(result.substring(begin, end));
    return true;
  }

  /**
   * For random access only.
   */
  public boolean first()
  {
    if (resultVec == null)
    {
      return false;
    }
    currentRow = 0;
    return true;
  }
  public boolean last()
  {
    if (resultVec == null)
    {
      return false;
    }
    currentRow = totalRowCount - 1;
    return true;
  }
  public boolean absolute(int row)
  {
    if (row > 0)
    {
      if (row >= totalRowCount)
      {
        return false;
      }
      currentRow = row - 1;
    }
    else if (row < 0)
    {
      if (-row > totalRowCount)
      {
        return false;
      }
      currentRow = totalRowCount + row;
    }
    else
    {
      return false;  // not zero row in 1 based index
    }
    return true;
  }

  /**
   * Return the whole row as a single String object.
   */
  public String getRowString()
  {
    if (resultProperty == RESULT_RANDOM)
    {
      return (String) resultVec.get(currentRow);
    }
    return result.substring(begin, end);
  }
  /**
   * Simply inform you whether you have obtained some results or not.
   *
   * @return true if AQL returned // 0 active objects
   */
  public boolean isEmpty()
  {
    return result == null;
  }

  /**
   * Returns number of columns returns by AQL.
   */
  public int getColumnCount()
  {
    return typeInfo.size();
  }

  /**
   * Returns column type at cnum as a String.
   *
   * @param cnum column number, 1-based
   */
  public String getColumnType(int cnum)
  {
    return (String) typeInfo.get(cnum - 1);
  }
  /**
   * Returns row as an array of Strings
   */
  public String[] getRowArray()
  {
    return rowArray;
  }

  /**
   * Returns the column at col as an integer.
   *
   * @param col column number starting from 1.
   */
  public int getInt(int col)
  {
    return Integer.parseInt(rowArray[col - 1]);
  }
  /**
   * Returns unquoted String. If null, then returns ""
   */
  public String getString(int col)
  {
    return rowArray[col - 1];
  }
  public float getFloat(int col)
  {
    return Float.parseFloat(rowArray[col - 1]);
  }
  public double getDouble(int col)
  {
    return Double.parseDouble(rowArray[col - 1]);
  }

  /**
   * Return the total rows in this result only if Result is set to be random
   * access. For sequential access, use getCurrentRow after reaching the end of
   * the Result. When next() has no more data. this method is only valid for
   * Random Access Results.
   */
  public int getRowCount()
  {
    if (resultProperty == RESULT_RANDOM)
    {
      return totalRowCount;
    }
    else
    {
      return currentRow;  // meaningful only after the end of next()
    }
  }

  /**
   * Return the rows that have been accessed through next().
   */
  public int getCurrentRow()
  {
    return currentRow;
  }

  public void close()
  {
    if (DB != null)
    {
      DB.close();
    }
    result = null;
    resultVec = null;
  }

  /**
   * Convert String s into fields of table and put result into rowArray, must be
   * tab separated. Null field is "" empty string.
	 *
   */
  private void split(String s)
  {
    int b = 0;
    int i = 0;
    int e;
    for (i = 0; i < getColumnCount() - 1; i++)
    {
      e = s.indexOf('\t', b);
      /* remove double quote */
      if (s.charAt(b) == '\"')
      {
        rowArray[i] = s.substring(b + 1, e - 1);
      }
      else
      {
        rowArray[i] = s.substring(b, e);
      }
      b = e + 1;
    }
    if (b == s.length())
    { // the last field is null
      // use empty string for null
      //System.err.println("Warning: the last field is NULL: " + s);
      rowArray[i] = "";
    }
    else
    {
      if (s.charAt(b) == '\"')
      {
        rowArray[i] = s.substring(b + 1, s.length() - 1);
      }
      else
      {
        rowArray[i] = s.substring(b);
      }
    }
  }
}
