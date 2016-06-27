/*
 * Copyright 2016 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.gradle.oomph.internal;

import java.io.*;

/**
 * Appends data, in chunks, to a file. Each chunk is defined by the moment
 * the stream is opened (created) and a call to #succeed is made. It is
 * necessary to use the <code>SafeChunkyInputStream</code> to read its
 * contents back. The user of this class does not need to know explicitly about
 * its chunk implementation.
 * It is only an implementation detail. What really matters to the outside
 * world is that it tries to keep the file data consistent.
 * If some data becomes corrupted while writing or later, upon reading
 * the file, the chunk that contains the corrupted data is skipped.
 * <p>
 * Because of this class purpose (keep data consistent), it is important that the
 * user only calls <code>#succeed</code> when the chunk of data is successfully
 * written. After this call, the user can continue writing data to the file and it
 * will not be considered related to the previous chunk. So, if this data is
 * corrupted, the previous one is still safe.
 *
 * @see SafeChunkyInputStream
 */
public class SafeChunkyOutputStream extends FilterOutputStream {
	protected String filePath;
	protected boolean isOpen;

	public SafeChunkyOutputStream(File target) throws IOException {
		this(target.getAbsolutePath());
	}

	public SafeChunkyOutputStream(String filePath) throws IOException {
		super(new BufferedOutputStream(new FileOutputStream(filePath, true)));
		this.filePath = filePath;
		isOpen = true;
		beginChunk();
	}

	protected void beginChunk() throws IOException {
		write(ILocalStoreConstants.BEGIN_CHUNK);
	}

	protected void endChunk() throws IOException {
		write(ILocalStoreConstants.END_CHUNK);
	}

	protected void open() throws IOException {
		out = new BufferedOutputStream(new FileOutputStream(filePath, true));
		isOpen = true;
		beginChunk();
	}

	public void succeed() throws IOException {
		try {
			endChunk();
			close();
		} finally {
			isOpen = false;
			safeClose(this);
		}
	}

	/**
	 * Closes a stream and ignores any resulting exception. This is useful
	 * when doing stream cleanup in a finally block where secondary exceptions
	 * are not worth logging.
	 *
	 *<p>
	 * <strong>WARNING:</strong>
	 * If the API contract requires notifying clients of I/O problems, then you <strong>must</strong>
	 * explicitly close() output streams outside of safeClose().
	 * Some OutputStreams will defer an IOException from write() to close().  So
	 * while the writes may 'succeed', ignoring the IOExcpetion will result in silent
	 * data loss.
	 * </p>
	 * <p>
	 * This method should only be used as a fail-safe to ensure resources are not
	 * leaked.
	 * </p>
	 * See also: https://bugs.eclipse.org/bugs/show_bug.cgi?id=332543
	 */
	private void safeClose(Closeable stream) {
		try {
			if (stream != null) {
				stream.close();
			}
		} catch (IOException e) {
			//ignore
		}
	}

	@Override
	public void write(int b) throws IOException {
		if (!isOpen)
			open();
		super.write(b);
	}
}
