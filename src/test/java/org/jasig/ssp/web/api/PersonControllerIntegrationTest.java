package org.jasig.ssp.web.api; // NOPMD

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.jasig.ssp.model.ObjectStatus;
import org.jasig.ssp.model.Person;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.transferobject.PersonTO;
import org.jasig.ssp.web.api.validation.ValidationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link PersonController} tests
 * 
 * @author jon.adams
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("../ControllerIntegrationTests-context.xml")
@TransactionConfiguration
@Transactional
public class PersonControllerIntegrationTest {

	@Autowired
	private transient PersonController controller;

	private static final UUID PERSON_ID = UUID
			.fromString("1010e4a0-1001-0110-1011-4ffc02fe81ff");

	private static final String PERSON_FIRSTNAME = "James";

	private static final String PERSON_LASTNAME = "Gosling";

	private static final String PERSON_SORTEDBY_FIRSTNAME_0 = "Alan";

	private static final String PERSON_SORTEDBY_FIRSTNAME_3 = "James";

	private static final String TEST_SCHOOLID = "legacy school id";

	/**
	 * Test the {@link PersonController#get(UUID)} action.
	 * 
	 * @throws ObjectNotFoundException
	 *             If object could not be found.
	 */
	@Test
	public void testControllerGet() throws ObjectNotFoundException {
		assertNotNull(
				"Controller under test was not initialized by the container correctly.",
				controller);

		final PersonTO obj = controller.get(PERSON_ID);

		assertNotNull(
				"Returned PersonTO from the controller should not have been null.",
				obj);

		assertEquals("Returned Person.FirstName did not match.",
				PERSON_FIRSTNAME, obj.getFirstName());
		assertEquals("Returned Person.LastName did not match.",
				PERSON_LASTNAME, obj.getLastName());
	}

	/**
	 * Test that the {@link PersonController#get(UUID)} action returns the
	 * correct validation errors when an invalid ID is sent.
	 * 
	 * @throws ObjectNotFoundException
	 *             If object could not be found.
	 */
	@Test(expected = ObjectNotFoundException.class)
	public void testControllerGetOfInvalidId() throws ObjectNotFoundException {
		assertNotNull(
				"Controller under test was not initialized by the container correctly.",
				controller);

		final PersonTO obj = controller.get(UUID.randomUUID());

		assertNull(
				"Returned PersonTO from the controller should have been null.",
				obj);
	}

	/**
	 * Test the
	 * {@link PersonController#getAll(ObjectStatus, Integer, Integer, String, String)}
	 * action.
	 */
	@Test
	public void testControllerAll() {
		final Collection<PersonTO> list = controller.getAll(
				ObjectStatus.ACTIVE,
				null, null, null, null).getRows();

		assertNotNull("List should not have been null.", list);
		assertFalse("List action should have returned some objects.",
				list.isEmpty());
	}

	/**
	 * Test the
	 * {@link PersonController#getAll(ObjectStatus, Integer, Integer, String, String)}
	 * action.
	 * 
	 * This test assumes that there are at least 3 valid, active Persons in the
	 * test database.
	 */
	@Test
	public void testControllerAllWithPaging() {
		final Collection<PersonTO> listAll = controller.getAll(
				ObjectStatus.ACTIVE,
				null, null, null, null).getRows();
		final Collection<PersonTO> listFiltered = controller.getAll(
				ObjectStatus.ACTIVE, 1, 2, null, null).getRows();

		assertNotNull("ListAll should not have been null.", listAll);
		assertNotNull("ListFiltered should not have been null.", listFiltered);
		assertFalse("ListAll action should have returned some objects.",
				listAll.isEmpty());
		assertFalse("ListFiltered action should have returned some objects.",
				listFiltered.isEmpty());
		assertTrue("ListFiltered should have had fewer items than ListAll.",
				listFiltered.size() < listAll.size());
		assertEquals("ListFiltered should have had exactly 2 items.", 2,
				listFiltered.size());
	}

	/**
	 * Test the
	 * {@link PersonController#getAll(ObjectStatus, Integer, Integer, String, String)}
	 * action.
	 * 
	 * This test assumes that there are at least 3 valid, active Persons in the
	 * test database.
	 */
	@Test
	public void testControllerAllWithSorting() {
		final Collection<PersonTO> list = controller.getAll(
				ObjectStatus.ACTIVE, 0,
				200, "firstName", "ASC").getRows();

		assertNotNull("The list should not have been null.", list);

		final Iterator<PersonTO> iter = list.iterator();

		final PersonTO person = iter.next(); // 1st
		assertNotNull("List[0] should not have been null.", person);
		assertEquals("First name properties do not match.",
				PERSON_SORTEDBY_FIRSTNAME_0, person.getFirstName());
		iter.next(); // skip checking 2nd
		iter.next(); // skip checking 3rd
		final PersonTO person4 = iter.next(); // check 4th
		assertEquals("4th",
				PERSON_SORTEDBY_FIRSTNAME_3, person4.getFirstName());
	}

	@Test(expected = ConstraintViolationException.class)
	public void testUniqueSchoolId() throws ObjectNotFoundException,
			ValidationException {
		final Person person1 = createPerson();
		person1.setUsername(TEST_SCHOOLID);
		controller.create(new PersonTO(person1));

		final Person person2 = createPerson();
		person2.setUsername(TEST_SCHOOLID);
		controller.create(new PersonTO(person2));
		fail("Exception should have been thrown.");
	}

	private Person createPerson() {
		final Person person = new Person();
		person.setFirstName(PERSON_FIRSTNAME);
		person.setLastName(PERSON_LASTNAME);
		person.setPrimaryEmailAddress("email");
		person.setUsername("username");
		person.setSchoolId("legacy id");
		return person;
	}
}