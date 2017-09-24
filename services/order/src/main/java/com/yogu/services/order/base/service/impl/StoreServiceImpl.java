package com.yogu.services.order.base.service.impl;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yogu.commons.utils.StringUtils;
import com.yogu.commons.utils.VOUtil;
import com.yogu.core.web.ParameterUtil;
import com.yogu.language.OrderMessages;
import com.yogu.services.order.base.dao.StoreDao;
import com.yogu.services.order.base.dto.Store;
import com.yogu.services.order.base.entry.StorePO;
import com.yogu.services.order.base.service.StoreService;


/**
 * StoreService 的实现类
 * 
 * @author JFan 2015-07-13
 */
@Named
public class StoreServiceImpl implements StoreService {

	private static final Logger logger = LoggerFactory.getLogger(StoreServiceImpl.class);

	@Inject
	private StoreDao dao;

	@Override
	public void save(Store dto) {
		StorePO po = dto2po(dto);
		dao.save(po);
	}

	// ####
	// ## private func
	// ####

	public Store po2dto(StorePO po) {
		return VOUtil.from(po, Store.class);
	}

	public StorePO dto2po(Store dto) {
		return VOUtil.from(dto, StorePO.class);
	}

	@Override
	public Store getByStoreNo(String storeNo){
		if(StringUtils.isBlank(storeNo)){
			return null;
		}
		StorePO store = dao.getByStoreNo(storeNo);
		if(null != store){
			return po2dto(store);
		}
		
		return null;
	}


}
