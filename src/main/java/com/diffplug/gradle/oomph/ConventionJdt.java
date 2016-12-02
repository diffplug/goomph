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
package com.diffplug.gradle.oomph;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.JavaVersion;

import com.diffplug.common.collect.ImmutableList;

/**
 * Adding the JDT convention to your project
 * adds the following features:
 * 
 * - `org.eclipse.platform.ide`
 * - `org.eclipse.jdt`
 * - `org.eclipse.ui.views.log`
 * 
 * You can set the installed JRE as follows:
 * 
 * ```gradle
 * oomphIde {
 *     jdt {
 *         
 *         installedJre {
 *             version = '1.6.0_45'
 *             installedLocation = new File('C:/jdk1.6.0_45')
 *             markDefault = true // or false
 *             executionEnvironments = ['JavaSE-1.6'] // any execution environments can be specified here.
 *         }
 *         compilerComplianceLevel('1.6')
 *         classpathVariable('myClasspath', '/var/lib/repo')
 *     }
 * }
 * ```
 */
public class ConventionJdt extends OomphConvention {
	final static String JDT_CORE_PREFS = ".metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.jdt.core.prefs";
	final static List<String> JDT_COMPLIANCE_PROPS = ImmutableList.of("org.eclipse.jdt.core.compiler.codegen.targetPlatform", "org.eclipse.jdt.core.compiler.compliance", "org.eclipse.jdt.core.compiler.source");
	final static String JDT_CLASSPATH_VAR_FMT = "org.eclipse.jdt.core.classpathVariable.%s";

	ConventionJdt(OomphIdeExtension extension) {
		super(extension);
		requireIUs(IUs.IDE, IUs.JDT, IUs.ERROR_LOG);
		setPerspectiveOver(Perspectives.JAVA, Perspectives.RESOURCES);
	}

	final Set<InstalledJre> installedJres = new HashSet<>();
	final Map<String, String> classpathVariables = new LinkedHashMap<>();
	private JavaVersion compilerComplianceLevel;

	/** Adds an installed JRE with the given content. */
	public void installedJre(Action<InstalledJre> action) {
		InstalledJre instance = new InstalledJre();
		action.execute(instance);
		installedJres.add(instance);
	}

	/** Sets default compliance level */
	public void compilerComplianceLevel(String compilerComplianceLevel) {
		this.compilerComplianceLevel = JavaVersion.toVersion(compilerComplianceLevel);
	}

	/** Adds a compiler class path variable. */
	public void classpathVariable(String name, String value) {
		classpathVariables.put(name, value);
	}

	@Override
	public void close() {
		// add installed jres
		if (!installedJres.isEmpty()) {
			extension.addSetupAction(new InstalledJreAdder(installedJres));
		}
		if (!classpathVariables.isEmpty()) {
			extension.workspaceProp(JDT_CORE_PREFS, new JavaClasspathVariableAction(classpathVariables));
		}
		if (compilerComplianceLevel != null) {
			extension.workspaceProp(JDT_CORE_PREFS, new JavaComplianceAction(compilerComplianceLevel));
		}
	}

	static class JavaComplianceAction implements Action<Map<String, String>> {
		private final JavaVersion complianceLevel;

		public JavaComplianceAction(JavaVersion complianceLevel) {
			super();
			this.complianceLevel = complianceLevel;
		}

		@Override
		public void execute(Map<String, String> props) {
			JDT_COMPLIANCE_PROPS.forEach(p -> props.put(p, complianceLevel.toString()));
			//Use default compliance settings.
			props.put("org.eclipse.jdt.core.compiler.codegen.inlineJsrBytecode", "enabled");
			props.put("org.eclipse.jdt.core.compiler.problem.assertIdentifier", "error");
			props.put("org.eclipse.jdt.core.compiler.problem.enumIdentifier", "error");
		}
	}

	static class JavaClasspathVariableAction implements Action<Map<String, String>> {
		private final Map<String, String> classpathVariables;

		public JavaClasspathVariableAction(Map<String, String> classpathVariables) {
			super();
			this.classpathVariables = classpathVariables;
		}

		@Override
		public void execute(Map<String, String> props) {
			classpathVariables.forEach((key, value) -> props.put(String.format(JDT_CLASSPATH_VAR_FMT, key), value));
		}
	}
}
