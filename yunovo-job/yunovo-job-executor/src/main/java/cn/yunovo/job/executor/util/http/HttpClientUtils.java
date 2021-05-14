/**
 * 
 */
package cn.yunovo.job.executor.util.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;



/**
 * @author kk
 *
 * 2017年7月12日
 */


public class HttpClientUtils {
	
	public static CloseableHttpClient createHttpsClient() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        HttpClientBuilder builder = HttpClientBuilder.create();
        RequestConfig requestConfig = RequestConfig.custom()  
                .setConnectTimeout(5000).setConnectionRequestTimeout(1000)  
                .setSocketTimeout(5000).build(); 
        builder.setDefaultRequestConfig(requestConfig);
        
        SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
            public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                return true;
            }
        }).build();
        builder.setSSLContext(sslContext);

        HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;

        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory)
                .build();
        
        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager( socketFactoryRegistry);
        
        connMgr.setMaxTotal(100);
        connMgr.setDefaultMaxPerRoute(20);
        builder.setConnectionManager( connMgr);

        CloseableHttpClient client = builder.build();
        
        return client;
    }
	
	
	public final static String httpGet(String url) throws Exception {
		HttpGet httpMethod = new HttpGet(url);
		return (String)httpExecute(url,httpMethod,false).get("Content");
	}	
	
	public final static String  httpGet(String url, Header[] headers) {
		
		HttpGet httpMethod = new HttpGet(url);
		httpMethod.setHeaders(headers);
		httpMethod.setConfig(RequestConfig.custom().setRedirectsEnabled(false).build());
		return (String)httpExecute(url,httpMethod,false).get("Content");
	}
	
	public final static String httpPost(String url,  Map<String, String>  params) throws Exception {

		HttpPost httpMethod = new HttpPost(url);
		List<NameValuePair> list = new ArrayList<NameValuePair>();

		for (Entry<String, String> en: params.entrySet()){
			if (en.getKey()!=null && en.getValue()!=null) {
				list.add(new BasicNameValuePair(en.getKey(),en.getValue()));
			}
		}
		
		if(list.size() > 0){
			try {
				UrlEncodedFormEntity reqEntity = new UrlEncodedFormEntity(list,"utf-8");
				httpMethod.setEntity(reqEntity);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
		}
		
		return (String)httpExecute(url,httpMethod,false).get("Content");
	}
	
	
	public final static Map<String,Object> httpPostAndRedirection(String url,  Map<String, String>  params) throws Exception {
		CloseableHttpResponse response = null;
		HttpPost httpMethod = new HttpPost(url);
		List<NameValuePair> list = new ArrayList<NameValuePair>();

		for (Entry<String, String> en: params.entrySet()){
			if (en.getKey()!=null && en.getValue()!=null) {
				list.add(new BasicNameValuePair(en.getKey(),en.getValue()));
			}
		}
		
		if(list.size() > 0){
			UrlEncodedFormEntity reqEntity = new UrlEncodedFormEntity(list,"utf-8");
			httpMethod.setEntity(reqEntity);
		}
		
		return httpExecute(url,httpMethod,true);

	}
	
	public final static String httpPostForJson(String url,  Header[] headers , String  params) throws Exception {
		HttpPost httpMethod = new HttpPost(url);
		if (params!=null && !params.equals("")) {
			StringEntity  reqEntity = new StringEntity (params,"utf-8");
			reqEntity.setContentEncoding("UTF-8");    
			reqEntity.setContentType("application/json");
			httpMethod.setEntity(reqEntity);
		}
		//cookie header
		if (headers!=null && headers.length>0) {
			httpMethod.setHeaders(headers);
		}
		return (String)httpExecute(url,httpMethod,false).get("Content");
	}
	
	
	public final static String httpDelete(String url, Header[] headers) throws Exception {
		HttpDelete httpMethod = new HttpDelete(url);
		httpMethod.setHeaders(headers);
		return (String)httpExecute(url,httpMethod,false).get("Content");
	}	
	
	
	public final static Map<String,Object> httpExecute(String url, HttpRequestBase httpMethod, boolean redirection) {
		CloseableHttpClient  httpClient =  HttpClients.createDefault();  
		CloseableHttpResponse response = null;
		Map<String,Object> ret = new HashMap();
		try {
//			if (redirection == false) {
//				HttpParams params = httpClient..getParams();    
//				params.setParameter(ClientPNames.HANDLE_REDIRECTS, redirection);
//			}
			response = httpClient.execute(httpMethod);
			HttpEntity respEntity = response.getEntity();
			Header[] hh = response.getHeaders("Location");
			if (response.getStatusLine().getStatusCode() == 302) {
				
				Header[] cookie = response.getHeaders("Set-Cookie");
				String location = response.getHeaders("Location")[0].getValue();
				ret.put("Cookie", cookie);
				for (Header h:cookie) {
					System.out.println("Header:"+h.getName()+"     "+ h.getValue());
				}
				System.out.println("location:"+location);
//				String content = httpPostForJson(location,cookie,null);
//				ret.put("Content", content);
				if (redirection==true) {
					ret.put("Content", httpGet(location, cookie));
				}
				return ret;
			} else if (response.getStatusLine().getStatusCode() >= 400) {
				throw new Exception(
					"HTTP Request is not success, Response code is " + response.getStatusLine().getStatusCode());
			}
		
			if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
				ret.put("Set-Cookie", response.getHeaders("Set-Cookie"));
				ret.put("Content", EntityUtils.toString(respEntity, "utf-8"));
				EntityUtils.consume(respEntity);
			}
		
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (response != null) {
					response.close();
				}
				httpClient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return ret;
		
	
	}
	


	
	public static void main(String[] args) throws Exception {
//		Map map = new HashMap();
//		map.put("username", "admin");
//		map.put("password", "admin");
//		map.put("format", "json");
//		httpGet("http://127.0.0.1:8080/console/login?password=admin&format=json&username=admin");
//		httpPost("http://127.0.0.1:8080/console/login", map);
		
//		String ret = httpGet("http://a.aipack.win:8000/open/appinfo/1439515316859446/");
//		JSONObject json = (JSONObject)JsonUtils.fromJSON(ret);
//		System.out.println(json.getString("secretKey"));
//		System.out.println(json.getInteger("maxDeviceNumber"));
//		System.out.println(json.getInteger("registeredDeviceNumber"));
//		System.out.println(json.getDate("expireDate").getTime()/1000);
		
//		httpGet("https://open.weixin.qq.com/connect/oauth2/authorize?appid=wxf120e49da368b3fd&redirect_uri=http%3a%2f%2fwx.yunovo.cn%2fapi&response_type=code&scope=snsapi_userinfo&state=1&connect_redirect=1");
	}

	

}
