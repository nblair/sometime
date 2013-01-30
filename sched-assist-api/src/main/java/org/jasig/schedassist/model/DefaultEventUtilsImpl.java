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

package org.jasig.schedassist.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.UUID;

import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Rsvp;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.Created;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.LastModified;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RDate;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Sequence;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Transp;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.schedassist.IAffiliationSource;
import org.jasig.schedassist.NullAffiliationSourceImpl;

/**
 * Default {@link IEventUtils} implementation.
 *  
 * @author Nicholas Blair
 */
public class DefaultEventUtilsImpl implements IEventUtils {

	/**
	 * Date/time format for iCalendar.
	 */
	public static final String ICAL_DATETIME_FORMAT = "yyyyMMdd'T'HHmmss'Z'";
	/**
	 * {@link ProdId} attached to {@link Calendar}s sent to the CalDAV server by the Scheduling Assistant.
	 */
	public static final ProdId PROD_ID = new ProdId("-//jasig.org//Jasig Scheduling Assistant 1.1//EN");
	// Commons-Lang provides a thread-safe replacement for SimpleDateFormat
	private static final FastDateFormat FASTDATEFORMAT = FastDateFormat.getInstance(ICAL_DATETIME_FORMAT, 
			TimeZone.getTimeZone("UTC"));

	protected final Log LOG = LogFactory.getLog(this.getClass());

	private final IAffiliationSource affiliationSource;
	private String eventClassForPersonOwners = Clazz.CONFIDENTIAL.getValue();
	private String eventClassForResourceOwners = Clazz.PUBLIC.getValue();

	/**
	 * Default constructor, sets the {@link IAffiliationSource} to the 
	 * {@link NullAffiliationSourceImpl} implementation.
	 */
	public DefaultEventUtilsImpl() {
		this(new NullAffiliationSourceImpl());
	}
	/**
	 * @param affiliationSource
	 */
	public DefaultEventUtilsImpl(IAffiliationSource affiliationSource) {
		this.affiliationSource = affiliationSource;
	}
	/**
	 * @return the affiliationSource
	 */
	public IAffiliationSource getAffiliationSource() {
		return affiliationSource;
	}

	/**
	 * @return the eventClassForPersonOwners
	 */
	public String getEventClassForPersonOwners() {
		return eventClassForPersonOwners;
	}
	/**
	 * @return the eventClassForResourceOwners
	 */
	public String getEventClassForResourceOwners() {
		return eventClassForResourceOwners;
	}
	/**
	 * @param eventClassForPersonOwners the eventClassForPersonOwners to set
	 */
	public void setEventClassForPersonOwners(String eventClassForPersonOwners) {
		this.eventClassForPersonOwners = eventClassForPersonOwners;
	}
	/**
	 * @param eventClassForResourceOwners the eventClassForResourceOwners to set
	 */
	public void setEventClassForResourceOwners(String eventClassForResourceOwners) {
		this.eventClassForResourceOwners = eventClassForResourceOwners;
	}
	/*
	 * (non-Javadoc)
	 * @see org.jasig.schedassist.model.IEventUtils#attendeeMatchesPerson(net.fortuna.ical4j.model.Property, org.jasig.schedassist.model.ICalendarAccount)
	 */
	@Override
	public boolean attendeeMatchesPerson(Property attendee,
			ICalendarAccount calendarAccount) {
		if(null == attendee) {
			return false;
		}

		Cn cn = (Cn) attendee.getParameter(Cn.CN);
		if(null == cn) {
			return false;
		}
		boolean cnResult = cn.getValue().equals(calendarAccount.getDisplayName());

		URI mailTo = emailToURI(calendarAccount.getEmailAddress());
		boolean mailResult = attendee.getValue().equals(mailTo.toString());

		return cnResult && mailResult;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jasig.schedassist.model.IEventUtils#constructAvailableAppointment(org.jasig.schedassist.model.AvailableBlock, org.jasig.schedassist.model.IScheduleOwner, org.jasig.schedassist.model.IScheduleVisitor, java.lang.String)
	 */
	@Override
	public VEvent constructAvailableAppointment(AvailableBlock block,
			IScheduleOwner owner, IScheduleVisitor visitor,
			String eventDescription) {
		Validate.notNull(block, "available block cannot be null");
		Validate.notNull(owner, "schedule owner cannot be null");
		Validate.notNull(visitor, "schedule visitor cannot be null");

		try {
			VEvent event = new VEvent();
			event.getProperties().add(new DtStart(new DateTime(convertToICalendarFormat(block.getStartTime()))));
			event.getProperties().add(new DtEnd(new DateTime(convertToICalendarFormat(block.getEndTime()))));

			Organizer ownerOrganizer = constructOrganizer(owner.getCalendarAccount());
			event.getProperties().add(ownerOrganizer);

			Attendee visitorAttendee = constructSchedulingAssistantAttendee(visitor.getCalendarAccount(), AppointmentRole.VISITOR);
			event.getProperties().add(visitorAttendee);
			Attendee ownerAttendee = constructSchedulingAssistantAttendee(owner.getCalendarAccount(), AppointmentRole.OWNER);
			event.getProperties().add(ownerAttendee);

			// add custom UW-AVAILABLE-APPOINTMENT and UW-AVAILABLE-VERSION
			event.getProperties().add(SchedulingAssistantAppointment.TRUE);
			event.getProperties().add(AvailableVersion.AVAILABLE_VERSION_1_2);
			// add X-Uw-AVAILABLE-VISITORLIMIT
			event.getProperties().add(new VisitorLimit(block.getVisitorLimit()));

			StringBuilder title = new StringBuilder();
			title.append(owner.getPreference(Preferences.MEETING_PREFIX));

			// update title with visitor name and add description only if visitorLimit == 1
			if(block.getVisitorLimit() == 1) {
				title.append(" with ");
				title.append(visitor.getCalendarAccount().getDisplayName());

				// build event description
				Description description = new Description(eventDescription);
				event.getProperties().add(description);
			} 

			// finally add meeting title
			event.getProperties().add(new Summary(title.toString()));

			if(owner.getCalendarAccount() instanceof IDelegateCalendarAccount) {
				event.getProperties().add(new Clazz(eventClassForResourceOwners));
			} else {
				event.getProperties().add(new Clazz(eventClassForPersonOwners));
			}

			// check if block overrides meeting location
			final String blockMeetingLocationOverride = block.getMeetingLocation();
			if(StringUtils.isNotBlank(blockMeetingLocationOverride)) {
				event.getProperties().add(new Location(blockMeetingLocationOverride));
			} else {
				// fall back to owner's preferred location (if set)
				final String preferredLocation = owner.getPreferredLocation();
				if(StringUtils.isNotBlank(preferredLocation)) {
					event.getProperties().add(new Location(preferredLocation));
				}
			}

			// add CONFIRMED status
			event.getProperties().add(Status.VEVENT_CONFIRMED);

			// lastly we must add a UID
			event.getProperties().add(generateNewUid());

			return event;
		} catch (ParseException e) {
			throw new IllegalArgumentException("caught ParseException creating event", e);
		}
	}

	/**
	 * Called by {@link #constructAvailableAppointment(AvailableBlock, IScheduleOwner, IScheduleVisitor, String)};
	 * returns an appropriate {@link Clazz} depending on whether or not the {@link ICalendarAccount} is a resource account.
	 * 
	 * @param calendarAccount
	 * @return an appropriate {@link Clazz} property to attach to the event
	 */
	protected Clazz determineAppropriateClassProperty(ICalendarAccount calendarAccount) {
		if(calendarAccount instanceof IDelegateCalendarAccount) {
			return new Clazz(eventClassForResourceOwners);
		}

		return new Clazz(eventClassForPersonOwners);
	}

	/**
	 * Construct an {@link Attendee} property for the specified user and role.
	 * The PARTSTAT parameter will be set to ACCEPTED.
	 * The RSVP parameter will be set to FALSE.
	 * The X-UW-AVAILABLE-APPOINTMENT-ROLE parameter will be set according to the role argument.
	 * The CN parameter will be set to the {@link ICalendarAccount}'s display name.
	 * The value will be a mailto address for the {@link ICalendarAccount}'s email address.
	 * 
	 * @see org.jasig.schedassist.model.IEventUtils#constructSchedulingAssistantAttendee(org.jasig.schedassist.model.ICalendarAccount, org.jasig.schedassist.model.AppointmentRole)
	 */
	@Override
	public Attendee constructSchedulingAssistantAttendee(
			ICalendarAccount calendarAccount, AppointmentRole role) {
		ParameterList parameterList = new ParameterList();
		parameterList.add(PartStat.ACCEPTED);
		parameterList.add(Rsvp.FALSE);
		parameterList.add(role);
		parameterList.add(new Cn(calendarAccount.getDisplayName()));
		Attendee attendee = new Attendee(parameterList, emailToURI(calendarAccount.getEmailAddress()));
		return attendee;
	}
	/**
	 * Construct an {@link Organizer} property for the specified {@link ICalendarAccount}.
	 * 
	 * @param calendarAccount
	 * @return an {@link Organizer} property for the {@link ICalendarAccount}
	 */
	public Organizer constructOrganizer(ICalendarAccount calendarAccount) {
		ParameterList parameterList = new ParameterList();
		parameterList.add(new Cn(calendarAccount.getDisplayName()));
		parameterList.add(AppointmentRole.OWNER);
		Organizer organizer = new Organizer(parameterList, emailToURI(calendarAccount.getEmailAddress()));
		return organizer;
	}
	/**
	 * Walk through the attendee list in the {@link VEvent} argument.
	 * Return the matching {@link Attendee} for the {@link ICalendarAccount} argument, or null
	 * if the {@link ICalendarAccount} is not in the attendee list.
	 * 
	 * @return the matching attendee property, or null
	 */
	@Override
	public Property getAttendeeForUserFromEvent(VEvent event,
			ICalendarAccount calendarUser) {
		if(null == event || null == calendarUser) {
			return null;
		}

		PropertyList propertyList = getAttendeeListFromEvent(event);
		for(Object o: propertyList) {
			Property attendee = (Property) o;
			if(attendeeMatchesPerson(attendee, calendarUser)) {
				return attendee;
			}
		}

		//otherwise the calendarUser might be the organizer
		if(isAttendingAsOwner(event, calendarUser)) {
			return event.getProperty(Organizer.ORGANIZER);
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jasig.schedassist.model.IEventUtils#getAttendeeListFromEvent(net.fortuna.ical4j.model.component.VEvent)
	 */
	@Override
	public PropertyList getAttendeeListFromEvent(VEvent event) {
		if(null == event) {
			return new PropertyList();
		} else {
			PropertyList attendees = event.getProperties(Attendee.ATTENDEE);
			return attendees;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.jasig.schedassist.model.IEventUtils#getScheduleVisitorCount(net.fortuna.ical4j.model.component.VEvent)
	 */
	@Override
	public int getScheduleVisitorCount(VEvent event) {
		if(null == event) {
			return 0;
		}
		PropertyList propertyList = event.getProperties(Attendee.ATTENDEE);
		int count = 0;
		for(Object o: propertyList) {
			Attendee attendee = (Attendee) o;
			Parameter role = attendee.getParameter(AppointmentRole.APPOINTMENT_ROLE);
			if(null != role && AppointmentRole.VISITOR.equals(role)) {
				count++;
			}
		}
		return count;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jasig.schedassist.model.IEventUtils#willEventCauseConflict(org.jasig.schedassist.model.ICalendarAccount, net.fortuna.ical4j.model.component.VEvent)
	 */
	@Override
	public boolean willEventCauseConflict(ICalendarAccount calendarAccount, VEvent event) {
		// check to see if the owner an attendee and has ACCEPTED
		Property ownerAttendee = getAttendeeForUserFromEvent(event, calendarAccount);

		if(ownerAttendee != null) {
			if(Organizer.ORGANIZER.equals(ownerAttendee.getName())) {
				return true;
			}
			Parameter p = ownerAttendee.getParameter(PartStat.PARTSTAT);
			return PartStat.ACCEPTED.equals(p);
		}
		return false;
	}

	/**
	 * Check the event to see if this event represents an existing available appointment
	 * that the visitor is "visiting" and the owner is "owning" (including the special case
	 * when visitor and owner are the same person)
	 * 
	 * Will return true if the event's attendees match the roles passed in.
	 * 
	 * The purpose is to test if this event should be marked with ATTENDING status for the visitor.
	 * 
	 * @param event
	 * @param visitor
	 * @param owner
	 * @return true if and only if the event if is an available appointment with attendees that match the visitor and owner with the matching roles
	 */
	public boolean isAttendingMatch(VEvent event, IScheduleVisitor visitor, IScheduleOwner owner) {
		// only test the appointment if it's marked as an available appointment
		if(event.getProperties().contains(SchedulingAssistantAppointment.TRUE)) {
			boolean visitorMatch = isAttendingAsVisitor(event, visitor.getCalendarAccount());
			if(!visitorMatch) {
				return false;
			}

			boolean ownerMatch = isAttendingAsOwner(event, owner.getCalendarAccount());
			return ownerMatch;
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jasig.schedassist.model.IEventUtils#isAttendingAsVisitor(net.fortuna.ical4j.model.component.VEvent, org.jasig.schedassist.model.ICalendarAccount)
	 */
	@Override
	public boolean isAttendingAsVisitor(VEvent event,
			ICalendarAccount proposedVisitor) {
		// only test the appointment if it's marked as an available appointment
		if(event.getProperties().contains(SchedulingAssistantAppointment.TRUE)) {
			PropertyList attendees = getAttendeeListFromEvent(event);
			// walk through attendee list
			for(Object obj : attendees) {
				Property attendee = (Property) obj;
				// extract UW APPOINTMENT_ROLE
				Parameter p = attendee.getParameter(AppointmentRole.APPOINTMENT_ROLE);
				if(null != p) {
					AppointmentRole role = new AppointmentRole(p.getValue());
					if(role.isVisitor() && attendeeMatchesPerson(attendee, proposedVisitor)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jasig.schedassist.model.IEventUtils#isAttendingAsOwner(net.fortuna.ical4j.model.component.VEvent, org.jasig.schedassist.model.ICalendarAccount)
	 */
	@Override
	public boolean isAttendingAsOwner(VEvent event,
			ICalendarAccount proposedOwner) {
		// only test the appointment if it's marked as an available appointment
		if(event.getProperties().contains(SchedulingAssistantAppointment.TRUE)) {
			Organizer organizer = (Organizer) event.getProperty(Organizer.ORGANIZER);
			if(organizer == null) {
				return false;

			}
			return attendeeMatchesPerson(organizer, proposedOwner);
		}

		return false;
	}


	/*
	 * (non-Javadoc)
	 * @see org.jasig.schedassist.model.IEventUtils#convertScheduleForReflection(org.jasig.schedassist.model.AvailableSchedule)
	 */
	@Override
	public List<net.fortuna.ical4j.model.Calendar> convertScheduleForReflection(
			final AvailableSchedule availableSchedule) {
		if(availableSchedule.isEmpty()) {
			return Collections.emptyList();
		}
		SortedSet<AvailableBlock> combinedBlocks = AvailableBlockBuilder.combine(availableSchedule.getAvailableBlocks());
		Map<String, VEvent> summaryToEvent = new HashMap<String, VEvent>();
		for(AvailableBlock block: combinedBlocks) {
			String summary = constructSummaryValueForReflectionEvent(block);
			VEvent event = summaryToEvent.get(summary);
			if(event == null) {
				event = convertBlockToReflectionEvent(block);
				summaryToEvent.put(summary, event);
			} else {
				// add Rdate to existing event
				net.fortuna.ical4j.model.Date start = new net.fortuna.ical4j.model.Date(DateUtils.truncate(block.getStartTime(), java.util.Calendar.DATE));
				DateList dates = new DateList(Value.DATE);
				dates.add(start);

				RDate rDate = new RDate(dates);
				event.getProperties().add(rDate);
			}
		}

		List<net.fortuna.ical4j.model.Calendar> results = new ArrayList<net.fortuna.ical4j.model.Calendar>();
		for(VEvent e: summaryToEvent.values()) {
			ComponentList components = new ComponentList();
			components.add(e);
			results.add(wrapInternal(components));
		}
		return results;
	}

	/* (non-Javadoc)
	 * @see org.jasig.schedassist.model.IEventUtils#wrapEventInCalendar(net.fortuna.ical4j.model.component.VEvent)
	 */
	@Override
	public net.fortuna.ical4j.model.Calendar wrapEventInCalendar(VEvent event) {
		ComponentList components = new ComponentList();
		components.add(event);
		net.fortuna.ical4j.model.Calendar result = wrapInternal(components);
		return result;
	}
	/**
	 * 
	 * @param components
	 * @return
	 */
	protected net.fortuna.ical4j.model.Calendar wrapInternal(ComponentList components) {
		net.fortuna.ical4j.model.Calendar result = new net.fortuna.ical4j.model.Calendar(components);
		result.getProperties().add(Version.VERSION_2_0);
		result.getProperties().add(PROD_ID);
		return result;
	}
	/* (non-Javadoc)
	 * @see org.jasig.schedassist.model.IEventUtils#generateNewUid()
	 */
	@Override
	public Uid generateNewUid() {
		UUID uuid = UUID.randomUUID();
		Uid result = new Uid(uuid.toString()); 
		return result;
	}

	/**
	 * Convert the {@link AvailableBlock} into a {@link VEvent} that can be stored in the
	 * calendar system to represent that same block.
	 * 
	 * This event MUST not cause conflicts.
	 * 
	 * @param block
	 * @return an appropriate event
	 */
	protected VEvent convertBlockToReflectionEvent(final AvailableBlock block) {
		Date blockStartTime = DateUtils.truncate(block.getStartTime(), Calendar.DATE);
		DtStart start = new DtStart(new net.fortuna.ical4j.model.Date(blockStartTime));
		DtStamp stamp = new DtStamp(new net.fortuna.ical4j.model.DateTime(new Date()));
		Date blockEndTime = DateUtils.addDays(blockStartTime, 1);
		DtEnd end = new DtEnd(new net.fortuna.ical4j.model.Date(blockEndTime));

		PropertyList properties = new PropertyList();
		properties.add(new Summary(constructSummaryValueForReflectionEvent(block)));
		properties.add(start);
		properties.add(stamp);
		properties.add(end);
		properties.add(new Created(new DateTime(new Date())));
		properties.add(new LastModified(new DateTime(new Date())));
		properties.add(Clazz.PRIVATE);
		properties.add(new Sequence(0));

		properties.add(Transp.TRANSPARENT);

		if(StringUtils.isNotBlank(block.getMeetingLocation())) {
			properties.add(new Location(block.getMeetingLocation()));
		} 
		properties.add(AvailabilityReflection.TRUE);
		VEvent event = new VEvent(properties);
		return event;
	}

	/**
	 * 
	 * @param block
	 * @return
	 */
	protected String constructSummaryValueForReflectionEvent(final AvailableBlock block) {
		SimpleDateFormat df = new SimpleDateFormat("h:mm a");
		StringBuilder summary = new StringBuilder();
		summary.append("Available ");
		summary.append(df.format(block.getStartTime()));
		summary.append(" - ");
		summary.append(df.format(block.getEndTime()));
		return summary.toString();
	}

	/**
	 * Helper method to convert the {@link Date} into the 
	 * {@link String} representation required for iCalendar.
	 * 
	 * The returned date/time will be in the UTC timezone.
	 * 
	 * @param date
	 * @return the date as a string
	 * @throws IllegalArgumentException if the date argument was null
	 */
	public static String convertToICalendarFormat(final Date date) {
		Validate.notNull(date, "cannot format null date");
		return FASTDATEFORMAT.format(DateUtils.truncate(date, Calendar.SECOND));
	}

	/**
	 * 
	 * @param attendee
	 * @return true if if the attendee property has PARSTAT=NEEDS_ACTION
	 */
	public static boolean isPartStatNeedsAction(final Property attendee) {
		Validate.notNull(attendee);
		Parameter p = attendee.getParameter(PartStat.PARTSTAT);
		if(PartStat.NEEDS_ACTION.equals(p)) {
			return true;
		}
		return false;
	}

	/**
	 * Convert the {@link String} argument to a mailto {@link URI} if possible.
	 * 
	 * @param emailAddress
	 * @return the email as a URI
	 * @throws IllegalArgumentException if conversion failed, or if the argument was empty
	 */
	public static URI emailToURI(final String emailAddress) {
		Validate.notEmpty(emailAddress, "emailAddress cannot be null");
		try {
			return new URI("mailto:" + emailAddress);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("caught URISyntaxException trying to construct mailto URI for " + emailAddress, e);
		}
	}


	/* (non-Javadoc)
	 * @see org.jasig.schedassist.model.IEventUtils#getEventVisitorLimit(net.fortuna.ical4j.model.component.VEvent)
	 */
	@Override
	public Integer getEventVisitorLimit(VEvent event) {
		if(event == null) {
			return null;
		}
		Property limit = event.getProperty(VisitorLimit.VISITOR_LIMIT);
		if(limit != null) {
			return Integer.parseInt(limit.getValue());
		}

		return null;
	}
	/*
	 * (non-Javadoc)
	 * @see org.jasig.schedassist.model.IEventUtils#isEventRecurring(net.fortuna.ical4j.model.component.VEvent)
	 */
	@Override
	public boolean isEventRecurring(VEvent event) {
		return event.getProperties(RDate.RDATE).size() > 0 || event.getProperties(RRule.RRULE).size() > 0;
	}
	/*
	 * (non-Javadoc)
	 * @see org.jasig.schedassist.model.IEventUtils#expandRecurrence(net.fortuna.ical4j.model.component.VEvent, java.util.Date, java.util.Date)
	 */
	@Override
	public PeriodList calculateRecurrence(VEvent event,
			Date startBoundary, Date endBoundary) {
		Period period = new Period(new DateTime(startBoundary), new DateTime(endBoundary));
		PeriodList periodList = event.calculateRecurrenceSet(period);
		PeriodList results = new PeriodList();
		for(Object o: periodList) {
			Period p = (Period) o;
			
			if(isAllDayPeriod(p)) {
				// this period is broken
				// the Periods returned by ical4j's calculateRecurrenceSet have range start/ends that are off by the system default's timezone offset
				TimeZone systemTimezone = java.util.TimeZone.getDefault();

				int offset = systemTimezone.getOffset(p.getStart().getTime());
				Period fixed = new Period(
						new DateTime(DateUtils.addMilliseconds(p.getRangeStart(), -offset)), 
						new DateTime(DateUtils.addMilliseconds(p.getRangeEnd(), -offset)));
				results.add(fixed);
			} else {
				results.add(p);
			}
		}
		return results;
	}
	protected boolean isAllDayPeriod(Period period) {
		Dur duration = period.getDuration();
		return duration.getDays() == 1 && duration.getHours() == 0 && duration.getMinutes() == 0;
	}
	/**
	 * 
	 * @param event
	 * @return
	 */
	protected boolean isAllDayEvent(VEvent event) {
		return Value.DATE.equals(event.getStartDate().getParameter(Value.VALUE));
	}
	/* (non-Javadoc)
	 * @see org.jasig.schedassist.model.IEventUtils#extractUid(net.fortuna.ical4j.model.Calendar)
	 */
	@Override
	public Uid extractUid(net.fortuna.ical4j.model.Calendar calendar) {
		ComponentList components = calendar.getComponents();
		Uid unique = null;
		for(Iterator<?> i = components.iterator(); i.hasNext();) {
			Component component = (Component) i.next();
			if(VEvent.VEVENT.equals(component.getName())) {
				Uid uid = ((VEvent) component).getUid();
				if(unique == null) {
					unique = uid;
				} else if (!unique.equals(uid) && uid != null) {
					LOG.info("extractUid encountered a calendar that has 2 (or more) distinct UID values (" + unique.getValue() + ", " + uid.getValue() +")");
					return null;
				}
			}
		}
		return unique;
	}

}
