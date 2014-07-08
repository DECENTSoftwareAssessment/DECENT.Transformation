package epsilon.launcher;

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
		File location = new File(plantUMLLocation);
		for (File file : location.listFiles()) {
			if (file.getName().endsWith("plantuml")) {
				app.visualize(file);
			}
		}
		System.exit(0);

		app.visualize(plantUMLLocation, "cfa.generated");
	}

	private void visualize(File file) throws IOException {
		String source = FileUtils.readFileToString(file);
		SourceStringReader reader = new SourceStringReader(source);
		reader.generateImage(new File(file.getAbsolutePath()+".png"));
		
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		// Write the first image to "os"
		String desc = reader.generateImage(os, new FileFormatOption(FileFormat.EPS));
		os.close();
		// The XML is stored into svg 
		FileUtils.writeByteArrayToFile(new File(file.getAbsolutePath()+".eps"), os.toByteArray());
		
	}

	public void visualize(String plantUMLLocation, String diagram)
			throws IOException {
		String source = FileUtils.readFileToString(new File(plantUMLLocation+"/"+diagram+".plantuml"));
		System.out.println(source);
		System.out.print("Loading..");
		SourceStringReader reader = new SourceStringReader(source.replaceAll("(?:[\\w\\d])-(?:[\\w\\d])", "\\_"));
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