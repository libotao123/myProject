package cn.yunovo.job.admin;

import javax.servlet.MultipartConfigElement;


import org.apache.log4j.xml.DOMConfigurator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.remoting.caucho.HessianProxyFactoryBean;
import org.sunshine.dcda.view.system.viewcomponent.ICooperateOrganViewComponent;
import org.sunshine.dcda.view.system.viewcomponent.ISystemResourceViewComponent;
import org.sunshine.dcda.view.system.viewcomponent.ISystemUserViewComponent;

//重点
/*@MapperScan(basePackages={"cn.yunovo.job.admin.dao"})*/
@ServletComponentScan
@SpringBootApplication
@ImportResource("file:/apps/conf/mtp/spring/*.xml")
public class JobApplication {
	
	@Value("${hessian.path.usercenter}")
    private String usercenter;

	public static void main(String[] args) {
		
		for (String arg : args) {
			if(arg.endsWith("log4j.xml")) {
				
				DOMConfigurator.configure(arg);
				break;
			}
		}
		SpringApplication.run(JobApplication.class, args);
	}
	
	@Bean(name = "systemUserViewComponent")
	public HessianProxyFactoryBean systemUserViewComponentClient() {
		HessianProxyFactoryBean factory = new HessianProxyFactoryBean();
		factory.setServiceUrl(usercenter+"/remote/systemUserViewComponent");
		factory.setServiceInterface(ISystemUserViewComponent.class);
		return factory;
	}

	@Bean(name = "systemResourceViewComponent")
	public HessianProxyFactoryBean systemResourceViewComponent() {
		HessianProxyFactoryBean factory = new HessianProxyFactoryBean();
		factory.setServiceUrl(usercenter+"/remote/systemResourceViewComponent");
		factory.setServiceInterface(ISystemResourceViewComponent.class);
		return factory;
	}
	
	@Bean(name = "cooperateOrganViewComponent")
	public HessianProxyFactoryBean cooperateOrganViewComponent() {
		HessianProxyFactoryBean factory = new HessianProxyFactoryBean();
		factory.setServiceUrl(usercenter+"/remote/cooperateOrganViewComponent");
		factory.setServiceInterface(ICooperateOrganViewComponent.class);
		return factory;
	}
	

	@Bean
	public MultipartConfigElement multipartConfigElement() {
		MultipartConfigFactory factory = new MultipartConfigFactory();
		// 文件最大
		factory.setMaxFileSize("1024000KB"); // KB,MB
		/// 设置总上传数据总大小
		factory.setMaxRequestSize("20480000KB");
		return factory.createMultipartConfig();
	}
}
