package org.studentsuccessplan.mygps.web;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.studentsuccessplan.mygps.business.SelfHelpGuideManager;
import org.studentsuccessplan.ssp.factory.reference.SelfHelpGuideTOFactory;
import org.studentsuccessplan.ssp.model.reference.SelfHelpGuide;
import org.studentsuccessplan.ssp.model.reference.SelfHelpGuideGroup;
import org.studentsuccessplan.ssp.service.ObjectNotFoundException;
import org.studentsuccessplan.ssp.service.reference.SelfHelpGuideGroupService;
import org.studentsuccessplan.ssp.service.reference.SelfHelpGuideService;
import org.studentsuccessplan.ssp.transferobject.reference.SelfHelpGuideDetailTO;
import org.studentsuccessplan.ssp.transferobject.reference.SelfHelpGuideTO;
import org.studentsuccessplan.ssp.util.sort.SortingAndPaging;

import com.google.common.collect.Lists;

public class MyGpsSelfHelpGuideControllerTest {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(MyGpsSelfHelpGuideControllerTest.class);

	private transient MyGpsSelfHelpGuideController controller;

	private transient SelfHelpGuideManager manager;

	private transient SelfHelpGuideService selfHelpGuideService;

	private transient SelfHelpGuideGroupService selfHelpGuideGroupService;

	private transient SelfHelpGuideTOFactory selfHelpGuideTOFactory;

	@Before
	public void setup() {
		manager = createMock(SelfHelpGuideManager.class);
		selfHelpGuideService = createMock(SelfHelpGuideService.class);
		selfHelpGuideGroupService = createMock(SelfHelpGuideGroupService.class);
		selfHelpGuideTOFactory = createMock(SelfHelpGuideTOFactory.class);

		controller = new MyGpsSelfHelpGuideController(manager,
				selfHelpGuideService, selfHelpGuideGroupService,
				selfHelpGuideTOFactory);
	}

	@Test
	public void getAll() {
		List<SelfHelpGuide> guides = Lists.newArrayList();
		guides.add(new SelfHelpGuide());
		List<SelfHelpGuideTO> guideTOs = Lists.newArrayList();
		expect(
				selfHelpGuideService.getAll(isA(SortingAndPaging.class)))
				.andReturn(guides);

		expect(selfHelpGuideTOFactory.asTOList(guides)).andReturn(guideTOs);

		replay(manager);
		replay(selfHelpGuideService);
		replay(selfHelpGuideGroupService);
		replay(selfHelpGuideTOFactory);

		try {
			List<SelfHelpGuideTO> results = controller.getAll();

			verify(manager);
			verify(selfHelpGuideService);
			verify(selfHelpGuideGroupService);
			verify(selfHelpGuideTOFactory);

			assertNotNull(results);
		} catch (Exception e) {
			LOGGER.error("controller error", e);
		}

	}

	@Test
	public void getContentById() throws ObjectNotFoundException {
		SelfHelpGuideDetailTO contentTO = new SelfHelpGuideDetailTO();
		UUID selfHelpGuideId = UUID
				.fromString("7CDD9ECE-C479-4AD6-4A1D-1BB3CDD4DDE4");

		expect(manager.getContentById(selfHelpGuideId)).andReturn(contentTO);

		replay(manager);
		replay(selfHelpGuideService);
		replay(selfHelpGuideGroupService);

		try {
			SelfHelpGuideDetailTO content = controller
					.getContentById(selfHelpGuideId);
			assertNotNull(content);

			verify(manager);
			verify(selfHelpGuideService);
			verify(selfHelpGuideGroupService);

		} catch (Exception e) {
			LOGGER.error("controller error", e);
		}
	}

	@Test
	public void getBySelfHelpGuideGroup() throws ObjectNotFoundException {
		SelfHelpGuideGroup group = new SelfHelpGuideGroup(UUID.randomUUID());
		List<SelfHelpGuide> guides = new ArrayList<SelfHelpGuide>();
		guides.add(new SelfHelpGuide());
		List<SelfHelpGuideTO> guideTOs = Lists.newArrayList();

		expect(selfHelpGuideGroupService.get(group.getId())).andReturn(group);
		expect(selfHelpGuideService.getBySelfHelpGuideGroup(group)).andReturn(
				guides);
		expect(selfHelpGuideTOFactory.asTOList(guides)).andReturn(guideTOs);

		replay(manager);
		replay(selfHelpGuideService);
		replay(selfHelpGuideGroupService);
		replay(selfHelpGuideTOFactory);

		try {
			List<SelfHelpGuideTO> results = controller
					.getBySelfHelpGuideGroup(group.getId());

			verify(manager);
			verify(selfHelpGuideService);
			verify(selfHelpGuideGroupService);
			verify(selfHelpGuideTOFactory);

			assertNotNull(results);
		} catch (Exception e) {
			LOGGER.error("controller error", e);
		}

	}
}
