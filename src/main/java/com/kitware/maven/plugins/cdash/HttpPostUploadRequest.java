package com.kitware.maven.plugins.cdash;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpPostUploadRequest {

  private String url;
  private InputStream is;
  private String contentType;


  HttpPostUploadRequest(String url, InputStream is) {
    this.url = url;
    this.is = is;
  }

  String execute() throws IOException {
    HttpURLConnection connection = null;

    try
    {
      connection = (HttpURLConnection)new URL(url).openConnection();
      connection.setDoOutput(true);
      connection.setDoInput(true);
      connection.setInstanceFollowRedirects(false);
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", contentType);
      connection.setRequestProperty("charset", "utf-8");

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      byte[] bytes = new byte[4048];
      int read = -1;

      while((read = is.read(bytes)) != -1) {
        baos.write(bytes, 0, read);
      }

      connection.setRequestProperty("Content-Length", "" + Integer.toString(baos.size()));
      connection.setUseCaches (false);

      DataOutputStream wr = new DataOutputStream(connection.getOutputStream ());
      wr.writeBytes(baos.toString());
      wr.flush();
      wr.close();

      connection.disconnect();

      char[] chars = new char[4048];

      StringBuilder response = new StringBuilder();
      InputStreamReader isr = new InputStreamReader(connection.getInputStream());

      while((read = isr.read(chars)) != -1) {
        response.append(chars, 0, read);
      }

      return response.toString();
    }
    finally
    {
      if(connection != null)
        connection.disconnect();
    }
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }
}
