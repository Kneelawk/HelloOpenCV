package org.kneelawk.helloopencv;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class HelloOpenCVLaunch {
	public static void main(String args[])
			throws IOException, ClassNotFoundException, NoSuchMethodException,
			SecurityException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		// CPControl c = new CPControl("org.kneelawk.helloopencv.HelloOpenCV");
		// c.addExtractingNativeDir().addNatives("opencv", CPControl.isMe,
		// str -> str.toLowerCase().contains("opencv"));
		// c.addExtractingLibrary().addLibraries("opencv", CPControl.isMe,
		// str -> str.toLowerCase().contains("opencv"));
		//
		// try {
		// c.execute(args);
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		URLClassLoader loader = null;
		try {
			File app = new File("app.jar");
			try {
				CPControl3.extractFileFromSystemClasspath(
						"/app/HelloOpenCV-0.0.1.jar", app);
			} catch (IOException e) {
				e.printStackTrace();
			}
			File opencv = new File("opencv.jar");
			try {
				CPControl3.extractFileFromSystemClasspath(
						"/libs/opencv-310.jar", opencv);
			} catch (IOException e) {
				e.printStackTrace();
			}
			File natives = new File("natives");
			if (!natives.exists())
				natives.mkdirs();
			try {
				CPControl3.extractFileFromSystemClasspath(
						"/natives/libopencv_java310.so",
						new File(natives, "libopencv_java310.so"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			CPControl3.addNativesDir(natives.getCanonicalPath());
			loader = new URLClassLoader(new URL[] {
					app.toURI().toURL(), opencv.toURI().toURL()
			});
			Class<?> c =
					loader.loadClass("org.kneelawk.helloopencv.HelloOpenCV");
			Method m = c.getMethod("main", String[].class);
			m.invoke(null, new Object[] {
					args
			});
		} finally {
			if (loader != null)
				loader.close();
		}
	}
}
