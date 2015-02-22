/*
 * Copyright � 2015 | Alexander01998 | All rights reserved.
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package tk.wurst_client.v1_6_alt_list_converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JProgressBar;

import tk.wurst_client.v1_6_alt_list_converter.alts.Alt;
import tk.wurst_client.v1_6_alt_list_converter.encryption.NewEncryption;
import tk.wurst_client.v1_6_alt_list_converter.encryption.OldEncryption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Converter implements Runnable
{
	private Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private String path;
	private JsonObject options;
	private JProgressBar progress;
	private JLabel action;
	private long total;
	private long converted;
	
	public Converter(String path, JsonObject options, JProgressBar progress, JLabel action)
	{
		this.path = path;
		this.options = options;
		this.progress = progress;
		this.action = action;
	}
	
	@Override
	public void run()
	{
		progress.setMinimum(0);
		progress.setMaximum(1000);
		Iterator<Entry<String, JsonElement>> itr = options.entrySet().iterator();
		while(itr.hasNext())
		{
			Map.Entry<String, JsonElement> entry = itr.next();
			File file = new File(path, entry.getKey() + ".txt");
			if(file.exists())
				total += file.length();
		}
		itr = options.entrySet().iterator();
		while(itr.hasNext())
		{
			Map.Entry<String, JsonElement> entry = itr.next();
			File oldFile = new File(path, entry.getValue().getAsJsonObject().get("oldfile").getAsString());
			File newFile = new File(path, entry.getValue().getAsJsonObject().get("newfile").getAsString());
			if(entry.getValue().getAsJsonObject().get("enabled").getAsBoolean()
				&& oldFile.exists())
			{
				setCurrentAction("Checking file size");
				long fileSize = oldFile.length();
				setCurrentAction("Converting " + oldFile.getName() + " to " + newFile.getName());
				try
				{
					convert(oldFile, newFile);
					setCurrentAction("Deleting old file (" + oldFile.getName() + ")");
					if(oldFile.exists())
						oldFile.delete();
					converted += fileSize;
				}catch(IOException e)
				{
					e.printStackTrace();
				}finally
				{
					updateProgress();
					setCurrentAction("");
				}
			}
		}
	}
	
	private void convert(File oldFile, File newFile) throws IOException
	{
		if(oldFile.getName().equals("alts.wurst"))
		{	
			try
			{
				JsonObject json = new JsonObject();
				ArrayList<Alt> alts = new ArrayList<Alt>();
				setCurrentAction("Reading old alt list");
				BufferedReader load = new BufferedReader(new FileReader(oldFile));
				for(String line = ""; (line = load.readLine()) != null;)
				{
					String data[] = line.split("�");
					String name = OldEncryption.decrypt(data[0]);
					String password = "";
					if(data.length == 2)
						password = OldEncryption.decrypt(data[1]);
					alts.add(new Alt(name, password));
				}
				load.close();
				setCurrentAction("Sorting alts");
				alts.sort(new Comparator<Alt>()
					{
					@Override
					public int compare(Alt o1, Alt o2)
					{
						return o1.getEmail().compareToIgnoreCase(o2.getEmail());
					}
				});
				setCurrentAction("Converting alts");
				for(Alt alt : alts)
				{
					setCurrentAction("Converting alts (" + json.entrySet().size() + " / " + alts.size() + ")");
					JsonObject jsonAlt = new JsonObject();
					jsonAlt.addProperty("name", alt.getName());
					jsonAlt.addProperty("password", alt.getPassword());
					jsonAlt.addProperty("cracked", alt.isCracked());
					json.add(alt.getEmail(), jsonAlt);
				}
				setCurrentAction("Encrypting  and saving new alt list");
				Files.write(newFile.toPath(), NewEncryption.encrypt(gson.toJson(json)).getBytes(NewEncryption.CHARSET));
			}catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private void setCurrentAction(String action)
	{
		this.action.setText("<html>"
			+ "<body width=512>"
			+ "<center>"
			+ action);
	}
	
	private void updateProgress()
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					progress.setValue((int)(converted * 1000 / total));
				}catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}).start();
	}
}
