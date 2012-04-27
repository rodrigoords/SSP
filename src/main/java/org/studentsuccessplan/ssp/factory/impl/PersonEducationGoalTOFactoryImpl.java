package org.studentsuccessplan.ssp.factory.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.studentsuccessplan.ssp.dao.PersonEducationGoalDao;
import org.studentsuccessplan.ssp.factory.AbstractAuditableTOFactory;
import org.studentsuccessplan.ssp.factory.PersonEducationGoalTOFactory;
import org.studentsuccessplan.ssp.model.PersonEducationGoal;
import org.studentsuccessplan.ssp.service.ObjectNotFoundException;
import org.studentsuccessplan.ssp.service.reference.EducationGoalService;
import org.studentsuccessplan.ssp.transferobject.PersonEducationGoalTO;

@Service
@Transactional(readOnly = true)
public class PersonEducationGoalTOFactoryImpl extends
		AbstractAuditableTOFactory<PersonEducationGoalTO, PersonEducationGoal>
		implements PersonEducationGoalTOFactory {

	public PersonEducationGoalTOFactoryImpl() {
		super(PersonEducationGoalTO.class, PersonEducationGoal.class);
	}

	@Autowired
	private transient PersonEducationGoalDao dao;

	@Autowired
	private transient EducationGoalService educationGoalService;

	@Override
	protected PersonEducationGoalDao getDao() {
		return dao;
	}

	@Override
	public PersonEducationGoal from(final PersonEducationGoalTO tObject)
			throws ObjectNotFoundException {
		final PersonEducationGoal model = super.from(tObject);

		model.setHowSureAboutMajor(tObject.getHowSureAboutMajor());
		model.setDescription(tObject.getDescription());
		model.setPlannedOccupation(tObject.getPlannedOccupation());
		if (tObject.getEducationGoalId() != null) {
			model.setEducationGoal(educationGoalService.get(tObject
					.getEducationGoalId()));
		}

		return model;
	}
}
