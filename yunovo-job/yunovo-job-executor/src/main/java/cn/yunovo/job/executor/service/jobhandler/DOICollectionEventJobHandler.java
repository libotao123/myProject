package cn.yunovo.job.executor.service.jobhandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

@JobHandler(value="doi_event_job_zc")
@Component
/**
 * doi 收集 设备能力信息
 * @description 
 * @author  zhangchuan
 * @time   2019年5月13日
 */
public class DOICollectionEventJobHandler extends IJobHandler {
    
    @Autowired
    public RestTemplate restTemplate;
    
    @Autowired
    private JdbcTemplate jt;
    
    // 云大使配置
    private String yundash_url = "http://yundash.yunovo.cn";
    
    private String yundash_api_key = "EKUtEeFlJwBwbTM2O9fRglcoMycHLR1M2MjX5vZ9";
    
    private int PAGE_NUM = 1000; 
    
    // 默认 2017-01-01 之后的设备都有doi功能 ,生产环境的是2019-01-01 
    private String START_TIME = "2015-01-01";
    
    @Override
    public ReturnT<String> execute(String param) throws Exception {
        XxlJobLogger.log("行车周报开始收集有数据的微信open_id");
        ReturnT<String> a = new ReturnT<String>();
        
        try
        {
            StartUpdateDeviceInfoWithYundashi();
            a.setCode(202);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            a.setCode(424);
            XxlJobLogger.log("行车周报收集有数据的微信异常");
        }
       
        a.setMsg("yunovo测试:"+new SimpleDateFormat("yyyyMMdd HH:mm:sss").format(new Date()));
        return a;
    }
    
    
   




    private int[] batchInsertInfo(List<JSONObject> device_Info)
    {
        final List<JSONObject> tempRep = device_Info;
        System.out.println(tempRep.toString());
        if(tempRep.isEmpty()) {
            return null;
        }
        
        String sql = "  INSERT INTO `clw`.`ocp_doi_device_baseInfo` ( `device_sn`, `device_type`, `organ_code`, "
            + " `customer_id`, `wx_appid`, `province`, `city`, `longitude`, `latitude`, `batch_datetime`, "
            + "`last_location_datetime`, `create_datetime`, `update_datetime`) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(),"
            + " now() ) ON DUPLICATE KEY  UPDATE organ_code = ?, customer_id=?,wx_appid = ?,province=?,city=?,longitude=?,"
            + " latitude=?,batch_datetime = ?,last_location_datetime=?,update_datetime= now() ";
        
        try
        {
            return jt.batchUpdate(sql, new BatchPreparedStatementSetter() {

                @Override
                public void setValues(PreparedStatement ps, int i)
                    throws SQLException
                {
                    ps.setString(1,tempRep.get(i).getString("device_sn"));
                    ps.setInt(2,tempRep.get(i).getInteger("device_type"));
                    ps.setString(3,tempRep.get(i).getString("organ_code")==null?"":tempRep.get(i).getString("organ_code"));
                    ps.setInt(4,tempRep.get(i).getInteger("customer_id")==null?0:tempRep.get(i).getInteger("customer_id"));
                    ps.setString(5,tempRep.get(i).getString("wx_appid")==null?"":tempRep.get(i).getString("wx_appid"));
                    ps.setString(6,tempRep.get(i).getString("province")==null?"":tempRep.get(i).getString("province"));
                    ps.setString(7,tempRep.get(i).getString("city")==null?"":tempRep.get(i).getString("city"));
                    ps.setString(8,tempRep.get(i).getString("longitude")==null?"":tempRep.get(i).getString("longitude"));
                    ps.setString(9,tempRep.get(i).getString("latitude")==null?"":tempRep.get(i).getString("latitude"));
                    ps.setString(10,tempRep.get(i).getString("batch_datetime")==null?"":tempRep.get(i).getString("batch_datetime"));
                    ps.setString(11,tempRep.get(i).getString("last_location_datetime")==null?"":tempRep.get(i).getString("last_location_datetime"));
                    ps.setString(12,tempRep.get(i).getString("organ_code")==null?"":tempRep.get(i).getString("organ_code"));
                    ps.setInt(13,tempRep.get(i).getInteger("customer_id")==null?0:tempRep.get(i).getInteger("customer_id"));
                    ps.setString(14,tempRep.get(i).getString("wx_appid")==null?"":tempRep.get(i).getString("wx_appid"));
                    ps.setString(15,tempRep.get(i).getString("province")==null?"":tempRep.get(i).getString("province"));
                    ps.setString(16,tempRep.get(i).getString("city")==null?"":tempRep.get(i).getString("city"));
                    ps.setString(17,tempRep.get(i).getString("longitude")==null?"":tempRep.get(i).getString("longitude"));
                    ps.setString(18,tempRep.get(i).getString("latitude")==null?"":tempRep.get(i).getString("latitude"));
                    ps.setString(19,tempRep.get(i).getString("batch_datetime")==null?"":tempRep.get(i).getString("batch_datetime"));
                    ps.setString(20,tempRep.get(i).getString("last_location_datetime")==null?"":tempRep.get(i).getString("last_location_datetime"));
                    
                }

                @Override
                public int getBatchSize()
                {
                    return tempRep.size();
                }
                
            } );
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        return null;
    }




    

    
    private void StartUpdateDeviceInfoWithYundashi() throws Exception
    {
        String url = yundash_url+"/api/queries/%s/results.csv?api_key=%s";
        String result = restTemplate.getForObject(String.format(url, 258,yundash_api_key), String.class);
        String[] arry = result.split("\r\n");
        int total = Integer.valueOf(arry[1]);
        
        int page = PAGE_NUM;
        List<JSONObject> device_Info;
        for(int i=0; i*page <= total; i++) {
            device_Info = pageQueryDeviceInfoWithYundashi(i*page, page);
            long long1 = System.currentTimeMillis();
            // batchInsertInfo(device_Info);
            long long2 = System.currentTimeMillis();
            System.out.println("插入数据花费时间"+(long2-long1));
        }
        
    }
    
    
    private List<JSONObject> pageQueryDeviceInfoWithYundashi(int start, int end) throws Exception{
        Map<String, Object> param =  new HashMap<>();
        param.put("time_start", START_TIME);
        param.put("page_s", start);
        param.put("page_e", end);
        long long1 = System.currentTimeMillis();
        JSONArray doc_count_arr = queryMainYundash(param,"257");
        long long2 = System.currentTimeMillis();
        System.out.println(long2);
        System.out.println("云大使查询花费时间"+(long2-long1));
        final List<JSONObject> tempRep = JSONObject.parseArray(doc_count_arr.toJSONString(), JSONObject.class);
        long long3 = System.currentTimeMillis();
        System.out.println("转list花费时间"+(long3-long2));
        return tempRep;
        
    }
    
    
    
    
    /**
     * 主查询
     * @return
     */
    public JSONArray queryMainYundash (Map<String, Object> param, String query_id) throws Exception {
        String param_url = "";
        Set<String> set = param.keySet();
        for (String key:set) {
            param_url += "&p_" + key + "=" + param.get(key);
        }
        JSONObject refresh_result = null;
        String result_url = String.format(yundash_url+"/api/queries/%s/refresh?api_key=%s", query_id,yundash_api_key) + param_url;
        refresh_result = refreshYundash(result_url);
        String id = refresh_result.getJSONObject("job").getString("id");
        Integer query_result_id = getYundashJobId(yundash_url+"/api/jobs/%s?api_key=%s",id,1);
        if (query_result_id == -1) {
            XxlJobLogger.log("yundash查询超时");
            return null;
        }
        JSONArray doc_count_arr = getYundashQueryResultId(yundash_url+"/api/query_results/%s?api_key=%s",query_result_id);
        return doc_count_arr;
    }
    
    /**
     * 刷新yundash
     */
    public JSONObject refreshYundash(String url){
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        String result = restTemplate.postForObject(url, headers, String.class);
        return JSONObject.parseObject(result);
    }
    
    /**
     * 获取yundash的jobid 
     */
    public int getYundashJobId(String url, String id, int index) throws InterruptedException{
        String result = restTemplate.getForObject(String.format(url, id,yundash_api_key), String.class);
        JSONObject job_result = JSONObject.parseObject(result);
        if (null != job_result.getJSONObject("job").getInteger("query_result_id")) {
            return job_result.getJSONObject("job").getInteger("query_result_id");
        } else {
            if (index>200) {
                return -1;
            }
            Thread.sleep(300);
            index++;
            return getYundashJobId(url, id, index);
        }
    }
    
    /**
     * 根据查询id获取yundash数据 
     */
    public JSONArray getYundashQueryResultId(String url,Integer query_result_id) {
        String result = restTemplate.getForObject(String.format(url, query_result_id,yundash_api_key), String.class);
        JSONObject res = JSONObject.parseObject(result);
        return res.getJSONObject("query_result").getJSONObject("data").getJSONArray("rows");
    }
    
    /**
     * 不带参数查询
     */
    public JSONArray queryMainYundash(String url) throws Exception {
        String result = restTemplate.getForObject(url, String.class);
        JSONObject res = JSONObject.parseObject(result);
        return res.getJSONObject("query_result").getJSONObject("data").getJSONArray("rows");
    }
    
}
