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
import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name="upload-report-surefire")
public class CDashSurefireMojo extends CDashAbstractMojo {

  @Parameter(defaultValue="TEST-.*\\.xml")
  String surefireReportsFilenameRegex;

  /**
   * The directories to search for reports
   */
  @Parameter(defaultValue="")
  private File[] surefireReportsDirectories;


  public String getSurefireReportsFilenameRegex() {
    return surefireReportsFilenameRegex;
  }

  public void setSurefireReportsFilenameRegex(String surefireReportsFilenameRegex) {
    this.surefireReportsFilenameRegex = surefireReportsFilenameRegex;
  }

  public void execute() throws MojoExecutionException {

    List<File> toUpload = findReports(getSurefireReportsDirectories(),
                                      surefireReportsFilenameRegex);

    upload(toUpload);
  }

  protected InputStream addSite(InputStream xmlStream) throws IOException {

    StringBuilder response = new StringBuilder();
    InputStreamReader isr = new InputStreamReader(xmlStream);
    char[] chars = new char[4048];
    int read;

    while((read = isr.read(chars)) != -1) {
      response.append(chars, 0, read);
    }

    String xml = response.toString();
    xml = xml.replaceFirst("<testsuite",
                           String.format("<testsuite hostname=\"%s\"", site));

    return new ByteArrayInputStream(xml.getBytes());
  }

  private File getSurefireReportsDirectory() {

    List<Plugin> plugins = mavenProject.getBuildPlugins();
    plugins.addAll(mavenProject.getPluginManagement().getPlugins());

    File reportsDirectory = null;

    for (Plugin plugin : plugins) {
      if(plugin.getArtifactId().equals("maven-surefire-plugin")) {

        Xpp3Dom configuration = (Xpp3Dom)plugin.getConfiguration();
        if(configuration == null || configuration.getChild("reportsDirectory") == null) {
          break;
        }
        Xpp3Dom reportsDirectoryNode
          = configuration.getChild("reportsDirectory");

        reportsDirectory = new File(reportsDirectoryNode.getValue());
        break;
      }
    }

    return reportsDirectory;
  }

  public File[] getSurefireReportsDirectories() {

    if(surefireReportsDirectories == null) {
      // first check for surefire configuration.

      File reportsDirectory = getSurefireReportsDirectory();

      // if no configuration was provided use the default
      if(reportsDirectory == null) {
        reportsDirectory = new File(baseDir, "target"
                                    + File.separator
                                    + "surefire-reports");
      }

      surefireReportsDirectories = new File[] { reportsDirectory };
    }

    return surefireReportsDirectories;
  }

  public void setSurefireReportsDirectories(File[] reportsDirectories) {
    this.surefireReportsDirectories = reportsDirectories;
  }
}
