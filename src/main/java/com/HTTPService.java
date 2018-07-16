package com;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.w3c.dom.Document;

import javax.net.ssl.SSLContext;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.stream.Collectors;



/**
 * Created by kalistrat on 19.04.2018.
 */
public class HTTPService {
    String webServiceURL;
    CloseableHttpClient httpClient;

    public HTTPService(){
        try {

            setHttpService();

        } catch (Exception e) {
            System.out.println("HTTP : Ошибка запуска службы");
        }

    }


//    public void setHttpService(){
//        try {
//
//            webServiceURL = Main.prop.getProperty("HTTP_UNIFIED_WS_URL");
//            httpClient = new DefaultHttpClient();
//
//            KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
//            FileInputStream instream = new FileInputStream(new File(Main.AbsPath + Main.prop.getProperty("HTTP_KEY_STORE_NAME")));
//            try {
//                trustStore.load(instream, "888888".toCharArray());
//            } finally {
//                instream.close();
//            }
//
//            SSLSocketFactory socketFactory = new SSLSocketFactory(trustStore);
//            Scheme sch = new Scheme("https", socketFactory, 443);
//            httpClient.getConnectionManager().getSchemeRegistry().register(sch);
//
//
//            String checkConnection = linkDevice("TEST");
//            if (checkConnection == null) {
//                System.out.println("HTTP : центральный веб-сервис " + webServiceURL + " недоступен");
//                httpClient = null;
//            } else {
//                System.out.println("HTTP : соединение с " + webServiceURL + " установлено");
//            }
//
//            //System.out.println("checkConnection : " + checkConnection);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("HTTP : Ошибка перезапуска службы");
//            httpClient = null;
//        }
//    }

    public void setHttpService(){
        try {

            webServiceURL = Main.prop.getProperty("HTTP_UNIFIED_WS_URL");


            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial((KeyStore)null, new TrustSelfSignedStrategy())
                    //I had a trust store of my own, and this might not work!
                    .build();

            httpClient = HttpClients.custom()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(new NoopHostnameVerifier())
                    .build();

            String checkConnection = linkDevice("TEST");
            if (checkConnection == null) {
                System.out.println("HTTP : центральный веб-сервис " + webServiceURL + " недоступен");
                httpClient = null;
            } else {
                System.out.println("HTTP : соединение с " + webServiceURL + " установлено");
            }


        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("HTTP : Ошибка перезапуска службы");
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
