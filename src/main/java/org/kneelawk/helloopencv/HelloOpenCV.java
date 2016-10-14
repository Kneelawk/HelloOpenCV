package org.kneelawk.helloopencv;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

public class HelloOpenCV {
	public static void main(String args[]) {
		System.out.println("Welcome to OpenCV " + Core.VERSION);
		Mat m = new Mat(5, 10, CvType.CV_8UC1, new Scalar(0));
		System.out.println("OpenCV Matrix: " + m);
		Mat r1 = m.row(1);
		r1.setTo(new Scalar(1));
		Mat c5 = m.col(5);
		c5.setTo(new Scalar(5));
		System.out.println("Matrix data: " + m.dump());
	}
}
