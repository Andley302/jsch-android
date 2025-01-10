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

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;


import java.util.LinkedList;
import java.util.List;

import app.jsch.androidtunnel.services.connectionservices.ssh.util.String2Forward;
import app.jsch.androidtunnel.logs.AppLogManager;

public class ConnectionInfo implements UserInfo, Parcelable, UIKeyboardInteractive {

	private String host = null;
	private String user = null;
	private String passphrase = null;
	private String password = null;
	private String keypath = null;
	private int port = 0;
	private int dynamic_port = 0;
	private Boolean compression = true;
	private Boolean remote_accept = false;
    private Boolean local_accept = false;
    private Boolean show_notifications = true;
	private List<Forward> forwards = new LinkedList<Forward>();

    public class InvalidException extends Exception {
		private static final long serialVersionUID = -5040005532652987428L;
		public InvalidException() {
			super();
		}
		public InvalidException(String detailMessage) {
			super(detailMessage);
		}
	}
	
	/**
	 * @author shaia
	 *
	 */
	private static class Forward implements Parcelable {
		private int localport, remoteport;
		private String remotehost, direction;
		private Boolean local_accept, remote_accept;

		public Forward(int localport, int remoteport, String remotehost) {
			super();
			this.localport = localport;
			this.remoteport = remoteport;
			this.remotehost = remotehost;
		}

		/**
		 * @param fwd
		 *            a string of the format XLocalPort=RemoteHost:RemotePort where X is either L, or R indicating Local, Remote, or Dynamic forwards. 
		 *            (e.g. L7000=mail:143) similar to the PuTTY port forward
		 *            format.
		 * @throws String2Forward.ParseException
		 */
		public Forward(String fwd, Boolean remote_accept, Boolean local_accept ) throws String2Forward.ParseException {
			super ();
			String2Forward s2f;
			s2f = new String2Forward (fwd);

			localport  = s2f.getLocalport ();
			remotehost = s2f.getRemotehost ();
			remoteport = s2f.getRemoteport ();
            direction  = s2f.getDirection ();
            this.remote_accept = remote_accept;
            this.local_accept = local_accept;
		}

		public void setPortForwardingL(Session s) throws JSchException {
			if ("L".equals(direction) ) {
				s.setPortForwardingL(local_accept ? "0.0.0.0" : "127.0.0.1" ,localport, remotehost, remoteport);
			} else {
				s.setPortForwardingR(remote_accept ? "*": null ,localport, remotehost, remoteport);
			}
		}

		public void delPortForwardingL(Session s) throws JSchException {
			if ("L".equals(direction) ) {
				s.delPortForwardingL(localport);
			} else {
				s.delPortForwardingR(localport);
			}
		}

		@SuppressWarnings("unused")
		public static final Parcelable.Creator<Forward> CREATOR
		= new Parcelable.Creator<Forward>() {
			public Forward createFromParcel(Parcel source) { return new Forward (source);}

			public Forward[] newArray(int size) { 	return new Forward[size];}
		};

		private Forward(Parcel source) {
			localport = source.readInt();
			remoteport = source.readInt();
			remotehost = source.readString();
		}

		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(localport);
			dest.writeInt(remoteport);
			dest.writeString(remotehost);
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "L" + localport + "=" + remotehost + ":" + remoteport;
		}

		public int describeContents() {
			return 0;
		}
	}

	public ConnectionInfo(String host, String user, int port,
			String passphrase, 
			String password, 
			String keypath, 
			Boolean compression,
			Boolean remote_accept,
			Boolean local_accept,
			int dynamic_port,
            Boolean show_notifications
            ) {
		super();
		this.host = host;
		this.user = user;
		this.port = port;
		this.dynamic_port = dynamic_port;
		this.passphrase = passphrase;
		this.password = password;
		this.keypath = keypath;
		this.compression = compression;
		this.remote_accept = remote_accept;
        this.local_accept = local_accept;
        this.show_notifications = show_notifications;
	}
	
	public void Validate () throws InvalidException {
		if (host.length()==0) {throw new InvalidException("Invalid Host");}
		if (user.length()==0) {throw new InvalidException("Invalid User");}
		if (port<=0) {throw new InvalidException("Invalid Port");}
		if (dynamic_port<0) {throw new InvalidException("Invalid Dynamic Port");}
	}

	public void AddForward(int localport, int remoteport, String remotehost) {
		forwards.add(new Forward(localport, remoteport, remotehost));
	}

	public void AddForwards(String fwds) throws InvalidException, String2Forward.ParseException {
		/*if (fwds.length()==0) {throw new InvalidException("No Forwards");}
		String fwd[] = fwds.split(",");
		for (String aFwd : fwd) {
			forwards.add(new Forward(aFwd, remote_accept, local_accept));
		}*/

	}

	public void setPortForwardingL(Session s) throws JSchException {
        for (Forward forward : forwards) {
            forward.setPortForwardingL(s);
        }
	}

	public void delPortForwardingL(Session s) throws JSchException {
        for (Forward forward : forwards) {
            forward.delPortForwardingL (s);
        }
	}

	/**
	 * @return A string representation of the Port forwards in the PuTTY format: L%localport%=%remoteport%:%remoteport%, ...
	 */
	public String getPortForwards () {
		String retval = "";
        for (Forward forward : forwards) {
            retval = retval + forward.toString() + ",";
        }
		return retval;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public int getDynamic_port() {
		return dynamic_port;
	}

	public String getUser() {
		return user;
	}

	public String getPassphrase() {
		return passphrase;
	}

	public String getPassword() {
		return password;
	}

	public String getKeypath() {
		return keypath;
	}

	public void setKeypath(String keypath) {
		this.keypath = keypath;
	}

	public Boolean getCompression () {
		return compression;
	}

	public Boolean getRemote_accept() {
		return remote_accept;
	}

    public Boolean getLocal_accept() { return local_accept; }

    public Boolean getShow_notifications() { return show_notifications; }

	public boolean promptPassphrase(String message) {
		return true;
	}

	public boolean promptPassword(String message) {
		return true;
	}

	public boolean promptYesNo(String message) {
		return true;
	}

	public void showMessage(String message) {
		AppLogManager.addToLog(message);
	}

    @Override
    public String[] promptKeyboardInteractive (String destination, String name, String instruction, String[] prompt, boolean[] echo) {
        String[] retval = new String[1];
        retval[0] = passphrase != null  && passphrase.length () > 0 ? passphrase : password;
        return retval;
    }

	/*
	 * Parceling methods
	 */

	public static final Parcelable.Creator<ConnectionInfo> CREATOR
	= new Parcelable.Creator<ConnectionInfo>() {
		public ConnectionInfo createFromParcel(Parcel arg0) { return new ConnectionInfo (arg0);}

		public ConnectionInfo[] newArray(int size) {return new ConnectionInfo[size];}
	};

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(host);
		dest.writeString(user);
		dest.writeString(password);
		dest.writeString(passphrase);
		dest.writeString(keypath);
		dest.writeInt   (port);
		dest.writeInt   (dynamic_port);
		dest.writeInt   (forwards.size());
        for (Forward forward : forwards) {
            dest.writeParcelable(forward, flags);
        }
	}

	private ConnectionInfo(Parcel in) {
		host       = in.readString();
		user       = in.readString();
		password   = in.readString();
		passphrase = in.readString();
		keypath    = in.readString();
		port       = in.readInt();
		dynamic_port = in.readInt();
		int sz     = in.readInt();
		for (int i=0; i<sz; i++) {
			forwards.add((Forward) in.readParcelable(Forward.class.getClassLoader()));
		}
	}

	public int describeContents() {
		return 0;
	}
}
