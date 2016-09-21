package org.kneelawk.helloopencv;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * CPControl v3.0
 * Sorry about the mess. This should be an entire library or at least a package,
 * but is stuffed into one class for ease of copy-and-paste.
 */
public class CPControl3 {
	public URLClassLoader loader;
	
	public void launch(String[] args) {
		
	}
	
	public static interface DependencyOperation {
		public void perform(ClassPath cp);
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
}
