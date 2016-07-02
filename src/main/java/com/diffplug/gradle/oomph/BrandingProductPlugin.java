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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.imageio.ImageIO;

import com.diffplug.common.base.Preconditions;
import com.diffplug.common.collect.ImmutableList;
import com.diffplug.common.io.ByteSource;
import com.diffplug.common.io.Resources;
import com.diffplug.gradle.FileMisc;

/**
 * Creates a product branding plugin which sets
 * icons, splash screen, etc.
 * 
 * Creates a bundle named "com.diffplug.goomph.branding",
 * whose contents come from the template.  The text files
 * come from the resources directory, and the images come
 * from the arguments passed by the user.
 */
class BrandingProductPlugin {
	/** Returns the Goomph icon image. */
	public static BufferedImage getGoomphIcon() throws IOException {
		return readImg("goomph_icon.png");
	}

	/** Returns the Goomph splash image. */
	public static BufferedImage getGoomphSplash() throws IOException {
		return readImg("goomph_splash.png");
	}

	private static BufferedImage readImg(String path) throws IOException {
		ByteSource source = Resources.asByteSource(BrandingProductPlugin.class.getResource(path));
		try (InputStream stream = source.openBufferedStream()) {
			return ImageIO.read(stream);
		}
	}

	static {
		/** Template files. */
		ImmutableList.Builder<String> builder = ImmutableList.builder();
		builder.add("META-INF/MANIFEST.MF");
		builder.add("LegacyIDE.e4xmi");
		builder.add("plugin.properties");
		builder.add("plugin.xml");
		builder.add("plugin_customization.ini");
		builder.add("plugin_customization.properties");
		builder.add("eclipse256.png");
		builder.add("eclipse128.png");
		builder.add("eclipse64.png");
		builder.add("eclipse48.png");
		builder.add("eclipse32.png");
		builder.add("eclipse16.png");
		builder.add("splash.bmp");
		template = builder.build();
	}

	static final ImmutableList<String> template;

	/**
	 * Writes out an eclipse product to the given root project.
	 *
	 * The map is used to modify the template files.
	 */
	public static void create(File root, BufferedImage splash, BufferedImage icon, Map<String, Function<String, String>> map) throws IOException {
		FileMisc.cleanDir(root);
		Objects.requireNonNull(splash);
		icon = makeSquare(icon);
		for (String subpath : template) {
			if (subpath.equals("splash.bmp")) {
				writeImage(new File(root, subpath), splash, RESOLUTION_UNCHANGED, "BMP");
			} else if (subpath.startsWith(eclipse) && subpath.endsWith(dot_png)) {
				String number = subpath.substring(eclipse.length(), subpath.length() - dot_png.length());
				int resolution = Integer.parseInt(number);
				writeImage(new File(root, subpath), icon, resolution, "PNG");
			} else {
				String templateValue = Resources.toString(BrandingProductPlugin.class.getResource(subpath), StandardCharsets.UTF_8);
				Function<String, String> function = map.getOrDefault(subpath, Function.identity());
				String content = function.apply(templateValue);
				Path path = root.toPath().resolve(subpath);
				Files.createDirectories(path.getParent());
				Files.write(path, content.getBytes(StandardCharsets.UTF_8));
			}
		}
	}

	private static final String eclipse = "eclipse";
	private static final String dot_png = ".png";

	/** Returns an image where the input image is centered. */
	private static BufferedImage makeSquare(BufferedImage input) {
		if (input.getHeight() == input.getWidth()) {
			return input;
		} else {
			int max = Math.max(input.getHeight(), input.getWidth());
			int dx = (input.getWidth() - max) / 2;
			int dy = (input.getHeight() - max) / 2;
			return createImg(max, max, input.getType(), graphics -> {
				graphics.drawImage(input, dx, dy, null);
			});
		}
	}

	private static void writeImage(File target, BufferedImage src, int resolution, String format) throws IOException {
		BufferedImage resultImage;
		if (resolution == RESOLUTION_UNCHANGED) {
			resultImage = src;
		} else {
			resultImage = createImg(resolution, resolution, src.getType(), creator -> {
				Image image = src.getScaledInstance(resolution, resolution, Image.SCALE_SMOOTH);
				creator.drawImage(image, 0, 0, null);
			});
		}
		boolean wrote = ImageIO.write(resultImage, format, target);
		if (!wrote) {
			BufferedImage otherFormat = createImg(resultImage.getWidth(), resultImage.getHeight(), BufferedImage.TYPE_INT_RGB, graphics -> {
				graphics.drawImage(resultImage, 0, 0, null);
			});
			Preconditions.checkArgument(ImageIO.write(otherFormat, format, target), "Couldn't write %s", target);
		}
	}

	static final int RESOLUTION_UNCHANGED = -1;

	private static BufferedImage createImg(int width, int height, int type, Consumer<Graphics2D> creator) {
		BufferedImage image = new BufferedImage(width, height, type);
		Graphics2D graphics = image.createGraphics();
		creator.accept(graphics);
		graphics.dispose();
		return image;
	}
}
