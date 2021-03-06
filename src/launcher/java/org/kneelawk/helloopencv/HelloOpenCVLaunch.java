package org.kneelawk.helloopencv;

import java.io.File;
import java.io.IOException;

import org.kneelawk.helloopencv.CPControl3.AndEntryFilter;
import org.kneelawk.helloopencv.CPControl3.DirectoryEntryFilter;
import org.kneelawk.helloopencv.CPControl3.NameContainsEntryFilter;

public class HelloOpenCVLaunch {
	public static void main(String args[]) {

		// initialize CPControl3 with a resources folder
		CPControl3 c = new CPControl3("org.kneelawk.helloopencv.HelloOpenCV",
				new File(CPControl3.PARENT, "resources"));

		// extract application jar(s) and opencv jar
		c.addExtractingFromFileLibrary(CPControl3.ME)
				.addLibrary("application", new DirectoryEntryFilter("/app/"),
						CPControl3.ALWAYS_DELETE)
				.addLibrary("opencv",
						new AndEntryFilter(new DirectoryEntryFilter("/libs/"),
								new NameContainsEntryFilter("opencv")),
						CPControl3.ALWAYS_DELETE);

		// extract opencv natives
		c.addExtractingFromFileNativeDir(CPControl3.ME).addNative("opencv",
				new AndEntryFilter(new DirectoryEntryFilter("/natives/"),
						new NameContainsEntryFilter("opencv")),
				CPControl3.ALWAYS_DELETE);

		// launch the program
		try {
			c.launch(args);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
