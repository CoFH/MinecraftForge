/*
 * Minecraft Forge
 * Copyright (c) 2016-2018.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.userdev;

import com.google.common.collect.Sets;
import net.minecraftforge.fml.loading.moddiscovery.IModLocator;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.minecraftforge.fml.Logging.SCAN;

public class ClasspathLocator implements IModLocator
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String COREMODS = "META-INF/coremods.json";
    private static final String MODS = "META-INF/mods.toml";

    public ClasspathLocator() {}

    private Path urlToPath(URL url)
    {
        try
        {
            if (url.getProtocol().equals("jar"))
            {
                String text = url.toString();
                int i = text.indexOf("!/");
                String jarFile = text.substring(0, i);
                String fileInJar = text.substring(i + 1);

                try (FileSystem jar = FileSystems.getFileSystem(new URI(jarFile)))
                {
                    return jar.getPath(fileInJar);
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
            else
            {
                return Paths.get(url.toURI());
            }
        }
        catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ModFile> scanMods() {
        Set<URL> modUrls = Sets.newHashSet();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try
        {
            modUrls.addAll(Collections.list(ClassLoader.getSystemResources(COREMODS)));
            modUrls.addAll(Collections.list(loader.getResources(COREMODS)));
            modUrls.addAll(Collections.list(ClassLoader.getSystemResources(MODS)));
            modUrls.addAll(Collections.list(loader.getResources(MODS)));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return modUrls.stream().map((url) -> {
            try
            {
                // We got URLs including "META-INF/<something>", so get two components up.
                return urlToPath(url).getParent().getParent();
            }
            catch (RuntimeException e)
            {
                e.printStackTrace();
            }
            return null;
        }).filter(Objects::nonNull).distinct()
                .map(path -> new ModFile(path, this))
                .collect(Collectors.toList());
    }

    @Override
    public String name() {
        return "classpath mods";
    }

    @Override
    public Path findPath(final ModFile modFile, final String... path) {
        if (path.length < 1) {
            throw new IllegalArgumentException("Missing path");
        }

        Path filePath = modFile.getFilePath();
        Path classesRoot = getClassesPath(filePath);
        String[] tail = Arrays.copyOfRange(path, 1, path.length);
        Path classPath = classesRoot.resolve(classesRoot.getFileSystem().getPath(path[0], tail));
        if(Files.exists(classPath))
        {
            return classPath;
        }
        return filePath.resolve(filePath.getFileSystem().getPath(path[0], tail));
    }

    @Override
    public void scanFile(final ModFile modFile, final Consumer<Path> pathConsumer) {
        LOGGER.debug(SCAN,"Scanning classpath");

        Path filePath = modFile.getFilePath();
        Path scanPath = getClassesPath(filePath);

        try (Stream<Path> files = Files.find(scanPath, Integer.MAX_VALUE, (p, a) -> p.getNameCount() > 0 && p.getFileName().toString().endsWith(".class"))) {
            files.forEach(pathConsumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.debug(SCAN,"Classpath scan complete");
    }

    private Path getClassesPath(Path filePath)
    {
        Path classesPath = filePath;

        // Hack 1: When running from within intellij, we get
        // "out/production/resources" + "out/production/classes"
        if(filePath.getNameCount() >= 1 && filePath.getName(filePath.getNameCount()-1).toString().equals("resources"))
        {
            classesPath = filePath.getParent().resolve("classes");
        }
        // Hack 2: When running from gradle, we get
        // "build/resources/<sourceset>" + "build/classes/<language>/<sourceset>"
        else if(filePath.getNameCount() >= 2 && filePath.getName(filePath.getNameCount()-2).toString().equals("resources"))
        {
            // We'll scan all the subdirectories for languages and sourcesets, hopefully that works...
            classesPath = filePath.getParent().getParent().resolve("classes");
        }
        return classesPath;
    }

    @Override
    public String toString()
    {
        return "{Classpath locator}";
    }

    @Override
    public Optional<Manifest> findManifest(Path file)
    {
        return Optional.empty();
    }
}
