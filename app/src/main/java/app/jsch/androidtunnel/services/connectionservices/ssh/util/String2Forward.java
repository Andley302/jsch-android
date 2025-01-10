package app.jsch.androidtunnel.services.connectionservices.ssh.util;

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
 *	Deconstructs PuTTY style port forwarding strings
 *
 */
public class String2Forward {
	private String remotehost,direction;
	private int localport, remoteport;
	public class ParseException extends Exception {
		private static final long serialVersionUID = 1L;

		public ParseException() {
			super();
		}
		public ParseException(String detailMessage) {
			super(detailMessage);
		}
	}
	
	/**
	 * @param fwd
	 *            a string of the format XLocalPort=RemoteHost:RemotePort where X is either L, or R indicating Local, Remote, or Dynamic forwards. 
	 *            (e.g. L7000=mail:143) similar to the PuTTY port forward
	 *            format. NOTE: currently all forwards are created as local forwards.
	 */
	public String2Forward(String fwd) throws ParseException {
		super ();
		// TODO implement R forwards
		//
		direction = fwd.substring(0,1);
		if ( ! ("L".equals(direction) || ("R".equals(direction)))) {throw new ParseException("illegal Remote/Local specifier");}
		fwd = fwd.replace("L", "").replace("R", "");
		int eq = fwd.indexOf('=');
		int cl = fwd.indexOf(':');
		
		if (eq == -1) {throw new ParseException("missing =");}
		if (cl == -1) {throw new ParseException("missing :");}
		if (eq < 1) {throw new ParseException("missing local port");}
		if (cl <= eq + 1) {throw new ParseException("missing remote host");}
		if (cl+1 >= fwd.length()) {throw new ParseException("missing remote port");}
		
		try {
			localport  = Integer.parseInt(fwd.substring(0, eq));
			remotehost = fwd.substring (eq+1 , cl);
			remoteport = Integer.parseInt(fwd.substring(cl+1));
		} catch (NumberFormatException e) {
			throw new ParseException("one of the ports is non numeric");
		}
	}
	
	public String toString() {
		return direction + localport + "=" + remotehost + ":" + remoteport;
	}

	/**
	 * @return the remotehost
	 */
	public String getRemotehost() {
		return remotehost;
	}

	/**
	 * @return the localport
	 */
	public int getLocalport() {
		return localport;
	}

	/**
	 * @return the remoteport
	 */
	public int getRemoteport() {
		return remoteport;
	}

	public String getDirection () {
		return direction;
	}
}
