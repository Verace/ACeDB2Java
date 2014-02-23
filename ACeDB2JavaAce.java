/*
 * Copyright (C) 2014 Kemin Zhou, Pól Ua Laoínecháin. 
 *
 * The original and  main author is Kemin Zhou and the original code is 
 * to be found here: http://orpara.com/acedbdriver/index.html. 
 * 
 * Pól Ua Laoínecháin has actually used the ACeDB server system recently, 
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

import java.io.*;
import java.util.HashMap;
import java.util.ArrayList;


public class ACeDB2JavaAce implements IACeDB2JavaConstants
{
  private ACeDB2JavaClient client = null;  // to make more connects
  private ACeDB2JavaConnect connect = null;
  private int restartCount = 0;

  private int resultProperty = RESULT_FORWARD;
  public static final int QUERY_LIMIT = 20400;

  /**
   * Constructing an Ace object. If user=anonymous, then pwd should be "" an
   * empty string when the server has world read accesss
   */

  public ACeDB2JavaAce(String host, int port, String user, String pwd)
  {
    client = new ACeDB2JavaClient(host, port, user, pwd);
    connect = client.getConnect();
    if (connect == null)
    {
      System.err.println("Ace not connected to a batabase ");
      System.exit(1);
    }
  }

  /**
   * Execute database commands and return the whole result as String. if
   * returned message too long it may cause trouble. Do not use for long
   * results! Use the execCommand() method instead (in the parent class).
   *
   * @param query should not exceed 20400 bytes
   * @return query result as string
   */
  
  public String exec(String query) throws ACeDB2JavaException
  {
    int smsgType = connect.execCommand(query);
    if (smsgType == ACESERV_MSGOK)
    {
      return connect.getMessage();
    }
    else if (smsgType == ACESERV_MSGENCORE)
    {
      StringBuffer tmp = new StringBuffer(connect.getMessage());
      while (connect.getMsgTypeInt() == ACESERV_MSGENCORE)
      {
        connect.execCommand("encore");
        tmp.append(connect.getMessage());
      }
      return tmp.toString();
    }
    else
    {
      System.err.println("DBstatus " + connect.getMsgType());
      System.err.println("Trying to restart dead socket ...");
      System.err.println("restartCount=" + restartCount);
      if (restartCount > 3)
      {
        throw new ACeDB2JavaException("Trying to restart more than 3 times and failed");
      }
      connect.close();
      connect = client.getConnect();
      restartCount++;
      return exec(query);
    }
  }

  /**
   * Use the tace Keyset-Read command to import a keyset into the server
   * database with class=type and all the keys from keys ArrayList. The keys when
   * translated into string should not execeed 20400 bytes. When keysets are too
   * large you have to sent command in separate trunks.
   *
   * @param type is a legal class name in acedb
   * @param keys are valid object ids limited to 20400 bytes
   *
   * @return number of valid keys in keyset read by server
   */
  
  public int keyset(String type, ArrayList keys) throws ACeDB2JavaException
  {
    String input = "Keyset-read = " + type + " ";
    int i;
    for (i = 0; i < keys.size(); i++)
    {
      input += (keys.get(i) + ";");
    }
    return keyset(input);
  }
  
  /**
   * Send a keyset to the server with keys containing the String of keys
   * separated with ";". Right now the QUERY_LIMIT is 20400.
   *
   * @param keys Keyset-read = CLASSNAME k1;k2;k3;..;kn;
   * @return number of valid keys, -1 if keys longer than QUERY_LIMIT
   */
  
  public int keyset(String keys) throws ACeDB2JavaException
  {
    if (keys.length() > QUERY_LIMIT)
    {
      System.err.println("Input too long: " + keys.length());
      return -1;
    }
    String output = exec(keys);

    if (output.indexOf("Objects not recognised") != -1)
    {
      System.err.println("Warning: " + output);
    }
    int i = output.indexOf("Active Objects") - 1;
    int j = i - 2;
    while (output.charAt(j) != ' ')
    {
      j--;
    }
    j++;
    return Integer.valueOf(output.substring(j, i)).intValue();
  }

  /**
   * Must be called after a regular query that returns sequence sets such as an
   * aql statement or keyset statement.
   *
   * @param DNA_Peptide can only be either DNA or Peptide nothing else
   * @return a SeqResult object for iteration
   */
  
  public ACeDB2JavaSequenceResult getSeqSet(String DNA_Peptide) throws ACeDB2JavaException
  {
    if (!DNA_Peptide.equals("DNA") && !DNA_Peptide.equals("Peptide"))
    {
      System.err.println(DNA_Peptide + " is not accepted.  Must be DNA or Peptide");
      System.exit(1);
    }
    int msgType = connect.execCommand(DNA_Peptide);
    if (msgType == ACESERV_MSGOK)
    {
      return new ACeDB2JavaSequenceResult(connect.getMessage());
    }
    else if (msgType == ACESERV_MSGENCORE)
    {
      ACeDB2JavaConnect resultConn = connect;
      connect = client.getConnect();
      return new ACeDB2JavaSequenceResult(resultConn.getMessage(), resultConn);
    }
    else
    {
      System.out.println("DNA failed inside execQuery");
      throw new ACeDB2JavaException("DNA execQuery failed to get anything");
      //return new Result();  // empty result
    }
  }

  /**
   * returns a SeqResult object from a list of protein or sequence names. An
   * overloaded version of getSeqSet(String DNA_Peptide). There is no need to
   * execute query that returns keyset of sequence or protein objects before
   * calling this method.
   */
  
  public ACeDB2JavaSequenceResult getSeqSet(String DNA_Peptide, ArrayList seqlist) throws ACeDB2JavaException
  {
    String ace_class = null;
    if (DNA_Peptide.equals("DNA"))
    {
      ace_class = "Sequence";
    }
    else if (DNA_Peptide.equals("Peptide"))
    {
      ace_class = "Protein";
    }
    else
    {
      System.err.println("Can only make sequence sets from DNA or Peptide");
      System.exit(1);
    }
    int goodkey = keyset(ace_class, seqlist);
    if (goodkey < seqlist.size())
    {
      System.err.println("Warning: Some keys are bad\n");
    }
    return getSeqSet(DNA_Peptide);
  }

  /**
   * Execute an AQL query and return a RESULT_FORWARD Result object. You need to
   * use the Result object to iterate through the result. For a randomly accessed
   * Result, you need to use execQuery(query, RESULT_RANDOM). If the result is
   * too large (i.e. needs ENCORE), then it will open another connection that will be
   * closed when next() reaches the end.
   *
   * @param query in the AQL querery syntax only. Other syntax will cause
   * trouble.
   * @return a Result object with RESULT_FORWARD property, for iterating
   * throught the resulting table. If retrieved 0 objects then Result object
   * will be empty.
   * @see Result#next
   */
  
  public ACeDB2JavaResult execQuery(String query) throws ACeDB2JavaException
  {
    if (query.startsWith("aql"))
    {
      System.err.println("It is not necessary to start query"
              + " with aql");
    }
    else
    {
      query = "aql -h " + query;
    }

    /* This eliminates the second line being in a strange
     * format, it is a bug in tace, it may be fixed in the
     * future release of tace */

    int tryCount = 0;
    while (tryCount < 3)
    {
      int msgType = connect.execCommand(query);
      if (msgType == ACESERV_MSGOK)
      {
        String msg = connect.getMessage();
        if (msg.startsWith("//"))
        {
          if (msg.indexOf("// 0 Active Objects") != -1)
          {
            return new ACeDB2JavaResult();
          }
          else
          {
            throw new ACeDB2JavaException("Query failed:\n" + msg);
          }
        }
        else
        {
          // return new Result(connect.getMessage());  // you got everything
          return new ACeDB2JavaResult(msg);  // you got everything
        }
        
				// No need to ask the connect for more
        // this should be null, there is no need to access
        // the aceclient
        
      }
      else if (msgType == ACESERV_MSGENCORE)
      {
        ACeDB2JavaConnect resultConn = connect;
        connect = client.getConnect();
        return new ACeDB2JavaResult(resultConn.getMessage(), resultConn);
      }
      else if (msgType == ACESERV_MSGKILL)
      {
			// Most likely due to client time out.  Recreate a connection
        // and try again.
        System.out.println("Backend failed on the query, check the server client time out.  The client may have been closed owing to time out, I am reconnecting and try again.\n");
        connect = client.getConnect();
        tryCount++;
        continue;
      }
      else
      {
        System.out.println(query + " failed inside execQuery");
        System.out.println("\n");
        System.out.println("return result: " + msgType);
        throw new ACeDB2JavaException(query + " execQuery failed to get anything");
        //return new Result();  // empty result
      }
    }  // try loop
    throw new ACeDB2JavaException("Try to connect to server 3 times and failed to execute query:\n" + query);
  }

  /**
   * Use to get random access results if rltProperty == RESULT_RANDOM.
   * Currently, only RESULT_FORWARD (default) and RESULT_RANDOM implemented.
   *
   * @param rltProperty takes two values now: RESULT_FORWARD or RESULT_RANDOM.
   * RESULT_BACKWARD not implemented yet.
   * @return a Result object, use this object to iterate through the result in a
   * table format.
   */
  
  public ACeDB2JavaResult execQuery(String query, int rltProperty) throws ACeDB2JavaException
  {
    if (rltProperty == RESULT_FORWARD)
    {
      return execQuery(query);
    }
    else if (rltProperty == RESULT_RANDOM)
    {
      return new ACeDB2JavaResult(exec(query), true);
    }
    else
    {
      System.err.println("not implemented RESULT_BACKWARD yet");
      System.exit(1);
      return null;
    }
  }

  /**
   * Retrieve an Aceobj from the server.
   *
   * @throws (ERROR) found none or more than one objects
	 *
   */
  
  public synchronized ACeDB2JavaObject fetch(String className, String objName)
          throws ACeDB2JavaException
  {
    String rslt = exec("find " + className + " " + objName);
    if (rslt.indexOf("1 Active Objects") == -1)
    {
      throw new ACeDB2JavaException(rslt);
    }
    rslt = exec("show -j");
    ACeDB2JavaObject obj = new ACeDB2JavaObject(rslt);
    if (!obj.getAceClass().equalsIgnoreCase(className))
    {
      System.err.println("Fetal eror: Class name does not match"
              + "Requested " + className + " Returned " + obj.getAceClass());
      System.exit(1);
    }
    return obj;
  }
  
  /**
   * follow an Aceobj into the database and retrieve a new object.
   *
   * @throws will throw an exception if node is not an authetic object in
 Acedb, such as Sequence, Author, etc.
   */
  
  public synchronized ACeDB2JavaObject fetch(ACeDB2JavaObject node) throws ACeDB2JavaException
  {
    //just let the server give you result and exceptions
    return fetch(node.type(), node.name());
  }
  
  /**
   * Fetch all objects in objlist into an Objset object, then use the next
   * method to iterate through the objcets in the set. Note: one pass only
   * iteration.
   *
   * @see Objset#next
   */
  
  public synchronized ACeDB2JavaObjectSet fetchMany(String classname, ArrayList objlist) throws ACeDB2JavaException
  {
    int validkeys = keyset(classname, objlist);
    if (validkeys < 1)
    {
      System.err.println("Nothing feteched from the object list\n");
      return null;
    }
    if (validkeys < objlist.size())
    {
      System.err.println("Warning: some object keys are not valid in this database\n");
    }
    return new ACeDB2JavaObjectSet(exec("Show -j"));
  }

  /**
   * Keys must be formated as "Keyset-read = CLASS key1;key2;...; . The keyset
   * method know how mamy object are not present in the server. This method does
   * not know because we don't have an ArrayList as input so we could not easily
   * count.
   *
   * @param keysetCmd a proper keyset-read command
   */
  
  public synchronized ACeDB2JavaObjectSet fetchMany(String keysetCmd) throws ACeDB2JavaException
  {
    int validkeys = keyset(keysetCmd);
    if (validkeys < 1)
    {
      System.err.println("Nothing feteched from the object list\n");
      return null;
    }
    return new ACeDB2JavaObjectSet(exec("Show -j"));
  }

  /**
   * Fetch all the object in objlist as a large string containing all object.
   * This is for human to read. The default is "show -h"
   */
  
  public synchronized String fetchManyString(String className, ArrayList objlist) throws ACeDB2JavaException
  {
    if (keyset(className, objlist) < 1)
    {
      System.err.println("Nothing fetched\n");
      return new String("");
    }
    return exec("Show");
  }

  /**
   * Retrieve DNA sequence as fasta formated, 50 nt per line, with the header
   * &gt seqName (as per fasta format). The format is the default from ace.
   */
  
  public synchronized String getFastaDNA(String seqName)
          throws ACeDB2JavaException, ACeDB2JavaObjectNotFoundException
  {
    String result = exec("Find Sequence " + seqName);
    if (result.indexOf("Found 1 objects in this class") != -1)
    {
      result = exec("DNA");
      int commPos = result.indexOf("//");
      if (commPos == 0)
      {
        return null;
      }
      else
      {
        return result.substring(0, commPos);
      }
    }
    else if (result.indexOf("Found 0 objects in this class") != -1)
    {
      throw new ACeDB2JavaObjectNotFoundException(seqName + " DNA not found in ace database ");
    }
    else
    {
      throw new ACeDB2JavaException(seqName + " returned " + result);
    }
  }

  /**
   * Retrieve Peptide sequence as fasta formated, 50 aa per line, with the
   * header &gt seqName (as per fasta format). The format is the default from ace. The definition line
   * is not retrieved.
   */
  
  public synchronized String getFastaPeptide(String seqName)
          throws ACeDB2JavaException, ACeDB2JavaObjectNotFoundException
  {
    String result = exec("Find Protein " + seqName);
    //System.out.println("getFastaPeptide: " + result); //debug
    if (result.indexOf("Found 1 objects in this class") != -1)
    {
      result = exec("Peptide");
      //System.out.println(result); //debug
      int commPos = result.indexOf("//");
      if (commPos == 0)
      {
        return null;
      }
      else
      {
        return result.substring(0, commPos);
      }
    }
    else if (result.indexOf("Found 0 objects in this class") != -1)
    {
      throw new ACeDB2JavaObjectNotFoundException(seqName + " protein not found in ace database ");
    }
    else
    {
      throw new ACeDB2JavaException(seqName + " returned " + result);
    }
  }

  /**
   * Retrieve the sequence string only, without \n and header no white
   * characters left. Extra work done to format the fasta formated sequence into
   * a simple string.
   */
  
  public String getDNA(String seqName) throws ACeDB2JavaException
  {
    String seqStr = getFastaDNA(seqName);
    return fasta2String(seqStr);
  }

  /**
   * Retrieve the peptide string only, without \n and header no white characters
   * left. Extra work done to format the fasta formated sequence into a simple
   * string.
   */
  
  public String getPeptide(String seqName) throws ACeDB2JavaException
  {
    String seqStr = getFastaPeptide(seqName);
    return fasta2String(seqStr);
  }

  /**
   * Obtain the CDS sequence from an mRNA object
   */
  
  public String getCDS(String seqName) throws ACeDB2JavaException
  {
    String r = exec("Find Sequence " + seqName);
    // may not find the sequence
    if (r.indexOf("Found 0 objects in this class") != -1)
    {
      System.err.println("ace " + r + " with key " + seqName);
      throw new ACeDB2JavaException(seqName + " not in db");
      //return null;
    }
    int i;
    if (r.indexOf("Found 1 objects in this class") != -1)
    {
      r = exec("Show");
      //System.out.println(r);
      if ((i = r.indexOf("Properties")) == -1)
      {
        System.err.println("query result from " + seqName + "\n" + r);
        throw new ACeDB2JavaException("Properties TAG missing in seqObj: " + seqName);
      }
      if ((i = r.indexOf("Coding", i + 11)) == -1)
      {
        throw new ACeDB2JavaException(seqName + ": Coding TAG missing");
      }
      if ((i = r.indexOf("CDS", i + 7)) == -1)
      {
        throw new ACeDB2JavaException(seqName + ": CDS TAG missing");
      }
      i += 4;
      while (Character.isWhitespace(r.charAt(i)))
      {
        i++;
      }
      int ii = i + 1;
      while (Character.isDigit(r.charAt(ii)))
      {
        ii++;
      }
      int begin = Integer.parseInt(r.substring(i, ii));
      i = ii + 1;
      while (Character.isWhitespace(r.charAt(i)))
      {
        i++;
      }
      ii = i + 1;
      while (Character.isDigit(r.charAt(ii)))
      {
        ii++;
      }
      int end = Integer.parseInt(r.substring(i, ii));
      String dna = exec("DNA");
      dna = fasta2String(dna);
      dna = dna.substring(begin - 1, end);
      return dna;
    }
    else
    {
      return null;
    }
  }
  
	///////////////// helper functions //////////////////////

  /**
   * Remove the comment info at the end of the ace output. example: RESULT here
   * // 58 objects dumped // 58 Active Objects
   */
  
  public static String chopComment(String aceoutput)
  {
    int i = aceoutput.indexOf("\n// ");
    return aceoutput.substring(0, ++i);
  }

  /**
   * Helper function to convert the ace fasta dump into String discarding header
   * line
   */
  
  public static String fasta2String(String fasSeq)
  {
    if (fasSeq == null)
    {
      return null;
    }
    int i = fasSeq.indexOf('\n');
    i++;
    fasSeq = fasSeq.substring(i);
    int line_len = fasSeq.indexOf('\n') + 1;  //including \n
    StringBuffer seq = new StringBuffer(fasSeq.length());
    //int line_num = 0;
    int start = 0, end = start + line_len - 1;
    while (end < fasSeq.length() - 1)
    {
      seq.append(fasSeq.substring(start, end));
      start += line_len;
      end = start + line_len - 1;
    }
    if (fasSeq.length() != start)
    { // partial line left 
      seq.append(fasSeq.substring(start, fasSeq.length() - 1));
    }
    return seq.toString();
  }

  /**
   * defalt line 50 nt per line
   */
  
  public static void writeFasta(PrintWriter pw, String seq)
  {
    writeFasta(pw, seq, 50);
  }

  public static void writeFasta(PrintWriter pw, String seq, int len)
  {
    int b = 0, e;
    while (b < seq.length())
    {
      e = b + len;
      if (e > seq.length())
      {
        e = seq.length();
      }
      pw.println(seq.substring(b, e));
      b += len;
    }
  }

  public static void writeFasta(PrintStream pw, String seq)
  {
    writeFasta(pw, seq, 50);
  }

  /**
   * Helper function to dump sequence string into pw, lineLen bases per line
   */
  
  public static void writeFasta(PrintStream pw, String seq, int lineLen)
  {
    int b = 0, e;
    while (b < seq.length())
    {
      e = b + lineLen;
      if (e > seq.length())
      {
        e = seq.length();
      }
      pw.println(seq.substring(b, e));
      b += lineLen;
    }
  }
  
  /**
   * Helper function, should be in a separete package
   */
  
  public static String reverseComplement(String DNA)
  {
    /*
     char[] tab = 
     { 'A', 'C','G','T','R','Y','K','M','S','W','B','D','H','V','N'};
     char[] rct = {NBDHVWSKMRYACGT};
     */
	
    // too clumsy to use hash, Java is bad in dealing with
    //primitive types
    HashMap rc = new HashMap(30);
    
    //use upper case
    
    rc.put(new Character('A'), new Character('T'));
    rc.put(new Character('C'), new Character('G'));
    rc.put(new Character('G'), new Character('C'));
    rc.put(new Character('T'), new Character('A'));
    rc.put(new Character('R'), new Character('Y'));
    rc.put(new Character('Y'), new Character('R'));

 /*
  *
  * From http://en.wikipedia.org/wiki/FASTA_format
    
    K   G, T or U           bases which are Ketones
    M   A or C              bases with aMino groups
    S 	C or G              Strong interaction 
    W 	A, T or U           Weak interaction

    B 	not A (i.e. C, G, T or U) 	B comes after A
    D 	not C (i.e. A, G, T or U) 	D comes after C
    H 	not G (i.e., A, C, T or U) 	H comes after G
    V 	not T nor U (i.e. A, C or G) 	V comes after U    
    
*/    
    rc.put(new Character('K'), new Character('M'));
    rc.put(new Character('M'), new Character('K'));
    rc.put(new Character('S'), new Character('S'));
    rc.put(new Character('W'), new Character('W'));
    rc.put(new Character('B'), new Character('V'));
    rc.put(new Character('D'), new Character('H'));
    rc.put(new Character('H'), new Character('D'));
    rc.put(new Character('V'), new Character('B'));
    rc.put(new Character('N'), new Character('N'));

    int i = 0, j = DNA.length() - 1;

    /* Look at this messy Java code, I hate to write java
     code for low level management
     */

    StringBuffer upd = new StringBuffer(DNA.toUpperCase());  // converts all to upper case
    while (i < j)
    {
      Character tmp = (Character) rc.get(new Character(upd.charAt(i)));
      upd.setCharAt(i, ((Character) rc.get(new Character(upd.charAt(j)))).charValue());
      upd.setCharAt(j, tmp.charValue());
      i++;
      j--;
    }
    if (i == j)
    {
      upd.setCharAt(i, ((Character) rc.get(new Character(upd.charAt(i)))).charValue());
    }
    return upd.toString();
  }

  public String getStatus() throws ACeDB2JavaException
  {
    return exec("status");
  }
  
  /**
   * Get a summary of info about the underlying acedb.
   */
  
  public String getDBInfo() throws ACeDB2JavaException
  {
    return exec("status -database");
  }
  
  /**
   * Obtain the title of the underlying acedb.
   */
  
  public String getDBTitle() throws ACeDB2JavaException
  {
    String tmp = exec("status -database");
    int i = tmp.indexOf("Title:");
    i += 8;
    int j = tmp.indexOf('\"', i);
    return tmp.substring(i, j);
  }
  public void close()
  {
    client.close();
  }
}
