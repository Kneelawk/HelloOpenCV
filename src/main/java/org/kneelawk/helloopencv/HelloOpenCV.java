package org.kneelawk.helloopencv;

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

		CascadeClassifier faceDetector = new CascadeClassifier(HelloOpenCV.class
				.getResource("/lbpcascade_frontalface.xml").getPath());
		Mat image = Imgcodecs.imread("images/people-02.png");

		MatOfRect detectedFaces = new MatOfRect();
		faceDetector.detectMultiScale(image, detectedFaces);

		System.out.println("Detected faces: " + detectedFaces.toArray().length);

		for (Rect rect : detectedFaces.toArray()) {
			Imgproc.rectangle(image, new Point(rect.x, rect.y),
					new Point(rect.x + rect.width, rect.y + rect.height),
					new Scalar(0, 255, 0));
		}

		Imgcodecs.imwrite("images/detectedFaces3.png", image);
		System.out.println("Detected faces image written");
	}
}
