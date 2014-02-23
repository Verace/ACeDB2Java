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



/*
*  This is largely based on the information to be found here
*
*  http://www.acedb.org/Development/wdoc/Socket_Server/SOCKET_interface.html
*
*
*/



package com.relvar.ACeDB2Java;

public interface IACeDB2JavaConstants
{
  int RESULT_FORWARD = 1;   // for Result iterator direction
  int RESULT_RANDOM = 2;    // result iterator random access
  int RESULT_BACKWARD = 3;  // result iterator reverse direction

  int ACESERV_MSGREQ = 101;         // for Aceconnect message type
  int ACESERV_MSGDATA = 102;      // 

  /* Only in message received by client
   * these will not gain a lot of efficiency, because it is only
   * used a few times
   */
  int ACESERV_MSGOK = 103;        // only used by client
  int ACESERV_MSGENCORE = 104;    // by client
  int ACESERV_MSGFAIL = 105;      // by client
  int ACESERV_MSGKILL = 106;  
  
}
