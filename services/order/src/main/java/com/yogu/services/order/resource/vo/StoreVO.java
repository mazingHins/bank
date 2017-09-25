package com.yogu.services.order.resource.vo;

/**
 * 商户信息vo
 *
 * @date 2017年09月23日 下午6:55:32
 * @author qiujie
 */
public class StoreVO {
	
	/** 商户idid */
	private long storeId;

	/** 商户编号 */
	private String storeNo;
	
	/** 商户名称 */
	private String storeName;

	/** 商户预留手机 */
	private String storeMobile;

	public long getStoreId() {
		return storeId;
	}

	public void setStoreId(long storeId) {
		this.storeId = storeId;
	}

	public String getStoreNo() {
		return storeNo;
	}

	public void setStoreNo(String storeNo) {
		this.storeNo = storeNo;
	}

	public String getStoreName() {
		return storeName;
	}

	public void setStoreName(String storeName) {
		this.storeName = storeName;
	}

	public String getStoreMobile() {
		return storeMobile;
	}

	public void setStoreMobile(String storeMobile) {
		this.storeMobile = storeMobile;
	}
	
	
	
}
