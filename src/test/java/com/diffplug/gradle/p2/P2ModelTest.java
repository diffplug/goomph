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
package com.diffplug.gradle.p2;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.gradle.FileMisc;

public class P2ModelTest {
	private P2Model testData() {
		P2Model model = new P2Model();
		model.addRepo("http://p2repo");
		model.addMetadataRepo("http://metadatarepo");
		model.addArtifactRepo("http://artifactrepo");
		model.addIU("com.diffplug.iu");
		model.addIU("com.diffplug.otheriu", "1.0.0");
		return model;
	}

	@Test
	public void testDirectorArgs() {
		File dest = new File("dest");
		String actual = testData().directorApp(dest, "profile").completeState();
		String expected = StringPrinter.buildStringFromLines(
				"--launcher.suppressErrors",
				"-nosplash",
				"-application org.eclipse.equinox.p2.director",
				"-clean",
				"-consolelog",
				"-repository http://p2repo",
				"-metadataRepository http://metadatarepo",
				"-artifactRepository http://artifactrepo",
				"-installIU com.diffplug.iu,com.diffplug.otheriu/1.0.0",
				"-profile profile",
				"-destination " + FileMisc.asUrl(dest));
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void testMirrorAntFile() {
		File dest = new File("dest");
		String actual = testData().mirrorApp(dest).completeState();
		String expected = StringPrinter.buildStringFromLines(
				"### ARGS ###",
				"--launcher.suppressErrors",
				"-nosplash",
				"-application org.eclipse.ant.core.antRunner",
				"",
				"### BUILD.XML ###",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><project>",
				"  <p2.mirror>",
				"    <source>",
				"      <repository location=\"http://p2repo\"/>",
				"      <repository kind=\"metadata\" location=\"http://metadatarepo\"/>",
				"      <repository kind=\"artifact\" location=\"http://artifactrepo\"/>",
				"    </source>",
				"    <destination location=\"" + FileMisc.asUrl(dest) + "\" append=\"false\"/>",
				"    <iu id=\"com.diffplug.iu\"/>",
				"    <iu id=\"com.diffplug.otheriu\" version=\"1.0.0\"/>",
				"  </p2.mirror>",
				"</project>");
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void testMirrorAntFileWithSlicingOptions() {
		File dest = new File("dest");
		P2Model p2 = testData();
		p2.addSlicingOption("latestVersionOnly", "true");
		p2.addSlicingOption("platformfilter", "win32,win32,x86");
		p2.addSlicingOption("filter", "key=value");
		String actual = p2.mirrorApp(dest).completeState();
		String expected = StringPrinter.buildStringFromLines(
				"### ARGS ###",
				"--launcher.suppressErrors",
				"-nosplash",
				"-application org.eclipse.ant.core.antRunner",
				"",
				"### BUILD.XML ###",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><project>",
				"  <p2.mirror>",
				"    <source>",
				"      <repository location=\"http://p2repo\"/>",
				"      <repository kind=\"metadata\" location=\"http://metadatarepo\"/>",
				"      <repository kind=\"artifact\" location=\"http://artifactrepo\"/>",
				"    </source>",
				"    <destination location=\"" + FileMisc.asUrl(dest) + "\" append=\"false\"/>",
				"    <iu id=\"com.diffplug.iu\"/>",
				"    <iu id=\"com.diffplug.otheriu\" version=\"1.0.0\"/>",
				"    <slicingOptions filter=\"key=value\" latestVersionOnly=\"true\" platformfilter=\"win32,win32,x86\"/>",
				"  </p2.mirror>",
				"</project>");
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void testMirrorAntFileWithAppend() {
		File dest = new File("dest");
		P2Model p2 = testData();
		p2.setAppend(true);
		String actual = p2.mirrorApp(dest).completeState();
		String expected = StringPrinter.buildStringFromLines(
				"### ARGS ###",
				"--launcher.suppressErrors",
				"-nosplash",
				"-application org.eclipse.ant.core.antRunner",
				"",
				"### BUILD.XML ###",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><project>",
				"  <p2.mirror>",
				"    <source>",
				"      <repository location=\"http://p2repo\"/>",
				"      <repository kind=\"metadata\" location=\"http://metadatarepo\"/>",
				"      <repository kind=\"artifact\" location=\"http://artifactrepo\"/>",
				"    </source>",
				"    <destination location=\"" + FileMisc.asUrl(dest) + "\" append=\"true\"/>",
				"    <iu id=\"com.diffplug.iu\"/>",
				"    <iu id=\"com.diffplug.otheriu\" version=\"1.0.0\"/>",
				"  </p2.mirror>",
				"</project>");
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void testMirrorAntFileWithAppendDefault() {
		File dest = new File("dest");
		P2Model p2 = testData();
		String actual = p2.mirrorApp(dest).completeState();
		String expected = StringPrinter.buildStringFromLines(
				"### ARGS ###",
				"--launcher.suppressErrors",
				"-nosplash",
				"-application org.eclipse.ant.core.antRunner",
				"",
				"### BUILD.XML ###",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><project>",
				"  <p2.mirror>",
				"    <source>",
				"      <repository location=\"http://p2repo\"/>",
				"      <repository kind=\"metadata\" location=\"http://metadatarepo\"/>",
				"      <repository kind=\"artifact\" location=\"http://artifactrepo\"/>",
				"    </source>",
				"    <destination location=\"" + FileMisc.asUrl(dest) + "\" append=\"false\"/>",
				"    <iu id=\"com.diffplug.iu\"/>",
				"    <iu id=\"com.diffplug.otheriu\" version=\"1.0.0\"/>",
				"  </p2.mirror>",
				"</project>");
		Assert.assertEquals(expected, actual);
	}
}
