package org.kneelawk.helloopencv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * CPControl v3.0 Sorry about the mess. This should be an entire library or at
 * least a package, but is stuffed into one class for ease of copy-and-paste.
 */
public class CPControl3 {
	protected String mainClassName;

	protected List<DependencyOperation> operations = new ArrayList<>();

	protected URLClassLoader loader;

	public CPControl3(String mainClassName) {
		this.mainClassName = mainClassName;

		Thread hook = new Thread(new Runnable() {
			@Override
			public void run() {
				if (loader != null) {
					try {
						loader.close();
					} catch (IOException e) {
						System.err.println("Error closing class loader");
						e.printStackTrace();
					}
				}
			}
		});

		Runtime.getRuntime().addShutdownHook(hook);
	}

	public void addOperation(DependencyOperation operation) {
		operations.add(operation);
	}

	public void launch(String[] args) throws IOException {
		ClassPath path = new ClassPath();

		for (DependencyOperation operation : operations) {
			operation.perform(path);
		}

		for (String dir : path.nativeDirs) {
			addNativesDir(dir);
		}

		URL[] urls = copyFilesToClassPath(path.classpath);
		loader = new URLClassLoader(urls);

		try {
			Class<?> mainClass = loader.loadClass(mainClassName);
			Method mainMethod = mainClass.getMethod("main", String[].class);
			mainMethod.invoke(null, new Object[] {
					args
			});
		} catch (ClassNotFoundException e) {
			throw new IOException("Unable to load main class", e);
		} catch (NoSuchMethodException e) {
			throw new IOException("Unable to load main method", e);
		} catch (SecurityException e) {
			throw new IOException("Unable to start main class", e);
		} catch (IllegalAccessException e) {
			throw new IOException("Unable to invoke main method", e);
		} catch (IllegalArgumentException e) {
			throw new IOException("Unable to invoke main method", e);
		} catch (InvocationTargetException e) {
			throw new IOException("Unable to invoke main method", e);
		}
	}

	public static final File ME = new File(CPControl3.class
			.getProtectionDomain().getCodeSource().getLocation().getPath());
	public static final File PARENT = ME.getParentFile();

	public static final FileFilter IS_JAR_FILE = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return pathname.getName().toLowerCase().endsWith(".jar");
		}
	};

	public static final FileFilter IS_NATIVE_FILE = new FileFilter() {
		@Override
		public boolean accept(File file) {
			String name = file.getName().toLowerCase();
			return name.endsWith(".so") || name.endsWith(".dll")
					|| name.endsWith(".jnilib") || name.endsWith(".dynlib");
		}
	};

	public static final FileFilter IS_ME = new FileFilter() {
		@Override
		public boolean accept(File file) throws IOException {
			return file.getCanonicalPath().equals(ME.getCanonicalPath());
		}
	};

	public static final EntryFilter IS_JAR_ENTRY = new EntryFilter() {
		@Override
		public boolean accept(String path) throws IOException {
			return path.toLowerCase().endsWith(".jar");
		}
	};

	public static final EntryFilter IS_NATIVE_ENTRY = new EntryFilter() {
		@Override
		public boolean accept(String path) throws IOException {
			String lower = path.toLowerCase();
			return lower.endsWith(".so") || lower.endsWith(".dll")
					|| lower.endsWith(".jnilib") || lower.endsWith(".dynlib");
		}
	};

	public static interface DependencyOperation {
		public void perform(ClassPath cp) throws IOException;
	}

	public static class LibraryAddOperation implements DependencyOperation {
		private File libFile;

		public LibraryAddOperation(File libFile) {
			this.libFile = libFile;
		}

		@Override
		public void perform(ClassPath cp) throws IOException {
			cp.addLibrary(libFile);
		}
	}

	public static class NativeAddOperation implements DependencyOperation {
		private File nativeDir;

		public NativeAddOperation(File nativeDir) {
			this.nativeDir = nativeDir;
		}

		@Override
		public void perform(ClassPath cp) throws IOException {
			cp.addNativeDir(nativeDir.getCanonicalPath());
		}
	}

	public static class LibraryExtractOperation implements DependencyOperation {

		@Override
		public void perform(ClassPath cp) throws IOException {
			// TODO: add more DependencyOperations
		}
	}

	public static class ClassPath {
		public Set<File> classpath = new HashSet<>();
		public Set<String> nativeDirs = new HashSet<>();

		public void addLibrary(File lib) {
			classpath.add(lib);
		}

		public void addLibraries(Collection<File> libs) {
			classpath.addAll(libs);
		}

		public void addNativeDir(String dir) {
			nativeDirs.add(dir);
		}

		public void addNativeDirs(Collection<String> dirs) {
			nativeDirs.addAll(dirs);
		}
	}

	public static interface FileFilter {
		public boolean accept(File file) throws IOException;
	}

	public static class AndFileFilter implements FileFilter {
		private FileFilter[] filters;

		public AndFileFilter(FileFilter... filters) {
			this.filters = filters;
		}

		@Override
		public boolean accept(File file) throws IOException {
			for (FileFilter filter : filters) {
				if (!filter.accept(file))
					return false;
			}
			return true;
		}
	}

	public static class NameContainsFileFilter implements FileFilter {
		private String contents;

		public NameContainsFileFilter(String contents) {
			this.contents = contents.toLowerCase();
		}

		@Override
		public boolean accept(File file) throws IOException {
			String name = file.getName().toLowerCase();
			return name.contains(contents);
		}
	}

	public static interface EntryFilter {
		public boolean accept(String path) throws IOException;
	}

	public static class AndEntryFilter implements EntryFilter {
		private EntryFilter[] filters;

		public AndEntryFilter(EntryFilter... filters) {
			this.filters = filters;
		}

		@Override
		public boolean accept(String path) throws IOException {
			for (EntryFilter filter : filters) {
				if (!filter.accept(path))
					return false;
			}
			return true;
		}
	}

	public static class NameContainsEntryFilter implements EntryFilter {
		private String contents;

		public NameContainsEntryFilter(String contents) {
			this.contents = contents.toLowerCase();
		}

		@Override
		public boolean accept(String path) {
			String name = getPathName(path).toLowerCase();
			return name.contains(contents);
		}
	}

	public static class DirectoryEntryFilter implements EntryFilter {
		private String dir;

		public DirectoryEntryFilter(String dir) {
			if (!dir.startsWith("/")) {
				dir = "/" + dir;
			}

			this.dir = dir;
		}

		@Override
		public boolean accept(String path) throws IOException {
			if (!path.startsWith("/"))
				path = "/" + path;
			return path.startsWith(dir);
		}
	}

	public static interface DestinationProvider {
		public File getFile(String path);
	}

	public static class DirectoryDestinationProvider
			implements DestinationProvider {
		private File parent;

		public DirectoryDestinationProvider(File parent) {
			this.parent = parent;
		}

		@Override
		public File getFile(String path) {
			return new File(parent, getPathName(path));
		}
	}

	public static class FlatDestinationProvider implements DestinationProvider {
		private File parent;

		public FlatDestinationProvider(File parent) {
			this.parent = parent;
		}

		@Override
		public File getFile(String path) {
			return new File(parent, path);
		}
	}

	public static String getPathName(String path) {
		return path.substring(path.lastIndexOf('/') + 1);
	}

	public static String[] getClassPath() {
		String classPath = System.getProperty("sun.boot.class.path")
				+ File.pathSeparator + System.getProperty("java.ext.path")
				+ File.pathSeparator + System.getProperty("java.class.path");
		return classPath.split(File.pathSeparator);
	}

	public static Set<File> findLibrariesOnClasspath() throws IOException {
		Set<File> found = new HashSet<>();

		String[] classPath = getClassPath();
		for (String path : classPath) {
			File file = new File(path);
			if (file.exists())
				recursiveSearch(found, new HashSet<>(), file, IS_JAR_FILE);
		}

		return found;
	}

	private static void recursiveSearch(Collection<File> found,
			Set<File> searched, File dir, FileFilter filter)
			throws IOException {
		if (searched.contains(dir))
			return;
		if (dir.isDirectory()) {
			searched.add(dir);
			File[] children = dir.listFiles();
			for (File child : children) {
				if (child.isDirectory()) {
					if (searched.contains(child))
						continue;
					recursiveSearch(found, searched, child, filter);
				} else if (filter.accept(child)) {
					found.add(child);
				}
			}
		} else {
			if (filter.accept(dir)) {
				found.add(dir);
			}
		}
	}

	public static Set<File> filterFiles(Collection<File> files,
			FileFilter filter) throws IOException {
		Set<File> filteredFiles = new HashSet<>();

		for (File inputFile : files) {
			if (filter.accept(inputFile)) {
				filteredFiles.add(inputFile);
			}
		}

		return filteredFiles;
	}

	public static URL[] copyFilesToClassPath(Collection<File> files)
			throws MalformedURLException {
		int size = files.size();
		URL[] urls = new URL[size];

		Iterator<File> it = files.iterator();
		for (int i = 0; i < size; i++) {
			File file = it.next();
			urls[i] = file.toURI().toURL();
		}

		return urls;
	}

	public static Set<File> extractFilesMatching(Collection<File> archives,
			EntryFilter filter, DestinationProvider destinations)
			throws IOException {
		Set<File> extractedFiles = new HashSet<>();
		for (File archive : archives) {
			extractedFiles.addAll(
					extractFilesMatching(archive, filter, destinations));
		}
		return extractedFiles;
	}

	public static Set<File> extractFilesMatching(File archive,
			EntryFilter filter, DestinationProvider destinations)
			throws IOException {
		Set<File> extractedFiles = new HashSet<>();

		ZipInputStream zis = new ZipInputStream(new FileInputStream(archive));
		ZipEntry entry;

		while ((entry = zis.getNextEntry()) != null) {
			String path = entry.getName();
			File dest;
			if (!entry.isDirectory() && filter.accept(path)
					&& (dest = destinations.getFile(path)) != null) {
				// make sure parent dirs exist
				File parent = dest.getParentFile();
				if (!parent.exists())
					parent.mkdirs();

				// copy the file
				FileOutputStream fos = new FileOutputStream(dest);
				copy(zis, fos);
				fos.close();

				// keep track of where we put the files
				extractedFiles.add(dest);
			}
			zis.closeEntry();
		}

		zis.close();

		return extractedFiles;
	}

	public static void extractFileFromSystemClasspath(String path, File to)
			throws IOException {
		extractFileFromSystemClasspath(CPControl3.class, path, to);
	}

	public static void extractFileFromSystemClasspath(Class<?> relative,
			String path, File to) throws IOException {
		InputStream is = relative.getResourceAsStream(path);
		if (is == null)
			throw new IOException("File: " + path + " not found on classapth");
		FileOutputStream fos = new FileOutputStream(to);
		copy(is, fos);
	}

	public static void copy(InputStream is, OutputStream os)
			throws IOException {
		byte[] buf = new byte[8192];
		int read;
		while ((read = is.read(buf)) >= 0) {
			os.write(buf, 0, read);
		}
	}

	public static void addNativesDir(String dirName) throws IOException {
		try {
			// This enables the java.library.path to be modified at runtime
			// From a Sun engineer at
			// http://forums.sun.com/thread.jspa?threadID=707176
			//
			Field field = ClassLoader.class.getDeclaredField("usr_paths");
			field.setAccessible(true);
			String[] paths = (String[]) field.get(null);
			for (int i = 0; i < paths.length; i++) {
				if (dirName.equals(paths[i])) {
					return;
				}
			}
			String[] tmp = new String[paths.length + 1];
			System.arraycopy(paths, 0, tmp, 0, paths.length);
			tmp[paths.length] = dirName;
			field.set(null, tmp);
			System.setProperty("java.library.path",
					System.getProperty("java.library.path") + File.pathSeparator
							+ dirName);
		} catch (IllegalAccessException e) {
			throw new IOException(
					"Failed to get permissions to set library path");
		} catch (NoSuchFieldException e) {
			throw new IOException(
					"Failed to get field handle to set library path");
		}
	}
}
