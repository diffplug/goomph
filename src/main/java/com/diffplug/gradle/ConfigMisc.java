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
package com.diffplug.gradle;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.gradle.api.Action;

import com.diffplug.common.base.Errors;

import groovy.util.Node;
import groovy.xml.XmlUtil;

/** Utilities for creating configuration content. */
public class ConfigMisc {
	/** Creates an XML string from a groovy.util.Node. */
	public static Supplier<byte[]> xml(Supplier<Node> node) {
		return () -> {
			Node root = node.get();
			return XmlUtil.serialize(root).getBytes(StandardCharsets.UTF_8);
		};
	}

	/** Creates an XML string from a groovy.util.Node. */
	public static Supplier<byte[]> props(Action<Map<String, String>> mapPopulate) {
		return () -> {
			Map<String, String> map = new LinkedHashMap<>();
			mapPopulate.execute(map);
			return props(map);
		};
	}

	/** Creates an XML string from a groovy.util.Node. */
	public static byte[] props(Map<String, String> map) {
		Properties properties = new Properties();
		map.forEach((key, value) -> properties.put(key, value));
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		Errors.rethrow().run(() -> properties.store(output, ""));
		return output.toByteArray();
	}
}
