package org.jasig.ssp.factory.external.impl;

import org.jasig.ssp.dao.external.ExternalPersonPlanStatusDao;
import org.jasig.ssp.factory.external.ExternalPersonPlanStatusTOFactory;
import org.jasig.ssp.model.external.ExternalCourseRequisite;
import org.jasig.ssp.model.external.ExternalPersonPlanStatus;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.transferobject.external.ExternalPersonPlanStatusTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExternalPersonPlanStatusTOFactoryImpl
		extends
		AbstractExternalDataTOFactory<ExternalPersonPlanStatusTO, ExternalPersonPlanStatus>
		implements ExternalPersonPlanStatusTOFactory {



	public ExternalPersonPlanStatusTOFactoryImpl() {
		super(ExternalPersonPlanStatusTO.class, ExternalPersonPlanStatus.class);
	}
 
	@Autowired
	private transient ExternalPersonPlanStatusDao dao;

	@Override
	protected ExternalPersonPlanStatusDao getDao() {
		return dao;
	}
	
	@Override
	public ExternalPersonPlanStatus from(ExternalPersonPlanStatusTO tObject)
			throws ObjectNotFoundException {
		final ExternalPersonPlanStatus model = super.from(tObject);
		model.setSchoolId(tObject.getSchoolId());
		model.setStatus(tObject.getStatus());
		model.setStatusReason(tObject.getStatusReason());
		return model;
	}
}
