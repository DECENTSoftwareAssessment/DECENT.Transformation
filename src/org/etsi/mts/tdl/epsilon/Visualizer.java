package org.etsi.mts.tdl.epsilon;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

public class Visualizer {
	//TODO: figure out how to integrate this with the epsilon launcher
	public static void main(String[] args) throws Exception {
		String plantUMLLocation = args[0];
		Visualizer app = new Visualizer();
		app.visualize(plantUMLLocation, "cfa.generated");
	}

	public void visualize(String plantUMLLocation, String diagram)
			throws IOException {
		String source = FileUtils.readFileToString(new File(plantUMLLocation+"/"+diagram+".plantuml"));
		System.out.print("Loading..");
		SourceStringReader reader = new SourceStringReader(source.replaceAll("(?:[\\w\\d])-(?:[\\w\\d])", "\\."));
		System.out.println(" done!");
		
		//reader.generateImage(new File(plantUMLLocation+"/"+diagram+".png"));
		
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		// Write the first image to "os"
		System.out.print("Generating..");
		String desc = reader.generateImage(os, new FileFormatOption(FileFormat.EPS));
		os.close();
		System.out.println(" done!");
		// The XML is stored into svg 
		System.out.print("Saving..");
		FileUtils.writeByteArrayToFile(new File(plantUMLLocation+"/"+diagram+".eps"), os.toByteArray());
		System.out.println(" done!");
	}

}