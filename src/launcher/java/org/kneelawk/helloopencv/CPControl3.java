package org.kneelawk.helloopencv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CPControl v3.0 Sorry about the mess. This should be an entire library or at
 * least a package, but is stuffed into one class for ease of copy-and-paste.
 */
public class CPControl3 {
	protected String mainClassName;

	protected List<DependencyOperation> operations =
			new ArrayList<DependencyOperation>();

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

		// do some stuff to path

		for (String dir : path.nativeDirs) {
			addNativesDir(dir);
		}

		URL[] urls = path.classpath.toArray(new URL[path.classpath.size()]);
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
			cp.addLibrary(libFile.toURI().toURL());
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

	public static class ClassPath {
		public Set<URL> classpath = new HashSet<URL>();
		public Set<String> nativeDirs = new HashSet<String>();

		public void addLibrary(URL lib) {
			classpath.add(lib);
		}

		public void addLibraries(Collection<URL> libs) {
			classpath.addAll(libs);
		}

		public void addNativeDir(String dir) {
			nativeDirs.add(dir);
		}

		public void addNativeDirs(Collection<String> dirs) {
			nativeDirs.addAll(dirs);
		}
	}
	
	public static void extractFileFromSystemClasspath(String path, File to) throws IOException {
		extractFileFromSystemClasspath(CPControl3.class, path, to);
	}

	public static void extractFileFromSystemClasspath(Class<?> relative, String path, File to)
			throws IOException {
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
