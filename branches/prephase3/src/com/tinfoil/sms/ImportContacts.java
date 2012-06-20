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
import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
* ImportContact activity allows for contacts to be imported from the native
* database to the tinfoil-sms database. When a contact is imported, a contacts'
* numbers, last message, date of last message, and type is stored. Once a 
* contact is imported they cannot be imported until deleted from tinfoil-sms's
* database. Changes made in the tinfoil-sms database will not apply to the 
* contact in the native database. An imported contact will appear in the
* ManageContactsActivity.
*/
public class ImportContacts extends Activity {
	private Button confirm;
	private ListView importList;
	private ArrayList<TrustedContact> tc;
	private boolean disable;
	private ArrayList<Boolean> inDb;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.importcontacts);
        
        confirm = (Button) findViewById(R.id.confirm);
        importList = (ListView)findViewById(R.id.import_contact_list);
        tc = new ArrayList<TrustedContact>();
        ArrayList<Number> number;
        String name;
        String id;
       
        Uri mContacts = ContactsContract.Contacts.CONTENT_URI;
        Cursor cur = managedQuery(mContacts, new String[] {Contacts._ID, 
        		Contacts.DISPLAY_NAME, Contacts.HAS_PHONE_NUMBER},
        		null, null, Contacts.DISPLAY_NAME);
        
        inDb = new ArrayList<Boolean>();
        
        if (cur.moveToFirst()) {
                do {
                	
                	number = new ArrayList<Number>();
            		name = cur.getString(cur.getColumnIndex(Contacts.DISPLAY_NAME));
            		id  = cur.getString(cur.getColumnIndex(Contacts._ID));
            		
            		if (cur.getString(cur.getColumnIndex(Contacts.HAS_PHONE_NUMBER)).equalsIgnoreCase("1"))
            		{
            			Cursor pCur = getContentResolver().query(Phone.CONTENT_URI, 	
            					new String[] { Phone.NUMBER, Phone.TYPE}, Phone.CONTACT_ID +" = ?", 
            	 	 		    new String[]{id}, null);
            			if (pCur.moveToFirst())
            			{
            				do
            				{
            					String numb = (pCur.getString(pCur.getColumnIndex(Phone.NUMBER)));
            					int type = pCur.getInt(pCur.getColumnIndex(Phone.TYPE));
            					Uri uriSMSURI = Uri.parse("content://sms/");
            					            					
            					//This now takes into account the different formats of the numbers. 
            					Cursor mCur = getContentResolver().query(uriSMSURI, new String[]
            							{"address", "body", "date"}, "address = ? or address = ? or address = ?",
            							new String[] {ContactRetriever.format(numb),
            							"+1" + ContactRetriever.format(numb),
            							"1" + ContactRetriever.format(numb)},
            							"date DESC LIMIT 1");
            					
            					if (mCur.moveToFirst())
            					{
            						//Toast.makeText(this, ContactRetriever.millisToDate(mCur.getLong(mCur.getColumnIndex("date"))), Toast.LENGTH_LONG);
            						number.add(new Number (ContactRetriever.format(numb), type, 
            								mCur.getString(mCur.getColumnIndex("body")), 
            								mCur.getLong(mCur.getColumnIndex("date"))));
            					}
            					else 
            					{
            						number.add(new Number (ContactRetriever.format(numb), type));
            					}
            				} while (pCur.moveToNext());
            			}
            			pCur.close();
            		}
            		
                    if(number!=null)
                    {
                    	
                    	if (!MessageService.dba.inDatabase(number))
                    	{
                    		tc.add(new TrustedContact(name, number));
                    		inDb.add(false);
                    	}
                    }
                    number = null;
                } while (cur.moveToNext());
        }
        
        Uri uriSMSURI = Uri.parse("content://sms/conversations/");
		Cursor convCur = getContentResolver().query(uriSMSURI, 
				new String[]{"thread_id", "snippet"}, null,
				null, "date DESC");
		
		Number newNumber;
		
		while (convCur.moveToNext()) 
		{
			id = convCur.getString(convCur.getColumnIndex("thread_id"));
			newNumber = new Number(null, convCur.getString(convCur.getColumnIndex("snippet")));
			
			Cursor nCur = getContentResolver().query(Uri.parse("content://sms/inbox"), 
					new String[]{"address", "date"}, "thread_id = ?",
					new String[] {id}, "date DESC");

			if (nCur.moveToFirst())
			{
				newNumber.setNumber(ContactRetriever.format(
						nCur.getString(nCur.getColumnIndex("address"))));
				newNumber.setDate(nCur.getLong(nCur.getColumnIndex("date")));
			}
			else
			{
				
				Cursor sCur = getContentResolver().query(Uri.parse("content://sms/sent"), 
						new String[]{"address", "date"}, "thread_id = ?",
						new String[] {id}, "date DESC");
				if (sCur.moveToFirst())
				{
					newNumber.setNumber(ContactRetriever.format(
							sCur.getString(sCur.getColumnIndex("address"))));
					newNumber.setDate(sCur.getLong(sCur.getColumnIndex("date")));
				}
			}
			if (!TrustedContact.isNumberUsed(tc, newNumber.getNumber()) 
					&& !MessageService.dba.inDatabase(newNumber.getNumber()))
			{
				tc.add(new TrustedContact(newNumber));
				inDb.add(false);
            }
		}
        
        if (tc != null && tc.size() > 0)
        {
        	disable = false;
        	importList.setAdapter(new ArrayAdapter<String>(this, 
					android.R.layout.simple_list_item_multiple_choice, getNames()));
	        
	        importList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        }
        else 
        {
        	disable = true;
        	importList.setAdapter(new ArrayAdapter<String>(this, 
					android.R.layout.simple_list_item_1, getNames()));
        }
        
        confirm.setOnClickListener(new View.OnClickListener() {
		
        	public void onClick(View v) {
        		//Add Contacts to the tinfoil-sms database from android's database
        		if (!disable)
        		{
        			for (int i = 0; i<tc.size();i++)
	        		{        			
        				if (inDb.get(i))
	        			{
        					MessageService.dba.addRow(tc.get(i));
	        			}
	        		}
	        		finish();
        		}
			}
        });    	
                
        importList.setOnItemClickListener(new OnItemClickListener(){
        	public void onItemClick(AdapterView<?> parent, View view,
        			int position, long id) {
        		//Keep track of the contacts selected.
        		if (!disable)
        		{
        			change(position);
        		}
        	}
        });
	}
	
	public void remove (int position)
	{
		inDb.set(position, false);
	}
	
	public void add(int position)
	{
		inDb.set(position, true);
	}
	
	public void change(int position)
	{
		if (tc != null)
		{
			if (inDb.get(position))
			{
				remove(position);
			}
			else
			{
				add(position);
			}
		}
	}
	/**
	 * Produces an ArrayList of contact names from the ArrayList of TrustedContacts
	 * @return : ArrayList, a list of the names of each person on the list.
	 */
	public ArrayList<String> getNames()
	{
		ArrayList <String> names = new ArrayList<String>();
		if (!disable)
		{
			for (int i = 0; i < tc.size();i++)
			{
				names.add(tc.get(i).getName());
			}
			return names;
		}
		names.add("No Contacts to Import");
		return names;
		
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
   	 
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.import_menu, menu);
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
	        case R.id.all:
	        	if (tc!=null)
				{
					for (int i = 0; i < tc.size();i++)
					{
						importList.setItemChecked(i, true);
						if (tc != null)
						{
							add(i);
						}
					}
				}
		        return true;
	        case R.id.rm_import:
	        	if (tc!=null)
				{
					for (int i = 0; i < tc.size();i++)
					{
						importList.setItemChecked(i, false);
						if (tc != null)
						{
							remove(i);
						}
					}
				}
	        	return true;
	        		    
	        default:
	        return super.onOptionsItemSelected(item);
    	}
     
    }
	
}
