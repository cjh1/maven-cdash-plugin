package com.kitware.maven.plugins.cdash;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * @goal upload-report-jacoco
 */
public class CDashJaCoCoMojo extends CDashAbstractMojo {

  /**
   * @parameter expression="${project.reporting.outputDirectory}"
   * @readonly
   */
  private File reportingOutputDirectory;

  /**
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
    upload(toUpload);

  }

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

  protected InputStream addSite(InputStream xmlStream) throws IOException {
    String siteTag = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Site BuildName=\"Linux-c++\" " +
                           "BuildStamp=\"20121211-0710-Nightly\" " +
        "Name=\"ulmus\" " +
        "Generator=\"maven-cdash-plugin\" " +
        "CompilerName=\"/usr/bin/c++\" " +
        "OSName=\"Linux\" " +
        "Hostname=\"ulmus\" " +
        "OSRelease=\"3.2.0-33-generic\" " +
        "OSVersion=\"#52-Ubuntu SMP Thu Oct 18 16:29:15 UTC 2012\" " +
        "OSPlatform=\"x86_64\" " +
        "Is64Bits=\"1\" " +
        "VendorString=\"\" " +
        "VendorID=\"\" " +
        "FamilyID=\"\" " +
        "ModelID=\"\" " +
        "ProcessorCacheSize=\"12288\" " +
        "NumberOfLogicalCPU=\"12\" " +
        "NumberOfPhysicalCPU=\"\" " +
        "TotalVirtualMemory=\"\" " +
        "TotalPhysicalMemory=\"24109\" " +
        "LogicalProcessorsPerPhysical=\"\" " +
        "ProcessorClockFrequency=\"\" >%s</Site>";

    String compilerName = System.getProperty("java.compiler");
    String osName = System.getProperty("os.name");
    String hostname = InetAddress.getLocalHost().getHostName();
    String osVersion = System.getProperty("os.version");
    String osPlatform = System.getProperty("os.arch");
    int numberLogicalCPU = Runtime.getRuntime().availableProcessors();
    long totalPhysicalMemory = Runtime.getRuntime().totalMemory();


    StringBuilder response = new StringBuilder();
    InputStreamReader isr = new InputStreamReader(xmlStream);
    char[] chars = new char[4048];
    int read;

    while((read = isr.read(chars)) != -1) {
      response.append(chars, 0, read);
    }

    System.out.println(response);

    Pattern reportRegex = Pattern.compile(".*(<report.*>).*");
    Matcher matcher = reportRegex.matcher(response);
    String xml = response.toString();

    // Error handling ...
    if(matcher.matches()) {
      xml = String.format(siteTag, matcher.group(1));
    }

    return new ByteArrayInputStream(xml.getBytes());
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
