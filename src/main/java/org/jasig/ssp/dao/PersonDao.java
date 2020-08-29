/**
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.ssp.dao;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.criterion.*;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.JoinType;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.jasig.ssp.model.ObjectStatus;
import org.jasig.ssp.model.Person;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.transferobject.CoachPersonLiteTO;
import org.jasig.ssp.transferobject.PersonTO;
import org.jasig.ssp.transferobject.reports.BaseStudentReportTO;
import org.jasig.ssp.transferobject.reports.DisabilityServicesReportTO;
import org.jasig.ssp.transferobject.reports.JournalStepSearchFormTO;
import org.jasig.ssp.transferobject.reports.PersonSearchFormTO;
import org.jasig.ssp.util.hibernate.BatchProcessor;
import org.jasig.ssp.util.hibernate.NamespacedAliasToBeanResultTransformer;
import org.jasig.ssp.util.sort.PagingWrapper;
import org.jasig.ssp.util.sort.SortingAndPaging;
import org.jasig.ssp.web.api.validation.ValidationException;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * CRUD methods for the Person model.
 * <p>
 * Default sort order is <code>lastName</code> then <code>firstName</code>.
 */
@Repository
public class PersonDao extends AbstractAuditableCrudDao<Person> implements AuditableCrudDao<Person> {

	/**
	 * Constructor
	 */
	public PersonDao() {
		super(Person.class);
	}

	public Person create(final Person obj) {
		final Person person = super.save(obj);
		sessionFactory.getCurrentSession().flush();
		return person;
	}

	/**
	 * Return all entities in the database, filtered only by the specified
	 * parameters. Sorted by <code>lastName</code> then <code>firstName</code>.
	 * 
	 * @param status
	 *            Object status filter
	 * @return All entities in the database, filtered only by the specified
	 *         parameters.
	 */
	@Override
	public PagingWrapper<Person> getAll(final ObjectStatus status) {
		return getAll(new SortingAndPaging(status));
	}

	@Override
	@SuppressWarnings(UNCHECKED)
	public PagingWrapper<Person> getAll(final SortingAndPaging sAndP) {

		Criteria criteria = createCriteria();
		sAndP.addStatusFilterToCriteria(criteria);
		
		final Long totalRows = (Long) criteria.setProjection(Projections.rowCount())
				.uniqueResult();

		criteria.setProjection(null);

		if ( sAndP == null || !(sAndP.isSorted())) {
			criteria.addOrder(Order.asc("lastName")).addOrder(Order.asc("firstName"));
		} else {
			if ( sAndP.isSorted() ) {
				sAndP.addSortingToCriteria(criteria);
			}
			sAndP.addPagingToCriteria(criteria);
		}

		criteria = createCriteria(sAndP);

		return new PagingWrapper<Person>(totalRows, criteria.list());
	}

	public Person fromUsername(@NotNull final String username) {
		if (!StringUtils.isNotBlank(username)) {
			throw new IllegalArgumentException("username can not be empty.");
		}

		final Dialect dialect = ((SessionFactoryImplementor) sessionFactory).getDialect();
		final Session currentSession = sessionFactory.getCurrentSession();
		final Criteria query = currentSession.createCriteria(Person.class);

		//Sqlserver does a case insensitive string compare so we don't do the string
		//normalization for sqlserver so that it hits index on person table idx_person_username
		if ( dialect instanceof SQLServerDialect) {
			query.add(Restrictions.eq("username", username)).setFlushMode(FlushMode.COMMIT);

            return (Person) query.uniqueResult();
		} else {
			//Postgres has an index on lower(username): idx_func_username_person
			query.add(Restrictions.eq("username", StringUtils.lowerCase(username))).setFlushMode(FlushMode.COMMIT);
		}

		return (Person) query.uniqueResult();
	}

	@SuppressWarnings(UNCHECKED)
	public List<Person> getPeopleInList(@NotNull final List<UUID> personIds, final SortingAndPaging sAndP)
                                                                                            throws ValidationException {
		if ((personIds == null) || personIds.isEmpty()) {
			throw new ValidationException("Missing or empty list of Person identifiers.");
		}

		final BatchProcessor<UUID, Person> processor =  new BatchProcessor<UUID,Person>(personIds, sAndP);

        do {
			final Criteria criteria = createCriteria();
			processor.process(criteria, "id");
			
		} while (processor.moreToProcess());
		
		return processor.getSortedAndPagedResultsAsList();
	}
	
	public void removeFromSession(Person person) {
		sessionFactory.getCurrentSession().evict(person);
	}
	
	public String getSchoolIdForPersonId(UUID personId) {
		final Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("id", personId));
		criteria.setProjection(Projections.property("schoolId"));

        return (String)criteria.uniqueResult();
	}

	/**
	 * Retrieves the specified Person by their school_id.
	 * 
	 * @param schoolId
	 *            Required school identifier for the Person to retrieve. Can not
	 *            be null.
	 * @exception ObjectNotFoundException
	 *                If the supplied identifier does not exist in the database.
	 * @return The specified Person instance.
	 */
	public Person getBySchoolId(final String schoolId) throws ObjectNotFoundException {

		if (!StringUtils.isNotBlank(schoolId)) {
			throw new IllegalArgumentException("schoolId can not be empty.");
		}

		final Person person = (Person) createCriteria().add(Restrictions.eq("schoolId", schoolId)).uniqueResult();

		if (person == null) {
			throw new ObjectNotFoundException("Person not found with schoolId: " + schoolId, Person.class.getName());
		}

		return person;
	}
	
	public Person getByUsername(final String username) throws ObjectNotFoundException {

		if (!StringUtils.isNotBlank(username)) {
			throw new IllegalArgumentException("username can not be empty.");
		}

		final Person person = (Person) createCriteria()
                .add(Restrictions.eq("username", StringUtils.lowerCase(username))).uniqueResult();

		if (person == null) {
			throw new ObjectNotFoundException(
					"Person not found with username: " + username,
					Person.class.getName());
		}

		return person;
	}

	/**
	 * Retrieves a List of People, likely used by the Address Labels Report
	 * 
	 * @param personSearchFormTO
	 *            Search criteria
	 * @param sAndP
	 *            Sorting and paging parameters
	 * @return List of People, filtered appropriately
	 * 
	 * @throws ObjectNotFoundException
	 *             If any referenced data is not found.
	 */
	@SuppressWarnings(UNCHECKED)
	public List<Person> getPeopleByCriteria(final PersonSearchFormTO personSearchFormTO, final SortingAndPaging sAndP)
                                                                                        throws ObjectNotFoundException {
        final Criteria criteria = setBasicSearchCriteria(createCriteria(sAndP),  personSearchFormTO);
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        return criteria.list();
	}

	/**
	 * Retrieves a List of People, likely used by the Address Labels Report
	 * 
	 * @param specialServiceGroups
	 *            Search criteria
	 * @param sAndP
	 *            Sorting and paging parameters.
	 * @return List of People, filtered appropriately
	 * @throws ObjectNotFoundException
	 *             If any referenced data is not found.
	 */
	@SuppressWarnings(UNCHECKED)
	public List<Person> getPeopleBySpecialServices(final List<UUID> specialServiceGroups, final SortingAndPaging sAndP)
			                                                                            throws ObjectNotFoundException {

		final Criteria criteria = createCriteria(sAndP);

		if (specialServiceGroups != null && !specialServiceGroups.isEmpty()) {
			criteria.createAlias("specialServiceGroups", "personSpecialServiceGroups")
                    .add(Restrictions.in("personSpecialServiceGroups.specialServiceGroup.id", specialServiceGroups));
		}

		// don't bring back any non-students, there will likely be a better way to do this later
		criteria.add(Restrictions.isNotNull("studentType"));
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

		return criteria.list();
	}
	
	@SuppressWarnings(UNCHECKED)
	public PagingWrapper<CoachPersonLiteTO> getCoachPersonsLiteByUsernames(final Collection<String> coachUsernames,
                                                                                        final SortingAndPaging sAndP) {

		if (CollectionUtils.isEmpty(coachUsernames)) {
            return new PagingWrapper<CoachPersonLiteTO>(new ArrayList<CoachPersonLiteTO>());
        }

		return getCoachPersonsLiteByUsernames(coachUsernames, sAndP, null);
	}
	
	@SuppressWarnings(UNCHECKED)
	public PagingWrapper<CoachPersonLiteTO> getCoachPersonsLiteByUsernames(final Collection<String> coachUsernames,
                                                           final SortingAndPaging sAndP, final String homeDepartment) {
		
        final List<String> normalizedCoachUsernames = new ArrayList<String>();

		for (String coachUsername : coachUsernames) {
			normalizedCoachUsernames.add(StringUtils.lowerCase(coachUsername)); //Normalize usernames as per SSP-1733
		}
		
		final BatchProcessor<String, CoachPersonLiteTO> processor = new BatchProcessor(normalizedCoachUsernames, sAndP);
		do {
			// ignore department name and office location for now... would
			// require join we know we don't actually need for existing call sites
			final Criteria criteria = createCriteria(sAndP);
			if (homeDepartment != null && homeDepartment.length() > 0) {
				criteria.createAlias("staffDetails", "personStaffDetails")
                        .add(Restrictions.eq("personStaffDetails.departmentName", homeDepartment));
			} else {
				criteria.createAlias("staffDetails", "personStaffDetails", JoinType.LEFT_OUTER_JOIN);
			}

			criteria.createAlias("homeCampus", "campus", JoinType.LEFT_OUTER_JOIN);

			criteria.setProjection(Projections.projectionList()
							.add(Projections.property("id").as("person_id"))
							.add(Projections.property("firstName").as("person_firstName"))
							.add(Projections.property("lastName").as("person_lastName"))
							.add(Projections.property("primaryEmailAddress").as("person_primaryEmailAddress"))
							.add(Projections.property("workPhone").as("person_workPhone"))
							.add(Projections.property("photoUrl").as("person_photoUrl"))
							.add(Projections.property("personStaffDetails.departmentName").as("person_departmentName"))
							.add(Projections.property("personStaffDetails.officeLocation").as("person_officeLocation"))
							.add(Projections.property("campus.name").as("person_homeCampusName")))
					        .setResultTransformer(new
                                NamespacedAliasToBeanResultTransformer(CoachPersonLiteTO.class, "person_"));

            processor.process(criteria, "username");

		} while(processor.moreToProcess());
		
		return processor.getSortedAndPagedResults();
	}

	public PagingWrapper<Person> getAllAssignedCoaches(SortingAndPaging sAndP) {
		final DetachedCriteria coach_ids = DetachedCriteria.forClass(Person.class, "coach_ids");
		final ProjectionList projections = Projections.projectionList();

        projections.add(Projections.distinct(Projections.property("coach.id")));
		coach_ids.setProjection(projections);
		coach_ids.add(Restrictions.isNotNull("coach"));

		final Criteria criteria = createCriteria().add(Subqueries.propertiesIn(new String[] {"id"}, coach_ids));

		if ( sAndP != null && sAndP.isFilteredByStatus() ) {
			sAndP.addStatusFilterToCriteria(criteria);
		}

		// item count
		Long totalRows = 0L;
		if ((sAndP != null) && sAndP.isPaged()) {
			totalRows = (Long) criteria.setProjection(Projections.rowCount()).uniqueResult();
		}

		criteria.setProjection(null);

		if ( sAndP == null || !(sAndP.isSorted())) {
			criteria.addOrder(Order.asc("lastName")).addOrder(Order.asc("firstName"));
		} else {
			if ( sAndP.isSorted() ) {
				sAndP.addSortingToCriteria(criteria);
			}
			sAndP.addPagingToCriteria(criteria);
		}

		return new PagingWrapper<Person>(totalRows, criteria.list());
	}

	public PagingWrapper<CoachPersonLiteTO> getAllAssignedCoachesLite(SortingAndPaging sAndP) {
		return  getAllAssignedCoachesLite(sAndP, null);
	}

	public PagingWrapper<CoachPersonLiteTO> getAllAssignedCoachesLite(SortingAndPaging sAndP, String homeDepartment) {

		final DetachedCriteria coach_ids = DetachedCriteria.forClass(Person.class, "coach_ids");
		final ProjectionList projections = Projections.projectionList();

        projections.add(Projections.distinct(Projections.property("coach.id")));
		coach_ids.setProjection(projections);
		coach_ids.add(Restrictions.isNotNull("coach"));

		final Criteria criteria = createCriteria().add(Subqueries.propertiesIn(new String[]{"id"}, coach_ids));

		if ( sAndP != null && sAndP.isFilteredByStatus() ) {
			sAndP.addStatusFilterToCriteria(criteria);
		}
		
		if (homeDepartment != null && homeDepartment.length() >= 0) {
			criteria.createAlias("staffDetails", "personStaffDetails");
			criteria.add(Restrictions.eq("personStaffDetails.departmentName", homeDepartment));
		} else {
			criteria.createAlias("staffDetails", "personStaffDetails", JoinType.LEFT_OUTER_JOIN);
		}

		criteria.createAlias("homeCampus", "campus", JoinType.LEFT_OUTER_JOIN);

		// item count
		Long totalRows = 0L;
		if ((sAndP != null) && sAndP.isPaged()) {
			totalRows = (Long) criteria.setProjection(Projections.rowCount()).uniqueResult();
		}

		criteria.setProjection(null);
		criteria.setProjection(Projections.projectionList()
					.add(Projections.property("id").as("person_id"))
					.add(Projections.property("firstName").as("person_firstName"))
					.add(Projections.property("lastName").as("person_lastName"))
					.add(Projections.property("primaryEmailAddress").as("person_primaryEmailAddress"))
					.add(Projections.property("workPhone").as("person_workPhone"))
					.add(Projections.property("personStaffDetails.departmentName").as("person_departmentName"))
					.add(Projections.property("campus.name").as("person_homeCampusName")))
				    .setResultTransformer(
						new NamespacedAliasToBeanResultTransformer(CoachPersonLiteTO.class, "person_"));

		return new PagingWrapper<CoachPersonLiteTO>(totalRows, criteria.list());
	}
	
	private Boolean setCoachAlias(Criteria criteria, String alias, Boolean created) {
		if (created.equals(true)) {
            return created;
        }

        criteria.createAlias("coach", alias);

        return true;
	}

	protected Criteria setBasicSearchCriteria(Criteria criteria, final PersonSearchFormTO personSearchTO) {
		Boolean coachCriteriaCreated = false;

        if (personSearchTO.getCoach() != null && personSearchTO.getCoach().getId() != null) {
			coachCriteriaCreated = setCoachAlias( criteria,  "c", coachCriteriaCreated);

			// Not 100% clear whether this should or shouldn't include a coach.objectStatus = ACTIVE
			// filter. Could argue it's a direct association so the status of coach shouldn't matter.
			// But it's an operational->operational association (not operational->reference), which
			// is a little bit different, semantically. E.g. studenttype.objectStatus doesn't matter
			// for person->studenttype, but journalentry.status does matter for person->journalentry.
			// In this case we've chosen not to filter since for persons in particular, soft-deletion
			// is strongly discouraged since ~2.4.0 since it leads to strange UI behaviors.
			criteria.add(Restrictions.eq("c.id", personSearchTO.getCoach().getId()));
		}

		if (personSearchTO.getWatcher() != null && personSearchTO.getWatcher().getId() != null) {
			criteria.createCriteria("watchers")
                .add(Restrictions.eq("person.id", personSearchTO.getWatcher().getId()))
                .add(Restrictions.eq("objectStatus", ObjectStatus.ACTIVE));
		}		

		if (personSearchTO.getHomeDepartment() != null && personSearchTO.getHomeDepartment().length() > 0) {
			coachCriteriaCreated = setCoachAlias( criteria,  "c", coachCriteriaCreated);

			// As with coaches, it's not 100% clear whether or not this should have a objectstatus filter.
			// If staffdetails were a reference type, we'd ignore its status. But, as noted above, the
			// status of directly associated operational types usually *does* matter. But in this
			// case the API and UI effectively ignore staffdetails.objectstatus (e.g. even if
			// coach.staffdetails.objectstatus=2, the coach's department will still render in the
			// caseload add/edit form). So we ignore it here as well.
			criteria.createAlias("c.staffDetails", "coachStaffDetails");
			criteria.add(Restrictions.eq("coachStaffDetails.departmentName", personSearchTO.getHomeDepartment()));
		}

		criteria.createAlias("programStatuses", "personProgramStatuses");
		criteria.add(Restrictions.isNull("personProgramStatuses.expirationDate"));

        if (personSearchTO.getProgramStatus() != null) {
			criteria.add(Restrictions.eq("personProgramStatuses.programStatus.id", personSearchTO.getProgramStatus()));
		}
		
		if (personSearchTO.getSpecialServiceGroupIds() != null) {
			criteria.createAlias("specialServiceGroups", "personSpecialServiceGroups");
			criteria.add(Restrictions.in("personSpecialServiceGroups.specialServiceGroup.id",
                                                                        personSearchTO.getSpecialServiceGroupIds()));
			criteria.add(Restrictions.eq("personSpecialServiceGroups.objectStatus", ObjectStatus.ACTIVE));

		} else if (personSearchTO.getSpecialServiceGroupRequired()) {
			/* Makes sure that at least one special service group has an active status */
			criteria.createAlias("specialServiceGroups", "personSpecialServiceGroups");
			criteria.add(Restrictions.eq("personSpecialServiceGroups.objectStatus", ObjectStatus.ACTIVE));
		}

		if (personSearchTO.getReferralSourcesIds() != null) {
			criteria.createAlias("referralSources", "personReferralSources")
					.add(Restrictions.in("personReferralSources.referralSource.id",
							personSearchTO.getReferralSourcesIds()))
					.add(Restrictions.eq("personReferralSources.objectStatus", ObjectStatus.ACTIVE));
		}

		if (personSearchTO.getAnticipatedStartTerm() != null && personSearchTO.getAnticipatedStartTerm().length() > 0) {
			criteria.add(Restrictions.eq("anticipatedStartTerm", personSearchTO.getAnticipatedStartTerm()).ignoreCase());
		}

		if (personSearchTO.getAnticipatedStartYear() != null) {
			criteria.add(Restrictions.eq("anticipatedStartYear", personSearchTO.getAnticipatedStartYear()));
		}
		
		if (personSearchTO.getActualStartTerms() != null  && personSearchTO.getActualStartTerms().size() > 0) {
			criteria.add(Restrictions.in("actualStartTerm", personSearchTO.getActualStartTerms()));
		}

		if (personSearchTO.getActualStartYear() != null) {
			criteria.add(Restrictions.eq("actualStartYear", personSearchTO.getActualStartYear()));
		}

		if (personSearchTO.getStudentTypeIds() != null) {
			criteria.add(Restrictions.in("studentType.id", personSearchTO.getStudentTypeIds()));
		}

		if (personSearchTO.getHomeCampusIds() != null) {
			criteria.add(Restrictions.in("homeCampus.id", personSearchTO.getHomeCampusIds()));
		}

		if (personSearchTO.getCreateDateFrom() != null) {
			criteria.add(Restrictions.ge("createdDate", personSearchTO.getCreateDateFrom()));
		}

		if (personSearchTO.getCreateDateTo() != null) {
			criteria.add(Restrictions.le("createdDate", personSearchTO.getCreateDateTo()));
		}
		
		if (personSearchTO.getStudentIntakeCompleteDateFrom() != null) {
			criteria.add(Restrictions.ge("studentIntakeCompleteDate", personSearchTO.getStudentIntakeCompleteDateFrom()));
		}
		
		if (personSearchTO.getStudentIntakeCompleteDateTo() != null) {
			criteria.add(Restrictions.le("studentIntakeCompleteDate", personSearchTO.getStudentIntakeCompleteDateTo()));
		}
		
		if (personSearchTO.getDisabilityIsNotNull() != null && personSearchTO.getDisabilityIsNotNull() == true) {
			// Not filtering on object status here b/c technically the person->persondisability association is
			// direct and thus the status of the associated object (persondisability) doesn't matter
			criteria.createAlias("disability", "personDisability");
			criteria.add(Restrictions.isNotNull("personDisability.id"));
		}
		
		if (personSearchTO.getDisabilityStatusId() != null) {
			if (personSearchTO.getDisabilityIsNotNull() == null || personSearchTO.getDisabilityIsNotNull() == false) {
                criteria.createAlias("disability", "personDisability");
            }
			criteria.add(Restrictions.eq("personDisability.disabilityStatus.id", personSearchTO.getDisabilityStatusId()));

			// Direct operational-to-operational relationship, so probably should have a persondisability.objectstatus
			// filter, but the UI completely disregards that field, so have decided against adding such a filter here.
		}
		
		if (personSearchTO.getDisabilityTypeId() != null) {
			criteria.createAlias("disabilityTypes", "personDisabilityTypes").add(Restrictions.eq(
					"personDisabilityTypes.disabilityType.id", personSearchTO.getDisabilityTypeId()))
			.add(Restrictions.eq("personDisabilityTypes.objectStatus", ObjectStatus.ACTIVE));
		}
		
		if (personSearchTO.getServiceReasonsIds() != null && personSearchTO.getServiceReasonsIds().size() > 0) {
			criteria.createAlias("serviceReasons", "serviceReasons");
			criteria.createAlias("serviceReasons.serviceReason", "serviceReason");
			criteria.add(Restrictions.in("serviceReason.id", personSearchTO.getServiceReasonsIds()));
			criteria.add(Restrictions.eq("serviceReasons.objectStatus", ObjectStatus.ACTIVE));
		}

		// don't bring back any non-students, there will likely be a better way
		// to do this later
		//criteria.add(Restrictions.isNotNull("studentType"));
        Criterion notNullStudentType =  Restrictions.isNotNull("studentType");
        Criterion notNullCoach = Restrictions.isNotNull("coach");
		Criterion notNullProgramStatus = Restrictions.isNotEmpty("programStatuses");
		criteria.add(Restrictions.or(notNullStudentType, Restrictions.and(notNullCoach, notNullProgramStatus)));

        return criteria;
	}
	
	@SuppressWarnings("unchecked")
	public List<UUID> getStudentUUIDs(PersonSearchFormTO form){
		final Criteria criteria = setBasicSearchCriteria(createCriteria(),  form);
		criteria.setProjection(Projections.distinct(Projections.property("id")));
		
		return (List<UUID>)criteria.list();
	}
	
	@SuppressWarnings("unchecked")
	public PagingWrapper<BaseStudentReportTO> getStudentReportTOs(PersonSearchFormTO form, final SortingAndPaging sAndP)
                                                                                        throws ObjectNotFoundException {
		final List<UUID> ids = getStudentUUIDs(form);

        if (ids.size() == 0) {
            return null;
        }

		final BatchProcessor<UUID, BaseStudentReportTO> processor =  new BatchProcessor<UUID,BaseStudentReportTO>(ids, sAndP);
		do {
			final Criteria criteria = createCriteria();
								
			final ProjectionList projections = Projections.projectionList();
		
			criteria.setProjection(projections);

			addBasicStudentProperties(projections, criteria, sAndP.getStatus());

			criteria.setResultTransformer(new AliasToBeanResultTransformer(BaseStudentReportTO.class));
            processor.process(criteria, "id");
			
		} while(processor.moreToProcess());

		return new PagingWrapper<BaseStudentReportTO>(ids.size(), processor.getSortedAndPagedResultsAsList());
	}
	
	@SuppressWarnings("unchecked")
	public PagingWrapper<DisabilityServicesReportTO> getDisabilityReport(PersonSearchFormTO form,
                                                         final SortingAndPaging sAndP) throws ObjectNotFoundException {
		final List<UUID> ids = getStudentUUIDs(form);

        if (ids.size() == 0) {
            return null;
        }

        final BatchProcessor<UUID, DisabilityServicesReportTO> processor =  new BatchProcessor<UUID,DisabilityServicesReportTO>(ids, sAndP);
		do {
			final Criteria criteria = createCriteria();
			
			// don't bring back any non-students, there will likely be a better way
			// to do this later
			final ProjectionList projections = Projections.projectionList();
			criteria.setProjection(projections);
			addBasicStudentProperties(projections, criteria, sAndP.getStatus());
			
			final Criteria demographics = criteria.createAlias("demographics", "demographics", JoinType.LEFT_OUTER_JOIN);
			demographics.createAlias("demographics.ethnicity", "ethnicity", JoinType.LEFT_OUTER_JOIN);
			demographics.createAlias("demographics.race", "race", JoinType.LEFT_OUTER_JOIN);
			demographics.createAlias("demographics.veteranStatus", "veteranStatus", JoinType.LEFT_OUTER_JOIN);
			
			criteria.createAlias("disabilityAgencies", "disabilityAgencies", JoinType.LEFT_OUTER_JOIN);
			
			criteria.createAlias("disabilityAgencies.disabilityAgency", "disabilityAgency", JoinType.LEFT_OUTER_JOIN);
			
			criteria.createAlias("disabilityTypes", "personDisabilityTypes", JoinType.LEFT_OUTER_JOIN);
			
			criteria.createAlias("personDisabilityTypes.disabilityType", "disabilityType", JoinType.LEFT_OUTER_JOIN);
			criteria.createAlias("disability", "personDisability");
			
			
			criteria.createAlias("personDisability.disabilityStatus", "disabilityStatus", JoinType.LEFT_OUTER_JOIN);
			
			criteria.createAlias("educationGoal", "educationGoal", JoinType.LEFT_OUTER_JOIN);
			
			final Dialect dialect = ((SessionFactoryImplementor) sessionFactory).getDialect();
			if ( dialect instanceof SQLServerDialect) {
	
				projections.add(Projections.groupProperty("ethnicity.name").as("ethnicity"));
				projections.add(Projections.groupProperty("race.name").as("race"));
				projections.add(Projections.groupProperty("veteranStatus.name").as("veteranStatus"));
				projections.add(Projections.groupProperty("disabilityAgency.name").as("disabilityAgencyName"));
				projections.add(Projections.groupProperty("disabilityType.name").as("disabilityType"));
				projections.add(Projections.groupProperty("disabilityAgency.createdDate").as("disabilityAgencyCreatedDate"));
				projections.add(Projections.groupProperty("educationGoal.plannedMajor").as("major"));
				projections.add(Projections.groupProperty("disabilityStatus.name").as("odsStatus"));
				projections.add(Projections.groupProperty("personDisability.createdDate").as("odsRegistrationDate"));
				projections.add(Projections.groupProperty("personDisability.noDocumentation").as("noDocumentation"));
				projections.add(Projections.groupProperty("personDisability.inadequateDocumentation").as("inadequateDocumentation"));
				projections.add(Projections.groupProperty("personDisability.noDisability").as("noDisability"));
				projections.add(Projections.groupProperty("personDisability.noSpecialEd").as("noSpecialEd"));
			} else {
				projections.add(Projections.groupProperty("ethnicity.name").as("ethnicity"));
				projections.add(Projections.groupProperty("race.name").as("race"));
				projections.add(Projections.groupProperty("veteranStatus.name").as("veteranStatus"));
				projections.add(Projections.groupProperty("disabilityType.name").as("disabilityType"));
				projections.add(Projections.groupProperty("disabilityAgency.name").as("disabilityAgencyName"));
				projections.add(Projections.groupProperty("disabilityAgency.createdDate").as("disabilityAgencyCreatedDate"));
				projections.add(Projections.groupProperty("educationGoal.plannedMajor").as("major"));
				projections.add(Projections.groupProperty("disabilityStatus.name").as("odsStatus"));
				projections.add(Projections.groupProperty("personDisability.createdDate").as("odsRegistrationDate"));
				projections.add(Projections.groupProperty("personDisability.noDocumentation").as("noDocumentation"));
				projections.add(Projections.groupProperty("personDisability.inadequateDocumentation").as("inadequateDocumentation"));
				projections.add(Projections.groupProperty("personDisability.noDisability").as("noDisability"));
				projections.add(Projections.groupProperty("personDisability.noSpecialEd").as("noSpecialEd"));
			}
			
			criteria.setResultTransformer(new AliasToBeanResultTransformer(DisabilityServicesReportTO.class));
			processor.process(criteria, "id");

		} while (processor.moreToProcess());

		return new PagingWrapper<DisabilityServicesReportTO>(ids.size(), processor.getSortedAndPagedResultsAsList());
	}
	
	private ProjectionList addBasicStudentProperties(ProjectionList projections, Criteria criteria, ObjectStatus status) {
		
		projections.add(Projections.groupProperty("firstName").as("firstName"));
		projections.add(Projections.groupProperty("middleName").as("middleName"));
		projections.add(Projections.groupProperty("lastName").as("lastName"));
		projections.add(Projections.groupProperty("schoolId").as("schoolId"));
		projections.add(Projections.groupProperty("primaryEmailAddress").as("primaryEmailAddress"));
		projections.add(Projections.groupProperty("secondaryEmailAddress").as("secondaryEmailAddress"));
        projections.add(Projections.groupProperty("alternatePhone").as("alternatePhone"));
        projections.add(Projections.groupProperty("workPhone").as("workPhone"));
        projections.add(Projections.groupProperty("cellPhone").as("cellPhone"));
		projections.add(Projections.groupProperty("homePhone").as("homePhone"));
		projections.add(Projections.groupProperty("addressLine1").as("addressLine1"));
		projections.add(Projections.groupProperty("addressLine2").as("addressLine2"));
		projections.add(Projections.groupProperty("city").as("city"));
		projections.add(Projections.groupProperty("state").as("state"));
		projections.add(Projections.groupProperty("zipCode").as("zipCode"));
		projections.add(Projections.groupProperty("actualStartTerm").as("actualStartTerm"));
		projections.add(Projections.groupProperty("actualStartYear").as("actualStartYear"));
		projections.add(Projections.groupProperty("createdDate").as("createdDate"));
		projections.add(Projections.groupProperty("id").as("id"));
		
		criteria.createAlias("programStatuses", "personProgramStatuses", JoinType.LEFT_OUTER_JOIN);
		criteria.add(Restrictions.isNull("personProgramStatuses.expirationDate"));
		criteria.createAlias("specialServiceGroups", "personSpecialServiceGroups", JoinType.LEFT_OUTER_JOIN);
		criteria.createAlias("personSpecialServiceGroups.specialServiceGroup", "specialServiceGroup", JoinType.LEFT_OUTER_JOIN);
		
		projections.add(Projections.groupProperty("specialServiceGroup.id").as("specialServiceGroupId"));
		projections.add(Projections.groupProperty("specialServiceGroup.name").as("specialServiceGroupName"));
		projections.add(Projections.groupProperty("personSpecialServiceGroups.objectStatus").as("specialServiceGroupAssocObjectStatus"));
		criteria.createAlias("personProgramStatuses.programStatus", "programStatus", JoinType.LEFT_OUTER_JOIN);
		
		projections.add(Projections.groupProperty("programStatus.name").as("programStatusName"));
		projections.add(Projections.groupProperty("programStatus.id").as("programStatusId"));
		projections.add(Projections.groupProperty("personProgramStatuses.expirationDate").as("programStatusExpirationDate"));
	
		// Join to Student Type
		criteria.createAlias("studentType", "studentType", JoinType.LEFT_OUTER_JOIN);
		// add StudentTypeName Column
		projections.add(Projections.groupProperty("studentType.name").as("studentTypeName"));
		projections.add(Projections.groupProperty("studentType.code").as("studentTypeCode"));

		// Join to Student Type
		criteria.createAlias("homeCampus", "campus", JoinType.LEFT_OUTER_JOIN);
		// add StudentTypeName Column
		projections.add(Projections.groupProperty("campus.name").as("homeCampusName"));
		projections.add(Projections.groupProperty("campus.code").as("homeCampusCode"));

		criteria.createAlias("coach", "c");
		criteria.createAlias("watchers", "watcher", JoinType.LEFT_OUTER_JOIN);

		final Dialect dialect = ((SessionFactoryImplementor) sessionFactory).getDialect();
		if ( dialect instanceof SQLServerDialect) {
			// sql server requires all these to part of the grouping
			//projections.add(Projections.groupProperty("c.id").as("coachId"));
			projections.add(Projections.groupProperty("c.lastName").as("coachLastName"))
					.add(Projections.groupProperty("c.firstName").as("coachFirstName"))
					.add(Projections.groupProperty("c.middleName").as("coachMiddleName"))
					.add(Projections.groupProperty("c.schoolId").as("coachSchoolId"))
					.add(Projections.groupProperty("c.username").as("coachUsername"));
		} else {
			// other dbs (postgres) don't need these in the grouping
			//projections.add(Projections.property("c.id").as("coachId"));
			projections.add(Projections.groupProperty("c.lastName").as("coachLastName"))
					.add(Projections.groupProperty("c.firstName").as("coachFirstName"))
					.add(Projections.groupProperty("c.middleName").as("coachMiddleName"))
					.add(Projections.groupProperty("c.schoolId").as("coachSchoolId"))
					.add(Projections.groupProperty("c.username").as("coachUsername"));
		}

		return projections;
	}

	public UUID getCoachIdForStudent(PersonTO obj) {
		final String query = "select p.coach.id from Person p where p = :person";
		final Object coachId =  createHqlQuery(query).setEntity("person", new Person(obj.getId())).uniqueResult();

        UUID coachIdUUID = null;
		if (coachId != null) {
			coachIdUUID = (UUID) coachId;
		}
			
		return coachIdUUID;
	}

  public Optional<PagingWrapper<BaseStudentReportTO>> getStudentReportTOsSortedByLastNameDesc(JournalStepSearchFormTO personSearchForm) throws ObjectNotFoundException {
		return Optional.ofNullable(this.getStudentReportTOs(personSearchForm, SortingAndPaging.createForSingleSortAll(ObjectStatus.ACTIVE, "lastName", "DESC")));
  }
}