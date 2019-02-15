/*
 * Copyright 2019 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.gradle;

import java.util.LinkedList;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.base.Unhandled;

/** Creates diffs which are easy to assert against. */
public class Diff {
	public static String computeDiff(String before, String after) {
		diff_match_patch differ = new diff_match_patch();
		LinkedList<diff_match_patch.Diff> diffs = differ.diff_main(before, after);
		differ.diff_cleanupEfficiency(diffs);
		differ.diff_cleanupSemantic(diffs);
		return StringPrinter.buildString(printer -> {
			for (diff_match_patch.Diff diff : diffs) {
				switch (diff.operation) {
				case EQUAL:
					// do nothing
					break;
				case DELETE:
				case INSERT:
					printer.println(diff.operation.name());
					printer.println(FileMisc.toUnixNewline(diff.text));
					break;
				default:
					throw Unhandled.enumException(diff.operation);
				}
			}
		});
	}
}
