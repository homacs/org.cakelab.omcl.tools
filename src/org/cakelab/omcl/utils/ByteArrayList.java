package org.cakelab.omcl.utils;

/**
 * Dynamically growing byte array
 * @author homac
 *
 */
public class ByteArrayList {
	byte[] buffer;
	private int size;
	
	public ByteArrayList(int initialCapacity) {
		buffer = new byte[initialCapacity];
		size = 0;
	}
	
	
	private void grow(int newsize) {
		if (newsize > buffer.length) {
			int capacity = buffer.length;
			while (capacity <= newsize) capacity *= 2;
			byte[] tmp = new byte[capacity];
			System.arraycopy(buffer, 0, tmp, 0, size);
			buffer = tmp;
		}
	}
	
	public void add(byte[] that) {
		add(that, that.length);
	}

	public void add(byte[] that, int length) {
		grow(size + length);
		System.arraycopy(that, 0, buffer, size, length);
		size += length;
	}


	public byte[] getBuffer() {
		return buffer;
	}


	public int getSize() {
		return size;
	}



	
	
}
