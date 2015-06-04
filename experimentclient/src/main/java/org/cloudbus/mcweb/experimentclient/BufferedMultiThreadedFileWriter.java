package org.cloudbus.mcweb.experimentclient;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.cloudbus.mcweb.util.Closeables;

import com.google.common.base.Preconditions;

/**
 * A buffered thread safe stream to a file.
 * 
 * @author nikolay
 *
 */
public class BufferedMultiThreadedFileWriter implements AutoCloseable{

	private static final String NEW_LINE = System.getProperty("line.separator");
	private static final String SEP = ";";
	
	private final BufferedWriter writer;
	
	/** Buffer to avoid creating new string builder upon every print. */
	private final StringBuilder buffer = new StringBuilder();

	/**
	 * Ctor.
	 * @param file - the file.
	 */
	public BufferedMultiThreadedFileWriter(final String file) {
		try {
			this.writer = new BufferedWriter(new FileWriter(file));
			Closeables.addSilentShutdownHook(writer);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Ctor.
	 * @param file - the file.
	 */
	public BufferedMultiThreadedFileWriter(final File file) {
		try {
			this.writer = new BufferedWriter(new FileWriter(file));
			Closeables.addSilentShutdownHook(writer);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Writes a CSV line to the file
	 * @param values
	 */
	public synchronized void writeCsv(final int[] colLengths, final Object ... values) {
		Preconditions.checkArgument(colLengths.length == values.length);
		buffer.setLength(0); // Clear the buffer
		for (int i = 0 ; i < values.length; i ++) {
			String val = values[i] == null ? "null" : values[i].toString();
			buffer.append(val);

			// Pad it to right
			for(int j = 0; j < colLengths[i] - val.length(); j++){
				buffer.append(" ");
			}
			
			
			if(i < values.length - 1) {
				buffer.append(SEP);
			} else {
				buffer.append(NEW_LINE);
			}
		}
		try {
			writer.append(buffer);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	/**
	 * Flushes the buffer.
	 */
	public synchronized void flush() {
		try {
			writer.flush();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}


	@Override
	public synchronized void close() throws Exception {
		Closeables.closeAll(writer);
	}

}
