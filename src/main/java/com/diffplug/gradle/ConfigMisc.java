/*
 * Copyright (C) 2016-2022 DiffPlug
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
package com.diffplug.gradle;


import com.diffplug.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.gradle.api.Action;
import org.gradle.api.XmlProvider;
import org.gradle.internal.xml.XmlTransformer;

/** Utilities for creating configuration content. */
public class ConfigMisc {
	/** Creates an XML string from a groovy.util.Node. */
	public static void modifyXmlInPlace(File file, Action<XmlProvider> action) throws IOException {
		String original = Files.toString(file, StandardCharsets.UTF_8);

		XmlTransformer transformer = new XmlTransformer();
		transformer.addAction(action);
		try (OutputStream output = Files.asByteSink(file).openBufferedStream()) {
			transformer.transform(original, output);
		}
	}

	/** Creates an XML string from a groovy.util.Node. */
	public static void writeProps(Map<String, String> map, File dest) throws IOException {
		Properties properties = new Properties();
		map.forEach((key, value) -> properties.put(key, value));
		try (OutputStream output = Files.asByteSink(dest).openBufferedStream()) {
			properties.store(output, "");
		}
	}

	public static Map<String, String> loadProps(File file) throws IOException {
		Properties props = new Properties();
		try (InputStream input = Files.asByteSource(file).openBufferedStream()) {
			props.load(input);
		}
		Map<String, String> map = new LinkedHashMap<>(props.size());
		for (String key : props.stringPropertyNames()) {
			map.put(key, props.getProperty(key));
		}
		return map;
	}
}
