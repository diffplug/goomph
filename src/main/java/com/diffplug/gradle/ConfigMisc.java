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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import groovy.util.Node;
import groovy.xml.XmlUtil;

import com.diffplug.common.base.Errors;

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
	public static byte[] props(Map<String, String> map) {
		Properties properties = new Properties();
		map.forEach((key, value) -> properties.put(key, value));
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			Errors.rethrow().run(() -> properties.store(output, ""));
			return output.toByteArray();
		} catch (IOException e) {
			throw Errors.asRuntime(e);
		}
	}

	/** Loads a properties file and puts it into a `Map<String, String>`. */
	public static Map<String, String> loadPropertiesFile(File file) {
		Map<String, String> initial = new LinkedHashMap<>();
		Properties props = new Properties();
		try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
			props.load(input);
		} catch (IOException e) {
			throw Errors.asRuntime(e);
		}
		for (String key : props.stringPropertyNames()) {
			initial.put(key, props.getProperty(key));
		}
		return initial;
	}
}
