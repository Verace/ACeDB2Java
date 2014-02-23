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

public class ACeDB2JavaSequenceResult implements IACeDB2JavaConstants
{
  private ACeDB2JavaConnect connect = null;
  private String pool = null;          // to hold all the sequences
	/* pointer to the beginning of the sequence >xxx */
  private int begin = 0;
  private int end = -1;                 // start with before the first
  //private Sequence currentSeq = null;
  private String name = null; //name for the sequence
  private StringBuffer seqstr = null;

  public ACeDB2JavaSequenceResult(String str)
  {
    pool = str;
  }
  public ACeDB2JavaSequenceResult(String str, ACeDB2JavaConnect con)
  {
    connect = con;
    pool = str;
    String tmp;
  }

  /**
   * return the String of the sequence
   */
  public String getSequence()
  {
    return seqstr.toString();
  }
  /**
   * return the name of the current sequence
   */
  public String getName()
  {
    return name;
  }

  /**
   * Starts with end = -1, begin=0, in each iteration begin is on >
	 *
   */
  public boolean next()
  {
    if (pool.charAt(begin) != '>')
    {
      if (pool.charAt(begin) == '/' && pool.charAt(begin + 1) == '/')
      {
        System.err.println("reaching the end: ");
        System.out.println(pool.substring(begin));
        // needs to do some cleaning work here
        if (connect != null)
        {
          connect.close();
          connect = null;
          pool = null;
        }
        return false;  //reaching the end of the sequence pool
        // normal termination condition if ace terminates with //
      }
      System.err.println("Sequence results not starting with >");
      System.exit(1);
    }
    end = pool.indexOf('\n', begin);
    name = pool.substring(begin + 1, end);  // > discarded
    begin = end + 1;
    end = pool.indexOf('\n', begin);
    seqstr = new StringBuffer(1200);          // average CDS 1200
    String tmp = pool.substring(begin, end);
    while (!tmp.startsWith("//") && !tmp.startsWith(">"))
    {
      seqstr.append(tmp);
      begin = end + 1;
      if (begin < pool.length())
      {
        end = pool.indexOf('\n', begin);
        tmp = pool.substring(begin, end);
      }
      else if (connect.getMsgTypeInt() == ACESERV_MSGENCORE)
      {
        int status = connect.execCommand("encore");
        if (status != ACESERV_MSGENCORE && status != ACESERV_MSGOK)
        {
          //something is wrong here
          System.err.println("encore failed. SeqResult::next()");
          System.exit(1);
        }
        pool = connect.getMessage();
        begin = 0;
        return true;
      }
      else
      {
        /* if the ace result is terminated by // the code should 
         * never reach here */
        if (connect == null)
        {
          System.out.println("connect has been disconnected ");
          return false;
        }
        if (connect.getMsgTypeInt() == ACESERV_MSGOK)
        {
          connect.close();
          connect = null;
        }
        else
        {
          System.err.println("needs to write more code for SeqResult");
          System.err.println("some connection error " + connect.getMsgType());
          System.exit(1);
        }
        return false;
      }  // setting up the next result segment
    }
    return true;
  }

  public void close()
  {
    if (connect != null)
    {
      connect.close();
    }
    connect = null;
    pool = null;
  }

}
