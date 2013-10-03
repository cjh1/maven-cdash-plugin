package com.kitware.maven.plugins.cdash;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
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
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Abstract Mojo that provides the base functionality to upload results to
 * CDash.
 */
public abstract class CDashAbstractMojo extends AbstractMojo {

  private static final SimpleDateFormat CDASH_TIMESTAMP
    = new SimpleDateFormat("yyyyMMdd-kkmm");

  /**
   * @parameter expression="${project}"
   * @readonly
   */
  protected MavenProject mavenProject;

  /**
   * @parameter expression="${project.basedir}"
   * @readonly
   */
  protected File baseDir;

  /**
   * Site that will be reported to CDash.
   * @parameter
   */
  protected String site;

  /**
   * The particular project dashboard that reports should be uploaded to.
   *
   * @parameter
   * @required
   */
  protected String project;

  /**
   * The URL for the CDash server.
   *
   * For example: open.cdash.org
   *
   * @parameter
   * @required
   */
  protected URL url;

  /**
   * The track that the upload should appear under, the default is Nightly
   *
   * @parameter default-value="Nightly"
   */
  protected String track;


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

  public String getSite() {
    return site;
  }

  public void setSite(String site) {
    this.site = site;
  }

  public String getTrack() {
    return track;
  }

  public void setTrack(String track) {
    this.track = track;
  }

  /**
   * @return The URL to which the POST request should be main to upload
   *         a report.
   */
  protected String getUploadUrl() {
    return String.format("%s/submit.php?project=%s", url, project);
  }

  /**
   * Uploads a list of files to CDash.
   *
   * @param toUpload The list of files to be uploaded.
   * @param buildName The build name to be reported to CDash.
   *
   * @throws MojoExecutionException
   */
  protected void upload(List<File> toUpload, String buildName) throws MojoExecutionException {
    // Now upload the report
    for (File file : toUpload) {

      InputStream fis = null;
      try {
        fis = addSite(new FileInputStream(file), buildName);
        HttpPostUploadRequest request = new HttpPostUploadRequest(getUploadUrl(),
                                                                  fis);
        request.setContentType("application/xml");
        String response = request.execute();

        // Now use XPath to extract out the response from CDash
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(response)));

        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        XPathExpression expr = xpath.compile("/cdash/status/text()");
        String status = expr.evaluate(doc);

        // If the status isn't OK report the error to Maven infrastructure.
        if(!status.equals("OK")) {
          expr = xpath.compile("/cdash/message/text()");
          String message = expr.evaluate(doc);

          getLog().error(String.format("CDash error uploading %s - status: %s, message:%s",
                                       file, status, message));
        }
      }
      catch (IOException e) {
        throw new MojoExecutionException("IO exception upload - file: " + file, e);
      } catch (ParserConfigurationException e) {
        throw new MojoExecutionException("XML configuration error - file: " + file, e);
      } catch (SAXException e) {
        throw new MojoExecutionException("SAX error - file: " + file, e);
      } catch (XPathExpressionException e) {
        throw new MojoExecutionException("XPath error - file: " + file, e);
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

  /**
   * Wrap the report XML in the Site tag that CDash expects.
   *
   * @param xmlStream The input stream containing the report XML.
   * @param buildName The build name to report to CDash.
   */
  protected InputStream addSite(InputStream xmlStream, String buildName) throws IOException {
    String siteTemplate = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                           "<Site BuildName=\"%s\" " +
                            "BuildStamp=\"%s\" " +
                            "Name=\"%s\" " +
                            "Generator=\"maven-cdash-plugin\" " +
                            "CompilerName=\"/usr/bin/c++\" " +
                            "OSName=\"Linux\" " +
                            "Hostname=\"%s\" " +
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

    String compilerName = System.getProperty("java.home");
    String osName = System.getProperty("os.name");
    String hostname = InetAddress.getLocalHost().getHostName();
    String osVersion = System.getProperty("os.version");
    String osPlatform = System.getProperty("os.arch");
    int numberLogicalCPU = Runtime.getRuntime().availableProcessors();
    long totalPhysicalMemory = Runtime.getRuntime().totalMemory();

    System.out.println("xml: "+ xml);

    return new ByteArrayInputStream(xml.getBytes());
  }

  /**
   * @param reportDirectories The directories to look for reports in.
   * @param reportsFilenameRegex The regex that matches the reports filesname.
   *
   * @return The list of report files found.
   */
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
