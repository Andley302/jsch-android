package app.jsch.androidtunnel.services.connectionservices.ssh.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;

public class LogLine implements Parcelable {
	Date	date;
	String	text;
	
	public LogLine(Date date, String text) {
		super();
		this.date = date;
		this.text = text;
	}

	public LogLine(String text) {
		super();
		this.text = text;
		this.date = new Date();
	}
	
	/**
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}
	
	/**
	 * @return the text
	 */
	public String getText() {
		return text;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss ").format (date) +
                text.replace ("com.jcraft.jsch.JSchException: ","")
                    .replace ("java.net.","");
	}

	// Parcelable methods
	private LogLine (Parcel in) {
		date = new Date (in.readLong());
		text = in.readString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int arg1) {
		out.writeLong(date.getTime());
		out.writeString(text);
	}
	
	public static final Parcelable.Creator<LogLine> CREATOR
	= new Parcelable.Creator<LogLine>() {
		public LogLine createFromParcel(Parcel in) {
			return new LogLine(in);
		}

		public LogLine[] newArray(int size) {
			return new LogLine[size];
		}
	};
}
