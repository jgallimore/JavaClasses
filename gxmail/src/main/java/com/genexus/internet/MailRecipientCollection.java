package com.genexus.internet;

import java.io.IOException;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

public class MailRecipientCollection
{
	private Vector vector = new Vector();

	public void add(MailRecipient recipient)
	{
		if	(recipient.getName().trim().length() != 0 || recipient.getAddress().trim().length() != 0)
		{
			vector.addElement(new MailRecipient(recipient.getName(), recipient.getAddress()));
		}
	}

	public void addNew(String name, String address)
	{
		add(new MailRecipient(name, address));
	}

	public void removeAllItems()
	{
		vector.removeAllElements();
	}

	public MailRecipient item(int idx)
	{
		Object o = vector.elementAt(idx - 1);
		return (MailRecipient) o;
	}

	public int getCount()
	{
		return vector.size();
	}

	public void clear()
	{
		vector.removeAllElements();
	}

	String getRecipientsString(int addressFormat)
	{
		String ret = "";

		for (Enumeration en = vector.elements(); en.hasMoreElements();)
		{
			MailRecipient recipient = (MailRecipient) en.nextElement();

			ret += recipient.getRecipientString(addressFormat) + (en.hasMoreElements()?";":"");
		}

		return ret;
	}

	String getRecipientsString()
	{
		return getRecipientsString(2);
	}

	static MailRecipientCollection getFromString(String list) throws IOException
	{
		MailRecipientCollection ret = new MailRecipientCollection();
		
		String separator = (list.indexOf(';') > 0) ? ";": ",";		
		StringTokenizer tokenizer = new StringTokenizer(list, separator);
   		while (tokenizer.hasMoreTokens()) 
		{
			ret.add(MailRecipient.getFromString(tokenizer.nextToken()));
		}

		return ret;
	}
}

