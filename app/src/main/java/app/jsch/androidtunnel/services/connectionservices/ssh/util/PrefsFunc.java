package app.jsch.androidtunnel.services.connectionservices.ssh.util;
import android.preference.Preference;

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
 * Abstract class for iteration over preferences
 */
public abstract class PrefsFunc {
	abstract public void go (Preference pref);
}
