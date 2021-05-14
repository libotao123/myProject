//package cn.yunovo.job.executor.service;
//
//import java.util.Date;
//
//import javax.annotation.PostConstruct;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.cassandra.config.CassandraSessionFactoryBean;
//import org.springframework.data.cassandra.core.CassandraOperations;
//import org.springframework.stereotype.Service;
//
//import com.datastax.driver.core.BoundStatement;
//import com.datastax.driver.core.PreparedStatement;
//
//@Service
//public class TaskExecErrorService {
//	
//	private PreparedStatement ps = null;
//	
//	@Autowired
//	private CassandraSessionFactoryBean session;
//	
//	@PostConstruct()
//	public void init() {
//		ps = session.getObject().prepare("insert into task_exec_error_info(id , task_id , task_content , create_datetime , create_by , err_message , is_success , type , err_code , is_process ) values (now(),? ,?,?,?,?,?,?,?,0)");
//	}
//
//	@Autowired
//	private CassandraOperations cassandraOperations;
//	
//	public boolean insert(String task_id, String task_content, String err_message, Integer is_success, String type, String err_code) {
//		String id = (task_id == null ? "" : task_id);
//		Long time = new Date().getTime();
//		//String sql = "insert into task_exec_error_info(id , task_id , task_content , create_datetime , create_by , err_message , is_success , type , err_code , is_process ) values (now(),? ,?,?,?,?,?,?,?,0)";
//		BoundStatement st = ps.bind(id, task_content,time, "MTP", err_message,is_success, type,err_code);
//		return cassandraOperations.getCqlOperations().execute(st);
//	}
//}
