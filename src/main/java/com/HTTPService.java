package com;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

/**
 * Created by kalistrat on 19.04.2018.
 */
public class HTTPService {
    String webServiceURL;
    HttpClient httpClient;

    public HTTPService(){
        try {

            setHttpService();

        } catch (Exception e) {
            e.printStackTrace();
            //httpClient = null;
        }

    }

    public void setHttpService(){
        try {

            webServiceURL = Main.prop.getProperty("HTTP_UNIFIED_WS_URL");
            httpClient = new DefaultHttpClient();


            String checkConnection = linkDevice("TEST");
            if (checkConnection == null) {
                System.out.println("центральный веб-сервис недоступен");
                httpClient = null;
            } else {
                System.out.println("соединение с " + webServiceURL + " установлено");
            }

            //System.out.println("checkConnection : " + checkConnection);

        } catch (Exception e) {
            e.printStackTrace();
            httpClient = null;
        }
    }

    public String linkDevice(
            String chainUID
    ){
        String respWs = null;

        try {


            HttpPost post = new HttpPost(webServiceURL);
            post.setHeader("Content-Type", "text/xml");

            String reqBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:com=\"http://com/\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <com:linkUserDevice>\n" +
                    "         <!--Optional:-->\n" +
                    "         <arg0>"+chainUID+"</arg0>\n" +
                    "      </com:linkUserDevice>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>";

            StringEntity input = new StringEntity(reqBody, Charset.forName("UTF-8"));
            post.setEntity(input);
            HttpResponse response = httpClient.execute(post);
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            Document resXml = staticMethods.loadXMLFromString(rd.lines().collect(Collectors.joining()));
            respWs = XPathFactory.newInstance().newXPath()
                    .compile("//return").evaluate(resXml);


        } catch (Exception e){
            e.printStackTrace();

        }
        return respWs;
    }

}
