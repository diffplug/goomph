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

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMStandin;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;

import com.diffplug.gradle.oomph.SetupAction.Internal;

public class InstalledJreAdderInternal extends Internal<InstalledJreAdder> {

	InstalledJreAdderInternal(InstalledJreAdder host) {
		super(host);
	}

	@Override
	protected void runWithinEclipse() throws Throwable {
		IVMInstallType[] types = JavaRuntime.getVMInstallTypes();
		for (IVMInstallType type : types) {
			if ("org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType".equals(type.getId())) {
				for (InstalledJre jreToAdd : host.installedJres) {
					IVMInstall realVM = addInstalledJre(type, jreToAdd);
					if (jreToAdd.isMarkDefault()) {
						JavaRuntime.setDefaultVMInstall(realVM, new NullProgressMonitor());
					}
					linkWithExecutionEnvironments(realVM, jreToAdd);
				}
			}
		}
	}

	protected IVMInstall addInstalledJre(IVMInstallType type, InstalledJre jreToAdd) throws Exception {
		IVMInstall retVal = findJre(jreToAdd.getVersion(), jreToAdd.getInstalledLocation());
		if (retVal == null) {
			IStatus validationStatus = type.validateInstallLocation(jreToAdd.getInstalledLocation());
			if (!validationStatus.isOK()) {
				throw new CoreException(validationStatus);
			}
			VMStandin vmStandin = new VMStandin(type, EcoreUtil.generateUUID());
			vmStandin.setInstallLocation(jreToAdd.getInstalledLocation());
			vmStandin.setName("JRE for " + jreToAdd.getVersion());
			IVMInstall realVM = vmStandin.convertToRealVM();
			retVal = realVM;
		}
		return retVal;
	}

	protected void linkWithExecutionEnvironments(IVMInstall installedVm, InstalledJre jreToAdd) {
		if (jreToAdd.getExecutionEnvironments().isEmpty()) {
			return;
		} else {
			Set<String> execEnvsToAdd = new HashSet<>(jreToAdd.getExecutionEnvironments());
			IExecutionEnvironment[] executionEnvironments = JavaRuntime.getExecutionEnvironmentsManager()
					.getExecutionEnvironments();
			for (IExecutionEnvironment iExecutionEnvironment : executionEnvironments) {
				if (execEnvsToAdd.contains(iExecutionEnvironment.getId())) {
					iExecutionEnvironment.setDefaultVM(installedVm);
				}
			}
		}
	}

	private IVMInstall findJre(String version, File location) throws Exception {
		for (IVMInstallType vmInstallType : JavaRuntime.getVMInstallTypes()) {
			for (IVMInstall vmInstall : vmInstallType.getVMInstalls()) {
				File installLocation = vmInstall.getInstallLocation();
				if (location.equals(installLocation)) {
					return vmInstall;
				}
			}
		}

		return null;
	}
}
