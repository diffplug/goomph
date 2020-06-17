/*
 * Copyright (C) 2016-2019 DiffPlug
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
package com.diffplug.gradle.oomph;


import com.diffplug.gradle.osgi.OsgiExecable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/** Utilities for working with eclipse internals (which should only be called from within an Eclipse instance). */
public class EclipseMisc {
	public static final String ID_PROJECT_IMPORT = "import projects";

	/** Consumer which can throw an exception. */
	public interface ThrowingConsumer<T> {
		void accept(T object) throws Throwable;
	}

	/** Performs some action using an Eclipse service. */
	public static <T> void withService(Class<T> clazz, ThrowingConsumer<T> action) {
		BundleContext bundleContext = FrameworkUtil.getBundle(OsgiExecable.class).getBundleContext();
		ServiceReference<T> reference = bundleContext.getServiceReference(clazz);
		T service = bundleContext.getService(reference);
		try {
			action.accept(service);
		} catch (Throwable t) {
			logException(t);
		} finally {
			bundleContext.ungetService(reference);
		}
	}

	/** Logs an exception to the console and the eclipse error log. */
	public static void logException(Throwable t) {
		// dump it to console
		t.printStackTrace();
		// and put it into the error log
		Status status = new Status(IStatus.ERROR, "goomph", t.getMessage(), t);
		StatusManager.getManager().handle(status);
	}

	/** Waits for all the jobs to finish. */
	public static void waitForJobsToFinish() {
		EclipseMisc.withService(IJobManager.class, jobManager -> {
			Job job;
			while ((job = jobManager.currentJob()) != null) {
				System.out.print("    waiting for " + job.getName() + " to finish... ");
				job.join();
				System.out.println("complete.");
			}
		});
	}
}
