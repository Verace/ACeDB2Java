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
import java.util.ArrayList;


public class ACeDB2JavaClient
{
  private int port = 23100;  // The default port for saceserver.
  private String host = "localhost"; // The saceserver hostname.
  private String user = "yourname";   // Established by running makeUserPasswd. See README for details.
  private String password = "yourpass";  // Must be at least 6 characters long.

  /**
   * Store all connect object for book keeping
   */
  
  private static ArrayList connections = new ArrayList();

  /**
   * Create a client object that is not connected yet.
   */
  
  public ACeDB2JavaClient(String h, int p, String usr, String pwd)
  {
    host = h;
    port = p;
    user = usr;
    password = pwd;
  }

  /**
   * Default constructors, emtpy client
   */
  
  public ACeDB2JavaClient()
  {
  }

  /**
   * Create a client with anonymous user.
   */
  
  public ACeDB2JavaClient(String host, int port)
  {
    this.host = host;
    this.port = port;
    user = "yourname";
    password = "yourpass";
  }

  /**
   * Creates a new Aceconnect object based on the information storeed in the
   * Aceclient object.
   */
  
  public ACeDB2JavaConnect getConnect()
  {
    Socket socket = null;
    try
    {
      socket = new Socket(host, port);
    }
    catch (IOException ioe)
    {
      System.err.println("Socket could not be created " + ioe);
    }
    ACeDB2JavaConnect tmpConn = new ACeDB2JavaConnect(socket);
    if (!tmpConn.login(user, password))
    {
      System.err.println("Login failed by " + user);
      return null;
    }
    connections.add(tmpConn);
			//System.err.println("Current connections: " + getConnectCount());
    //debug
    return tmpConn;
  }

  public int getPort()
  {
    return port;
  }
  
  public String getHost()
  {
    return host;
  }
  
  public String getUser()
  {
    return user;
  }
  
  public void close()
  {
    for (int i = 0; i < connections.size(); i++)
    {
      ((ACeDB2JavaConnect) connections.get(i)).close();
    }
  }
  
  public int getConnectCount()
  {
    return connections.size();
  }
  
}
