/*
 * Copyright (C) 2023 DiffPlug
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

public class EquoMigration {
	private static boolean silenceEquoIde = false;

	public static void silenceEquoIDE() {
		silenceEquoIde = true;
	}

	private static boolean eclipseMavenCentral = false;

	public static void eclipseMavenCentral() {
		if (silenceEquoIde || eclipseMavenCentral) {
			return;
		}
		eclipseMavenCentral = true;
		System.out.println("We strongly recommend that you migrate");
		System.out.println("  from 'com.diffplug.eclipse.mavencentral'");
		System.out.println("    to 'dev.equo.p2deps'");
		System.out.println("The new plugin is faster and works seamlessly with both mavencentral");
		System.out.println("and all p2 repositories (e.g. CDT, WTP, m2e, gradle buildship, etc.)");
		System.out.println("");
		System.out.println("For more info: https://github.com/equodev/equo-ide/tree/main/plugin-gradle#user-plugins");
		System.out.println("");
		System.out.println("You can silence this warning with");
		System.out.println("```");
		System.out.println("eclipseMavenCentral {");
		System.out.println("  silenceEquoIDE()");
		System.out.println("  ...");
		System.out.println("```");
	}

	private static boolean asMaven = false;

	public static void asMaven() {
		if (silenceEquoIde || asMaven) {
			return;
		}
		asMaven = true;
		System.out.println("We strongly recommend that you migrate");
		System.out.println("  from 'com.diffplug.p2.asmaven'");
		System.out.println("    to 'dev.equo.p2deps'");
		System.out.println("The new plugin is far faster and works seamlessly with both mavencentral");
		System.out.println("and all p2 repositories (e.g. CDT, WTP, m2e, gradle buildship, etc.)");
		System.out.println("");
		System.out.println("For more info: https://github.com/equodev/equo-ide/tree/main/plugin-gradle#user-plugins");
		System.out.println("");
		System.out.println("You can silence this warning with");
		System.out.println("```");
		System.out.println("p2AsMaven {");
		System.out.println("  silenceEquoIDE()");
		System.out.println("  ...");
		System.out.println("```");
	}

	private static boolean oomph = false;

	public static void oomph() {
		if (silenceEquoIde || oomph) {
			return;
		}
		oomph = true;
		System.out.println("We strongly recommend that you migrate");
		System.out.println("  from 'com.diffplug.oomph.ide'");
		System.out.println("    to 'dev.equo.ide'");
		System.out.println("The new plugin is far faster and works seamlessly with both mavencentral");
		System.out.println("and all p2 repositories (e.g. CDT, WTP, m2e, gradle buildship, etc.)");
		System.out.println("");
		System.out.println("For more info: https://github.com/equodev/equo-ide/blob/main/plugin-gradle/README.md");
		System.out.println("");
		System.out.println("You can silence this warning with");
		System.out.println("```");
		System.out.println("oomphIde {");
		System.out.println("  silenceEquoIDE()");
		System.out.println("  ...");
		System.out.println("```");
	}
}
