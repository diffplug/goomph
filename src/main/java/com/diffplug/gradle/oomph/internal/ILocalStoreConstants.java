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

public interface ILocalStoreConstants {

	/** Common constants for History Store classes. */
	public final static int SIZE_LASTMODIFIED = 8;
	public static final int SIZE_COUNTER = 1;
	public static final int SIZE_KEY_SUFFIX = SIZE_LASTMODIFIED + SIZE_COUNTER;

	/** constants for safe chunky streams */

	// 40b18b8123bc00141a2596e7a393be1e
	public static final byte[] BEGIN_CHUNK = {64, -79, -117, -127, 35, -68, 0, 20, 26, 37, -106, -25, -93, -109, -66, 30};

	// c058fbf323bc00141a51f38c7bbb77c6
	public static final byte[] END_CHUNK = {-64, 88, -5, -13, 35, -68, 0, 20, 26, 81, -13, -116, 123, -69, 119, -58};

	/** chunk delimiter size */
	// BEGIN_CHUNK and END_CHUNK must have the same length
	public static final int CHUNK_DELIMITER_SIZE = BEGIN_CHUNK.length;
}
