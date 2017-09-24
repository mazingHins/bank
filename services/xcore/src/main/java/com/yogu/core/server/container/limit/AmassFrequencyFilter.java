/**
 * 
 */
package com.yogu.core.server.container.limit;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yogu.commons.utils.ThreadLocalContext;
import com.yogu.core.base.BaseParams;
import com.yogu.core.server.annotation.FrequencyLimitation;
import com.yogu.core.server.annotation.FrequencyLimitation.FrequencyKey;
import com.yogu.core.web.ResultCode;
import com.yogu.core.web.context.SecurityContext;
import com.yogu.core.web.exception.ServiceException;

/**
 * 访问平率检测 <br>
 * 基于集中式cache，也就是集群内统一限制
 * 
 * @author JFan 2015年7月17日 下午2:14:07
 */
@Priority(Priorities.AUTHORIZATION)
public class AmassFrequencyFilter implements ContainerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(FrequencyAnnoFeature.class);

	private FrequencyKey key;
	private String module;
	private int second;
	private int num;
	private String visitFlag;

	public AmassFrequencyFilter(int num, int second, FrequencyKey key, String visitFlag) {
		this.second = second;
		this.num = num;
		this.key = key;
		this.visitFlag = visitFlag;
		this.module = key.toString();
	}

	/*
	 * （非 Javadoc）
	 * 
	 * @see javax.ws.rs.container.ContainerRequestFilter#filter(javax.ws.rs.container.ContainerRequestContext)
	 */
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		if (0 >= num) {
			logger.warn("core#frequency | num incorrect setting | module: {}, second: {}, num: {}", module, second, num);
			return;
		}

		BaseParams baseParams = SecurityContext.getBaseParams();
		if (null == baseParams)
			throw new ServiceException(ResultCode.FREQUENCY_OVERLOAD, "not found baseParams.");

		String source = getSource(key, baseParams);
		cannot(num, second, module, source, visitFlag);
	}

	/**
	 * 请求来源
	 */
	private String getSource(FrequencyKey key, BaseParams baseParams) {
		if (FrequencyKey.IP == key)
			// return RequestInfo.getRequestIp(request);
			return ThreadLocalContext.getThreadValue(ThreadLocalContext.REQ_CLIENT_IP);
		if (FrequencyKey.UTOKEN == key)
			// return request.getParameter("ut");
			return SecurityContext.getUserToken();

		// if(FrequencyKey.DID == key) def
		// return request.getParameter("did");
		return baseParams.getDid();
	}

	/**
	 * 检测是否仍然能够访问服务<br>
	 * 超出限制，则抛出异常（ResultCode.FREQUENCY_OVERLOAD）
	 * 
	 * @param num 限制的次数
	 * @param second 限制的时间数 (单位秒)
	 * @param module 标识用户的信息(标识用户IP,token,DID 的key {@link FrequencyLimitation.FrequencyKey})
	 * @param source 标识用户的信息 (用户的IP/token/DID)
	 * @param visitFlag 访问的类名与方法名的组合key
	 * @author sky
	 * 
	 */
	private void cannot(int num, int second, String module, String source, String visitFlag) {}

	

}
