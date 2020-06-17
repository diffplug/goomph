/*
 * Copyright (C) 2015-2019 DiffPlug
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


import java.util.ArrayList;
import java.util.List;

/** DSL for {@link ResourceFiltersPlugin}. */
public class ResourceFiltersExtension {
	static final String NAME = "eclipseResourceFilters";

	List<ResourceFilter> filters = new ArrayList<>();

	/** Creates a filter which will include the given resources. */
	public ResourceFilter include() {
		return addFilter(ResourceFilter.include());
	}

	/** Creates a filter which will exclude the given resources. */
	public ResourceFilter exclude() {
		return addFilter(ResourceFilter.exclude());
	}

	private ResourceFilter addFilter(ResourceFilter filter) {
		filters.add(filter);
		return filter;
	}
}
