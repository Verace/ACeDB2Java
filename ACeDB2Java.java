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


import java.io.*;
import java.net.*;
import java.security.*;

/**
 * The <code>Aceconnect</code> class is the driver for interacting the socket
 * aceserver. The key methods are <code><A HREF="Aceconnect.html#login()">login</A>
 * </code> and <code><A HREF="Aceconnect.html#talk()">talk</A></code>. All
 * command defined in tace can be executed through talk.
 *
 * @author Kemin Zhou
 * @version 1.0 Rewrite by Kemin Zhou from Affymetrix
 */
public class ACeDB2JavaConnect implements IACeDB2JavaConstants
{
  /**
   * Nested class Message used by class Aceconnect Message structure | header 50
   * bytes | body variable bytes | header: |Magic 4|length 4|version 4|clientID
   * 4|maxBytes 4|msgType 30| int int int int int byte[] msgType: ACESERV_MSGREQ
   * ACESERV_MSGDATA ACESERV_MSGOK ACESERV_MSGENCORE ACESERV_MSGFAIL
   * ACESERV_MSGKILL
   */
  private class Message
  {
    private int[] header = new int[5];    // no Message Type
    private final static int fullHeaderLen = 50;
    private String msgType;
    private int msgTypeInt = 0;  // used for speed
    //private String body;             // store as bytes cheaper
    private byte[] bodyBytes = null; // this can be very large

    /**
     * Default constructor create empty Message object with some default values.
     */
    public Message()
    {
      header[0] = 0x12345678;  // magic
      header[2] = 1;           // version currently 1
      header[3] = 0;           // clientID set by server
      header[4] = 0;           // maxBytes set by server
    }
    ;
  		/** Constrator constructs a Message object with message 
  		 *  body = message, length is the 
  		 *  this one is not used anywhere.
  		 *  @param length length of the message including null.
  		 *  @param clientID set by the server.
  		 *  @param maxBytes set by the server.
  		 *  @param request  one of the msgType.
  		 *  @param message  the body of message
  		 */
      public Message(int length, int clientID, int maxBytes,
            String request, String message)
    {
      header[0] = 0x12345678;  // magic
      header[1] = length + 1;   // null terminator included
      header[2] = 1;           // version currently 1
      header[3] = clientID;
      header[4] = maxBytes;
      this.msgType = request;
      bodyBytes = message.getBytes();
    }
    public int getLength()
    {
      return header[1];
    }
    public int getClientID()
    {
      return header[3];
    }
    public String getMsgType()
    {
      return msgType;
    }
    public int getMsgTypeInt()
    {
      return msgTypeInt;
    }
    public int getMaxBytes()
    {
      return header[4];
    }
    /**
     * Get the message body as String. Does converstion from bytes array to
     * string
     */
    public String getBodyString()
    {
      return new String(bodyBytes);
    }
    public void setBody(String msg)
    {
      if (msg.equalsIgnoreCase("encore"))
      {
        header[1] = 7;
        msgType = "ACESERV_MSGENCORE";
      }
      else
      {
        header[1] = msg.length() + 1;
        msgType = "ACESERV_MSGREQ";
      }
      bodyBytes = msg.getBytes();
    }

    /**
     * Encodes data message to be sent to the server.
     *
     * @return the byte array to be send to the socket The calling function is
     * responsible for breaking message into slices &lg= to the maxBytes
     */
    public byte[] encodeData(String msg)
    {
      if (msg.length() >= getMaxBytes())
      {
        System.err.println("Message longer than maxBytes: " + getMaxBytes());
        System.exit(1);
      }
      msgType = "ACESERV_MSGDATA";
      bodyBytes = msg.getBytes();
      header[1] = msg.length() + 1;  // set length of message
      return encode();
    }
    /**
     * encode regular usually short messages.
     */
    public byte[] encode(String m)
    {
      setBody(m);
      return encode();
    }
    public byte[] encode()
    {
      byte[] rtn = null;
      try
      {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bao);
        for (int i = 0; i < 5; i++)
        {
          dos.writeInt(header[i]);
        }
        dos.writeBytes(msgType);  //Write the msgtype as bytes
        //Padding the msgtype to 30 bytes
        byte pad = (byte) '\u0000';
        while (bao.size() < fullHeaderLen)
        {
          dos.writeByte(pad);
        }
        dos.write(bodyBytes);
        //Adding one byte null
        byte nu[] = new byte[1];
        nu[0] = (byte) '\u0000';
        dos.write(nu);          // this is ok
        //dos.write(nu, 0, 1);
        //Transform all these information into a byte array
        rtn = bao.toByteArray();
        bao.close();  //Close these two streams
        dos.close();
      }
      catch (IOException e)
      {
        System.out.println("Message encoding problem " + e);
        System.exit(1);
      }
      return rtn;
    }

    //protected synchronized void parseHeader(InputStream in) {
    //protected synchronized void parseHeader(DataInputStream in) {
    protected void parseHeader(DataInputStream in)
    {
      //throws AceEmptyMessageException {
      try
      {
        //DataInputStream dis = new DataInputStream(in);
        //for (int i=0; i<5; i++) header[i] = dis.readInt();
        for (int i = 0; i < 5; i++)
        {
          header[i] = in.readInt();
        }
        byte[] tmp = new byte[30];
        in.read(tmp, 0, 30);
        msgType = new String(tmp).trim();  // remove spaces at ends
        if (msgType.equals("ACESERV_MSGOK"))
        {
          msgTypeInt = ACESERV_MSGOK;
        }
        else if (msgType.equals("ACESERV_MSGENCORE"))
        {
          msgTypeInt = ACESERV_MSGENCORE;
        }
        else if (msgType.equals("ACESERV_MSGFAIL"))
        {
          msgTypeInt = ACESERV_MSGFAIL;
        }
        else if (msgType.equals("ACESERV_MSGKILL"))
        {
          msgTypeInt = ACESERV_MSGKILL;
        }
        else
        {
          System.err.println(msgType + " impossible");
          System.exit(1);
        }
        bodyBytes = null;  // get rid of the old message body
  				/*
         if (header[1] == 0) { 
         throw new AceEmptyMessageException("fetal error in parseHeader(DAtainputStream in)"); 
         }
         */
      }
      catch (IOException e)
      {
        System.err.println("Fetal error in parseHeader " + e);
        e.printStackTrace();
        System.exit(1);
      }
    }

    protected byte[] readBody(DataInputStream in)
    {
      try
      {
        //System.out.println("msgLen=" + getLength());  //debug
        if (getLength() == 0)
        { // empty body
          bodyBytes = null;
          //make sure msgType is MSG_KILL
          if (!getMsgType().equals("ACESERV_MSGKILL"))
          {
            System.err.println("Message type " + getMsgType()
                    + " combined with empty body.  Realy bad");
            System.exit(1);
          }
          return null;
        }
        bodyBytes = new byte[getLength() - 1];
        in.readFully(bodyBytes);
        in.skipBytes(1);
      }
      catch (IOException e)
      {
        System.err.println("fetal error in readBody" + e);
        System.exit(1);
      }
      return bodyBytes;
    }

    /**
     * reads in the whole messages and store inside this object.
     */
    protected void read(DataInputStream ins)
    {
      //protected void read(DataInputStream ins) throws AceEmptyMessageException {
      parseHeader(ins);
      readBody(ins);
    }

    /**
     * constructs new Message objects from the socket
     *
     * @return message body as String
     */
    protected String parse(DataInputStream socket_dis)
    //protected String parse(DataInputStream socket_dis) throws AceEmptyMessageException
    {
      read(socket_dis);
      return new String(bodyBytes);
    }
  }
  ///// end of Message Class///////////////////////////////////////
  //the Aceconnect should be named Aceconnect class ///////////////

  private Socket dbSocket = null;
  // take output from sockeIns
  protected DataInputStream socketIns = null;
  //private InputStream socketIns = null;
  private PrintStream intoSocket_ps = null;

  private Message the_message = new Message();

  public ACeDB2JavaConnect(Socket socket)
  {
    try
    {
      dbSocket = socket;
      socketIns = new DataInputStream(dbSocket.getInputStream());
      intoSocket_ps = new PrintStream(dbSocket.getOutputStream(), true);
    }
    catch (IOException ioe)
    {
      System.err.println("Aceconnect error " + ioe);
    }
  }

  /**
   * The client only consructs ACESERV_MSGREQ, ACESERV_MSGENCORE for requesting
   * information. The other types are only defined on the server side, such as
   * ACESERV_MSGOK.
   */
  public String getMsgType()
  {
    return the_message.msgType;
  }
  /**
   * returns the message type as int defined in Constants.java
   */
  public int getMsgTypeInt()
  {
    return the_message.msgTypeInt;
  }
  /**
   * returns the message body as String. Could be very long
   */
  public String getMessage()
  {
    return new String(the_message.bodyBytes);
  }

  /**
   * signing on to the server. You need to obtain username and password from the
   * database (acedb) administrator. You can use anonymous with no password to
   * login if the database provide world-read access
   */
  public boolean login(String user, String pwd)
  {
    //	boolean ok = true;
    //try {
    if (execCommand(HELLO) == ACESERV_MSGOK)
    {
      String hash = getHash(user, pwd, getMessage());
      //ok = execCommand(user + " " + hash).equalsIgnoreCase("ACESERV_MSGOK");
      return execCommand(user + " " + hash) == ACESERV_MSGOK;
    }
    else
    {
      return false;
    }
    /*
     }
     catch (AceEmptyMessageException em) {
     System.exit(1);
     }
     return ok;
     */
  }

  /**
   * Send query command to Saceserver and return the Status of the command
   * execution; use getMessage() to get the message. You can use all the
   * commands that are taken by tace. Return type is a string defined by
   * ACESERVER indicating the status of the command execution. The actual
   * message should be retrieved by calling
   * <code>Aceconnect.getMessage()</code>.
   * <A HREF="Aceconnect.html#getMessage()">getMessage</A></code>
   *
   * @return the status of acedb command
   */
  public int execCommand(String msg)
  {
    //public String execCommand(String msg) {
    //throws AceEmptyMessageException {
    sendCommand(msg);
    the_message.readBody(socketIns);
    //return getMsgType();
    return getMsgTypeInt();
  }

  /**
   * I don't know what type of data should be send through this channel. May be
   * *.ace files contents.
   */
  public String execData(String data)
  {
    System.err.println(data + " is being sent to server");
    sendData(data);
    the_message.readBody(socketIns);
    System.err.println("the data returned from server " + getMessage());
    return getMsgType();
  }

  /**
   * Sends commands to server and read the header from the socket.
   *
   * @return String representing the status of command.
   */
  protected int sendCommand(String msg)
  {
    //protected String sendCommand(String msg) {
    //throws AceEmptyMessageException {
    byte[] byteMessage = the_message.encode(msg);
    //System.err.println(new String(byteMessage) + " with length=" + byteMessage.length); // debug
    intoSocket_ps.write(byteMessage, 0, byteMessage.length);
    intoSocket_ps.flush();
    //try {
    the_message.parseHeader(socketIns);
  		//} 
  		/*
     catch (AceEmptyMessageException em) {
     System.err.println("We got empty message with type " + 
     getMsgType());
     System.err.println("The command is: " + msg);
     dumpSocketInfo();
     em.printStackTrace();
     the_message.

     //System.exit(1);
     }
     */
    //return getMsgType();
    return getMsgTypeInt();
  }

  /**
   * return a BufferedReader for reading long messages.
   */
  /* not useful
   protected BufferedReader getBufferedReader() { 
   return new BufferedReader(new InputStreamReader(socketIns));
   }
   */
  /**
   * Send data to be parsed into the server database. Right now there is now way
   * to send messages longer than maxBytes long. Message must be broken into &lt
   * maxBytes to be sent separately. Each message must be self contained and
   * should not be truncated. There is no way of telling the server that there
   * are more comming, such as the ENCORE message.
   *
   * @return MESSAGE_TYPE String, string version is easier for debug
   */
  public String sendData(String msg)
  {
    if (the_message.getMaxBytes() <= msg.length())
    {
      System.err.println("Message body length longer than maxByte "
              + the_message.getMaxBytes() + " Bytes");
      System.exit(1);
    }  // needs to figure out how to send long data
    //System.err.println("Sending message to server"); // debug
    byte[] byteMessage = the_message.encodeData(msg);
    intoSocket_ps.write(byteMessage, 0, byteMessage.length);
    intoSocket_ps.flush();
    //	try {
    the_message.parse(socketIns);
    //}
  		/*
     catch (AceEmptyMessageException em) {
     System.err.println("Got empty message from sendData" + em);
     }
     */
    return getMsgType();
  }

  /**
   * Calculate the hash of user and pwd with random.
   */
  private String getHash(String user, String pwd, String rand)
  {
    MD5 md = new MD5();
    String h1 = md.getMD5(user + pwd);
    return md.getMD5(h1 + rand);
  }
  /**
   * Terminate the server.
   */
  public void quit()
  {
    //public void quit() throws AceEmptyMessageException {   
    if (execCommand("quit") == ACESERV_MSGKILL)
    {
      System.err.println("Connection terminated normally");
    }
    else
    {
      System.err.println("Connection not properly terminated");
    }
  }

  /**
   * Tell the server to shutdown.
   */
  public void shutdown()
  {  // only administrator can use
    //public void shutdown() throws AceEmptyMessageException {  // only administrator can use
    if (execCommand("shutdown now") == ACESERV_MSGKILL)
    {
      System.err.println(" Shutdown normally");
    }
    else
    {
      System.err.println(" Not properly terminated");
    }
  }
  /**
   * to see that this connection is closed or not
   */
  public boolean isClosed()
  {
    return socketIns == null;
  }

  /**
   * Close the socket and associated streams.
   */
  public void close()
  {
    if (socketIns != null)
    {
      try
      {
        socketIns.close();
        socketIns = null;
        intoSocket_ps.close();
        intoSocket_ps = null;
        dbSocket.close();
        dbSocket = null;
      }
      catch (IOException e)
      {
        System.out.println("Socket Closing problem " + e);
        System.exit(1);
      }
      System.err.println("Aceconnect to host closed");
    }
  }
  /**
   * for debuging
   */
  public void dumpSocketInfo()
  {
    try
    {
      System.err.println("Local port: " + dbSocket.getLocalPort());
      System.err.println("timeout: " + dbSocket.getSoTimeout());
      boolean kp = dbSocket.getKeepAlive();
      System.err.println("keepAlive: " + kp);
      System.err.println("String version: " + dbSocket);
    }
    catch (SocketException se)
    {
      System.err.println(se);
      System.exit(1);
    }
  }

  /*  Static variables */
  /*
   public final static int MAGIC = 0x12345678;
   public final static int VERSION = 1;
   public final static String ACESERV_MSGREQ = new String("ACESERV_MSGREQ");
   public final static String ACESERV_MSGOK = new String("ACESERV_MSGOK");
   public final static String ACESERV_MSGDATA = new String("ACESERV_MSGDATA");
   public final static String ACESERV_MSGENCORE = new String("ACESERV_MSGENCORE");
   public final static String ACESERV_MSGKILL = new String("ACESERV_MSGKILL");
   public final static String ACESERV_MSGFAIL = new String("ACESERV_MSGFAIL");
   */
  private final static String HELLO = new String("bonjour");
  //private final static String ENCORE = new String("encore");

  /**
   * final number used in the objects of this class message header
   */
  private final int HEAD_LENGTH = 50;

  /**
   * security encoding class for hashing password using the MD5 algorithm nested
   * class of Aceconnect
   */
  private class MD5
  {
    private MessageDigest md = null; //MD engine class holder

    /**
     * Constructor for initializing the MD5 engine: default from SUN
     */
    public MD5()
    {
      try
      {
        md = MessageDigest.getInstance("MD5");
        md.reset();
      }
      catch (NoSuchAlgorithmException e)
      {
        System.out.println("MD5 algorithm not implemeted " + e);
        System.exit(1);
      }
    }

    public void update(byte[] input)
    {
      md.update(input);
    }
    public byte[] digest()
    {
      return md.digest();
    }

    /**
     * produce MD5 HexString from String
     */
    public String getMD5(String input)
    {
      update(input.getBytes());
      return computeHexString(md.digest());
    }

    /**
     * The method for distributing some responsibility from the getMD5() method
     */
    protected String computeHexString(byte[] input)
    {
      StringBuffer result = new StringBuffer();
      for (int i = 0; i < input.length; i++)
      {
        if ((0xFF & input[i]) < 0x10)
        {
          result.append("0" + Integer.toHexString(0xFF & input[i]));
        }
        else
        {
          result.append(Integer.toHexString(0xFF & input[i]));
        }
      }
      return result.toString();
    }
  }  // end of nested class
}
