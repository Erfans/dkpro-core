	package de.tudarmstadt.ukp.dkpro.core.flextag.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

public class InstallAsMavenArtifact {
	
	static final String groupId = "de.tudarmstadt.ukp.dkpro.core";
	static final String artifactId = "de.tudarmstadt.ukp.dkpro.core.flextag";
	static final String tool = "tagger";

	public static void main(String[] args) throws IOException {

		String modelLocation = "/path/to/your/trained/model";

		
		String version = "20150720";
		String language = "en";
		String variant = "XYZ";

		String targetName = language + "-" + variant;
		String workingDirectory = Files.createTempDirectory("installModel",
				new FileAttribute<?>[] {}).toAbsolutePath().toString();
		
		String preparedScript = prepareBuildScript(modelLocation, groupId, artifactId, version, tool, language, variant, targetName, workingDirectory);
		
		File script = new File(workingDirectory+"/build.xml");
		FileUtils.writeStringToFile(script, preparedScript);

		runAntToInstall(script);

	}

	private static String prepareBuildScript(String modelLocation,
			String groupId, String artifactId, String version, String tool,
			String language, String variant, String targetName,
			String workingDirectory) throws IOException {
		
		String buildFile = FileUtils.readFileToString(new File("src/main/resources/build.xml"));
		buildFile = buildFile.replaceAll("#TARGETNAME#", targetName);
		buildFile = buildFile.replaceAll("#MODELLOCATION#", modelLocation);
		buildFile = buildFile.replaceAll("#GROUPID#", groupId);
		buildFile = buildFile.replaceAll("#ARTIFACTID#", artifactId);
		buildFile = buildFile.replaceAll("#DATE#", version);
		buildFile = buildFile.replaceAll("#VERSION#", "1");
		buildFile = buildFile.replaceAll("#TOOL#", tool);
		buildFile = buildFile.replaceAll("#LANGUAGE#", language);
		buildFile = buildFile.replaceAll("#VARIANT#", variant);
		return buildFile;
	}

	private static void runAntToInstall(File script) {
		DefaultLogger consoleLogger = getConsoleLogger();
		Project p = new Project();
		p.setUserProperty("ant.file", script.getAbsolutePath());
		p.addBuildListener(consoleLogger);
		p.init();
		ProjectHelper helper = ProjectHelper.getProjectHelper();
		p.addReference("ant.projectHelper", helper);
		helper.parse(p, script);

		// p.executeTarget("separate-jars");
		p.executeTarget("local-maven");
	}

	private static DefaultLogger getConsoleLogger() {
		DefaultLogger consoleLogger = new DefaultLogger();
		consoleLogger.setErrorPrintStream(System.err);
		consoleLogger.setOutputPrintStream(System.out);
		consoleLogger.setMessageOutputLevel(Project.MSG_INFO);

		return consoleLogger;
	}

}
