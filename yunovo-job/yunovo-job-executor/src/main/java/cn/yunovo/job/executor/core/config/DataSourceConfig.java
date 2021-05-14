package cn.yunovo.job.executor.core.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.alibaba.druid.pool.DruidDataSource;

@Configuration
public class DataSourceConfig {

	@Bean
    @ConfigurationProperties(prefix="datasource.clw")
	public DataSource dataSource() {
		DruidDataSource  dataSource = new DruidDataSource();
		return dataSource;
	}
	
	
	 @Bean
    public JdbcTemplate jdbcTemplate (
        @Qualifier("dataSource")  DataSource dataSource ) {
        return new JdbcTemplate(dataSource);
    }
	 
	 @Bean
	 public NamedParameterJdbcTemplate namedParameterJdbcTemplate (
			 @Qualifier("dataSource")  DataSource dataSource ) {
		 return new NamedParameterJdbcTemplate(dataSource);
	 }
}
