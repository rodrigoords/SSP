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
package org.jasig.ssp.service.impl; // NOPMD by jon.adams

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.jasig.ssp.config.EarlyAlertResponseReminderRecipientsConfig;
import org.jasig.ssp.dao.EarlyAlertDao;
import org.jasig.ssp.factory.EarlyAlertSearchResultTOFactory;
import org.jasig.ssp.model.EarlyAlert;
import org.jasig.ssp.model.EarlyAlertRouting;
import org.jasig.ssp.model.EarlyAlertSearchResult;
import org.jasig.ssp.model.Message;
import org.jasig.ssp.model.ObjectStatus;
import org.jasig.ssp.model.Person;
import org.jasig.ssp.model.PersonProgramStatus;
import org.jasig.ssp.model.SubjectAndBody;
import org.jasig.ssp.model.WatchStudent;
import org.jasig.ssp.model.external.FacultyCourse;
import org.jasig.ssp.model.external.Term;
import org.jasig.ssp.model.reference.Campus;
import org.jasig.ssp.model.reference.EarlyAlertReason;
import org.jasig.ssp.model.reference.EarlyAlertSuggestion;
import org.jasig.ssp.model.reference.EnrollmentStatus;
import org.jasig.ssp.model.reference.ProgramStatus;
import org.jasig.ssp.model.reference.StudentType;
import org.jasig.ssp.security.SspUser;
import org.jasig.ssp.service.AbstractPersonAssocAuditableService;
import org.jasig.ssp.service.EarlyAlertRoutingService;
import org.jasig.ssp.service.EarlyAlertService;
import org.jasig.ssp.service.MessageService;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.PersonProgramStatusService;
import org.jasig.ssp.service.PersonService;
import org.jasig.ssp.service.SecurityService;
import org.jasig.ssp.service.external.FacultyCourseService;
import org.jasig.ssp.service.external.TermService;
import org.jasig.ssp.service.reference.ConfigService;
import org.jasig.ssp.service.reference.EarlyAlertReasonService;
import org.jasig.ssp.service.reference.EarlyAlertSuggestionService;
import org.jasig.ssp.service.reference.EnrollmentStatusService;
import org.jasig.ssp.service.reference.MessageTemplateService;
import org.jasig.ssp.service.reference.ProgramStatusService;
import org.jasig.ssp.service.reference.StudentTypeService;
import org.jasig.ssp.transferobject.EarlyAlertSearchResultTO;
import org.jasig.ssp.transferobject.PagedResponse;
import org.jasig.ssp.transferobject.form.EarlyAlertSearchForm;
import org.jasig.ssp.transferobject.messagetemplate.CoachPersonLiteMessageTemplateTO;
import org.jasig.ssp.transferobject.messagetemplate.EarlyAlertMessageTemplateTO;
import org.jasig.ssp.transferobject.reports.EarlyAlertCourseCountsTO;
import org.jasig.ssp.transferobject.reports.EarlyAlertReasonCountsTO;
import org.jasig.ssp.transferobject.reports.EarlyAlertStudentReportTO;
import org.jasig.ssp.transferobject.reports.EarlyAlertStudentSearchTO;
import org.jasig.ssp.transferobject.reports.EntityCountByCoachSearchForm;
import org.jasig.ssp.transferobject.reports.EntityStudentCountByCoachTO;
import org.jasig.ssp.util.DateTimeUtils;
import org.jasig.ssp.util.collections.Pair;
import org.jasig.ssp.util.collections.Triple;
import org.jasig.ssp.util.sort.PagingWrapper;
import org.jasig.ssp.util.sort.SortingAndPaging;
import org.jasig.ssp.web.api.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.*;


/**
 * EarlyAlert service implementation
 * 
 * @author jon.adams
 * 
 */
@Service
@Transactional
public class EarlyAlertServiceImpl extends // NOPMD
		AbstractPersonAssocAuditableService<EarlyAlert>
		implements EarlyAlertService {

	private EarlyAlertDao dao;
	private ConfigService configService;
	private EarlyAlertRoutingService earlyAlertRoutingService;
	private MessageService messageService;
	private MessageTemplateService messageTemplateService;
	private EarlyAlertReasonService earlyAlertReasonService;
	private EarlyAlertSuggestionService earlyAlertSuggestionService;
	private PersonService personService;
	private FacultyCourseService facultyCourseService;
	private TermService termService;
	private PersonProgramStatusService personProgramStatusService;
	private ProgramStatusService programStatusService;
	private StudentTypeService studentTypeService;
	private SecurityService securityService;
	private EarlyAlertSearchResultTOFactory searchResultFactory;
	private EarlyAlertResponseReminderRecipientsConfig earReminderRecipientConfig;
	private EnrollmentStatusService enrollmentStatusService;

	@Autowired
	public EarlyAlertServiceImpl(EarlyAlertDao dao, ConfigService configService, EarlyAlertRoutingService earlyAlertRoutingService, MessageService messageService, MessageTemplateService messageTemplateService, EarlyAlertReasonService earlyAlertReasonService, EarlyAlertSuggestionService earlyAlertSuggestionService, PersonService personService, FacultyCourseService facultyCourseService, TermService termService, PersonProgramStatusService personProgramStatusService, ProgramStatusService programStatusService, StudentTypeService studentTypeService, SecurityService securityService, EarlyAlertSearchResultTOFactory searchResultFactory, EarlyAlertResponseReminderRecipientsConfig earReminderRecipientConfig, EnrollmentStatusService enrollmentStatusService) {
		this.dao = dao;
		this.configService = configService;
		this.earlyAlertRoutingService = earlyAlertRoutingService;
		this.messageService = messageService;
		this.messageTemplateService = messageTemplateService;
		this.earlyAlertReasonService = earlyAlertReasonService;
		this.earlyAlertSuggestionService = earlyAlertSuggestionService;
		this.personService = personService;
		this.facultyCourseService = facultyCourseService;
		this.termService = termService;
		this.personProgramStatusService = personProgramStatusService;
		this.programStatusService = programStatusService;
		this.studentTypeService = studentTypeService;
		this.securityService = securityService;
		this.searchResultFactory = searchResultFactory;
		this.earReminderRecipientConfig = earReminderRecipientConfig;
		this.enrollmentStatusService = enrollmentStatusService;
	}

	private static final Logger LOGGER = LoggerFactory
			.getLogger(EarlyAlertServiceImpl.class);

	@Override
	protected EarlyAlertDao getDao() {
		return dao;
	}

	@Override
	@Transactional(rollbackFor = { ObjectNotFoundException.class, ValidationException.class })
	public EarlyAlert create(@NotNull final EarlyAlert earlyAlert)
			throws ObjectNotFoundException, ValidationException {
		// Validate objects

		earlyAlert.getPersonOptional().orElseThrow(() -> new ValidationException("EarlyAlert Student data must be provided."));

		final Person student = earlyAlert.getPerson();

		// Figure student advisor or early alert coordinator
		final UUID assignedAdvisor = getEarlyAlertAdvisor(earlyAlert).orElseThrow(() -> new ValidationException("Could not determine the Early Alert Advisor for student ID " + student.getId()));

		if (Objects.isNull(student.getCoach()) || assignedAdvisor.equals(student.getCoach().getId())) {
			student.setCoach(personService.get(assignedAdvisor));
		}

		ensureValidAlertedOnPersonStateNoFail(student);

		// Create alert
		final EarlyAlert saved = getDao().save(earlyAlert);

		// Send e-mail to assigned advisor (coach)
		try {
			sendMessageToAdvisor(saved, earlyAlert.getEmailCC());
		} catch (final Exception e) {
			LOGGER.warn("Could not send Early Alert message to advisor.", e);
			throw new ValidationException("Early Alert notification e-mail could not be sent to advisor. Early Alert was NOT created.", e);
		}

		// Send e-mail CONFIRMATION to faculty
		try {
			sendConfirmationMessageToFaculty(saved);
		} catch (final Exception e) {
			LOGGER.warn("Could not send Early Alert confirmation to faculty.", e);
			throw new ValidationException("Early Alert confirmation e-mail could not be sent. Early Alert was NOT created.", e);
		}

		return saved;
	}

	@Override
	public void closeEarlyAlert(UUID earlyAlertId)
			throws ObjectNotFoundException, ValidationException {
		final EarlyAlert earlyAlert = getDao().get(earlyAlertId);

		// DAOs don't implement ObjectNotFoundException consistently and we'd
		// rather they not implement it at all, so a small attempt at 'future
		// proofing' here
		if ( earlyAlert == null ) {
			throw new ObjectNotFoundException(earlyAlertId, EarlyAlert.class.getName());
		}

		if ( earlyAlert.getClosedDate() != null ) {
			// already closed
			return;
		}

		final SspUser sspUser = securityService.currentUser();
		if ( sspUser == null ) {
			throw new ValidationException("Early Alert cannot be closed by a null User.");
		}

		earlyAlert.setClosedDate(new Date());
		earlyAlert.setClosedBy(sspUser.getPerson());

		// This save will result in a Hib session flush, which works fine with
		// our current usage. Future use cases might prefer to delay the
		// flush and we can address that when the time comes. Might not even
		// need to change anything here if it turns out nothing actually
		// *depends* on the flush.
		getDao().save(earlyAlert);
	}
	
	@Override
	public void openEarlyAlert(UUID earlyAlertId)
			throws ObjectNotFoundException, ValidationException {
		final EarlyAlert earlyAlert = getDao().get(earlyAlertId);

		// DAOs don't implement ObjectNotFoundException consistently and we'd
		// rather they not implement it at all, so a small attempt at 'future
		// proofing' here
		if ( earlyAlert == null ) {
			throw new ObjectNotFoundException(earlyAlertId, EarlyAlert.class.getName());
		}

		if ( earlyAlert.getClosedDate() == null ) {
			return;
		}

		final SspUser sspUser = securityService.currentUser();
		if ( sspUser == null ) {
			throw new ValidationException("Early Alert cannot be closed by a null User.");
		}

		earlyAlert.setClosedDate(null);
		earlyAlert.setClosedBy(null);

		// This save will result in a Hib session flush, which works fine with
		// our current usage. Future use cases might prefer to delay the
		// flush and we can address that when the time comes. Might not even
		// need to change anything here if it turns out nothing actually
		// *depends* on the flush.
		getDao().save(earlyAlert);
	}

	@Override
	public EarlyAlert save(@NotNull final EarlyAlert obj)
			throws ObjectNotFoundException {
		final EarlyAlert current = getDao().get(obj.getId());

		current.setCourseName(obj.getCourseName());
		current.setCourseTitle(obj.getCourseTitle());
		current.setEmailCC(obj.getEmailCC());
		current.setCampus(obj.getCampus());
		current.setEarlyAlertReasonOtherDescription(obj
				.getEarlyAlertReasonOtherDescription());
		current.setComment(obj.getComment());
		current.setClosedDate(obj.getClosedDate());
		if ( obj.getClosedById() == null ) {
			current.setClosedBy(null);
		} else {
			current.setClosedBy(personService.get(obj.getClosedById()));
		}

		if (obj.getPerson() == null) {
			current.setPerson(null);
		} else {
			current.setPerson(personService.get(obj.getPerson().getId()));
		}

		final Set<EarlyAlertReason> earlyAlertReasons = new HashSet<>();
		for (final EarlyAlertReason reason : obj.getEarlyAlertReasons()) {
			earlyAlertReasons.add(earlyAlertReasonService.load(reason
					.getId()));
		}

		current.setEarlyAlertReasons(earlyAlertReasons);

		final Set<EarlyAlertSuggestion> earlyAlertSuggestions = new HashSet<>();
		if (obj.getEarlyAlertSuggestions() != null) {
			for (final EarlyAlertSuggestion reason : obj
					.getEarlyAlertSuggestions()) {
				earlyAlertSuggestions.add(earlyAlertSuggestionService
						.load(reason
								.getId()));
			}
		}

		current.setEarlyAlertSuggestions(earlyAlertSuggestions);

		return getDao().save(current);
	}

	@Override
	public PagingWrapper<EarlyAlert> getAllForPerson(final Person person,
			final SortingAndPaging sAndP) {
		return getDao().getAllForPersonId(person.getId(), sAndP);
	}

	/**
	 * Business logic to determine the advisor that is assigned to the student
	 * for this Early Alert.
	 * 
	 * @param earlyAlert
	 *            EarlyAlert instance
	 * @throws ValidationException
	 *             If Early Alert, Student, and/or system information could not
	 *             determine the advisor for this student.
	 * @return The assigned advisor
	 */
	private Optional<UUID> getEarlyAlertAdvisor(final EarlyAlert earlyAlert) {

		if(earlyAlert.getPersonCoachId().isPresent())
			return earlyAlert.getPersonCoachId();

		// Get campus Early Alert coordinator
		if (earlyAlert.getCampus() == null) {
			throw new IllegalArgumentException("Campus ID can not be null.");
		}

		if(earlyAlert.getCampusEarlyAlertCoordinatorId().isPresent())
			return earlyAlert.getCampusEarlyAlertCoordinatorId();

		return Optional.empty();
	}

	private void ensureValidAlertedOnPersonStateNoFail(Person person) {
		try {
			ensureValidAlertedOnPersonStateOrFail(person);
		} catch ( Exception e ) {
			LOGGER.error("Unable to set a program status or student type on "
					+ "person '{}'. This is likely to prevent that person "
					+ "record from appearing in caseloads, student searches, "
					+ "and some reports.", person.getId(), e);
		}
	}

	private void ensureValidAlertedOnPersonStateOrFail(Person person)
			throws ObjectNotFoundException, ValidationException {

		person.setObjectStatus(ObjectStatus.ACTIVE);

		final ProgramStatus programStatus =  programStatusService.getActiveStatus();
		if ( programStatus == null ) {
			throw new ObjectNotFoundException(
					"Unable to find a ProgramStatus representing \"activeness\".",
					"ProgramStatus");
		}

		Set<PersonProgramStatus> programStatuses = person.getProgramStatuses();
		if ( programStatuses == null || programStatuses.isEmpty() ) {
			PersonProgramStatus personProgramStatus = new PersonProgramStatus();
			personProgramStatus.setEffectiveDate(new Date());
			personProgramStatus.setProgramStatus(programStatus);
			personProgramStatus.setPerson(person);
			programStatuses.add(personProgramStatus);
			person.setProgramStatuses(programStatuses);
			// save should cascade, but make sure custom create logic fires
			personProgramStatusService.create(personProgramStatus);
		}

		if ( person.getStudentType() == null ) {
			StudentType studentType = studentTypeService.get(StudentType.EAL_ID);
			if ( studentType == null ) {
				throw new ObjectNotFoundException(
						"Unable to find a StudentType representing an early "
								+ "alert-assigned type.", "StudentType");
			}
			person.setStudentType(studentType);
		}
	}

	/**
	 * Send e-mail ({@link Message}) to the assigned advisor for the student.
	 * 
	 * @param earlyAlert
	 *            Early Alert
	 * @param emailCC
	 *            Email address to also CC this message
	 * @throws ObjectNotFoundException
	 * @throws ValidationException
	 */
	private void sendMessageToAdvisor(@NotNull final EarlyAlert earlyAlert,
																		final String emailCC) throws ObjectNotFoundException, ValidationException {

		earlyAlert.getPersonOptional().orElseThrow(() -> new IllegalArgumentException("EarlyAlert Person is missing."));

		final SubjectAndBody subjAndBody = messageTemplateService.createEarlyAlertAdvisorConfirmationMessage(fillTemplateParameters(earlyAlert));

		this.notifyPersonWatcher(earlyAlert, emailCC, subjAndBody);

		// Send same message to all applicable Campus Early Alert routing
		// entries
		final PagingWrapper<EarlyAlertRouting> routes = earlyAlertRoutingService.getAllForCampus(earlyAlert.getCampus(), new SortingAndPaging(ObjectStatus.ACTIVE));

		if (routes.getResults() > 0) {
			final Set<String> alreadySent = new HashSet<>();
		
 			for (final EarlyAlertRouting route : routes.getRows()) {
				// Check that route applies
				route.getEarlyAlertReasonOptional().orElseThrow(() -> new ObjectNotFoundException("EarlyAlertRouting missing EarlyAlertReason.", "EarlyAlertReason"));

				// Only routes that are for any of the Reasons in this EarlyAlert should be applied.
				if (earlyAlert.getEarlyAlertReasons().contains(route.getEarlyAlertReason()) &&
								!alreadySent.contains(route.getPersonPrimaryEmailAddress())) {

					sendToCoach(earlyAlert, subjAndBody, alreadySent, route);

					sendToGroup(earlyAlert, subjAndBody, route);

				}
			}
		}
	}

	private void sendToCoach(@NotNull EarlyAlert earlyAlert, SubjectAndBody subjAndBody, Set<String> alreadySent, EarlyAlertRouting route) throws ObjectNotFoundException, ValidationException {
		final Person to = route.getPerson();
		if ( Objects.nonNull(to) && StringUtils.isNotBlank(to.getPrimaryEmailAddress()) ) {
			//check if this alert has already been sent to this recipient, if so skip
			final Message message = messageService.createMessage(to, null, subjAndBody);
			LOGGER.info("Message {} for EarlyAlert {} also routed to {}",
							new Object[]{message, earlyAlert, to}); // NOPMD
			alreadySent.add(route.getPersonPrimaryEmailAddress());
		}
	}

	private void sendToGroup(@NotNull EarlyAlert earlyAlert, SubjectAndBody subjAndBody, EarlyAlertRouting route) throws ObjectNotFoundException, ValidationException {
		// Send e-mail to a group
		if ( !StringUtils.isEmpty(route.getGroupName()) && !StringUtils.isEmpty(route.getGroupEmail()) ) {
			final Message message = messageService.createMessage(route.getGroupEmail(), null,subjAndBody);
			LOGGER.info("Message {} for EarlyAlert {} also routed to {}", new Object[] { message, earlyAlert, // NOPMD
							route.getGroupEmail() });
		}
	}

	private void notifyPersonWatcher(@NotNull EarlyAlert earlyAlert, String emailCC, SubjectAndBody subjAndBody) throws ObjectNotFoundException, ValidationException {
		final Person coach = earlyAlert.getCoach();
		Set<String> watcherEmailAddresses = earlyAlert.getPersonWatcherEmailAddresses();

		if(emailCC != null && !emailCC.isEmpty()) {
			watcherEmailAddresses.add(emailCC);
		}

		if (Objects.isNull(coach)) {
			LOGGER.warn("Student {} had no coach when EarlyAlert {} was"
					+ " created. Unable to send message to coach.",
					earlyAlert.getPerson(), earlyAlert);
		} else {
			// Create and queue the message
			final Message message = messageService.createMessage(coach, String.join(";", watcherEmailAddresses), subjAndBody);
			LOGGER.info("Message {} created for EarlyAlert {}", message, earlyAlert);
		}
	}

	@Override
	public void sendMessageToStudent(@NotNull final EarlyAlert earlyAlert) throws ObjectNotFoundException, ValidationException {

		earlyAlert.getPersonOptional().orElseThrow(() -> new IllegalArgumentException("EarlyAlert.Person is missing."));

		final Person person = earlyAlert.getPerson();
		final SubjectAndBody subjAndBody = messageTemplateService
				.createEarlyAlertToStudentMessage(fillTemplateParameters(earlyAlert));

		// Create and queue the message
		final Message message = messageService.createMessage(person, String.join(";", person.getWatcherEmailAddresses()), subjAndBody);

		LOGGER.info("Message {} created for EarlyAlert {}", message, earlyAlert);
	}

	/**
	 * Send confirmation e-mail ({@link Message}) to the faculty who created
	 * this alert.
	 * 
	 * @param earlyAlert
	 *            Early Alert
	 * @throws ObjectNotFoundException
	 * @throws ValidationException
	 */
	private void sendConfirmationMessageToFaculty(final EarlyAlert earlyAlert) throws ObjectNotFoundException, ValidationException {
		if (earlyAlert == null) {
			throw new IllegalArgumentException("EarlyAlert was missing.");
		}

		if (earlyAlert.getPerson() == null) {
			throw new IllegalArgumentException("EarlyAlert.Person is missing.");
		}

        if (!configService.getByNameOrDefaultValue("send_faculty_mail")) {
            LOGGER.debug("Skipping Faculty Early Alert Confirmation Email: Config Turned Off");
            return; //skip if faculty early alert email turned off
        }

		final UUID personId = earlyAlert.getCreatedBy().getId();
		Person person = personService.get(personId);
		if ( person == null ) {
			LOGGER.warn("EarlyAlert {} has no creator. Unable to send"
					+ " confirmation message to faculty.", earlyAlert);
		} else {
			final SubjectAndBody subjAndBody = messageTemplateService
					.createEarlyAlertFacultyConfirmationMessage(fillTemplateParameters(earlyAlert));

			// Create and queue the message
			final Message message = messageService.createMessage(person, null,
					subjAndBody);

			LOGGER.info("Message {} created for EarlyAlert {}", message, earlyAlert);
		}
	}

	@Override
	public Map<String, Object> fillTemplateParameters(final EarlyAlert earlyAlert) {

		doFillTemplateParametersValidations(earlyAlert);

		final Map<String, Object> templateParameters = Maps.newHashMap();

		final String courseName = earlyAlert.getCourseName();
		if (StringUtils.isNotBlank(courseName) ) {
			Person creator;
			try {
				creator = personService.get(earlyAlert.getCreatedBy().getId());
			} catch (ObjectNotFoundException e1) {
				throw new IllegalArgumentException(
						"EarlyAlert.CreatedBy.Id could not be loaded.", e1);
			}
			final String facultySchoolId = creator.getSchoolId();
			if (StringUtils.isNotBlank(facultySchoolId)) {
				final String termCode = earlyAlert.getCourseTermCode();
				Optional<FacultyCourse> courseOptional = Optional.empty();
				try {
					if ( StringUtils.isBlank(termCode) ) {
						courseOptional = Optional.ofNullable(facultyCourseService.getCourseByFacultySchoolIdAndFormattedCourse(facultySchoolId, courseName));
					} else {
						courseOptional = Optional.ofNullable(facultyCourseService.getCourseByFacultySchoolIdAndFormattedCourseAndTermCode(facultySchoolId, courseName, termCode));
					}
				} catch ( ObjectNotFoundException e ) {
					// Trace irrelevant. see below for logging. prefer to
					// do it there, after the null check b/c not all service
					// methods implement ObjectNotFoundException reliably.
				}
				courseOptional.ifPresentOrElse(course -> {
					templateParameters.put("course", course);
					String code = termCode;

					if ( StringUtils.isBlank(termCode) ) {
						code = course.getTermCode();
					}

					if (StringUtils.isNotBlank(code)) {
						Term term = null;
						try {
							term = termService.getByCode(code);
						} catch ( ObjectNotFoundException e ) {
							// Trace irrelevant. See below for logging.
						}
						if ( Objects.nonNull(term)) {
							templateParameters.put("term", term);
						} else {
							LOGGER.info("Not adding term to message template"
															+ " params or early alert {} because"
															+ " the term code {} did not resolve to"
															+ " an external term record",
											earlyAlert.getId(), code);
						}
					}
				}, ()-> LOGGER.info("Not adding course nor term to message template"
													+ " params for early alert {} because the associated"
													+ " course {} and faculty school id {} did not"
													+ " resolve to an external course record.",
									new Object[] { earlyAlert.getId(), courseName,
													facultySchoolId})
				);
			}
		}
		Person creator = null;
		try{
			creator = personService.get(earlyAlert.getCreatedBy().getId());
		}catch(ObjectNotFoundException exp)	{
			LOGGER.error("Early Alert Creator Not found sending message for early alert:" + earlyAlert.getId(), exp);
		}
		
		EarlyAlertMessageTemplateTO eaMTO = new EarlyAlertMessageTemplateTO(earlyAlert,creator);
		
		//Only early alerts response late messages sent to coaches
		if(eaMTO.getCoach() == null){
			try{
				// if no earlyAlert.getCampus()  error thrown by design, should never not be a campus.
				eaMTO.setCoach(new CoachPersonLiteMessageTemplateTO(personService.get(earlyAlert.getCampus().getEarlyAlertCoordinatorId())));
			}catch(ObjectNotFoundException exp){
				LOGGER.error("Early Alert with id: " + earlyAlert.getId() + " does not have valid campus coordinator, no coach assigned: " + earlyAlert.getCampus().getEarlyAlertCoordinatorId(), exp);
			}
		}
		
		String statusCode = eaMTO.getEnrollmentStatus();
		if(statusCode != null) {
			EnrollmentStatus enrollmentStatus = enrollmentStatusService.getByCode(statusCode);
			if(enrollmentStatus != null) {
				
				//if we have made it here... we can add the status!
				templateParameters.put("enrollment", enrollmentStatus);
			}
		}
		
		
		final String TERM_MESSAGE = "term_to_represent_early_alert";
		
		templateParameters.put("earlyAlert", eaMTO);
		templateParameters.put("termToRepresentEarlyAlert",
				configService.getByNameEmpty(TERM_MESSAGE));
		templateParameters.put("TermToRepresentEarlyAlert",
				configService.getByNameEmpty(TERM_MESSAGE));
		templateParameters.put("termForEarlyAlert",
				configService.getByNameEmpty(TERM_MESSAGE));
		templateParameters.put("linkToSSP",
				configService.getByNameEmpty("serverExternalPath"));
		templateParameters.put("applicationTitle",
				configService.getByNameEmpty("app_title"));
		templateParameters.put("institutionName",
				configService.getByNameEmpty("inst_name"));
		
		templateParameters.put("FirstName", eaMTO.getPerson().getFirstName());
		templateParameters.put("LastName", eaMTO.getPerson().getLastName());
		templateParameters.put("CourseName", eaMTO.getCourseName());

		return templateParameters;
	}

	private void doFillTemplateParametersValidations(EarlyAlert earlyAlert) {

		if (Objects.isNull(earlyAlert)) {
			throw new IllegalArgumentException("EarlyAlert was missing.");
		}

		earlyAlert.getPersonOptional().orElseThrow(() -> new IllegalArgumentException("EarlyAlert.Person is missing."));

		if (earlyAlert.getCreatedBy() == null) {
			throw new IllegalArgumentException("EarlyAlert.CreatedBy is missing.");
		}

		if (Objects.isNull(earlyAlert.getCampus())) {
			throw new IllegalArgumentException("EarlyAlert.Campus is missing.");
		}

		// ensure earlyAlert.createdBy is populated
		if (Objects.isNull(earlyAlert.getCreatorFirstName())){
				throw new IllegalArgumentException("EarlyAlert.CreatedBy is missing.");
		}
	}

	@Override
	public void applyEarlyAlertCounts(Person person) {}

	@Override
	public Map<UUID, Number> getCountOfActiveAlertsForPeopleIds(
			final Collection<UUID> peopleIds) {
		return dao.getCountOfActiveAlertsForPeopleIds(peopleIds);
	}

	@Override
	public Map<UUID, Number> getCountOfClosedAlertsForPeopleIds(
			final Collection<UUID> peopleIds) {
		return dao.getCountOfClosedAlertsForPeopleIds(peopleIds);
	}
	
	@Override
	public Long getCountOfEarlyAlertsForSchoolIds(
			final Collection<String> schoolIds, Campus campus) {
		return dao.getCountOfAlertsForSchoolIds(schoolIds, campus);
	}

	@Override
	public Long getEarlyAlertCountForCoach(Person coach, Date createDateFrom, Date createDateTo, List<UUID> studentTypeIds) {
		return dao.getEarlyAlertCountForCoach(coach, createDateFrom, createDateTo, studentTypeIds);
	}

	@Override
	public Long getStudentEarlyAlertCountForCoach(Person coach, Date createDateFrom, Date createDateTo, List<UUID> studentTypeIds) {
		return dao.getStudentEarlyAlertCountForCoach(coach, createDateFrom, createDateTo, studentTypeIds);
	}
	
	@Override
	public Long getEarlyAlertCountForCreatedDateRange(String termCode, Date createDatedFrom, Date createdDateTo, Campus campus, String rosterStatus) {
		return dao.getEarlyAlertCountForCreatedDateRange(termCode, createDatedFrom, createdDateTo, campus, rosterStatus);
	}

	@Override
	public Long getClosedEarlyAlertCountForClosedDateRange(Date closedDateFrom, Date closedDateTo, Campus campus, String rosterStatus) {
		return dao.getClosedEarlyAlertCountForClosedDateRange(closedDateFrom, closedDateTo, campus, rosterStatus);
	}

	@Override
	public Long getClosedEarlyAlertsCountForEarlyAlertCreatedDateRange(String termCode, Date createDatedFrom, Date createdDateTo, Campus campus, String rosterStatus) {
		return dao.getClosedEarlyAlertsCountForEarlyAlertCreatedDateRange(termCode, createDatedFrom, createdDateTo, campus, rosterStatus);
	}

	@Override
	public Long getStudentCountForEarlyAlertCreatedDateRange(String termCode, Date createDatedFrom,
															 Date createdDateTo, Campus campus, String rosterStatus) {
		return dao.getStudentCountForEarlyAlertCreatedDateRange(termCode, createDatedFrom, createdDateTo, campus, rosterStatus);
	}

	@Override
	public PagingWrapper<EarlyAlertStudentReportTO> getStudentsEarlyAlertCountSetForCriteria(
			EarlyAlertStudentSearchTO earlyAlertStudentSearchTO,
			SortingAndPaging createForSingleSort) {
		return dao.getStudentsEarlyAlertCountSetForCriteria(earlyAlertStudentSearchTO, createForSingleSort);
	}

    @Override
    public  List<EarlyAlertCourseCountsTO> getStudentEarlyAlertCountSetPerCourses(
            String termCode, Date createdDateFrom, Date createdDateTo, Campus campus, ObjectStatus objectStatus ) {
        return dao.getStudentEarlyAlertCountSetPerCourses(termCode, createdDateFrom, createdDateTo, campus, objectStatus);
    }
	@Override
	public Long getStudentEarlyAlertCountSetPerCoursesTotalStudents(
			String termCode, Date createdDateFrom, Date createdDateTo, Campus campus, ObjectStatus objectStatus ) {
		return dao.getStudentEarlyAlertCountSetPerCoursesTotalStudents(termCode, createdDateFrom, createdDateTo, campus, objectStatus);
	}


    @Override
    public  List<Triple<String, Long, Long>> getEarlyAlertReasonTypeCountByCriteria(
            Campus campus, String termCode, Date createdDateFrom, Date createdDateTo, ObjectStatus status) {
        return dao.getEarlyAlertReasonTypeCountByCriteria(campus, termCode, createdDateFrom, createdDateTo, status);
    }

    @Override
    public List<EarlyAlertReasonCountsTO> getStudentEarlyAlertReasonCountByCriteria(
            String termCode, Date createdDateFrom, Date createdDateTo, Campus campus, ObjectStatus objectStatus) {
        return dao.getStudentEarlyAlertReasonCountByCriteria(termCode, createdDateFrom, createdDateTo, campus, objectStatus);
    }

	@Override
	public Long getStudentEarlyAlertReasonCountByCriteriaTotalStudents(
			String termCode, Date createdDateFrom, Date createdDateTo, Campus campus, ObjectStatus objectStatus) {
		return dao.getStudentEarlyAlertReasonCountByCriteriaTotalStudents(termCode, createdDateFrom, createdDateTo, campus, objectStatus);
	}


	@Override
	public PagingWrapper<EntityStudentCountByCoachTO> getStudentEarlyAlertCountByCoaches(EntityCountByCoachSearchForm form) {
		return dao.getStudentEarlyAlertCountByCoaches(form);
	}
	
	@Override
	public Long getEarlyAlertCountSetForCriteria(EarlyAlertStudentSearchTO searchForm){
		return dao.getEarlyAlertCountSetForCriteria(searchForm);
	}
	
	@Override
	public void sendAllEarlyAlertReminderNotifications() {
		Date lastResponseDate = getMinimumResponseComplianceDate();
		// if no responseDate is given no emails are sent
		if (lastResponseDate == null) {
			return;
		}
		List<EarlyAlert> eaOutOfCompliance = dao.getResponseDueEarlyAlerts(lastResponseDate);
		
		Map<UUID, List<EarlyAlertMessageTemplateTO>> easByCoach = new HashMap<>();
		Map<UUID, Person> coaches = new HashMap<>();
		final boolean includeCoachAsRecipient = this.earReminderRecipientConfig.includeCoachAsRecipient();
		final boolean includeEarlyAlertCoordinatorAsRecipient = this.earReminderRecipientConfig.includeEarlyAlertCoordinatorAsRecipient();
		final boolean includeEarlyAlertCoordinatorAsRecipientOnlyIfStudentHasNoCoach = this.earReminderRecipientConfig.includeEarlyAlertCoordinatorAsRecipientOnlyIfStudentHasNoCoach();
		LOGGER.info("Config: includeCoachAsRecipient(): {}", includeCoachAsRecipient);
		LOGGER.info("Config: includeEarlyAlertCoordinatorAsRecipient(): {}", includeEarlyAlertCoordinatorAsRecipient);
		LOGGER.info("Config: includeEarlyAlertCoordinatorAsRecipientOnlyIfStudentHasNoCoach(): {}", includeEarlyAlertCoordinatorAsRecipientOnlyIfStudentHasNoCoach);
		for (EarlyAlert earlyAlert: eaOutOfCompliance){
			final Set<Person> recipients = new HashSet<>();
			Person coach = earlyAlert.getPerson().getCoach();
			if (includeCoachAsRecipient) {
				if (coach == null) {
					LOGGER.warn("Early Alert with id: {} is associated with a person without a coach, so skipping email to coach.", earlyAlert.getId());
				} else {
					recipients.add(coach);
				}
			}
			if (includeEarlyAlertCoordinatorAsRecipient || (coach == null && includeEarlyAlertCoordinatorAsRecipientOnlyIfStudentHasNoCoach)) {
				final Campus campus = earlyAlert.getCampus();
				if (campus == null) {
					LOGGER.error("Early Alert with id: {} does not have valid a campus, so skipping email to EAC.", earlyAlert.getId());
				} else {
					final UUID earlyAlertCoordinatorId = campus.getEarlyAlertCoordinatorId();
					if ( earlyAlertCoordinatorId == null ) {
						LOGGER.error("Early Alert with id: {} has campus with no early alert coordinator, so skipping email to EAC.", earlyAlert.getId());
					} else {
						try {
							final Person earlyAlertCoordinator = personService.get(earlyAlertCoordinatorId);
							if (earlyAlertCoordinator == null) { // guard against change in behavior where ObjectNotFoundException is not thrown (which we've seen)
								LOGGER.error("Early Alert with id: {} has campus with an early alert coordinator with a bad ID ({}), so skipping email to EAC.", earlyAlert.getId(), earlyAlertCoordinatorId);
							} else {
								recipients.add(earlyAlertCoordinator);
							}
						} catch(ObjectNotFoundException exp){
							LOGGER.error("Early Alert with id: {} has campus with an early alert coordinator with a bad ID ({}), so skipping email to coach because no coach can be resolved.", new Object[] { earlyAlert.getId(), earlyAlertCoordinatorId, exp });
						}
					}
				}
			}
			LOGGER.debug("Early Alert: {}; Recipients: {}", earlyAlert.getId(), recipients);
			if (recipients.isEmpty()) {
				continue;
			} else {
				for (Person person : recipients) {
					// We've definitely got a coach by this point
					if (easByCoach.containsKey(person.getId())){
						final List<EarlyAlertMessageTemplateTO> coachEarlyAlerts = easByCoach.get(person.getId());
						coachEarlyAlerts.add(createEarlyAlertTemplateTO(earlyAlert));
					} else {
						coaches.put(person.getId(), person);
						final ArrayList<EarlyAlertMessageTemplateTO> eam = Lists.newArrayList();
						eam.add(createEarlyAlertTemplateTO(earlyAlert)); // add separately from newArrayList() call else list will be sized to 1
						easByCoach.put(person.getId(), eam);
					}
				}
			}
			List<WatchStudent> watchers = earlyAlert.getPerson().getWatchers();
			for (WatchStudent watcher : watchers) {
				if(easByCoach.containsKey(watcher.getPerson().getId())){
					final List<EarlyAlertMessageTemplateTO> coachEarlyAlerts = easByCoach.get(watcher.getPerson().getId());
					coachEarlyAlerts.add(createEarlyAlertTemplateTO( earlyAlert));
				}else{
					coaches.put(watcher.getPerson().getId(), watcher.getPerson());
					final ArrayList<EarlyAlertMessageTemplateTO> eam = Lists.newArrayList();
					eam.add(createEarlyAlertTemplateTO( earlyAlert)); // add separately from newArrayList() call else list will be sized to 1
					easByCoach.put(watcher.getPerson().getId(), eam);
				}				
			}
		}
		for(UUID coachId: easByCoach.keySet()){
			Map<String,Object> messageParams = new HashMap<>();

			easByCoach.get(coachId).sort((p1, p2) -> {
				Date p1Date = p1.getLastResponseDate();
				if (p1Date == null)
					p1Date = p1.getCreatedDate();
				Date p2Date = p2.getLastResponseDate();
				if (p2Date == null)
					p2Date = p2.getCreatedDate();
				return p1Date.compareTo(p2Date);
			});

			Integer daysSince1900ResponseExpected =  DateTimeUtils.daysSince1900(lastResponseDate);
			List<Pair<EarlyAlertMessageTemplateTO,Integer>> earlyAlertTOPairs = new ArrayList<>();
			for(EarlyAlertMessageTemplateTO ea:easByCoach.get(coachId)){
				Integer daysOutOfCompliance;
				if(ea.getLastResponseDate() != null){
					daysOutOfCompliance = daysSince1900ResponseExpected - DateTimeUtils.daysSince1900(ea.getLastResponseDate());
				}else{
				    daysOutOfCompliance = daysSince1900ResponseExpected - DateTimeUtils.daysSince1900(ea.getCreatedDate());
				}
				
				// Just in case attempt to only send emails for EA full day out of compliance
				if(daysOutOfCompliance >= 0)
					earlyAlertTOPairs.add(new Pair<>(ea, daysOutOfCompliance));
			}
			messageParams.put("earlyAlertTOPairs", earlyAlertTOPairs);
			messageParams.put("coach", coaches.get(coachId));
			messageParams.put("DateTimeUtils", DateTimeUtils.class);
			messageParams.put("termToRepresentEarlyAlert",
					configService.getByNameEmpty("term_to_represent_early_alert"));
			
			
			SubjectAndBody subjAndBody = messageTemplateService.createEarlyAlertResponseRequiredToCoachMessage(messageParams);
			try{
				messageService.createMessage(coaches.get(coachId), null, subjAndBody);
			}catch(Exception exp){
				LOGGER.error("Unable to send reminder emails to coach: " + coaches.get(coachId).getFullName() + "\n", exp);
			}
		}
		
	}
	
	private EarlyAlertMessageTemplateTO createEarlyAlertTemplateTO(EarlyAlert earlyAlert){
		Person creator = null;
		try{
			 creator = personService.get(earlyAlert.getCreatedBy().getId());
		}catch(ObjectNotFoundException exp){
			LOGGER.error("Early Alert with id: " + earlyAlert.getId() + " does not have valid creator: " + earlyAlert.getCreatedBy(), exp);
		}
		return new EarlyAlertMessageTemplateTO(earlyAlert, creator,earlyAlert.getPerson().getWatcherEmailAddresses());
	}
	
	public Map<UUID,Number> getResponsesDueCountEarlyAlerts(List<UUID> personIds){
		Date lastResponseDate = getMinimumResponseComplianceDate();
		if(lastResponseDate == null)
			return new HashMap<>();
		return dao.getResponsesDueCountEarlyAlerts(personIds, lastResponseDate);
	}
	
	private Date getMinimumResponseComplianceDate(){
		final String numVal = configService
				.getByNameNull("maximum_days_before_early_alert_response");
		if(StringUtils.isBlank(numVal))
			return null;
		Integer allowedDaysPastResponse = Integer.parseInt(numVal);
		
	    return DateTimeUtils.getDateOffsetInDays(new Date(), -allowedDaysPastResponse);
				
	}

	@Override
	public PagedResponse<EarlyAlertSearchResultTO> searchEarlyAlert(
			EarlyAlertSearchForm form) {
		PagingWrapper<EarlyAlertSearchResult> models = dao.searchEarlyAlert(form);
		return new PagedResponse<>(true,
				models.getResults(), searchResultFactory.asTOList(models.getRows()));	
	}
}
