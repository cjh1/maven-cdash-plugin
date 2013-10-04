package com.kitware.maven.plugins.cdash;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public abstract class CDashAbstractMojo extends AbstractMojo {

  @Parameter(readonly=true, defaultValue="${project}")
  protected MavenProject mavenProject;

  @Parameter(readonly=true, defaultValue="${project.basedir}")
  protected File baseDir;

  @Parameter(required=true)
  protected String site;

  @Parameter(required=true)
  protected String project;

  @Parameter(required=true)
  protected URL url;

  public MavenProject getMavenProject() {
    return mavenProject;
  }

  public void setMavenProject(MavenProject mavenProject) {
    this.mavenProject = mavenProject;
  }

  public File getBaseDir() {
    return baseDir;
  }

  public void setBaseDir(File baseDir) {
    this.baseDir = baseDir;
  }

  public URL getUrl() {
    return url;
  }

  public void setUrl(URL url) {
    this.url = url;
  }

  public String getProject() {
    return project;
  }

  public void setProject(String project) {
    this.project = project;
  }

  protected String getUploadUrl() {
    return String.format("%s/submit.php?project=%s", url, project);
  }

  protected void upload(List<File> toUpload) throws MojoExecutionException {
    // Now upload the report
    for (File file : toUpload) {

      InputStream fis = null;
      try {
        fis = addSite(new FileInputStream(file));
        HttpPostUploadRequest request = new HttpPostUploadRequest(getUploadUrl(),
                                                                  fis);
        request.setContentType("application/xml");
        String response = request.execute();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(response)));

        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        XPathExpression expr = xpath.compile("/cdash/status/text()");
        String status = expr.evaluate(doc);
        if(!status.equals("OK")) {
          expr = xpath.compile("/cdash/message/text()");
          String message = expr.evaluate(doc);

          getLog().error(String.format("CDash error uploading %s - status: %s, message:%s",
                                       file, status, message));
        }
      }
      catch (IOException e) {
        throw new MojoExecutionException("Error uploading file", e);
      } catch (ParserConfigurationException e) {
        throw new MojoExecutionException("XML configuration error", e);
      } catch (SAXException e) {
        throw new MojoExecutionException("SAX error", e);
      } catch (XPathExpressionException e) {
        throw new MojoExecutionException("XPath error", e);
      }
      finally {
        if(fis != null) {
          try {
            fis.close();
          }
          catch (IOException e) { }
        }
      }
    }
  }

  protected InputStream addSite(InputStream is) throws IOException {
    return is;
  }

  protected List<File> findReports(File[] reportDirectories,
                                   final String reportsFilenameRegex) {

    List<File> toUpload = new ArrayList<File>();

    // Gather all the reports we need to upload
    for (File reportDir : reportDirectories) {

      File[] files = reportDir.listFiles(new FilenameFilter() {

        public boolean accept(File dir, String name) {
          return Pattern.matches(reportsFilenameRegex, name);
        }
      });
      if(files != null)
        toUpload.addAll(Arrays.asList(files));
    }
    return toUpload;
  }

}
