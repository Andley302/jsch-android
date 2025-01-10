package app.jsch.androidtunnel.services.connectionservices.ssh.util;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.os.Environment;

/* Copyright 2011 Shai Ayal
 * 
 * This file is part of SPT.
 *
 * SPT is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPT is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SPT.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * @author Shai Ayal
 * 
 * Creates a sorted list of files in the SDCARD root
 *
 */
public class FileList {
	private String[] namesList = null;
	private String[] fnamesList = null;
	
	public FileList() {
		// build list of all files in sdcard root
		final File sdcard = Environment.getExternalStorageDirectory();

		// Don't show a dialog if the SD card is completely absent.
		final String state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)
				&& !Environment.MEDIA_MOUNTED.equals(state)) {
			return;
		}

		List<String> names = new LinkedList<String>();
		List<String> fnames = new LinkedList<String>();
		{
			File[] files = sdcard.listFiles();
			if (files != null) {
				for(File file : sdcard.listFiles()) {
					if(file.isDirectory()) continue;
					fnames.add(file.getAbsolutePath());
				}
			}
			Collections.sort(fnames);
			for (String name : fnames) {
				names.add( (new File (name)).getName());
			}
		}
		names.add(0,"- None -");
		fnames.add(0,"");
		
		namesList = names.toArray(new String[] {});
		fnamesList = fnames.toArray(new String[] {});
	}


	/**
	 * @return the namesList
	 */
	public String[] getNamesList() {
		return namesList;
	}


	/**
	 * @return the fnamesList
	 */
	public String[] getFnamesList() {
		return fnamesList;
	}

}
