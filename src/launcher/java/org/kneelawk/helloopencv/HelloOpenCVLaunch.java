package org.kneelawk.helloopencv;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.kneelawk.helloopencv.CPControl3.AndEntryFilter;
import org.kneelawk.helloopencv.CPControl3.DirectoryEntryFilter;
import org.kneelawk.helloopencv.CPControl3.LibraryExtractFromFileOperation;
import org.kneelawk.helloopencv.CPControl3.NameContainsEntryFilter;
import org.kneelawk.helloopencv.CPControl3.NativeExtractFromFileOperation;

public class HelloOpenCVLaunch {
	public static void main(String args[])
			throws IOException, ClassNotFoundException, NoSuchMethodException,
			SecurityException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {

		// initialize CPControl3 with a resources folder
		CPControl3 c = new CPControl3("org.kneelawk.helloopencv.HelloOpenCV",
				new File(CPControl3.PARENT, "resources"));

		// extract application jar(s) and opencv jar
		c.addOperation(new LibraryExtractFromFileOperation(CPControl3.ME)
				.addLibrary("application", new DirectoryEntryFilter("/app/"),
						CPControl3.ALWAYS_DELETE)
				.addLibrary("opencv",
						new AndEntryFilter(new DirectoryEntryFilter("/libs/"),
								new NameContainsEntryFilter("opencv")),
						CPControl3.ALWAYS_DELETE));

		// extract opencv natives
		c.addOperation(new NativeExtractFromFileOperation(CPControl3.ME)
				.addNative("opencv",
						new AndEntryFilter(
								new DirectoryEntryFilter("/natives/"),
								new NameContainsEntryFilter("opencv")),
						CPControl3.ALWAYS_DELETE));

		// launch the program
		c.launch(args);
	}
}
