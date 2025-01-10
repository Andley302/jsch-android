package app.jsch.androidtunnel.services.connectionservices.ssh.util;

import java.util.LinkedList;

public class FIFO<E> extends LinkedList<E> {
	private static final long serialVersionUID = -5634021890210730059L;
	
	private int limit;

	public FIFO(int limit) {
		this.limit = limit;
	}

	@Override
	public boolean add(E o) {
		super.add(o);
		while (size() > limit) { super.remove(); }
		return true;
	}
}
