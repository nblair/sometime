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

package org.jasig.schedassist.impl.caldav;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.util.Calendars;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jasig.schedassist.ConflictExistsException;
import org.jasig.schedassist.ICalendarDataDao;
import org.jasig.schedassist.NullAffiliationSourceImpl;
import org.jasig.schedassist.SchedulingException;
import org.jasig.schedassist.impl.caldav.xml.ReportResponseHandlerImpl;
import org.jasig.schedassist.impl.events.AutomaticAppointmentCancellationEvent;
import org.jasig.schedassist.impl.events.AutomaticAppointmentCancellationEvent.Reason;
import org.jasig.schedassist.impl.events.AutomaticAttendeeRemovalEvent;
import org.jasig.schedassist.model.AppointmentRole;
import org.jasig.schedassist.model.AvailabilityReflection;
import org.jasig.schedassist.model.AvailableBlock;
import org.jasig.schedassist.model.AvailableSchedule;
import org.jasig.schedassist.model.AvailableVersion;
import org.jasig.schedassist.model.CommonDateOperations;
import org.jasig.schedassist.model.DefaultEventUtilsImpl;
import org.jasig.schedassist.model.ICalendarAccount;
import org.jasig.schedassist.model.IEventUtils;
import org.jasig.schedassist.model.IScheduleOwner;
import org.jasig.schedassist.model.IScheduleVisitor;
import org.jasig.schedassist.model.SchedulingAssistantAppointment;
import org.jasig.schedassist.model.VisitorLimit;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link ICalendarDataDao} for CalDAV-capable calendar servers.
 * 
 * Requires the following be provided (via setter injection):
 * <ul>
 * <li>{@link HttpClient} instance.</li>
 * <li>{@link HttpCredentialsProvider} for authentication.</li>
 * <li>{@link CaldavDialect} instance.</li>
 * <li>A String containing the value of the <i>caldav.cancelUpdatesVisitorCalendar property</i> (false by default).</li>
 * </ul>
 * 
 * The cancelUpdatesVisitorCalendar property can be useful for CalDAV servers that do not delete appointments in an attendee's calendar
 * when the organizer deletes the event. Oracle Communications Suite Calendar server for example will not delete the event from the visitor's
 * calendar when the event is removed from the owner's calendar; the event remains with it's STATUS property set to CANCELLED. Setting the
 * <i>caldav.cancelUpdatesVisitorCalendar property</i> to true will add behavior to {@link #cancelAppointment(IScheduleVisitor, IScheduleOwner, VEvent)}
 * and {@link #leaveAppointment(IScheduleVisitor, IScheduleOwner, VEvent)} remove the CANCELLED entries from the visitor's calendar.
 * 
 * This instance constructs a {@link DefaultCaldavEventUtilsImpl} instance with a {@link NullAffiliationSourceImpl}; if you need to override the {@link IEventUtils}
 * instance, a setter is provided ({@link #setEventUtils(IEventUtils)}).
 * 
 * Lastly this instance constructs a {@link NoopHttpMethodInterceptorImpl} instance; if you need to
 * override the {@link HttpMethodInterceptor} a setter is provided ({@link #setMethodInterceptor(HttpMethodInterceptor)}).
 *
 * @author Nicholas Blair, nblair@doit.wisc.edu
 * @version $Id: CaldavCalendarDataDaoImpl.java 50 2011-05-05 21:07:25Z nblair $
 */
@Service("caldavCalendarDataDao")
public class CaldavCalendarDataDaoImpl implements ICalendarDataDao, InitializingBean {

	static final Header IF_NONE_MATCH_HEADER = new BasicHeader("If-None-Match", "*");
	static final Header ICALENDAR_CONTENT_TYPE_HEADER = new BasicHeader("Content-Type", "text/calendar");
	static final String IF_MATCH_HEADER = "If-Match";

	private static final Header DEPTH_HEADER = new BasicHeader("Depth", "1");
	protected final Log log = LogFactory.getLog(this.getClass());
	private HttpClient httpClient;
	private CredentialsProviderFactory credentialsProviderFactory;
	private HttpHost httpHost;
	private AuthScope caldavAdminAuthScope;
	
	private IEventUtils eventUtils = new CaldavEventUtilsImpl(new NullAffiliationSourceImpl());
	private CaldavDialect caldavDialect;
	private HttpMethodInterceptor methodInterceptor = new NoopHttpMethodInterceptorImpl();
	private boolean cancelUpdatesVisitorCalendar = false;
	private boolean reflectionEnabled = false;
	private boolean preemptiveAuthenticationEnabled = false;
	private boolean getCalendarPerformsPurgeDeclinedAttendees = true;
	private AuthScheme preemptiveAuthenticationScheme;
	private ApplicationEventPublisher applicationEventPublisher;

	/**
	 * @param httpClient the httpClient to set
	 */
	@Autowired
	public void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}
	/**
	 * @return the httpClient
	 */
	public HttpClient getHttpClient() {
		return httpClient;
	}
	/**
	 * @return the credentialsProviderFactory
	 */
	public CredentialsProviderFactory getCredentialsProviderFactory() {
		return credentialsProviderFactory;
	}
	/**
	 * @param credentialsProviderFactory the credentialsProviderFactory to set
	 */
	@Autowired
	public void setCredentialsProviderFactory(
			CredentialsProviderFactory credentialsProviderFactory) {
		this.credentialsProviderFactory = credentialsProviderFactory;
	}
	/**
	 * 
	 * @return
	 */
	public HttpHost getHttpHost() {
		return httpHost;
	}
	/**
	 * 
	 * @param httpHost
	 */
	@Autowired
	public void setHttpHost(HttpHost httpHost) {
		this.httpHost = httpHost;
	}
	/**
	 * @param caldavAdminAuthScope the caldavAdminAuthScope to set
	 */
	@Autowired
	public void setCaldavAdminAuthScope(AuthScope caldavAdminAuthScope) {
		this.caldavAdminAuthScope = caldavAdminAuthScope;
	}
	/**
	 * @return the caldavAdminAuthScope
	 */
	public AuthScope getCaldavAdminAuthScope() {
		return caldavAdminAuthScope;
	}
	/**
	 * @param eventUtils the eventUtils to set
	 */
	@Autowired(required=false)
	public void setEventUtils(IEventUtils eventUtils) {
		this.eventUtils = eventUtils;
	}
	/**
	 * @param caldavDialect the caldavDialect to set
	 */
	@Autowired
	public void setCaldavDialect(CaldavDialect caldavDialect) {
		this.caldavDialect = caldavDialect;
	}
	/**
	 * @param methodInterceptor the methodInterceptor to set
	 */
	@Autowired(required=false)
	public void setMethodInterceptor(HttpMethodInterceptor methodInterceptor) {
		this.methodInterceptor = methodInterceptor;
	}
	/**
	 * @return the methodInterceptor
	 */
	public HttpMethodInterceptor getMethodInterceptor() {
		return methodInterceptor;
	}
	/**
	 * @param applicationEventPublisher the applicationEventPublisher to set
	 */
	@Autowired
	public void setApplicationEventPublisher(
			ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}
	/**
	 * @param cancelUpdatesVisitorCalendar the cancelUpdatesVisitorCalendar to set
	 */
	@Value("${caldav.cancelUpdatesVisitorCalendar:false}")
	public void setCancelUpdatesVisitorCalendar(String cancelUpdatesVisitorCalendar) {
		this.cancelUpdatesVisitorCalendar = Boolean.parseBoolean(cancelUpdatesVisitorCalendar);
	}
	/**
	 * @param reflectionEnabled the reflectionEnabled to set
	 */
	@Value("${caldav.reflectionEnabled:false}")
	public void setReflectionEnabled(boolean reflectionEnabled) {
		this.reflectionEnabled = reflectionEnabled;
	}
	/**
	 * @return the cancelUpdatesVisitorCalendar
	 */
	public boolean isCancelUpdatesVisitorCalendar() {
		return cancelUpdatesVisitorCalendar;
	}
	/**
	 * @param cancelUpdatesVisitorCalendar the cancelUpdatesVisitorCalendar to set
	 */
	public void setCancelUpdatesVisitorCalendar(boolean cancelUpdatesVisitorCalendar) {
		this.cancelUpdatesVisitorCalendar = cancelUpdatesVisitorCalendar;
	}
	/**
	 * @return the eventUtils
	 */
	public IEventUtils getEventUtils() {
		return eventUtils;
	}
	/**
	 * @return the caldavDialect
	 */
	public CaldavDialect getCaldavDialect() {
		return caldavDialect;
	}
	/**
	 * @return the reflectionEnabled
	 */
	public boolean isReflectionEnabled() {
		return reflectionEnabled;
	}
	/**
	 * @return the preemptiveAuthenticationEnabled
	 */
	public boolean isPreemptiveAuthenticationEnabled() {
		return preemptiveAuthenticationEnabled;
	}
	/**
	 * @param preemptiveAuthenticationEnabled the preemptiveAuthenticationEnabled to set
	 */
	@Value("${caldav.preemptiveAuthenticationEnabled:false}")
	public void setPreemptiveAuthenticationEnabled(
			boolean preemptiveAuthenticationEnabled) {
		this.preemptiveAuthenticationEnabled = preemptiveAuthenticationEnabled;
	}
	
	/**
	 * @return the getCalendarPerformsPurgeDeclinedAttendees
	 */
	public boolean isGetCalendarPerformsPurgeDeclinedAttendees() {
		return getCalendarPerformsPurgeDeclinedAttendees;
	}
	/**
	 * @param getCalendarPerformsPurgeDeclinedAttendees the getCalendarPerformsPurgeDeclinedAttendees to set
	 */
	@Value("${caldav.getCalendarPerformsPurgeDeclinedAttendees:true}")
	public void setGetCalendarPerformsPurgeDeclinedAttendees(
			boolean getCalendarPerformsPurgeDeclinedAttendees) {
		this.getCalendarPerformsPurgeDeclinedAttendees = getCalendarPerformsPurgeDeclinedAttendees;
	}
	/**
	 * 
	 * @param scheme
	 * @return
	 */
	protected AuthScheme identifyScheme(String scheme) {
		if(new BasicScheme().getSchemeName().equalsIgnoreCase(scheme)) {
			return new BasicScheme();
		} else if (new DigestScheme().getSchemeName().equalsIgnoreCase(scheme)) {
			return new DigestScheme();
		} else {
			throw new IllegalArgumentException("cannot determine AuthScheme implementation from " + scheme);
		}
	}
	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		if(isPreemptiveAuthenticationEnabled()) {
			// addRequestInterceptor method not visible on the HttpClient interface
			((AbstractHttpClient) this.httpClient).addRequestInterceptor(new PreemptiveAuthInterceptor(caldavAdminAuthScope), 0);
			this.preemptiveAuthenticationScheme = identifyScheme(caldavAdminAuthScope.getScheme());
		}
	}
	/* (non-Javadoc)
	 * @see org.jasig.schedassist.ICalendarDataDao#getCalendar(org.jasig.schedassist.model.ICalendarAccount, java.util.Date, java.util.Date)
	 */
	@Override
	public Calendar getCalendar(ICalendarAccount calendarAccount,
			Date startDate, Date endDate) {
		List<CalendarWithURI> calendars = getCalendarsInternal(calendarAccount, startDate, endDate);
		Calendar result = consolidate(calendars);
		return result;
	}
	/* (non-Javadoc)
	 * @see org.jasig.schedassist.ICalendarDataDao#getExistingAppointment(org.jasig.schedassist.model.IScheduleOwner, org.jasig.schedassist.model.AvailableBlock)
	 */
	@Override
	public VEvent getExistingAppointment(IScheduleOwner owner,
			AvailableBlock block) {
		CalendarWithURI calendarWithUri = getExistingAppointmentInternal(owner, block.getStartTime(), block.getEndTime());
		if(null != calendarWithUri) {
			ComponentList componentList = calendarWithUri.getCalendar().getComponents(VEvent.VEVENT);
			return (VEvent) componentList.get(0);
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.jasig.schedassist.ICalendarDataDao#createAppointment(org.jasig.schedassist.model.IScheduleVisitor, org.jasig.schedassist.model.IScheduleOwner, org.jasig.schedassist.model.AvailableBlock, java.lang.String)
	 */
	@Override
	public VEvent createAppointment(IScheduleVisitor visitor,
			IScheduleOwner owner, AvailableBlock block, String eventDescription) {
		VEvent event = this.eventUtils.constructAvailableAppointment(
				block, 
				owner,
				visitor, 
				eventDescription);
		try {
			int statusCode = putNewEvent(owner.getCalendarAccount(), event);
			if(log.isDebugEnabled()) {
				log.debug("createAppointment status code: " + statusCode);
			}
			if(statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED) {
				return event;
			} else {
				throw new CaldavDataAccessException("createAppointment for " + visitor + ", " + owner + ", " + block + " failed with unexpected status code: " + statusCode);
			}
		} catch (HttpException e) {
			log.error("an HttpException occurred in createAppointment for " + owner + ", " + visitor + ", " + block);
			throw new CaldavDataAccessException(e);
		} catch (IOException e) {
			log.error("an IOException occurred in createAppointment for " + owner + ", " + visitor + ", " + block);
			throw new CaldavDataAccessException(e);
		} 
	}

	/*
	 * (non-Javadoc)
	 * @see org.jasig.schedassist.ICalendarDataDao#cancelAppointment(org.jasig.schedassist.model.IScheduleVisitor, org.jasig.schedassist.model.IScheduleOwner, net.fortuna.ical4j.model.component.VEvent)
	 */
	@Override
	public void cancelAppointment(IScheduleVisitor visitor, IScheduleOwner owner, VEvent appointment) {
		Date startTime = appointment.getStartDate().getDate();
		Date endTime = appointment.getEndDate(true).getDate();

		// first locate event/calendar in owner's account
		CalendarWithURI calendarWithURI = getExistingAppointmentInternal(owner, startTime, endTime);
		if(null != calendarWithURI) {
			VEvent event = extractSchedulingAssistantAppointment(calendarWithURI);
			Uid eventUid = event.getUid();

			int status = deleteCalendar(calendarWithURI, owner.getCalendarAccount());
			if(log.isDebugEnabled()) {
				log.debug("cancelAppointment status code " + status + " for " + owner + ", " + eventUid);
			}

			if(cancelUpdatesVisitorCalendar) {
				CalendarWithURI visitorCalendarWithURI = getExistingAppointmentInternalForVisitor(visitor, startTime, endTime, eventUid);
				if(visitorCalendarWithURI != null) {
					status = deleteCalendar(visitorCalendarWithURI, visitor.getCalendarAccount());
					if(log.isDebugEnabled()) {
						log.debug("cancelAppointment status code " + status + " for " + visitor + ", " + eventUid);
					}
				} else {
					log.warn("cancelAppointment unable to locate event in schedule for visitor " + visitor + " with uid " + eventUid);
				}
			}
		} else {
			log.warn("cannot cancelAppointment for " + owner + ", no matching appointment found (" + appointment + ")");
		}
	}

	/**
	 * Construct an {@link HttpContext} with a {@link CredentialsProvider} appropriate
	 * for the {@link ICalendarAccount} argument.
	 * Returned value is intended for use with {@link HttpClient#execute(HttpHost, HttpRequest, HttpContext)}.
	 * 
	 * @param calendarAccount
	 * @return an appropriate {@link HttpContext} for the {@link ICalendarAccount}.
	 */
	protected HttpContext constructHttpContext(ICalendarAccount calendarAccount) {
		CredentialsProvider credentialsProvider = this.credentialsProviderFactory.getCredentialsProvider(calendarAccount);
		HttpContext context = new BasicHttpContext();
		if(isPreemptiveAuthenticationEnabled()) {
			if(preemptiveAuthenticationScheme == null) {
				throw new IllegalStateException("preemptiveAuthentication is enabled, but the preemptiveAuthenticationScheme is null. Was afterPropertiesSet invoked?");
			}
			context.setAttribute(PreemptiveAuthInterceptor.PREEMPTIVE_AUTH, preemptiveAuthenticationScheme);
		}
		context.setAttribute(ClientContext.CREDS_PROVIDER, credentialsProvider);
		return context;
	}
	/**
	 * 
	 * @param calendarWithURI
	 * @param calendarAccount
	 * @return
	 */
	protected int deleteCalendar(CalendarWithURI calendarWithURI, ICalendarAccount calendarAccount) {
		URI uri = this.caldavDialect.resolveCalendarURI(calendarWithURI);
		HttpDelete method = new HttpDelete(uri.toString());
		if(log.isDebugEnabled()) {
			log.debug("deleteCalendar executing " + methodToString(method) + " for " + calendarAccount);
		}
		HttpRequest toExecute = methodInterceptor.doWithMethod(method, calendarAccount);
		final HttpContext context = constructHttpContext(calendarAccount);

		HttpEntity entity = null;
		try {
			HttpResponse response = this.httpClient.execute(httpHost, toExecute, context);
			entity = response.getEntity();
			int statusCode = response.getStatusLine().getStatusCode();
			log.debug("deleteCalendar status code: " + statusCode);
			if(statusCode == HttpStatus.SC_NO_CONTENT) {
				return statusCode;
			} else {
				throw new CaldavDataAccessException("deleteCalendar for " + calendarAccount + ", " + calendarWithURI +" failed with unexpected status code: " + statusCode);
			}
		} catch (IOException e) {
			log.error("an IOException occurred in deleteCalendar for " + calendarAccount + ", " + calendarWithURI);
			throw new CaldavDataAccessException(e);
		} finally {
			quietlyConsume(entity);
		}
	}

	/* (non-Javadoc)
	 * @see org.jasig.schedassist.ICalendarDataDao#joinAppointment(org.jasig.schedassist.model.IScheduleVisitor, org.jasig.schedassist.model.IScheduleOwner, net.fortuna.ical4j.model.component.VEvent)
	 */
	@Override
	public VEvent joinAppointment(IScheduleVisitor visitor,
			IScheduleOwner owner, VEvent appointment)
					throws SchedulingException {
		Date startTime = appointment.getStartDate().getDate();
		Date endTime = appointment.getEndDate(true).getDate();

		CalendarWithURI calendarWithURI = getExistingAppointmentInternal(owner, startTime, endTime);
		if(null != calendarWithURI) {
			VEvent event = extractSchedulingAssistantAppointment(calendarWithURI);

			Attendee attendee = this.eventUtils.constructSchedulingAssistantAttendee(visitor.getCalendarAccount(), AppointmentRole.VISITOR);
			event.getProperties().add(attendee);
			try {
				int statusCode = putExistingEvent(owner.getCalendarAccount(), event, calendarWithURI.getEtag());
				log.debug("joinAppointment status code: " + statusCode);
				if(statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED || statusCode == HttpStatus.SC_NO_CONTENT) {
					return event;
				} else if (statusCode == HttpStatus.SC_PRECONDITION_FAILED) {
					// event changed in the interim, fail fast
					throw new SchedulingException("joinAppointment failed for " + visitor + " and " + owner + ", appointment was altered");
				} else {
					throw new CaldavDataAccessException("joinAppointment for " + visitor + ", " + owner + ", " + startTime + " failed with unexpected status code: " + statusCode);
				}
			} catch (IOException e) {
				log.error("an IOException occurred in joinAppointment for " + owner + ", " + visitor + ", " + startTime);
				throw new CaldavDataAccessException(e);
			} 
		} else {
			log.warn("cannot joinAppointment for " + owner + ", no matching appointment found (" + appointment + ")");
			throw new SchedulingException("joinAppointment failed for " + visitor + " and " + owner + ", no matching appointment found");
		}
	}

	/* (non-Javadoc)
	 * @see org.jasig.schedassist.ICalendarDataDao#leaveAppointment(org.jasig.schedassist.model.IScheduleVisitor, org.jasig.schedassist.model.IScheduleOwner, net.fortuna.ical4j.model.component.VEvent)
	 */
	@Override
	public VEvent leaveAppointment(IScheduleVisitor visitor,
			IScheduleOwner owner, VEvent appointment)
					throws SchedulingException {
		Date startTime = appointment.getStartDate().getDate();
		Date endTime = appointment.getEndDate(true).getDate();

		CalendarWithURI calendarWithURI = getExistingAppointmentInternal(owner, startTime, endTime);
		if(null != calendarWithURI) {
			VEvent event = extractSchedulingAssistantAppointment(calendarWithURI);
			Uid eventUid = event.getUid();
			Property attendee = this.eventUtils.getAttendeeForUserFromEvent(event, visitor.getCalendarAccount());
			event.getProperties().remove(attendee);
			try {
				int statusCode = putExistingEvent(owner.getCalendarAccount(), event, calendarWithURI.getEtag());
				log.debug("leaveAppointment status code: " + statusCode);
				if(statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED || statusCode == HttpStatus.SC_NO_CONTENT) {
					log.debug("leaveAppointment owner calendar update successful");
				} else if (statusCode == HttpStatus.SC_PRECONDITION_FAILED) {
					// event changed in the interim, fail fast
					throw new SchedulingException("leaveAppointment failed for " + visitor + " and " + owner + ", appointment was altered");
				} else {
					throw new CaldavDataAccessException("leaveAppointment for " + visitor + ", " + owner + ", " + startTime + " failed with unexpected status code: " + statusCode);
				}
			} catch (IOException e) {
				log.error("an IOException occurred in leaveAppointment for " + owner + ", " + visitor + ", " + startTime);
				throw new CaldavDataAccessException(e);
			} 

			if(cancelUpdatesVisitorCalendar) {
				CalendarWithURI visitorCalendarWithURI = getExistingAppointmentInternalForVisitor(visitor, startTime, endTime, eventUid);
				if(visitorCalendarWithURI != null) {
					int status = deleteCalendar(visitorCalendarWithURI, visitor.getCalendarAccount());
					if(log.isDebugEnabled()) {
						log.debug("leaveAppointment status code " + status + " for " + visitor + ", " + eventUid);
					}
				} else {
					log.warn("leaveAppointment unable to locate event in schedule for visitor " + visitor + " with uid " + eventUid);
				}
			}
			return event;
		} else {
			log.warn("cannot leaveAppointment for " + owner + ", no matching appointment found (" + appointment + ")");
			throw new SchedulingException("leaveAppointment failed for " + visitor + " and " + owner + ", no matching appointment found");
		}
	}

	/* (non-Javadoc)
	 * @see org.jasig.schedassist.ICalendarDataDao#checkForConflicts(org.jasig.schedassist.model.IScheduleOwner, org.jasig.schedassist.model.AvailableBlock)
	 */
	@Override
	public void checkForConflicts(IScheduleOwner owner, AvailableBlock block)
			throws ConflictExistsException {
		// use a start and end time slightly smaller than the block to avoid events that start/end on the edge of the block
		Date start = DateUtils.addSeconds(block.getStartTime(), 1);
		Date end = DateUtils.addSeconds(block.getEndTime(), -1);
		List<CalendarWithURI> calendars = getCalendarsInternal(owner.getCalendarAccount(), start, end);
		for(CalendarWithURI calendar: calendars) {
			ComponentList events = calendar.getCalendar().getComponents(VEvent.VEVENT);
			for(Object component : events) {
				VEvent event = (VEvent) component;
				if(this.eventUtils.willEventCauseConflict(owner.getCalendarAccount(), event)) {
					if(log.isDebugEnabled()) {
						log.debug("conflict detected for " + owner + " at block " + block + ", event: " + event);
					}
					throw new ConflictExistsException("an appointment already exists for " + block);
				} 
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.jasig.schedassist.ICalendarDataDao#reflectAvailableSchedule(org.jasig.schedassist.model.IScheduleOwner, org.jasig.schedassist.model.AvailableSchedule)
	 */
	@Override
	public void reflectAvailableSchedule(IScheduleOwner owner,
			AvailableSchedule schedule) {
		if(reflectionEnabled) {
			if(schedule.isEmpty()) {
				return;
			}
			Date startDate = CommonDateOperations.beginningOfDay(schedule.getScheduleStartTime());
			Date endDate = CommonDateOperations.endOfDay(schedule.getScheduleEndTime());
			purgeAvailableScheduleReflections(owner, startDate, endDate);

			List<Calendar> calendars = this.eventUtils.convertScheduleForReflection(schedule);
			for(Calendar calendar: calendars) {
				Uid uid = this.eventUtils.extractUid(calendar);
				if(uid != null) {
					//put!
					try {
						int statusCode = putNewCalendar(owner.getCalendarAccount(), calendar, uid.getValue());
						if(statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED || statusCode == HttpStatus.SC_NO_CONTENT) {
							//success
						} else {
							throw new CaldavDataAccessException("reflectAvailableSchedule for " + owner  + " failed with unexpected status code: " + statusCode);
						}
					} catch (HttpException e) {
						log.error("an HttpException occurred in reflectAvailableSchedule for " + owner);
						throw new CaldavDataAccessException(e);
					} catch (IOException e) {
						log.error("an IOException occurred in reflectAvailableSchedule for " + owner);
						throw new CaldavDataAccessException(e);
					}
				} else {
					log.warn("cannot store reflection for calendar with no UID: " + calendar);
				}
			}
		} else {
			log.debug("experimental feature 'Availability Schedule reflection' disabled by default");
		}
	}

	/* (non-Javadoc)
	 * @see org.jasig.schedassist.ICalendarDataDao#purgeAvailableScheduleReflections(org.jasig.schedassist.model.IScheduleOwner, java.util.Date, java.util.Date)
	 */
	@Override
	public void purgeAvailableScheduleReflections(IScheduleOwner owner,
			Date startDate, Date endDate) {
		if(reflectionEnabled) {
			List<CalendarWithURI> calendars = peekAtAvailableScheduleReflections(owner, startDate, endDate);
			for(CalendarWithURI calendar: calendars) {
				// delete!
				URI uri = this.caldavDialect.resolveCalendarURI(calendar);
				HttpDelete method = new HttpDelete(uri.toString());
				if(log.isDebugEnabled()) {
					log.debug("purgeAvailableScheduleReflections executing " + methodToString(method) + " for " + owner + ", " + startDate + ", " + endDate);
				}
				final HttpContext context = constructHttpContext(owner.getCalendarAccount());
				HttpRequest toExecute = methodInterceptor.doWithMethod(method,owner.getCalendarAccount());
				HttpEntity entity = null;
				try {
					HttpResponse response = this.httpClient.execute(httpHost, toExecute, context);
					entity = response.getEntity();
					int statusCode = response.getStatusLine().getStatusCode();
					log.debug("cancelAppointment status code: " + statusCode);
					if(statusCode == HttpStatus.SC_NO_CONTENT) {
						//success
					} else {
						throw new CaldavDataAccessException("purgeAvailableScheduleReflections for " + owner + ", " + startDate +  ", " + endDate +" failed with unexpected status code: " + statusCode);
					}
				} catch (IOException e) {
					log.error("an IOException occurred in purgeAvailableScheduleReflections for " + owner + ", " + startDate + ", " + endDate);
					throw new CaldavDataAccessException(e);
				} finally {
					quietlyConsume(entity);
				}
			}
		} else {
			log.debug("experimental feature 'Availability Schedule reflection' disabled");
		}
	}

	/**
	 * 
	 * @param owner
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public List<CalendarWithURI> peekAtAvailableScheduleReflections(IScheduleOwner owner,
			Date startDate, Date endDate){
		if(reflectionEnabled) {
			List<CalendarWithURI> calendars = getCalendarsInternal(owner.getCalendarAccount(), startDate, endDate);
			List<CalendarWithURI> results = new ArrayList<CalendarWithURI>();
			for(CalendarWithURI calendar: calendars) {
				ComponentList events = calendar.getCalendar().getComponents(VEvent.VEVENT);
				for(Object component : events) {
					VEvent event = (VEvent) component;
					if(event.getProperties().contains(AvailabilityReflection.TRUE)) {
						results.add(calendar);
					}
				}
			}

			return results;
		} else {
			log.debug("experimental feature 'Availability Schedule reflection' disabled");
			return Collections.emptyList();
		}
	}
	/**
	 * This method is intended to generate a unique URI to use with the PUT method
	 * in {@link #createAppointment(IScheduleVisitor, IScheduleOwner, AvailableBlock, String)}.
	 * 
	 * It is implemented by the following:
	 * <pre>
	 caldavDialect.calculateCalendarAccountHome(owner.getCalendarAccount) + "/sched-assist-" + randomAlphanumeric + ".ics"
	 * </pre>
	 * @param owner
	 * @return
	 */
	protected String generateEventUri(ICalendarAccount owner, VEvent event) {
		Validate.notNull(event, "event argument cannot be null");
		Validate.notNull(event.getUid(), "cannot generateEventUri for event with null UID");
		String accountHome = this.caldavDialect.getCalendarAccountHome(owner);

		StringBuilder eventUri = new StringBuilder(accountHome);
		eventUri.append(event.getUid().getValue());
		eventUri.append(".ics");
		return eventUri.toString();
	}

	/**
	 * 
	 * @param owner
	 * @param eventUid
	 * @return
	 */
	protected String generateEventUri(ICalendarAccount owner, String eventUid) {
		String accountHome = this.caldavDialect.getCalendarAccountHome(owner);
		StringBuilder eventUri = new StringBuilder(accountHome);
		eventUri.append(eventUid);
		eventUri.append(".ics");
		return eventUri.toString();
	}

	/**
	 * 
	 * @param calendarAccount
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws IOException 
	 */
	protected List<CalendarWithURI> getCalendarsInternal(ICalendarAccount calendarAccount,
			Date startDate, Date endDate) {

		String accountUri = this.caldavDialect.getCalendarAccountHome(calendarAccount);
		HttpEntity requestEntity = caldavDialect.generateGetCalendarRequestEntity(startDate, endDate);
		ReportMethod method = new ReportMethod(accountUri);
		method.setEntity(requestEntity);
		//method.addHeader(CONTENT_LENGTH_HEADER, Long.toString(requestEntity.getContentLength()));
		method.addHeader(DEPTH_HEADER);
		if(log.isDebugEnabled()) {
			log.debug("getCalendarsInternal executing " + methodToString(method) + " for " + calendarAccount + ", start " + startDate + ", end " + endDate);
		}
		HttpRequest toExecute = methodInterceptor.doWithMethod(method,calendarAccount);
		final HttpContext context = constructHttpContext(calendarAccount);

		HttpEntity entity = null;
		try {
			HttpResponse response = this.httpClient.execute(httpHost, toExecute, context);
			entity = response.getEntity();
			int statusCode = response.getStatusLine().getStatusCode();
			log.debug("getCalendarsInternal status code: " + statusCode);
			if(statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_MULTI_STATUS) {
				InputStream content = entity.getContent();
				ReportResponseHandlerImpl reportResponseHandler = new ReportResponseHandlerImpl();
				List<CalendarWithURI> calendars = reportResponseHandler.extractCalendars(content);
				if(isGetCalendarPerformsPurgeDeclinedAttendees()) {
					List<CalendarWithURI> results = new ArrayList<CalendarWithURI>();
					for(CalendarWithURI c: calendars) {
						if(purgeDeclinedAttendees(c, calendarAccount) != null) {
							results.add(c);
						}
					}
					return results;
				}
				// purgeDeclinedAttendees disabled
				return calendars;
			} else {
				throw new CaldavDataAccessException("unexpected status code: " + statusCode);
			}
		} catch (IOException e) {
			log.error("an IOException occurred in getCalendarsInternal for " + calendarAccount + ", " + startDate + ", " + endDate);
			throw new CaldavDataAccessException(e);
		} finally {
			quietlyConsume(entity);
		}
	}
	/**
	 * Consolidate the {@link Calendar}s within the argument, returning 1.
	 * 
	 * @see Calendars#merge(Calendar, Calendar)
	 * @param calendars
	 * @return never null
	 * @throws ParserException
	 */
	protected Calendar consolidate(List<CalendarWithURI> calendars) {
		final int size = calendars.size();
		if(size == 0) {
			return new Calendar();
		} else if(size == 1) {
			return calendars.get(0).getCalendar();
		} else if (size == 2) {
			return merge(calendars.get(0).getCalendar(), calendars.get(1).getCalendar());
		} else {
			// create target by merging first 2
			Calendar main = merge(calendars.get(0).getCalendar(), calendars.get(1).getCalendar());
			// loop over the rest
			List<CalendarWithURI> remaining = calendars.subList(2, calendars.size());
			for(Iterator<CalendarWithURI> i = remaining.iterator(); i.hasNext();) {
				CalendarWithURI left = i.next();
				// if there aren't any more in the iterator, merge an empty calendar
				Calendar right = new Calendar();
				if(i.hasNext()) {
					right = i.next().getCalendar();
				} 
				merge(main, left.getCalendar(), right);
			}
			return main;
		}
	}
	
	/**
	 * Merge the components from all calendars into one result.
	 * 
	 * @param calendars
	 * @return
	 */
	protected Calendar merge(Calendar left, Calendar right) {
		Calendar result = new Calendar();
		result.getProperties().add(DefaultEventUtilsImpl.PROD_ID);
		result.getProperties().add(Version.VERSION_2_0);
		merge(result, left, right);
		return result;
	}
	
	/**
	 * Mutative method.
	 * The first {@link Calendar} argument is altered by this method.
	 * 
	 * @param target
	 * @param left
	 * @param right
	 */
	protected void merge(Calendar target, Calendar left, Calendar right) {
		Map<String, VTimeZone> existingTimezones = new HashMap<String, VTimeZone>();
		// first pass is through the target to id VTIMEZONEs already stored
		for(Iterator<?> i = target.getComponents().iterator(); i.hasNext();) {
			Component c = (Component) i.next();
			if(VTimeZone.VTIMEZONE.equals(c.getName())) {
				VTimeZone tz = (VTimeZone) c;
				existingTimezones.put(tz.getTimeZoneId().getValue(), tz);
			}
		}
		// 2nd: pass through left
		for(Iterator<?> i = left.getComponents().iterator(); i.hasNext();) {
			Component c = (Component) i.next();
			final boolean componentIsTimezone = VTimeZone.VTIMEZONE.equals(c.getName());
			if(componentIsTimezone && existingTimezones.containsKey(((VTimeZone) c).getTimeZoneId().getValue())) {
				// don't add this timezone, we've already got a copy
			} else {
				target.getComponents().add(c);
				if(componentIsTimezone) {
					VTimeZone tz = (VTimeZone) c;
					existingTimezones.put(tz.getTimeZoneId().getValue(), tz);
				}
			}
		}
		// 3rd: iterate over the right
		for(Iterator<?> i = right.getComponents().iterator(); i.hasNext();) {
			Component c = (Component) i.next();
			if(VTimeZone.VTIMEZONE.equals(c.getName()) && existingTimezones.containsKey(((VTimeZone) c).getTimeZoneId().getValue())) {
				// don't add this timezone, we've already got a copy
			} else {
				target.getComponents().add(c);
			}
		}
	}

	/**
	 * This method returns the {@link CalendarWithURI} containing a single {@link VEvent} that
	 * was created with the Scheduling Assistant with the specified {@link IScheduleOwner} as the owner
	 * and the specified start and end times.
	 * 
	 * @param owner
	 * @param startTime
	 * @param endTime
	 * @return the matching Scheduling Assistant {@link VEvent}, or null if none for this {@link IScheduleOwner} at the specified times
	 */
	protected CalendarWithURI getExistingAppointmentInternal(IScheduleOwner owner,
			Date startTime, Date endTime) {
		final DateTime targetStartTime = new DateTime(startTime);
		final DateTime targetEndTime = new DateTime(endTime);

		List<CalendarWithURI> calendars = getCalendarsInternal(owner.getCalendarAccount(), startTime, endTime);
		for(CalendarWithURI calendarWithUri : calendars) {
			ComponentList componentList = calendarWithUri.getCalendar().getComponents(VEvent.VEVENT);
			if(componentList.size() != 1) {
				// scheduling assistant creates calendars with only a single event, short-circuit on calendars with > 1 events
				continue;
			}
			for(Object o: componentList) {
				VEvent event = (VEvent) o;
				Date eventStart = event.getStartDate().getDate();
				Date eventEnd = event.getEndDate(true).getDate();
				Property schedAssistProperty = event.getProperty(SchedulingAssistantAppointment.AVAILABLE_APPOINTMENT);
				if(!SchedulingAssistantAppointment.TRUE.equals(schedAssistProperty)) {
					// immediately skip over non-scheduling assistant appointments
					continue;
				}
				// check for version first
				Property versionProperty = event.getProperty(AvailableVersion.AVAILABLE_VERSION);
				if (AvailableVersion.AVAILABLE_VERSION_1_2.equals(versionProperty)) {
					// event has to be (1) an available appointment
					// with (2) owner recognized as appointment owner and
					// (3) start and (4) end date have to match
					if(this.eventUtils.isAttendingAsOwner(event, owner.getCalendarAccount()) &&
							eventStart.equals(targetStartTime) &&
							eventEnd.equals(targetEndTime)) {
						if(log.isDebugEnabled()) {
							log.debug("getExistingAppointmentInternal found " + event);
						}

						return calendarWithUri;
					}
				}
			}
		}
		// not found
		return null;
	}
	/**
	 * Special method used when cancelUpdatesVisitorCalendar is set to true.
	 * Returns the {@link CalendarWithURI} in the visitor's account for the event
	 * with the specified start, end and eventuid.
	 * 
	 * @param owner
	 * @param startTime
	 * @param endTime
	 * @param eventUid
	 * @return the matching event, or null if not found.
	 */
	protected CalendarWithURI getExistingAppointmentInternalForVisitor(IScheduleVisitor visitor, Date startTime, Date endTime, Uid eventUid) {
		final DateTime targetStartTime = new DateTime(startTime);
		final DateTime targetEndTime = new DateTime(endTime);
		if(eventUid == null) {
			log.debug("cannot call getExistingAppointmentInternal with null eventUid, visitor: " + visitor);
			return null;
		}
		List<CalendarWithURI> calendars = getCalendarsInternal(visitor.getCalendarAccount(), startTime, endTime);
		for(CalendarWithURI calendarWithUri : calendars) {
			ComponentList componentList = calendarWithUri.getCalendar().getComponents(VEvent.VEVENT);
			if(componentList.size() != 1) {
				// scheduling assistant creates calendars with only a single event, short-circuit on calendars with > 1 events
				continue;
			}
			for(Object o: componentList) {
				VEvent event = (VEvent) o;
				Date eventStart = event.getStartDate().getDate();
				Date eventEnd = event.getEndDate(true).getDate();

				Uid uid = event.getUid();
				if(uid != null && eventUid.equals(uid) 
						&& Status.VEVENT_CANCELLED.equals(event.getStatus()) 
						&& eventStart.equals(targetStartTime) &&
						eventEnd.equals(targetEndTime)) {
					return calendarWithUri;
				}
			}
		}
		// not found
		return null;
	}
	/**
	 * Store a new calendar using CalDAV PUT.
	 * 
	 * @param eventOwner
	 * @param event
	 * @return
	 * @throws HttpException
	 * @throws IOException
	 */
	protected int putNewCalendar(ICalendarAccount eventOwner, Calendar calendar, String eventUid) throws HttpException, IOException {
		String uri = generateEventUri(eventOwner, eventUid);

		HttpPut method = constructPutMethod(uri, calendar);
		method.addHeader(IF_NONE_MATCH_HEADER);

		HttpRequest toExecute = this.methodInterceptor.doWithMethod(method, eventOwner);
		if(log.isDebugEnabled()) {
			log.debug("putNewCalendar executing " + methodToString(method) + " for " + eventOwner);
		}
		final HttpContext context = constructHttpContext(eventOwner);

		HttpEntity entity = null;
		try {
			HttpResponse response = this.httpClient.execute(httpHost, toExecute, context);
			entity = response.getEntity();
			if(log.isDebugEnabled()) {
				if(entity == null) {
					log.debug("putNewCalendar response entity was null, statusline: " + response.getStatusLine());
				} else {
					InputStream content = entity.getContent();
					log.debug("putNewCalendar response body: " + IOUtils.toString(content));
				}
			}
			int statusCode = response.getStatusLine().getStatusCode();
			return statusCode;
		} finally {
			EntityUtils.consume(entity);
		}

	}
	/**
	 * Store a new event using CalDAV PUT.
	 * 
	 * @param eventOwner
	 * @param event
	 * @return
	 * @throws HttpException
	 * @throws IOException
	 */
	protected int putNewEvent(ICalendarAccount eventOwner, VEvent event) throws HttpException, IOException {
		String uri = generateEventUri(eventOwner, event);

		HttpPut method = constructPutMethod(uri, event);
		method.addHeader(IF_NONE_MATCH_HEADER);

		HttpRequest toExecute = this.methodInterceptor.doWithMethod(method, eventOwner);
		if(log.isDebugEnabled()) {
			log.debug("putNewEvent executing " + methodToString(method) + " for " + eventOwner);
		}
		final HttpContext context = constructHttpContext(eventOwner);

		HttpEntity entity = null;
		try {
			HttpResponse response = this.httpClient.execute(httpHost, toExecute, context);
			entity = response.getEntity();
			if(log.isDebugEnabled()) {
				if(entity == null) {
					log.debug("putNewEvent response entity was null, statusline: " + response.getStatusLine());
				} else {
					InputStream content = entity.getContent();
					log.debug("putNewEvent response body: " + IOUtils.toString(content));
				}
			}
			int statusCode = response.getStatusLine().getStatusCode();
			return statusCode;
		} finally {
			EntityUtils.consume(entity);
		}

	}
	
	/**
	 * Update an existing event using CalDAV PUT.
	 * 
	 * @param eventOwner
	 * @param event
	 * @param etag
	 * @return
	 * @throws HttpException
	 * @throws IOException
	 */
	protected int putExistingEvent(ICalendarAccount eventOwner, VEvent event, String etag) throws IOException {
		String uri = generateEventUri(eventOwner, event);

		HttpPut method = constructPutMethod(uri, event);
		method.addHeader(IF_MATCH_HEADER, etag);

		HttpRequest toExecute = this.methodInterceptor.doWithMethod(method, eventOwner);
		if(log.isDebugEnabled()) {
			log.debug("putExistingEvent executing " + methodToString(method) + " for " + eventOwner);
		}
		final HttpContext context = constructHttpContext(eventOwner);

		HttpEntity entity = null;
		try {
			HttpResponse response = this.httpClient.execute(httpHost, toExecute, context);
			entity = response.getEntity();
			if(log.isDebugEnabled()) {
				log.debug("putExistingEvent response entity is null, response status line: " + response.getStatusLine());
			}
			int statusCode = response.getStatusLine().getStatusCode();
			return statusCode;
		} finally {
			EntityUtils.consume(entity);
		}
	}
	/**
	 * This method will inspect {@link IScheduleVisitor} {@link Attendee}s among the {@link SchedulingAssistantAppointment}s
	 * in the {@link Calendar} argument.
	 * If an {@link Attendee} on an {@link SchedulingAssistantAppointment} has {@link Partstat#DECLINED}, the appointment
	 * will be cancelled (if one on one or lone visitor on group appt) or the attendee will be removed (group appointment
	 * with multiple attending visitors).
	 * 
	 * @param calendarWithURI
	 * @param session
	 * @param owner
	 * @return the calendar minus any events or attendees that have been removed.
	 * @throws SchedulingException 
	 * @throws StatusException 
	 */
	protected CalendarWithURI purgeDeclinedAttendees(CalendarWithURI calendarWithURI, ICalendarAccount owner)  {
		ComponentList componentList = calendarWithURI.getCalendar().getComponents(VEvent.VEVENT);
		if(componentList.size() != 1) {
			return calendarWithURI;
		}
		for(Object o: componentList) {
			VEvent event = (VEvent) o;
			if(event.getStartDate().getDate().before(new java.util.Date())) {
				// short-circuit non events in the past
				continue;
			}
			final boolean hasAvailableAppointmentProperty = SchedulingAssistantAppointment.TRUE.equals(event.getProperty(SchedulingAssistantAppointment.AVAILABLE_APPOINTMENT));
			final boolean isAttendingAsOwner = this.eventUtils.isAttendingAsOwner(event, owner);
			if(hasAvailableAppointmentProperty && isAttendingAsOwner) {
				PropertyList attendeeList = this.eventUtils.getAttendeeListFromEvent(event);		
				Property visitorLimitProp = event.getProperty(VisitorLimit.VISITOR_LIMIT);
				final int visitorLimit = Integer.parseInt(visitorLimitProp.getValue());

				for(Object a : attendeeList) {
					Property attendee = (Property) a;
					if(PartStat.DECLINED.equals(attendee.getParameter(PartStat.PARTSTAT))) {
						log.trace("found attendee that has DECLINED event: " + attendee);
						Parameter appointmentRole = attendee.getParameter(AppointmentRole.APPOINTMENT_ROLE);
						if(AppointmentRole.OWNER.equals(appointmentRole) ) {
							// remove whole appointment
							deleteCalendar(calendarWithURI, owner);
							log.warn("purgeDeclinedAttendees successfully cancelled appointment due to owner decline " + event);

							this.applicationEventPublisher.publishEvent(new AutomaticAppointmentCancellationEvent(event, owner, Reason.OWNER_DECLINED));
							return null;
						} else if (AppointmentRole.VISITOR.equals(appointmentRole)) {
							int availableVisitorCount = this.eventUtils.getScheduleVisitorCount(event);
							if(visitorLimit > 1 && availableVisitorCount > 1) {
								// remove only the attendee (leave event)
								event.getProperties().remove(attendee);

								try {
									int statusCode = putExistingEvent(owner, event, calendarWithURI.getEtag());
									log.debug("purgeDeclinedAttendees leave status code: " + statusCode);
									if(statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED || statusCode == HttpStatus.SC_NO_CONTENT) {
										log.warn("purgeDeclinedAttendees successfully removed declined attendee from group appointment " + event);
										this.applicationEventPublisher.publishEvent(new AutomaticAttendeeRemovalEvent(event, owner, attendee));
									} else if (statusCode == HttpStatus.SC_PRECONDITION_FAILED) {
										// event changed in the interim, fail fast
										//throw new SchedulingException("purgeDeclinedAttendees leave failed for " + attendee + " and " + owner + ", appointment was altered");
										// leave appointment as is
										log.warn("purgeDeclinedAttendees leave failed for " + attendee + " and " + owner + ", appointment was altered");
									} else {
										throw new CaldavDataAccessException("purgeDeclinedAttendees leave failed for " + attendee + ", " + owner + " failed with unexpected status code: " + statusCode);
									}
								} catch (IOException e) {
									log.error("an IOException occurred in joinAppointment for " + owner + ", " + attendee);
									throw new CaldavDataAccessException(e);
								} 


							} else {
								// either one on one appointment or group appointment with only 1 visitor
								// remove whole appointment
								deleteCalendar(calendarWithURI, owner);
								log.warn("purgeDeclinedAttendees successfully cancelled appointment due to no remaining visitors " + event);
								this.applicationEventPublisher.publishEvent(new AutomaticAppointmentCancellationEvent(event, owner, Reason.NO_REMAINING_VISITORS));
								return null;
							}
						}
					} 
				}


			} else {
				if(log.isTraceEnabled()) {
					String eventUid = "not set";
					if(event.getUid() != null) {
						eventUid = event.getUid().getValue();
					}
					log.trace("event (UID=" + eventUid + ") not a candidate for purge, hasAvailableAppointmentProperty=" + hasAvailableAppointmentProperty + ", isAttendingAsOwner=" + isAttendingAsOwner);
				}
			}
		}
		return calendarWithURI;
	}

	/**
	 * 
	 * @param uri
	 * @param event
	 * @return
	 */
	HttpPut constructPutMethod(String uri, VEvent event) {
		HttpPut method = new HttpPut(uri);
		method.addHeader(ICALENDAR_CONTENT_TYPE_HEADER);
		HttpEntity requestEntity = caldavDialect.generatePutAppointmentRequestEntity(event);
		method.setEntity(requestEntity);
		return method;
	}

	/**
	 * 
	 * @param uri
	 * @param event
	 * @return
	 */
	HttpPut constructPutMethod(String uri, Calendar calendar) {
		HttpPut method = new HttpPut(uri);
		method.addHeader(ICALENDAR_CONTENT_TYPE_HEADER);
		HttpEntity requestEntity = caldavDialect.generatePutAppointmentRequestEntity(calendar);
		method.setEntity(requestEntity);
		return method;
	}
	/**
	 * Method intended to pull the single {@link VEvent} from a 
	 * {@link CalendarWithURI} containing a scheduling assistant appointment.
	 * 
	 * @param calendar
	 * @return
	 */
	VEvent extractSchedulingAssistantAppointment(CalendarWithURI calendar) {
		ComponentList events = calendar.getCalendar().getComponents(VEvent.VEVENT);
		Validate.isTrue(events.size() == 1, "expecting calendar with single event");
		return (VEvent) events.get(0);
	}

	/**
	 * Basic toString for {@link HttpRequest} to output method name and path.
	 * 
	 * @param method
	 * @return
	 */
	String methodToString(HttpRequest method) {
		return method.getRequestLine().toString();
	}

	/**
	 * 
	 * @param entity
	 */
	void quietlyConsume(HttpEntity entity) {
		try {
			EntityUtils.consume(entity);
		} catch (IOException e) {
			log.info("caught IOException from EntityUtils#consume", e);
		}
	}
}
