/*
 * Copyright © 2018 Thomas Broyer
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
package com.diffplug.gradle.eclipse.apt;

import java.util.Map;

import org.gradle.api.file.ProjectLayout;
import org.gradle.api.tasks.Internal;
import org.gradle.plugins.ide.api.PropertiesGeneratorTask;

import javax.inject.Inject;

public class GenerateEclipseJdtApt extends PropertiesGeneratorTask<JdtApt> {
  @SuppressWarnings("NullAway.Init") // will be initialized by setJdtApt right after creation
  private EclipseJdtApt jdtApt;

  private final ProjectLayout layout;

  @Deprecated
  public GenerateEclipseJdtApt() {
    this(null);
  }

  @Inject
  public GenerateEclipseJdtApt(ProjectLayout layout) {
    this.layout = layout != null ? layout : getProject().getLayout();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void configure(JdtApt jdtApt) {
    EclipseJdtApt jdtAptModel = getJdtApt();
    jdtAptModel.getFile().getBeforeMerged().execute(jdtApt);
    jdtApt.setAptEnabled(jdtAptModel.isAptEnabled());
    jdtApt.setGenSrcDir(layout.getProjectDirectory().file(jdtAptModel.getGenSrcDir().getAbsolutePath()).getAsFile().getPath());
    jdtApt.setGenTestSrcDir(layout.getProjectDirectory().file(jdtAptModel.getGenTestSrcDir().getAbsolutePath()).getAsFile().getPath());
    jdtApt.setReconcileEnabled(jdtAptModel.isReconcileEnabled());
    jdtApt.getProcessorOptions().clear();
    if (jdtAptModel.getProcessorOptions() != null) {
      for (Map.Entry<String, ?> entry : jdtAptModel.getProcessorOptions().entrySet()) {
        jdtApt
            .getProcessorOptions()
            .put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().toString());
      }
    }
    jdtAptModel.getFile().getWhenMerged().execute(jdtApt);
  }

  @Override
  protected JdtApt create() {
    return new JdtApt(getTransformer());
  }

  @Internal
  public EclipseJdtApt getJdtApt() {
    return this.jdtApt;
  }

  public void setJdtApt(EclipseJdtApt jdtApt) {
    this.jdtApt = jdtApt;
  }
}
