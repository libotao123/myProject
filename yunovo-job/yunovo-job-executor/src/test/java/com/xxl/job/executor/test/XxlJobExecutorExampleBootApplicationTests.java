/*package com.xxl.job.executor.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import cn.yunovo.job.executor.Application;
import cn.yunovo.job.executor.service.TaskExecErrorService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class ,webEnvironment= SpringBootTest.WebEnvironment.DEFINED_PORT)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class})
public class XxlJobExecutorExampleBootApplicationTests {

	
	@Autowired
	private TaskExecErrorService s;
	
	@Test
	public void test() {

		s.insert("123123", "test", "test", 0, "test", "test");
	}

}*/