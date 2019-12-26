/*
 * Copyright 2020 DiffPlug
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
package com.diffplug.gradle.oomph.thirdparty;


import com.diffplug.gradle.oomph.OomphConvention;
import com.diffplug.gradle.oomph.OomphIdeExtension;
import javax.annotation.Nullable;

/** A Convention which is setup to add a default p2 repository which can be overridden. */
public class WithRepoConvention extends OomphConvention {
	@Nullable
	protected String repo;

	protected WithRepoConvention(OomphIdeExtension extension, String defaultRepo) {
		super(extension);
		this.repo = defaultRepo;
	}

	/** Overrides the default repo.  Setting to null will remove the repo completely. */
	public void usingRepo(@Nullable String repo) {
		this.repo = repo;
	}

	@Override
	public void close() {
		if (repo != null) {
			extension.repo(repo);
		}
	}
}
