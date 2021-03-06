package com.baidu.statistics.dataapi.svc;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import com.baidu.statistics.dataapi.core.HolmesResponse;
import com.baidu.statistics.dataapi.core.ResHeader;
import com.baidu.statistics.dataapi.om.profile.GetsitesResponse;
import com.baidu.statistics.dataapi.om.report.GetStatusParameter;
import com.baidu.statistics.dataapi.om.report.GetStatusResponse;
import com.baidu.statistics.dataapi.om.report.GetStatusResult;
import com.baidu.statistics.dataapi.om.report.QueryParameter;
import com.baidu.statistics.dataapi.om.report.QueryResponse;
import com.baidu.statistics.login.om.DoLoginResponse;
import com.baidu.statistics.login.svc.BaseLoginTest;

public class BaseApiTest {

	private static Integer ucid;
	private static String st;
	
	private static BaseLoginTest login;
	
	protected static Profile profile;
	protected static Report report;
	
	@BeforeClass
	public static void login() throws Exception {
		login = new BaseLoginTest();
		
		DoLoginResponse retData = login.doLogin();
		Assert.assertNotNull(retData);
		
		ucid = retData.getUcid();
		st = retData.getSt();
		profile = new Profile(ucid, st);
		report = new Report(ucid, st);
	}
	
	@AfterClass
	public static void logout() throws Exception {
		boolean ret = login.doLogout(ucid, st);
		Assert.assertSame(ret, true);
	}
	
	public GetsitesResponse getSites() throws Exception {
		HolmesResponse<GetsitesResponse> sitesInfo = profile.getSites();
		GetsitesResponse sites = sitesInfo.getBody();
		if (sites == null || sites.getSites().size() <= 0) {
			System.out.println("getSites failed!");
			return null;
		}
		System.out.println("getSites successfully!");
		return sites;
	}
	
	public HolmesResponse<QueryResponse> query() throws Exception {
		GetsitesResponse sitesResponse = getSites();
		if (sitesResponse == null) {
			return null;
		}
		QueryParameter queryParam = new QueryParameter();
		queryParam.setReportid(QueryParameter.REPROTID_PAGEVIEW);
		queryParam.setMetrics(Arrays.asList("pageviews", "visitors", "ips", "entrances", "outwards", "exits", "stayTime", "exitRate"));
		queryParam.setDimensions(Arrays.asList("pageid"));
		queryParam.setStart_time("20151004000000");
		queryParam.setEnd_time("20151101235959");
		queryParam.setFilters(new ArrayList<String>());
		queryParam.setStart_index(0);
		queryParam.setMax_results(10);
		queryParam.setSort(Arrays.asList("pageviews desc"));
		queryParam.setSiteid(sitesResponse.getSites().get(0).getSiteid());
		HolmesResponse<QueryResponse> response = report.query(queryParam);
		
		ResHeader header = response.getHeader();
		QueryResponse queryRetData = response.getBody();
		if (header == null || queryRetData == null) {
			System.out.println("query failed!");
			return null;
		}
		if (header.getStatus() != 0 || StringUtils.isBlank(queryRetData.getQuery().getResult_id())) {
			System.out.println("data error, query failed!");
			return null;
		}
		System.out.println("query successfully!");
		return response;
	}
	
	public GetStatusResponse getStatus() throws Exception {
		HolmesResponse<QueryResponse> queryResponse = this.query();
		if (queryResponse == null || queryResponse.getBody() == null) {
			return null;
		}
		/**
		 * 由于产生报告需要一定的时间，我们采用异步的方式对报告进行处理。您首先需要通过 query()，
		 * 然后可以调用 getstatus()方法查询报告生成状态
		 */
		for (int i = 0; i < 6; i++) {
			System.out.println("[notice] sleep, wait for generating result");
			Thread.sleep(5 * 1000);
		}
		HolmesResponse<GetStatusResponse> response = report.getStatus(
				new GetStatusParameter(queryResponse.getBody().getQuery().getResult_id()));
		GetStatusResponse retData = response.getBody();
		if (retData == null) {
			System.out.println("getStatus failed");
			return null;
		}
	
		Integer status = retData.getResult().getStatus();
		if (status == null || status == GetStatusResult.STATUS_INVALID ) {
			System.out.println("status invalid");
			return null;
		} else if (status == GetStatusResult.STATUS_GENERATIN) {
			System.out.println("status generating");
			return null;
		} else if (status == GetStatusResult.STATUS_FAILED ) {
			System.out.println("status failed");
			return null;
		} else if (status == GetStatusResult.STATUS_SUCCESS ) {
			System.out.println("getStatus success");
			return retData;
		} else {
			System.out.println("unkown status");
			return null;
		}
	}
}
