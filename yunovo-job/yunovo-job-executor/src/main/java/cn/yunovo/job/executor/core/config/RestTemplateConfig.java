/**
 * 
 */
package cn.yunovo.job.executor.core.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import cn.yunovo.job.executor.util.http.HttpClientUtils;



/**
* @author kk
*/

@Configuration
public class RestTemplateConfig {
	
	@Bean
	public RestTemplate restTemplate(ClientHttpRequestFactory factory){
		return new RestTemplate(factory);
	}
	
	@Bean
	public ClientHttpRequestFactory simpleClientHttpRequestFactory(){
		try {
			CloseableHttpClient httpClient = HttpClientUtils.createHttpsClient();
	        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
	        factory.setReadTimeout(10000);
	        return factory;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}

//		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
//		factory.setConnectTimeout(1000);
//		factory.setReadTimeout(5000);
		
//		return factory;
	 }
}
