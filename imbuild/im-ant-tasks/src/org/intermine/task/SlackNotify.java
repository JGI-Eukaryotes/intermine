package org.intermine.task;


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.util.StringUtils;
import org.intermine.task.project.Project;
import org.intermine.task.project.ProjectXmlBinding;
import org.intermine.task.project.Source;
import org.intermine.task.project.UserProperty;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class SlackNotify extends Task {
  
  String sourceAttribute = "all sources.";
  /**
   * Set the source to integrate.  null means integrate all sources.
   * @param source the source
   */
  public void setSource(String source) {
      this.sourceAttribute = source.replaceAll(" ","+");
  }
  public void execute() throws BuildException {
    System.out.println("Sending notification for completion of "+sourceAttribute);
    String url = "https://slack.com/api/chat.postMessage";
    URL obj;

    String token = PropertiesUtil.getProperties().getProperty("slack.token");

    String urlParameters = "token="+token+"&channel=intermine-build&text=Loaded+source+"+sourceAttribute;
    
    HttpsURLConnection con;
    try {
      obj = new URL(url);
      con = (HttpsURLConnection) obj.openConnection();
      //add header
      con.setRequestMethod("POST");
      // Send post request
      con.setDoOutput(true);
      DataOutputStream wr = new DataOutputStream(con.getOutputStream());
      wr.writeBytes(urlParameters);
      wr.flush();
      wr.close();

    } catch (Exception e) {
      throw new BuildException("Exception while sending message in notify task: "+e.getMessage());
    }

    BufferedReader in;
    String inputLine;
    StringBuffer response = new StringBuffer();
    try {
      in = new BufferedReader(new InputStreamReader(con.getInputStream()));
      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
      in.close();
    } catch (Exception e) {
      throw new BuildException("Exception while receiving message in notify task: "+e.getMessage());
    }
    //print result
    System.out.println(response.toString());

  }
}
