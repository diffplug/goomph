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
package com.diffplug.gradle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.diffplug.common.collect.ImmutableList;

public class InputStreamCollector extends Thread {

	private final InputStream iStream;

	private final PrintStream pStream;

	private final Charset charset;

	private final ImmutableList.Builder<String> output;

	private volatile IOException exception;

	public InputStreamCollector(@Nonnull InputStream is, @Nullable PrintStream ps, @Nullable Charset cs) {
		this.iStream = Objects.requireNonNull(is);
		this.pStream = ps;
		this.charset = cs != null ? cs : Charset.defaultCharset();
		this.output = ImmutableList.builder();
	}

	@Override
	public void run() {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(iStream, charset))) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				output.add(line);
				if (pStream != null) {
					pStream.println(line);
				}
			}
		} catch (IOException ex) {
			this.exception = ex;
		}
	}

	public ImmutableList<String> getOutput() {
		return output.build();
	}

	public IOException getException() {
		return exception;
	}

}
