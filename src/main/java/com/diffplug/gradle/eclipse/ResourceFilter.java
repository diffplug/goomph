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
package com.diffplug.gradle.eclipse;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.util.Node;

import com.diffplug.common.base.Preconditions;

/** Models an eclipse resource filter. */
public class ResourceFilter {
	/** Creates a ResourceFilter which includes the specified resources. */
	public static ResourceFilter include() {
		return new ResourceFilter(Kind.INCLUDE_ONLY);
	}

	/** Creates a ResourceFilter which excludes the specified resources. */
	public static ResourceFilter exclude() {
		return new ResourceFilter(Kind.EXCLUDE_ALL);
	}

	private ResourceFilter(Kind initial) {
		kinds.add(initial);
	}

	/////////////////////
	// must set a type //
	/////////////////////
	/** Sets this resource filter to include files. */
	public ResourceFilter files() {
		return addKind(Kind.FILES);
	}

	/** Sets this resource filter to include folders. */
	public ResourceFilter folders() {
		return addKind(Kind.FOLDERS);
	}

	/** Sets this resource filter to include files and folders. */
	public ResourceFilter filesAndFolders() {
		return addKind(Kind.FILES, Kind.FOLDERS);
	}

	ResourceFilter addKind(Kind... toAdd) {
		for (Kind kind : toAdd) {
			kinds.add(kind);
		}
		return this;
	}

	////////////////////////
	// must set a matcher //
	////////////////////////
	/** Matches on `name`, as opposed to `location` and `projectRelativePath`. */
	public ResourceFilter name(String value) {
		return setMatcher(value, Matcher.name);
	}

	/** Matches on `location`, as opposed to `name` and `projectRelativePath`. */
	public ResourceFilter location(String value) {
		return setMatcher(value, Matcher.location);
	}

	/** Matches on `projectRelativePath`, as opposed to `name` and `location`. */
	public ResourceFilter projectRelativePath(String value) {
		return setMatcher(value, Matcher.projectRelativePath);
	}

	private ResourceFilter setMatcher(String matchValue, Matcher matcher) {
		Preconditions.checkArgument(this.matcher == null, "Can only call one of %s", Arrays.asList(Matcher.values()));
		this.matchValue = Objects.requireNonNull(matchValue);
		this.matcher = Objects.requireNonNull(matcher);
		return this;
	}

	///////////////////////
	// optional settings //
	///////////////////////
	/** The match will be caseSensitive. */
	public ResourceFilter caseSensitive() {
		caseSensitive = true;
		return this;
	}

	/** The match is a regex. */
	public ResourceFilter regex() {
		isRegex = true;
		return this;
	}

	/** The match will be recursive from the root directory. */
	public ResourceFilter recursive() {
		return addKind(Kind.INHERITABLE);
	}

	////////////////////
	// implementation //
	////////////////////
	private EnumSet<Kind> kinds = EnumSet.noneOf(Kind.class);
	private boolean caseSensitive = false;
	private boolean isRegex = false;
	private Matcher matcher;
	private String matchValue;

	@Override
	public int hashCode() {
		return Objects.hash(kinds, caseSensitive, isRegex, matcher, matchValue);
	}

	void checkValid() {
		Preconditions.checkState(matcher != null, "Must call one of %s", Arrays.asList(Matcher.values()));
		Preconditions.checkState(matchValue != null, "Must call one of %s", Arrays.asList(Matcher.values()));
		Preconditions.checkState(kinds.contains(Kind.FILES) || kinds.contains(Kind.FOLDERS), "Must call one of files(), folders(), or filesAndFolders()");
	}

	@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_INFERRED")
	void appendToFilteredResources(Node filteredResources) {
		Preconditions.checkArgument(filteredResources.name().equals("filteredResources"));
		checkValid();
		Node filterNode = filteredResources.appendNode("filter");
		filterNode.appendNode("id", hashCode()); // any random string will work, we'll use our hash
		filterNode.appendNode("name", "");
		filterNode.appendNode("type", Kind.create(kinds));
		// make sure that it matches the build folder
		Node matcherNode = filterNode.appendNode("matcher");
		matcherNode.appendNode("id", "org.eclipse.ui.ide.multiFilter");
		matcherNode.appendNode("arguments", "1.0-" + matcher.name() + "-matches-" + caseSensitive + "-" + isRegex + "-" + matchValue);
	}

	/** Values from from [IResourceFilterDescription](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Freference%2Fapi%2Forg%2Feclipse%2Fcore%2Fresources%2Fclass-use%2FIResourceFilterDescription.html) */
	enum Kind {
		INCLUDE_ONLY, EXCLUDE_ALL, FILES, FOLDERS, INHERITABLE;

		int flag() {
			return 1 << ordinal();
		}

		public static int create(EnumSet<Kind> kinds) {
			return type(kinds.toArray(new Kind[kinds.size()]));
		}

		public static int type(Kind... kinds) {
			int value = 0;
			for (Kind kind : kinds) {
				value |= kind.flag();
			}
			return value;
		}
	}

	/** Kinds of matcher. */
	enum Matcher {
		name, projectRelativePath, location
	}
}
