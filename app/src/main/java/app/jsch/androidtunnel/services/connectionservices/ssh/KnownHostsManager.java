package app.jsch.androidtunnel.services.connectionservices.ssh;

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

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;

import java.io.File;

import app.jsch.androidtunnel.R;


public class KnownHostsManager extends ListActivity {

	public final static String TAG = "sshT.KnownHostsManager";
	private ArrayAdapter<String> known_hosts = null;
	HostKeyRepository hkr = null;
	private JSch jsch = null;


	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		registerForContextMenu(getListView());

		jsch = new JSch();

		RefreshHostList();
		setListAdapter(known_hosts);
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
		super.onCreateContextMenu(menu, v, menuInfo);
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		default:
			return super.onContextItemSelected(item);
		}
	}

	private void RefreshHostList () {
		try {
			jsch.setKnownHosts(getFilesDir() + File.separator + getResources().getText(R.string.knownhosts));
			hkr=jsch.getHostKeyRepository();
			HostKey[] hk = hkr.getHostKey();
			String[] hosts;
			if (hk != null) {
				hosts = new String[hk.length];
				for (int i=0; i< hk.length ; i++) {
					hosts[i] = HostKeytoString(hk[i]); 
				}
			}
			else {
				hosts = new String[0];
			}
			known_hosts	= new ArrayAdapter<String> (this, android.R.layout.simple_list_item_1 , hosts);
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
		setListAdapter(known_hosts);
		known_hosts.notifyDataSetChanged();
		known_hosts.notifyDataSetInvalidated();
		getListView().invalidate();
	}

	private String HostKeytoString (HostKey hk) {
		return hk.getHost() + "\n" + hk.getType();
	}

}
