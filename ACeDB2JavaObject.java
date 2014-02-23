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

public class ACeDB2JavaObject
{

  /////////////// AceobjNode nested helper //////////////////////

  protected class AceobjNode
  {
    /**
     * A tree node with Ace class name, tags, build-in type derived from ace show
     * -j option from tace. This is the building block for Aceobj.
     */
    
    protected String type = null;
    protected String value = null;
    protected AceobjNode right = null;
    protected AceobjNode down = null;
    protected AceobjNode parent = null;

    /**
     * Default constructor with all members set to null.
     */
    
    protected AceobjNode()
    {
    }

    /**
     * Constructs AceobjNode from String like &#063Sequence&#063X97021&#063. The
     * ?type?value? format is the standard in ACE to represent a node.
     */
    
    public AceobjNode(String jstr)
    {
      int seploc = jstr.indexOf('?', 1);
      type = jstr.substring(1, seploc);
      value = jstr.substring(seploc + 1, jstr.length() - 1);
    }
    
    /**
     * Constructor, also set parent node as parent
     */
    
    public AceobjNode(String jstr, AceobjNode parent)
    {
      this(jstr);
      this.parent = parent;
    }
    public AceobjNode(Object obj, AceobjNode parent)
    {
      this((String) obj, parent);
    }
    
    /**
     * Add a node to the buttom of this one with str containing the Java String
     * representation of the node.
     */
    
    public void addChild(String str)
    {
      down = new AceobjNode(str, this);
    }
    
    /**
     * Add a node to the right of this one.
     */
    public void addSibling(String str)
    {
      right = new AceobjNode(str, this.parent);
    }

    public String toString()
    {
      StringBuffer tmp = new StringBuffer(type);
      tmp.append(":").append(value);
      return tmp.toString();
    }
    public boolean isTag()
    {
      return type.equalsIgnoreCase("Tag");
    }
  }
  
	////////////////////////////////////////////////////////////////
  //////////////////  Aceobj  ////////////////////////////////////

  private AceobjNode tree = null;  // General tree representation

  /**
   * Construct an empty object.
   */
  
  public ACeDB2JavaObject()
  {
  }

  /**
   * Construct a full Aceobj from the acedump, should implement construction one
   * line at time ON-DEMAND for efficiency. Most object operations don't need
   * the whole object and the object construction is very expensive!
   */
  
  public ACeDB2JavaObject(String objdump)
  {
    make(objdump);
  }

  /**
   * Construct an object from a tree node.
   */
  
  public ACeDB2JavaObject(AceobjNode node)
  {
    this();
    tree = node;
  }

  /**
   * Make an object out of ace dump string.
   *
   * @param aceDump is the dump from one object dump only
   */
  
  public void make(String aceDump)
  {
    DumpStore acedump = new DumpStore(aceDump);
    tree = acedump.rootBranch();        // the root node
    if (acedump.getTabCount() != 0)
    {
      System.err.println("Fetal error in constructing Aceobj");
      System.exit(1);
    }
    ArrayList guidv = new ArrayList();
    AceobjNode tmp = tree.down;
    while (tmp != null)
    {
      guidv.add(tmp);
      tmp = tmp.right;
    }

    AceobjNode ptr;
    int tabs = 0, currIdx;
    String ln = acedump.nextline();
    while (ln != null)
    {
      while (ln.charAt(tabs) == '\t')
      {
        tabs++;  // count the number of tabs
      }			// the number of tabs determines the attachement point
      currIdx = tabs - 1;
      ptr = (AceobjNode) guidv.get(tabs - 1);
      int b, e;
      b = tabs;
      e = ln.indexOf('\t', b);
      if (e == -1)
      {
        ptr.addChild(ln.substring(b));
        ptr = ptr.down;
        guidv.set(tabs - 1, ptr);
      }
      else
      {
        ptr.addChild(ln.substring(b, e));
        guidv.set(tabs - 1, ptr.down);
        b = e + 1;
        e = ln.indexOf('\t', b);
        ptr = ptr.down;
        while (e != -1)
        {
          ptr.addSibling(ln.substring(b, e));
          ptr = ptr.right;
          currIdx++;
          if (currIdx < guidv.size())
          {
            guidv.set(currIdx, ptr);
          }
          else
          {
            guidv.add(ptr);
          }
          b = e + 1;
          e = ln.indexOf('\t', b);
        }
        ptr.addSibling(ln.substring(b));
        ptr = ptr.right;
        currIdx++;
        if (currIdx < guidv.size())
        {
          guidv.set(currIdx, ptr);
        }
        else
        {
          guidv.add(ptr);
        }
				// erasing the extra nodes from guidv is not needed
        // because the text representation will not give you
        // a tree beyond the last node
      }
      if (currIdx < guidv.size())
      {
        // The below line was necessary for when guidv was a Vector - ArrayList doesn't need it.
        // if this presents problems, change guidv back to a Vector - don't have time to test - pul.
        ; //guidv.setSize(currIdx + 1);
      }
      ln = acedump.nextline();
    }
  }

  /**
   * Dump out the node type:value, same format as ACE model. This is not working
   * right yet.
   */
  
  protected static void print(AceobjNode o)
  {
    if (o != null)
    {
      System.out.print(o.type + ":" + o.value + "->");
      print(o.right);
      if (o.down != null)
      {
        System.out.println("\t|");
      }
      print(o.down);
    }
  }
  
  /**
   * Print the object tree, row first.
   */
  
  public void print()
  {
    print(tree);
    System.out.println("\n");
  }

  /**
   * A search function, not to be used by the public. Only search the tags.
   */
  
  protected static AceobjNode find(AceobjNode n, String tag)
  {
    if (n == null || !n.type.equalsIgnoreCase("Tag"))
    {
      return null;
    }
    if (n.value.equalsIgnoreCase(tag))
    {
      // System.out.println("Found tag" + tag + n.right); // debug
      return n;
    }
    else
    {
      AceobjNode result = find(n.down, tag);
      if (result != null)
      {
        return result;
      }
      else
      {
        return find(n.right, tag);
      }
    }
  }

  /**
   * Return the Ace class type such as Sequence, Paper, int,.
   *
   * @return String reprenting the calss type
   */
  
  public String getAceClass()
  {
    return tree.type;
  }
  
  /**
   * Return the ace class type
   */
  
  public String type()
  {
    return tree.type;
  }
  
  /**
   * Return the value of the class, such as Sequence, Paper, etc
   */
  
  public String name()
  {
    return tree.value;
  }
  
  /**
   * Return the root's value as string
   */
  
  public String toString()
  {
    return tree.value;
  }
  
  public boolean isTag()
  {
    return tree.type.equalsIgnoreCase("Tag");
  }

  /**
   * Return the top level tags as an ArrayList.
   *
   * @return ArrayList of String type as tags names
   */
  public ArrayList tags()
  {
    ArrayList tmpvec = new ArrayList();
    System.out.println(tree); //debug
    AceobjNode ptr = tree.right;
    while (ptr != null)
    {
      tmpvec.add(ptr.value);
      ptr = ptr.down;
    }
    return tmpvec;
  }
  
  /**
   * low level tree traversal, will not follow into the database.
   *
   * @return a new Aceobj as a subtree of the parent object, or null if nothing
   * follows
   */
  
  public ACeDB2JavaObject right()
  {
    return new ACeDB2JavaObject(tree.right);
  }
  
  /**
   * similar to right(), return a subtree of the parent
   */
  
  public ACeDB2JavaObject down()
  {
    return new ACeDB2JavaObject(tree.down);
  }

  /**
   * take a tag path separated by dot (.). Such as Origin.from_species. To get
   * the value of the tag you need to use the right function
   *
   * @return the object starting at tag, null if tag not found.
   */
  
  public ACeDB2JavaObject at(String tag_path) throws ACeDB2JavaException
  {
    int dotindex = tag_path.indexOf('.');
    int start = 0;
    String tag;
    AceobjNode ptr = tree;
    while (dotindex != -1)
    {
      tag = tag_path.substring(start, dotindex);
      ptr = searchDown(ptr, tag);
      if (ptr == null)
      {
        StringBuffer tmp = new StringBuffer(tag);
        tmp.append(" ").append("Tag not found");
        throw new ACeDB2JavaException(tmp.toString());
      }
      start = dotindex + 1;
      dotindex = tag_path.indexOf('.', start);
    }
    tag = tag_path.substring(start);
    ptr = searchDown(ptr, tag);
    if (ptr == null)
    {
      return null;
    }

		//	StringBuffer tmp = new StringBuffer(tag);
    //	tmp.append(" ").append("Tag not found");
    //	throw new AceException(tmp.toString());
    //}
    
    return new ACeDB2JavaObject(ptr);
  }

  /**
   * Search the object for the tag.
   *
   * @return the node as an Aceobj or null
	 *
   */
  
  public ACeDB2JavaObject get(String tag)
  {   // not done yet
    AceobjNode ptr = tree.right;
    AceobjNode rslt = null;
    rslt = find(ptr, tag);
    if (rslt == null)
    {
      return null;
    }
    else
    {
      return new ACeDB2JavaObject(rslt);
    }
  }

  /**
   * Search the immediate right tags downward.
   *
   * @return returns null if not found
   */
  
  protected AceobjNode searchDown(AceobjNode subtree, String tag)
          throws ACeDB2JavaException
  {
    AceobjNode ptr = subtree.right;
    if (ptr == null)
    {
      throw new ACeDB2JavaException("Right side of root is null");
    }
    if (!ptr.isTag())
    {
      throw new ACeDB2JavaException("Right side of root not tag");
    }
    while (ptr != null && !ptr.value.equalsIgnoreCase(tag))
    {
      ptr = ptr.down;
    }
    return ptr;
  }

	///////////////////////////////////////////////////////////////////	
  /////////////  DumpStore helper class ////////////////////////////
  /**
   * For storing the aceDump temperarily
   */
  
  protected class DumpStore
  {

    /**
     * rawTree is a string dump of a Ace object obtained from show -j A typical
     * example is shown below. Starts with an empty line.
     *
     * ?Protein?CAD12727?	?tag?Title?	?Text?high affinity choline transporter?
     * ?tag?function?	?tag?Description?	?txt?neuronal Na-dependent choline
     * transporter? ?tag?Peptide?	?Peptide?CAD12727?	?int?584?	?int?-1522093612?
     * ?tag?DB_info?	?tag?db_xref?	?Text?GI:17148509? ?tag?Protein_id?
     * ?txt?CAD12727.1? ?tag?Origin?	?tag?Species?	?Species?Torpedo marmorata?
     * ?tag?Visible?	?tag?Corresponding_DNA?	?Sequence?AJ420808?
     *
     * // 1 object dumped // 1 Active Objects
     *
     * Ends with an empty line. Followed by one or two lines of summary info. if
     * More than one objects are dumped then blank lines separate objects. the
     * Last two lines are the summary report as to how many objects are dumped.
     */
    
    private String rawTree = null;  // the java string representation
    private int start = 0;          // start of line
    private int end = -1;            // end of line
    private int tabCount = 0;      // leading tabs

    /**
     * Constructing the object from a acedump string.
     *
     * @param s java string representation of aceobject, from the show -j
     * command
     */
    
    public DumpStore(String s)
    {
      //System.err.println("Trying to make a DumpStore object from: " + s);
      rawTree = s.substring(s.indexOf('?'));
    }

    /**
     * Obtain the next line of text.
     */
    
    public String nextline()
    {
      start = end + 1;
      if ((end = rawTree.indexOf('\n', start)) == -1)
      {
        return new String(""); // detect end of string
      }
      return rawTree.substring(start, end);
    }

    /**
     * Make the root branch from the first line of the Ace object dump in the
     * Java format. Make other nodes the child of root.
     *
     * @return the root node
     */
    
    public AceobjNode rootBranch()
    {
      tabCount = 0;
      String ln = nextline();
      int b = tabCount;
      int e = ln.indexOf('\t', b);
      AceobjNode root = null;
      if (e == -1)
      {
        // no more information than just a root node!
        root = new AceobjNode(ln.substring(b));
      }
      else
      {
        root = new AceobjNode(ln.substring(b, e));
        AceobjNode tmp = root;
        b = e + 1;
        e = ln.indexOf('\t', b);
        if (e == -1)
        {
          tmp.addChild(ln.substring(b));
        }
        else
        {
          tmp.addChild(ln.substring(b, e));
          b = e + 1;
          e = ln.indexOf('\t', b);
          tmp = tmp.down;
          while (e != -1)
          {
            tmp.addSibling(ln.substring(b, e));
            tmp = tmp.right;
            b = e + 1;
            e = ln.indexOf('\t', b);
          }
          tmp.addSibling(ln.substring(b));
        }
      }
      return root;
    }

    /**
     * Get the next horizontal branch as AceobjNode (pointer), if no more branch
     * left returns null.
     *
     * @return AceobjNode or null if no more branche (reaching empty line)
     */
    /*
     public AceobjNode nextBranch() {
     tabCount = 0;
     String ln = nextline();
     if (ln.equals("")) return null;  // stop when hitting the empty line
     while (ln.charAt(tabCount) == '\t') tabCount++;

     int b = tabCount;
     int e = ln.indexOf('\t', b);
     AceobjNode branch;
     if (e != -1) { // more than one node
     branch = new AceobjNode(ln.substring(b,e));
     AceobjNode tmp = branch;
     b = e + 1;
     e = ln.indexOf('\t', b);
     while (e != -1) {
     tmp.right = new AceobjNode(ln.substring(b,e), tmp);
     tmp = tmp.right;
     b = e + 1;
     e = ln.indexOf('\t', b);
     }
     tmp.right = new AceobjNode(ln.substring(b), tmp);
     }
     else { // only one node
     branch = new AceobjNode(ln.substring(b));
     }
     return branch;
     }
     */
    
    public int getTabCount()
    {
      return tabCount;
    }
  }
 
}
