package com.diffplug.gradle.eclipserunner;


import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.List;

public class StartupClassLoader extends URLClassLoader {

	private final List<String> extensionPaths = new ArrayList<>();

	public StartupClassLoader(URL[] urls) {
		super(urls);
	}

	public StartupClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}

	public StartupClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
		super(urls, parent, factory);
	}

	@Override
	protected String findLibrary(String name) {
		if (extensionPaths.isEmpty()) {
			return super.findLibrary(name);
		}
		String libName = System.mapLibraryName(name);
		for (String extensionPath : extensionPaths) {
			File libFile = new File(extensionPath, libName);
			if (libFile.isFile())
				return libFile.getAbsolutePath();
		}
		return super.findLibrary(name);
	}

	public void addExtensionPath(String path) {
		extensionPaths.add(path);
	}

	@Override
	public void addURL(URL url) {
		super.addURL(url);
	}

	// preparing for Java 9
	protected URL findResource(String moduleName, String name) {
		return findResource(name);
	}

	// preparing for Java 9
	protected Class<?> findClass(String moduleName, String name) {
		try {
			return findClass(name);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
}
