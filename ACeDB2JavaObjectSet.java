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

public class ACeDB2JavaObjectSet
{


	private String strdump = null;  // Multiple string dump from aceserver

  /* an example show -j dump looks like this

?Protein?CAC43234?	?tag?Title?	?Text?glycoprotein hormone alpha subunit protein?
	?tag?Peptide?	?Peptide?CAC43234?	?int?120?	?int?-60884869?
	?tag?DB_info?	?tag?db_xref?	?Text?GI:14589315?
		?tag?Protein_id?	?txt?CAC43234.1?
	?tag?Origin?	?tag?Species?	?Species?Scyliorhinus canicula?
	?tag?Visible?	?tag?Corresponding_DNA?	?Sequence?AJ310343?

?Protein?CAC43235?	?tag?Title?	?Text?follicle stimulating hormone beta subunit?
	?tag?Peptide?	?Peptide?CAC43235?	?int?121?	?int?-2055911037?
	?tag?DB_info?	?tag?db_xref?	?Text?GI:14589317?
		?tag?Protein_id?	?txt?CAC43235.1?
	?tag?Properties?	?tag?N_term_missing?
	?tag?Origin?	?tag?Species?	?Species?Scyliorhinus canicula?
	?tag?Visible?	?tag?Corresponding_DNA?	?Sequence?AJ310344?

// 2 objects dumped
// 2 Active Objects
  */
	
  private int start=0, end=-1; // Start and end of substring delimiting the next object
	
  /** Construct an Objset object from a ace object string dump with the show -j 
	 * command. 
	 * @param input should have one or more ace object in string format.
	 */
	public ACeDB2JavaObjectSet(String input) { strdump = input; }

	/** Fetch the next Aceobj, one pass only; FORWARD_ONLY iteration!.
	 * @return next Aceobj or null if the end is reached.
	 */
	public ACeDB2JavaObject next() {
		start = end + 1;
		end = strdump.indexOf("\n\n", start); 
		if (end == -1) return null;  // no more empty line
		// reacing // 2 objects dumped
		//System.err.println("start: " + start + " end: " + end);
		String substr = strdump.substring(start, end);
		//System.err.println(substr);
		return new ACeDB2JavaObject(substr);
	}

}
