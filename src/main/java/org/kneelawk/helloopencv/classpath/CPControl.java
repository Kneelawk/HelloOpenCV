package org.kneelawk.helloopencv.classpath;

import java.io.File;
import java.io.FileInputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * CPControl v2.1
 */
public class CPControl {
	public static final File me = new File(CPControl.class
			.getProtectionDomain().getCodeSource().getLocation().getPath());
	public static final File parent = me.getParentFile();

	public static final FileFilter isMe = new FileFilter() {
		@Override
		public boolean test(File file) {
			try {
				return file.getCanonicalPath().equals(me.getCanonicalPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}
	};

	/**
	 * Supplies the file's name if the file is a native, otherwise it supplies
	 * null.
	 */
	public static final ExtractionNames nativeName = new ExtractionNames() {
		@Override
		public String getName(String t) {
			String result = t.replaceFirst("^.*\\/", "");
			String lower = result.toLowerCase();
			if (lower.endsWith(".dll") || lower.endsWith(".so")
					|| lower.endsWith(".dylib") || lower.endsWith(".jnilib"))
				return result;
			return null;
		}
	};

	public static final ExtractionNames jarName = new ExtractionNames() {
		@Override
		public String getName(String path) {
			String result = path.replaceFirst("^.*\\/", "");
			String lower = result.toLowerCase();
			if (lower.endsWith(".jar"))
				return result;
			return null;
		}
	};

	private ArrayList<DependencyOperation> operations;
	private String mainClass;

	private File libraries;
	private File natives;

	/**
	 * Construct a CPControl with this file's parent dir as the parent for the
	 * libraries and natives dirs.
	 * 
	 * @param mainClass
	 *            the main class to be loaded when this CPControl is executed.
	 */
	public CPControl(String mainClass) {
		operations = new ArrayList<DependencyOperation>();
		this.mainClass = mainClass;
		libraries = new File(parent, "libraries");
		natives = new File(parent, "natives");
	}

	/**
	 * Construct a CPControl with parent as the parent for the libraries and
	 * natives dirs.
	 * 
	 * @param mainClass
	 *            the main class to be loaded when this CPControl is executed.
	 * @param parent
	 *            the parent for the libraries and natives dirs.
	 */
	public CPControl(String mainClass, File parent) {
		operations = new ArrayList<DependencyOperation>();
		this.mainClass = mainClass;
		libraries = new File(parent, "libraries");
		natives = new File(parent, "natives");
	}

	public ExtractingNativeDirAddOperation addExtractingNativeDir() {
		ExtractingNativeDirAddOperation operation = new ExtractingNativeDirAddOperation(
				natives);
		operations.add(operation);
		return operation;
	}

	public ExtractingLibraryAddOperation addExtractingLibrary() {
		ExtractingLibraryAddOperation operation = new ExtractingLibraryAddOperation(
				libraries);
		operations.add(operation);
		return operation;
	}

	public void addNativeDir(File loc) {
		operations.add(new NativeDirAddOperation(loc));
	}

	public void addLibraryFile(File loc) {
		operations.add(new LibraryAddOperation(loc));
	}

	public void addOperation(DependencyOperation operation) {
		operations.add(operation);
	}

	public void removeOperation(DependencyOperation operation) {
		operations.remove(operation);
	}

	public void execute(String[] args) throws IOException {
		for (DependencyOperation op : operations) {
			op.preform();
		}
		try {
			Class<?> cls = Class.forName(mainClass);
			Method main = cls.getDeclaredMethod("main", String[].class);
			main.invoke(null, new Object[] { args });
		} catch (ClassNotFoundException | NoSuchMethodException
				| SecurityException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			throw new IOException("Unable to load class: " + mainClass, e);
		}
	}

	public static void extractFromDir(File baseDir, File outDir,
			ExtractionNames extractedName, ExtractionFinished finished)
			throws IOException {
		if (baseDir.isDirectory()) {
			File[] children = baseDir.listFiles();
			for (File child : children) {
				if (child.isDirectory()) {
					extractFromDir(child, outDir, extractedName, finished);
				} else {
					String newName = extractedName.getName(child
							.getAbsolutePath());
					if (newName != null && !"".equals(newName)) {
						File outFile = new File(outDir, newName);
						outFile.deleteOnExit();
						FileOutputStream fos = new FileOutputStream(outFile);
						FileInputStream fis = new FileInputStream(child);
						copy(fis, fos);
						fos.close();
						fis.close();
						if (finished != null)
							finished.accept(outFile);
					}
				}
			}
		} else {
			String newName = extractedName.getName(baseDir.getAbsolutePath());
			if (newName != null && !"".equals(newName)) {
				File outFile = new File(outDir, newName);
				outFile.deleteOnExit();
				FileOutputStream fos = new FileOutputStream(outFile);
				FileInputStream fis = new FileInputStream(baseDir);
				copy(fis, fos);
				fos.close();
				fis.close();
			}
		}
	}

	public static void extract(InputStream is, File outDir,
			ExtractionNames extractedName, ExtractionFinished finished)
			throws IOException {
		ZipInputStream zis = new ZipInputStream(is);
		ZipEntry entry;
		while ((entry = zis.getNextEntry()) != null) {
			String name = entry.getName();
			if (!entry.isDirectory()) {
				String newName = extractedName.getName(name);
				if (newName != null && !"".equals(newName)) {
					File output = new File(outDir, newName);
					output.deleteOnExit();
					FileOutputStream fos = new FileOutputStream(output);
					copy(zis, fos);
					fos.close();
					if (finished != null)
						finished.accept(output);
				}
			}
			zis.closeEntry();
		}
		zis.close();
	}

	/**
	 * Copies from an input stream to an output stream.
	 * 
	 * @param is
	 *            the input stream to copy from.
	 * @param os
	 *            the output stream to copy to.
	 * @throws IOException
	 *             if anything bad happens.
	 */
	public static void copy(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[8192];
		int read;
		while ((read = is.read(buf)) > -1) {
			os.write(buf, 0, read);
		}
	}

	/*
	 * ######## ADD NATIVES DIR ########
	 */

	public static void addNativesDir(File dir) throws IOException {
		addNativesDir(dir.getAbsolutePath());
	}

	/**
	 * Add a dir to the native search path.
	 * 
	 * @param s
	 *            the location of the dir to add.
	 * @throws IOException
	 *             if anything bad happens.
	 */
	public static void addNativesDir(String s) throws IOException {
		try {
			// This enables the java.library.path to be modified at runtime
			// From a Sun engineer at
			// http://forums.sun.com/thread.jspa?threadID=707176
			//
			Field field = ClassLoader.class.getDeclaredField("usr_paths");
			field.setAccessible(true);
			String[] paths = (String[]) field.get(null);
			for (int i = 0; i < paths.length; i++) {
				if (s.equals(paths[i])) {
					return;
				}
			}
			String[] tmp = new String[paths.length + 1];
			System.arraycopy(paths, 0, tmp, 0, paths.length);
			tmp[paths.length] = s;
			field.set(null, tmp);
			System.setProperty("java.library.path",
					System.getProperty("java.library.path")
							+ File.pathSeparator + s);
		} catch (IllegalAccessException e) {
			throw new IOException(
					"Failed to get permissions to set library path");
		} catch (NoSuchFieldException e) {
			throw new IOException(
					"Failed to get field handle to set library path");
		}
	}

	/*
	 * ######## ADD LIBRARY ########
	 */

	public static void addLibrary(File loc) throws IOException {
		addLibrary(loc.toURI().toURL());
	}

	/**
	 * Add a library to the classpath.
	 * 
	 * @param loc
	 *            the url of the library to add.
	 * @throws IOException
	 *             if anything bad happens.
	 */
	public static void addLibrary(URL loc) throws IOException {
		URLClassLoader loader = (URLClassLoader) ClassLoader
				.getSystemClassLoader();
		try {
			Method addURL = URLClassLoader.class.getDeclaredMethod("addURL",
					URL.class);
			addURL.setAccessible(true);
			addURL.invoke(loader, loc);
			System.setProperty("java.class.path",
					System.getProperty("java.class.path") + File.pathSeparator
							+ loc.getPath());
		} catch (NoSuchMethodException | SecurityException
				| IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new IOException("Unable to add library to classpath: " + loc,
					e);
		}
	}

	/*
	 * ######## INTERFACES ########
	 */

	/**
	 * Control interface for renaming extracted files.
	 * 
	 * @author Kneelawk
	 *
	 */
	public static interface ExtractionNames {
		public abstract String getName(String path);
	}

	/**
	 * Listener interface for a finished extraction.
	 * 
	 * @author Kneelawk
	 *
	 */
	public static interface ExtractionFinished {
		public abstract void accept(File file) throws IOException;
	}

	/**
	 * Generic interface for dependency oriented operations.
	 * 
	 * @author Kneelawk
	 *
	 */
	public static interface DependencyOperation {
		public abstract void preform() throws IOException;
	}

	/**
	 * Adds a library to the classpath.
	 * 
	 * @author Kneelawk
	 *
	 */
	public static class LibraryAddOperation implements DependencyOperation {
		private File libFile;

		public LibraryAddOperation(File libFile) {
			this.libFile = libFile;
		}

		@Override
		public void preform() throws IOException {
			addLibrary(libFile);
		}
	}

	/**
	 * Adds a dir to the native search path.
	 * 
	 * @author Kneelawk
	 *
	 */
	public static class NativeDirAddOperation implements DependencyOperation {
		private File nativeDir;

		public NativeDirAddOperation(File nativeDir) {
			this.nativeDir = nativeDir;
		}

		@Override
		public void preform() throws IOException {
			addNativesDir(nativeDir);
		}
	}

	/**
	 * Extracts libraries from jars on the classpath and adds them to the
	 * classpath.
	 * 
	 * @author Kneelawk
	 *
	 */
	public static class ExtractingLibraryAddOperation implements
			DependencyOperation {
		private ArrayList<LibrarySpecial> specials = new ArrayList<LibrarySpecial>();

		private File libraries;

		public ExtractingLibraryAddOperation(File libraries) {
			this.libraries = libraries;
		}

		public ExtractingLibraryAddOperation addLibraries(
				String libraryDirName, FileFilter archiveFilter,
				StringFilter libraryFilter) {
			specials.add(new LibrarySpecial(archiveFilter, libraryFilter,
					libraryDirName));
			return this;
		}

		public ExtractingLibraryAddOperation addLibraries(LibrarySpecial special) {
			specials.add(special);
			return this;
		}

		public boolean removeLibraries(LibrarySpecial special) {
			return specials.remove(special);
		}

		@Override
		public void preform() throws IOException {
			String classpath = System.getProperty("java.class.path");
			String[] paths = classpath.split(File.pathSeparator);
			for (String path : paths) {
				File library = new File(path);
				for (final LibrarySpecial special : specials) {
					if (special.getArchiveFilter().test(library)) {
						File libraryDir = new File(libraries,
								special.getLibraryDirName());
						if (!libraryDir.exists())
							libraryDir.mkdirs();
						if (library.isDirectory()) {
							extractFromDir(library, libraryDir,
									new ExtractionNames() {
										@Override
										public String getName(String path) {
											String result = jarName
													.getName(path);
											return result != null
													&& !"".equals(result) ? (special
													.getLibraryFilter().test(
															path) ? result
													: null) : null;
										}
									}, new ExtractionFinished() {
										@Override
										public void accept(File file)
												throws IOException {
											addLibrary(file);
										}
									});
						} else {
							extract(new FileInputStream(library), libraryDir,
									new ExtractionNames() {
										@Override
										public String getName(String path) {
											String result = jarName
													.getName(path);
											return result != null
													&& !"".equals(result) ? (special
													.getLibraryFilter().test(
															path) ? result
													: null) : null;
										}
									}, new ExtractionFinished() {
										@Override
										public void accept(File file)
												throws IOException {
											addLibrary(file);
										}
									});
						}
					}
				}
			}
		}

		public static class LibrarySpecial {
			private FileFilter archiveFilter;
			private StringFilter libraryFilter;
			private String libraryDirName;

			public LibrarySpecial(FileFilter archiveFilter,
					StringFilter libraryFilter, String libraryDirName) {
				this.archiveFilter = archiveFilter;
				this.libraryFilter = libraryFilter;
				this.libraryDirName = libraryDirName;
			}

			public FileFilter getArchiveFilter() {
				return archiveFilter;
			}

			public StringFilter getLibraryFilter() {
				return libraryFilter;
			}

			public String getLibraryDirName() {
				return libraryDirName;
			}
		}
	}

	/**
	 * Extracts natives from the jars on the classpath to a dir that it adds to
	 * the native search path.
	 * 
	 * @author Kneelawk
	 *
	 */
	public static class ExtractingNativeDirAddOperation implements
			DependencyOperation {
		private ArrayList<NativeSpecial> specials = new ArrayList<NativeSpecial>();

		private File natives;

		public ExtractingNativeDirAddOperation(File natives) {
			this.natives = natives;
		}

		public ExtractingNativeDirAddOperation addNatives(String nativeDirName,
				FileFilter archiveFilter, StringFilter nativeFilter) {
			specials.add(new NativeSpecial(archiveFilter, nativeFilter,
					nativeDirName));
			return this;
		}

		public ExtractingNativeDirAddOperation addNatives(NativeSpecial special) {
			specials.add(special);
			return this;
		}

		public boolean removeNatives(NativeSpecial special) {
			return specials.remove(special);
		}

		@Override
		public void preform() throws IOException {
			String classpath = System.getProperty("java.class.path");
			String[] paths = classpath.split(File.pathSeparator);
			for (String path : paths) {
				File library = new File(path);
				for (final NativeSpecial special : specials) {
					if (special.getArchiveFilter().test(library)) {
						File nativesDir = new File(natives,
								special.getNativeDirName());
						if (!nativesDir.exists())
							nativesDir.mkdirs();
						if (library.isDirectory()) {
							extractFromDir(library, nativesDir,
									new ExtractionNames() {
										@Override
										public String getName(String path) {
											String result = nativeName
													.getName(path);
											return result != null
													&& !"".equals(result) ? (special
													.getNativeFilter().test(
															path) ? result
													: null) : null;
										}
									}, null);
						} else {
							extract(new FileInputStream(library), nativesDir,
									new ExtractionNames() {
										@Override
										public String getName(String path) {
											String result = nativeName
													.getName(path);
											return result != null
													&& !"".equals(result) ? (special
													.getNativeFilter().test(
															path) ? result
													: null) : null;
										}
									}, null);
						}
						addNativesDir(nativesDir);
					}
				}
			}
		}

		public static class NativeSpecial {
			private FileFilter archiveFilter;
			private StringFilter nativeFilter;
			private String nativeDirName;

			public NativeSpecial(FileFilter archiveFilter,
					StringFilter nativeFilter, String nativeDirName) {
				this.archiveFilter = archiveFilter;
				this.nativeFilter = nativeFilter;
				this.nativeDirName = nativeDirName;
			}

			public FileFilter getArchiveFilter() {
				return archiveFilter;
			}

			public StringFilter getNativeFilter() {
				return nativeFilter;
			}

			public String getNativeDirName() {
				return nativeDirName;
			}
		}
	}

	/**
	 * Used instead of Predacate&lt;File&gt; to retain Java 7 compatibility.
	 * 
	 * @author Kneelawk
	 *
	 */
	public static interface FileFilter {
		public abstract boolean test(File file);
	}

	/**
	 * Used instead of Predacate&lt;String&gt; to retain Java 7 compatibility.
	 * 
	 * @author Kneelawk
	 *
	 */
	public static interface StringFilter {
		public abstract boolean test(String str);
	}
}