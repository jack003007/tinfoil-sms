/** 
 * Copyright (C) 2011 Tinfoilhat
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.tinfoil.sms;

import java.util.ArrayList;
import java.util.HashMap;
import android.app.ProgressDialog;
import android.content.Context;

public class ExchangeKey implements Runnable {

	private Context c;		//Currently not used but IS needed because messages will be sent from this thread
	public static ProgressDialog keyDialog;
	private ArrayList<String> untrusted;
	private ArrayList<String> trusted;
	//private HashMap<String, Boolean> subSelected;
	//private boolean[] selected;
	//private ArrayList<TrustedContact> tc;
	
	private ArrayList<ContactParent> contacts;
	
	private Number number;
	
	/**
	 * A constructor used by the ManageContactsActivity to set up the key exchange thread
	 * @param c The context of the activity
	 * @param tc The list of contacts
	 * @param subSelected The hash of numbers whether they are selected or not
	 * @param selected The list of whether contacts have been selected or not
	 */
	/*public void startThread(Context c, ArrayList<TrustedContact> tc, HashMap<String, Boolean> subSelected, boolean[] selected)
	{
		this.c = c;
		
		this.subSelected = subSelected;
		this.selected = selected;
		this.tc = tc;
		this.trusted = null;
		this.untrusted = null;
		
		Thread thread = new Thread(this);
		thread.start();
	}*/
	
	public void startThread(Context c, ArrayList<ContactParent> contacts)
	{
		this.c = c;
		this.contacts = contacts;
		this.trusted = null;
		this.untrusted = null;
		
		/*
		 * Start the thread from the constructor
		 */
		Thread thread = new Thread(this);
		thread.start();
	}	
	
	public void run() {
		
		/* 
		 * Used by ManageContacts Activity to determine from the 
		 * contacts that have been selected need to exchange keys or
		 * stop sending secure messages
		 */
		if(trusted == null && untrusted == null)
		{
			trusted = new ArrayList<String>();
			untrusted = new ArrayList<String>();
			
			for(int i = 0; i < contacts.size(); i++)
			{
				for(int j = 0; j < contacts.get(i).getNumbers().size(); j++)
				{
					if(contacts.get(i).getNumber(j).isSelected())
					{
						if(!contacts.get(i).getNumber(j).isTrusted())
						{
							trusted.add(contacts.get(i).getNumber(j).getNumber());
						}
						else
						{
							untrusted.add(contacts.get(i).getNumber(j).getNumber());
						}
					}				
				}
			}
		}
		
		
		
		/*
		 * This is actually how removing contacts from trusted should look since it is just a
		 * deletion of keys. We don't care if the contact will now fail to decrypt messages that
		 * is the user's problem
		 */
		if(untrusted != null)
		{
			for(int i = 0; i < untrusted.size(); i++)
			{
				//untrusted.get(i).clearPublicKey();
				number = MessageService.dba.getRow(untrusted.get(i)).getNumber(untrusted.get(i));
				number.clearPublicKey();
				MessageService.dba.updateKey(number);
			}
		}
		
		//TODO update to actually use proper key exchange (via sms)
		//Start Key exchanges 1 by 1, using the user specified time out.
		if(trusted != null)
		{
			for(int i = 0; i < trusted.size(); i++)
			{
				number = MessageService.dba.getRow(trusted.get(i)).getNumber(trusted.get(i));
				number.setPublicKey();
				MessageService.dba.updateKey(number);
			}
		}
		
		//Dismisses the load dialog since the load is finished
		keyDialog.dismiss();
	}

}