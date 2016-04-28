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
package com.diffplug.gradle.swt;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.diffplug.common.base.Unhandled;
import com.diffplug.common.swt.os.SwtPlatform;

public class SwtExtension {
	static final String NAME = "goomphSwt";

	public static final String LUNA_SR2 = "4.4.2";
	public static final String MARS_SR2 = "4.5.2";

	public static final String SWT = "org.eclipse.swt";
	public static final String SWT_PLATFORM = "org.eclipse.swt.${platform}";
	public static final String JFACE = "org.eclipse.jface";
	public static final String CORE_COMMANDS = "org.eclipse.core.commands";
	public static final String EQUINOX_COMMON = "org.eclipse.equinox.common";

	static final List<String> DEPS = Collections.unmodifiableList(Arrays.asList(
			SWT, SWT_PLATFORM, JFACE, CORE_COMMANDS, EQUINOX_COMMON));

	public String version = LUNA_SR2;

	// @formatter:off
	String updateSite() {
		String UPDATE_ROOT = "http://download.eclipse.org/eclipse/updates/";
		switch (version) {
		case LUNA_SR2:		return UPDATE_ROOT + "4.4/R-4.4.2-201502041700/";
		case MARS_SR2:		return UPDATE_ROOT + "4.5/R-4.5.2-201602121500/";
		default:
			throw Unhandled.stringException(version);
		}
	}

	String version(String type) {
		switch (version) {
		case LUNA_SR2:
			switch (type) {
			case JFACE:			return "3.10.2.v20141021-1035";
			case SWT:			return "3.103.2.v20150203-1313";
			case SWT_PLATFORM:	return "3.103.2.v20150203-1351";
			case CORE_COMMANDS:	return "3.6.100.v20140528-1422";
			case EQUINOX_COMMON:return "3.6.200.v20130402-1505";
			default:	throw Unhandled.stringException(version);
			}
		case MARS_SR2:
			switch (type) {
			case JFACE:			return "3.11.1.v20160128-1644";
			case SWT:			return "3.104.2.v20160212-1350";
			case SWT_PLATFORM:	return "3.104.2.v20160212-1350";
			case CORE_COMMANDS:	return "3.7.0.v20150422-0725";
			case EQUINOX_COMMON:return "3.7.0.v20150402-1709";
			default:	throw Unhandled.stringException(version);
			}
		default:
			throw Unhandled.stringException(version);
		}
	}
	// @formatter:on

	String fullDep(String dep) {
		if (dep.equals(SWT_PLATFORM)) {
			return "p2:org.eclipse.swt." + SwtPlatform.getRunning() + ":" + version(dep);
		} else {
			return "p2:" + dep + ":" + version(dep);
		}
	}
}
