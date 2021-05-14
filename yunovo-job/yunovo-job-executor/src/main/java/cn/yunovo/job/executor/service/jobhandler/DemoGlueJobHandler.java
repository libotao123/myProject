package cn.yunovo.job.executor.service.jobhandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;

@JobHandler(value="testjob_zc")
@Component
public class DemoGlueJobHandler extends IJobHandler {
    
    @Autowired
    public RestTemplate restTemplate;
    
    @Autowired
    private JdbcTemplate jt;
    
    //外部第三方接口服务地址("http://localhost:7000";)
    private String GET_THRID_API_URL = "http://192.168.3.241:8080/rest/api/trace/search?deviceSn=%s&startTime=%s&endTime=%s&size=%s";
    
    @Override
    public ReturnT<String> execute(String param) throws Exception {
        XxlJobLogger.log("行车周报开始收集有数据的微信open_id");
        ReturnT<String> a = new ReturnT<String>();
        
        try
        {
            String time = getLastTimeInterval();
            String[] time_arr =  time.split(",");
            
//            postReport(time_arr[0],time_arr[1],"3");
            postReport("1541001600000","1543593600000","3");
            a.setCode(202);
        }
        catch (Exception e)
        {
            a.setCode(424);
            XxlJobLogger.log("行车周报收集有数据的微信异常");
        }
       
        a.setMsg("yunovo测试:"+new SimpleDateFormat("yyyyMMdd HH:mm:sss").format(new Date()));
        return a;
    }
    
  // 获取当前的时间的上一周 起止时间，返回时间戳
    public  String getLastTimeInterval() {  
         Calendar calendar1 = Calendar.getInstance();  
         Calendar calendar2 = Calendar.getInstance();  
         int dayOfWeek = calendar1.get(Calendar.DAY_OF_WEEK) - 1;  
         int offset1 = 1 - dayOfWeek;  
         int offset2 = 7 - dayOfWeek;  
         calendar1.add(Calendar.DATE, offset1 - 7);
         calendar1.set(Calendar.HOUR_OF_DAY , 0);
         calendar1.set(Calendar.MINUTE, 0);
         calendar1.set(Calendar.SECOND, 0);
         calendar1.set(Calendar.MILLISECOND, 0);
         
         
         calendar2.add(Calendar.DATE, offset2 - 7);  
         calendar2.set(Calendar.HOUR_OF_DAY , 23);
         calendar2.set(Calendar.MINUTE, 59);
         calendar2.set(Calendar.SECOND, 59);
         calendar2.set(Calendar.MILLISECOND, 999);
         
         long lastBeginDate =  calendar1.getTimeInMillis();  
         long lastEndDate =  calendar2.getTimeInMillis();  
         return lastBeginDate + "," + lastEndDate;  
    }  
  
  
  // 获取到有数据，可以作为推送对象的open_id
  public void postReport(String startTime, String endTime, String size) {
      long long1 =  System.currentTimeMillis();
        List<JSONObject> postReport =  getDeviceSN();
        List<JSONObject> list_open_id = new ArrayList<>();
        int num = 0;
        for(int i = 0;i<postReport.size();i++) {
            JSONObject js = null;
            JSONObject rep = postReport.get(i);
            try {
                js = search(rep.getString("device_sn"), startTime, endTime, size);
                if(js.containsKey("data")) {
                    JSONArray datas = js.getJSONArray("data");
                    if(datas != null ) {
                        if(datas.size() >= 2) {
                            System.out.println(rep.toString()+",size="+datas.size());
                            list_open_id.add(rep);
                            num ++;
                        }
                        
                    }
                }
                
            } catch (Exception e) {
                i--;
                continue;
            }
            // 批量插入 （大于1000的时候插入一次数据）
            if(num >= 10 ) {
                batchInsertPostReport(list_open_id,startTime,endTime);
                num = 0;
                list_open_id.clear();
            }
            
            
        }
        if(num < 10) {
            batchInsertPostReport(list_open_id,startTime,endTime);
        }

        long long2 =  System.currentTimeMillis();
        XxlJobLogger.log("集合大小:{0},花费时间:{1}", list_open_id.size(),(long2  -  long1));
    }
  
  
  
  
  // 获取到有数据，可以作为推送对象的open_id
      public List<JSONObject> getDeviceSN() {
          String sql = " SELECT " + 
                  " dev.device_id device_id  ," + 
                  " dev.device_sn device_sn  ," + 
                  " wx.open_id    open_id   , " + 
                  " wx.wx_domain  wx_domain   " + 
                  "FROM  " + 
                  " clw.cc_device dev " + 
                  "INNER JOIN clw.cc_device_bind bind ON dev.device_id = bind.device_id " + 
                  "AND bind. STATUS != 0 " + 
                  "INNER JOIN clw.cc_customer_wx wx ON wx.wx_id = bind.wx_id  ";
           
//          List <PostReport>  postReportList = jt.query(sql, new BeanPropertyRowMapper<PostReport>(PostReport.class));       
//          JSONArray json = new JSONArray();
          NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jt);
          List<JSONObject> result  = namedParameterJdbcTemplate.query(sql, new RowMapper() {

            @Override
            public Object mapRow(ResultSet rs, int rowNum)
                throws SQLException
            {
                JSONObject json = new JSONObject();
                json.put("device_id", rs.getInt("device_id"));
                json.put("device_sn", rs.getString("device_sn"));
                json.put("open_id", rs.getString("open_id"));
                json.put("wx_domain", rs.getString("wx_domain"));
                
                return json;
            }
              
          });
          XxlJobLogger.log("行车周报收集结果："+result.size());
          return result;
      }
  
// 批量插入
    public int[] batchInsertPostReport(List<JSONObject> list_PostReport, String startTime, String endTime) {
        final List<JSONObject> tempRep = list_PostReport;
        
        String sql = " INSERT INTO `clw`.`cc_device_rep_pushdata` (`device_sn`, `device_id`, `open_id`, `wx_domain`, `startTime`, "
                + "`endTime`,  `created_by`, `created_time`,  `updated_time`,`updated_by`) "
                + "VALUES (?,?,?,?,?,?,?,now(),now(),'JOB定时执行')";
        
        try
        {
            return jt.batchUpdate(sql, new BatchPreparedStatementSetter() {
                
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setString(1,tempRep.get(i).getString("device_sn"));
                    ps.setString(2, tempRep.get(i).getString("device_id"));
                    ps.setString(3,tempRep.get(i).getString("open_id"));
                    ps.setString(4,tempRep.get(i).getString("wx_domain"));
                    ps.setLong(5, Long.valueOf(startTime));
                    ps.setLong(6, Long.valueOf(endTime));
                    ps.setString(7,"JOB定时执行");
                    
                    
                }
                
                @Override
                public int getBatchSize() {
                    
                    return tempRep.size();
                }
            });
        }
        catch (Exception e)
        {
           e.printStackTrace();
        }
        return null;
        
        
    }
  
    public JSONObject search(String device_sn,String startTime,String endTime,String size) {
        
        String result = restTemplate.getForObject(String.format(GET_THRID_API_URL,device_sn,startTime,endTime,size),String.class);
//        String result = restTemplate.getForObject(THRID_API_URL+TRACE_SEARCH_API, String.class, prarms);
        
        // 判断result是否为空,为空 return null,
        
        if(StringUtils.isEmpty(result)) {
            return null;
        }else {
            JSONObject json=JSONObject.parseObject(result);
            return json;
        }
        
    }
  
  
  
  // PostReport 类
    public class PostReport  {

    private Integer device_id;
      private String device_sn;
      private String open_id;
      private String  wx_domain;
    
      public Integer getDevice_id() {
          return device_id;
      }
      public void setDevice_id(Integer device_id) {
          this.device_id = device_id;
      }
      public String getDevice_sn() {
          return device_sn;
      }
      public void setDevice_sn(String device_sn) {
          this.device_sn = device_sn;
      }
      public String getOpen_id() {
          return open_id;
      }
      public void setOpen_id(String open_id) {
          this.open_id = open_id;
      }
          public String getWx_domain() {
          return wx_domain;
      }
      public void setWx_domain(String wx_domain) {
          this.wx_domain = wx_domain;
      }
        public PostReport(Integer device_id, String device_sn, String open_id, String wx_domain)
        {
            super();
            this.device_id = device_id;
            this.device_sn = device_sn;
            this.open_id = open_id;
            this.wx_domain = wx_domain;
        }
    
    
    }
}
