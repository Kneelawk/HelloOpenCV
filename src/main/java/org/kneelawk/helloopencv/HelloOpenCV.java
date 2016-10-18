package org.kneelawk.helloopencv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

public class HelloOpenCV {
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	public static void main(String args[]) {
		System.out.println("Welcome to OpenCV " + Core.VERSION);

		CascadeClassifier faceDetector;
		try {
			faceDetector = new CascadeClassifier(extract("/lbpcascade_frontalface.xml").getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		Mat image = Imgcodecs.imread("images/people.jpg");
		Mat gray = new Mat();
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

		MatOfRect detectedFaces = new MatOfRect();
		faceDetector.detectMultiScale(gray, detectedFaces);

		Rect[] array = detectedFaces.toArray();

		System.out.println("Detected faces: " + array.length);

		int n = 1;
		String out;
		while (new File(out = "images/detectedFaces" + n + "/").exists())
			n++;

		System.out.println("Writing to " + out);

		new File(out).mkdirs();

		for (int i = 0; i < array.length; i++) {
			Rect rect = array[i];
			System.out.println("Writing subimage with coords: x:" + rect.x + " - " + (rect.x + rect.width) + ", y: "
					+ rect.y + " - " + (rect.y + rect.height));
			int minx = Math.max(rect.x - rect.width / 2, 0);
			int maxx = Math.min(rect.x + rect.width + rect.width / 2, image.width());
			int miny = Math.max(rect.y - rect.height / 2, 0);
			int maxy = Math.min(rect.y + rect.height + rect.height / 2, image.height());
			Mat nimg = image.submat(miny, maxy, minx, maxx);
			Imgcodecs.imwrite(out + "face" + i + ".png", nimg);
		}

		for (Rect rect : detectedFaces.toArray()) {
			Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height),
					new Scalar(0, 255, 0), 2);
		}

		Imgcodecs.imwrite(out + "fullImage.png", image);
		Imgcodecs.imwrite(out + "fullImageGray.png", gray);
		System.out.println("Detected faces image written");
	}

	public static File extract(String cp) throws IOException {
		InputStream is = HelloOpenCV.class.getResourceAsStream(cp);

		File toDir = new File("resources/extracted/");
		if (!toDir.exists())
			toDir.mkdirs();

		File to = new File(toDir, cp.substring(cp.lastIndexOf('/') + 1));
		to.deleteOnExit();
		FileOutputStream fos = new FileOutputStream(to);
		byte[] buf = new byte[8192];
		int read;
		while ((read = is.read(buf)) >= 0) {
			fos.write(buf, 0, read);
		}
		is.close();
		fos.close();
		return to;
	}
}
