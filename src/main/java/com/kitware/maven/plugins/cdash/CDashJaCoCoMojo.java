package com.kitware.maven.plugins.cdash;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Mojo to upload JaCoCo reports to CDash
 *
 * @goal upload-report-jacoco
 */
public class CDashJaCoCoMojo extends CDashAbstractMojo {

  /**
   * This is the default directory for JaCoCo report to written to.
   *
   * @parameter expression="${project.reporting.outputDirectory}"
   * @readonly
   */
  private File reportingOutputDirectory;

  /**
   * This is the default filename for JaCoCo reports.
   *
   * @parameter default-value="jacoco.xml"
   */
  private String jacocoReportsFilenameRegex;

  /**
   * The directories to search for reports
   * @parameter
   */
  private File[] jacocoOutputDirectories;

  public void execute() throws MojoExecutionException {

    List<File> toUpload = findReports(getJacocoOutputDirectories(),
                                      getJacocoReportsFilenameRegex());
    upload(toUpload, "JaCoCo");
  }

  /**
   * @return The JaCoCo output directory if the Maven project has one configured,
   *         null otherwise.
   */
  private File getJacocoOutputDirectory() {

    List<Plugin> buildPlugins = mavenProject.getBuildPlugins();

    File outputDirectory = null;

    for (Plugin plugin : buildPlugins) {
      if(plugin.getArtifactId().equals("jacoco-maven-plugin")) {

        Xpp3Dom configuration = (Xpp3Dom)plugin.getConfiguration();
        Xpp3Dom reportsDirectoryNode
          = configuration.getChild("outputDirectory");

        if(reportsDirectoryNode != null) {
          outputDirectory = new File(reportsDirectoryNode.getValue());
        }
      }
    }

    return outputDirectory;
  }

  public String getJacocoReportsFilenameRegex() {
    return jacocoReportsFilenameRegex;
  }

  public void setJacocoReportsFilenameRegex(String jacocoReportsFilenameRegex) {
    this.jacocoReportsFilenameRegex = jacocoReportsFilenameRegex;
  }
  public File[] getJacocoOutputDirectories() {

    if(jacocoOutputDirectories == null) {
      // first check for surefire configuration.

      File outputDirectory = getJacocoOutputDirectory();

      // if no configuration was provided use the default
      if(outputDirectory == null) {
        outputDirectory = new File(reportingOutputDirectory, "jacoco");
      }

      jacocoOutputDirectories = new File[] { outputDirectory };
    }

    return jacocoOutputDirectories;
  }

  public void setJacocoOuputDirectories(File[] jacocoReportsDirectories) {
    this.jacocoOutputDirectories = jacocoReportsDirectories;
  }
}
