/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jasig.schedassist.impl.reminder;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.mail.internet.InternetAddress;

import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.property.Location;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.schedassist.ICalendarAccountDao;
import org.jasig.schedassist.NullAffiliationSourceImpl;
import org.jasig.schedassist.SchedulingAssistantService;
import org.jasig.schedassist.impl.owner.OwnerDao;
import org.jasig.schedassist.model.AvailableBlock;
import org.jasig.schedassist.model.AvailableBlockBuilder;
import org.jasig.schedassist.model.CommonDateOperations;
import org.jasig.schedassist.model.DefaultEventUtilsImpl;
import org.jasig.schedassist.model.ICalendarAccount;
import org.jasig.schedassist.model.InputFormatException;
import org.jasig.schedassist.model.mock.MockCalendarAccount;
import org.jasig.schedassist.model.mock.MockScheduleOwner;
import org.jasig.schedassist.model.mock.MockScheduleVisitor;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import com.sun.mail.smtp.SMTPAddressFailedException;

/**
 * @author Nicholas Blair
 * @version $Id: DefaultReminderServiceImplTest.java $
 */
public class DefaultReminderServiceImplTest {

	private Log LOG = LogFactory.getLog(this.getClass());
	private StaticMessageSource messageSource = new StaticMessageSource();
	public DefaultReminderServiceImplTest() {
		messageSource.addMessage("reminder.email.footer", Locale.getDefault(), "Footer - link");
		messageSource.addMessage("reminder.email.introduction", Locale.getDefault(), "Reminder, meeting with {0}");
		messageSource.addMessage("reminder.email.location", Locale.getDefault(), "Location: {0}");
		messageSource.addMessage("reminder.email.time", Locale.getDefault(), "Time: {0} to {1}");
		messageSource.addMessage("reminder.email.title", Locale.getDefault(), "Title: {0}");
	}
	@Test
	public void testCreateMessageBodyControl() throws InputFormatException {
		VEvent event = new VEvent(new Date(CommonDateOperations.parseDateTimePhrase("20110830-1200")), 
				new Date(CommonDateOperations.parseDateTimePhrase("20110830-1300")), 
				"some summary");

		event.getProperties().add(new Location("somewhere"));
		
		DefaultReminderServiceImpl reminderService = new DefaultReminderServiceImpl();
		reminderService.setMessageSource(messageSource);
		MockCalendarAccount account = new MockCalendarAccount();
		account.setDisplayName("Some Person");
		MockScheduleOwner owner = new MockScheduleOwner(account, 1L);
		String messageBody = reminderService.createMessageBody(event, owner);
		LOG.debug("testCreateMessageBodyControl: " + messageBody);
		Assert.assertTrue(messageBody.contains("Title: some summary"));
		Assert.assertTrue(messageBody.contains("Location: somewhere"));
	}
	
	@Test
	public void testCreateMessageBodyNoLocation() throws InputFormatException {
		VEvent event = new VEvent(new Date(CommonDateOperations.parseDateTimePhrase("20110830-1200")), 
				new Date(CommonDateOperations.parseDateTimePhrase("20110830-1300")), 
				"some summary");
		
		DefaultReminderServiceImpl reminderService = new DefaultReminderServiceImpl();
		reminderService.setMessageSource(messageSource);

		MockCalendarAccount account = new MockCalendarAccount();
		account.setDisplayName("Some Person");
		MockScheduleOwner owner = new MockScheduleOwner(account, 1L);
		String messageBody = reminderService.createMessageBody(event, owner);
		LOG.debug("testCreateMessageBodyNoLocation: " + messageBody);
		Assert.assertTrue(messageBody.contains("Title: some summary"));
		Assert.assertFalse(messageBody.contains("Location"));
	}
	/**
	 * SA-21 verify service continues to process reminders after failed email.
	 * 
	 * @throws InputFormatException 
	 * 
	 */
	@Test
	public void testPersistedReminderWithInvalidEmailAddress() throws InputFormatException {
		java.util.Date now = new java.util.Date();
		java.util.Date later = DateUtils.addHours(now, 1);
		DefaultEventUtilsImpl eventUtils = new DefaultEventUtilsImpl(new NullAffiliationSourceImpl());
		AvailableBlock targetBlock = AvailableBlockBuilder.createBlock(now, later);
		
		PersistedReminderImpl persisted = new PersistedReminderImpl();
		persisted.setOwnerId(1L);
		persisted.setReminderId(1L);
		persisted.setRecipientId("recipientid");
		persisted.setSendTime(now);
		persisted.setBlockStartTime(now);
		persisted.setBlockEndTime(later);
		
		MockCalendarAccount recipient = new MockCalendarAccount();
		recipient.setDisplayName("Some Visitor");
		recipient.setEmailAddress("bogus@nowhere.com");
		
		MockCalendarAccount account = new MockCalendarAccount();
		account.setDisplayName("Some Person");
		account.setEmailAddress("somebodyelse@nowhere.com");
		MockScheduleOwner owner = new MockScheduleOwner(account, 1L);
		AvailableBlock block = AvailableBlockBuilder.createBlock(new Date(), DateUtils.addMinutes(new Date(), 30));
		VEvent event = eventUtils.constructAvailableAppointment(block, owner, new MockScheduleVisitor(recipient), "test event");
		
		ReminderDao reminderDao = mock(ReminderDao.class);
		OwnerDao ownerDao = mock(OwnerDao.class);
		ICalendarAccountDao calendarAccountDao = mock(ICalendarAccountDao.class);
		SchedulingAssistantService schedAssistService = mock(SchedulingAssistantService.class);
		MailSender mailSender = mock(MailSender.class);
		
		List<PersistedReminderImpl> pending = new ArrayList<PersistedReminderImpl>();
		pending.add(persisted);
		
		when(reminderDao.getPendingReminders()).thenReturn(pending);
		when(ownerDao.locateOwnerByAvailableId(1L)).thenReturn(owner);
		when(calendarAccountDao.getCalendarAccount("recipientid")).thenReturn(recipient);
		
		when(schedAssistService.getExistingAppointment(targetBlock, owner)).thenReturn(event);
		SMTPAddressFailedException smtpFailure =  new SMTPAddressFailedException(new InternetAddress(), "DATA", 550, "illegal alias");

		MailSendException exception = new MailSendException("failed", smtpFailure);
		
		doThrow(exception).when(mailSender).send(isA(SimpleMailMessage.class));
		
		DefaultReminderServiceImpl reminderService = new DefaultReminderServiceImpl();
		reminderService.setCalendarAccountDao(calendarAccountDao);
		reminderService.setMailSender(mailSender);
		reminderService.setMessageSource(messageSource);
		reminderService.setOwnerDao(ownerDao);
		reminderService.setReminderDao(reminderDao);
		reminderService.setSchedulingAssistantService(schedAssistService);
		
		reminderService.setEventUtils(eventUtils);
		List<IReminder> pendingCheck = reminderService.getPendingReminders();
		Assert.assertEquals(1, pendingCheck.size());
		Assert.assertTrue(reminderService.shouldSend(pendingCheck.get(0)));
		reminderService.processPendingReminders();
		
		verify(reminderDao, times(1)).deleteEventReminder(isA(ReminderImpl.class));
	}
	
	@Test
	public void testShouldSend() {
		IReminder reminder = mock(IReminder.class);
		
		DefaultReminderServiceImpl reminderService = new DefaultReminderServiceImpl();
		Assert.assertFalse(reminderService.shouldSend(reminder));
		MockCalendarAccount account = new MockCalendarAccount();
		account.setDisplayName("Some Person");
		account.setEmailAddress("someone@nowhere.com");
		MockScheduleOwner owner = new MockScheduleOwner(account, 1L);
		when(reminder.getScheduleOwner()).thenReturn(owner);
		Assert.assertFalse(reminderService.shouldSend(reminder));
		
		MockCalendarAccount recipient = new MockCalendarAccount();
		recipient.setDisplayName("Some Visitor");
		recipient.setEmailAddress("bogus@nowhere.com");
		MockScheduleVisitor visitor = new MockScheduleVisitor(recipient);
		when(reminder.getRecipient()).thenReturn(recipient);
		Assert.assertFalse(reminderService.shouldSend(reminder));
		
		DefaultEventUtilsImpl eventUtils = new DefaultEventUtilsImpl(new NullAffiliationSourceImpl());
		reminderService.setEventUtils(eventUtils);
		AvailableBlock block = AvailableBlockBuilder.createBlock(new Date(), DateUtils.addMinutes(new Date(), 30));
		VEvent event = eventUtils.constructAvailableAppointment(block, owner, visitor, "test event");
		
		when(reminder.getEvent()).thenReturn(event);
		// owner, visitor set, event exists and visitor attending - return true!
		Assert.assertTrue(reminderService.shouldSend(reminder));
		
		// just change the participation status for the attendee
		Property attendee = eventUtils.getAttendeeForUserFromEvent(event, recipient);
		Parameter partstat = attendee.getParameter(PartStat.PARTSTAT);
		attendee.getParameters().remove(partstat);
		attendee.getParameters().add(PartStat.DECLINED);
		// no longer attending, should return false
		Assert.assertFalse(reminderService.shouldSend(reminder));
		
		// and remove the attendee altogether
		event.getProperties().remove(attendee);
		
		Assert.assertFalse(reminderService.shouldSend(reminder));
	}
	
	@Test
	public void testValidatorSaysNo() {
		IReminder reminder = mock(IReminder.class);
		DefaultReminderServiceImpl reminderService = new DefaultReminderServiceImpl();
		reminderService.setEmailAddressValidator(new EmailAddressValidator() {
			@Override
			public boolean canSendToEmailAddress(ICalendarAccount calendarAccount) {
				return false;
			}
		});
		
		MockCalendarAccount account = new MockCalendarAccount();
		account.setDisplayName("Some Person");
		account.setEmailAddress("someone@nowhere.com");
		MockScheduleOwner owner = new MockScheduleOwner(account, 1L);
		when(reminder.getScheduleOwner()).thenReturn(owner);
		
		MockCalendarAccount recipient = new MockCalendarAccount();
		recipient.setDisplayName("Some Visitor");
		recipient.setEmailAddress("bogus@nowhere.com");
		
		when(reminder.getRecipient()).thenReturn(recipient);
		Assert.assertFalse(reminderService.shouldSend(reminder));
		
		
	}
}
