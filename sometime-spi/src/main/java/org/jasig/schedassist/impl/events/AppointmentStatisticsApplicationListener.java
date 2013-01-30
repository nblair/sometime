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

package org.jasig.schedassist.impl.events;

import java.util.Date;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.schedassist.impl.EventType;
import org.jasig.schedassist.model.ICalendarAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * {@link ApplicationListener} that inserts a row in the 
 * statistics table for each {@link AbstractAppointmentEvent}.
 *  
 * @author Nicholas Blair, nblair@doit.wisc.edu
 * @version $Id: AppointmentStatisticsApplicationListener.java 2832 2010-11-02 17:07:37Z npblair $
 */
@Component
public class AppointmentStatisticsApplicationListener implements
		ApplicationListener<AbstractAppointmentEvent> {

	private Log LOG = LogFactory.getLog(this.getClass());
	private SimpleJdbcTemplate simpleJdbcTemplate;
	private DataFieldMaxValueIncrementer statisticsEventIdSequence;
	private String identifyingAttributeName = "uid";
	
	/**
	 * 
	 * @param dataSource
	 */
	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}
	/**
	 * @param statisticsEventIdSequence the statisticsEventIdSequence to set
	 */
	@Autowired
	public void setStatisticsEventIdSequence(
			@Qualifier("statistics") DataFieldMaxValueIncrementer statisticsEventIdSequence) {
		this.statisticsEventIdSequence = statisticsEventIdSequence;
	}
	/**
	 * 
	 * @param identifyingAttributeName
	 */
	@Value("${users.visibleIdentifierAttributeName:uid}")
	public void setIdentifyingAttributeName(String identifyingAttributeName) {
		this.identifyingAttributeName = identifyingAttributeName;
	}
	/**
	 * 
	 * @return the attribute used to commonly uniquely identify an account
	 */
	public String getIdentifyingAttributeName() {
		return identifyingAttributeName;
	}
	/* (non-Javadoc)
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	@Async
	@Override
	public void onApplicationEvent(final AbstractAppointmentEvent event) {
		final EventType type = EventType.fromEvent(event);
		final long eventId = statisticsEventIdSequence.nextLongValue();
		if(LOG.isDebugEnabled()) {
			LOG.debug("attempting to store event " + event + " with new event ID " + eventId);
		}
		final String accountIdentifier = getIdentifyingAttribute(event.getVisitor().getCalendarAccount());
		int rows = this.simpleJdbcTemplate.update("insert into event_statistics (event_id,owner_id,visitor_id,event_type,event_timestamp,event_start) values (?,?,?,?,?,?)", 
				eventId,
				event.getOwner().getId(),
				accountIdentifier,
				type.toString(),
				new Date(event.getTimestamp()),
				event.getBlock().getStartTime());
		if(LOG.isDebugEnabled()) {
			LOG.debug("insert complete, " + rows + " rows affected ");
		}
	}

	/**
	 * 
	 * @param account
	 * @return the value of {@link #getIdentifyingAttributeName()} for the account
	 * @throws IllegalStateException if the account does not have a value for that attribute.
	 */
	protected String getIdentifyingAttribute(ICalendarAccount account) {
		final String accountIdentifier = account.getAttributeValue(identifyingAttributeName);
		if(StringUtils.isBlank(accountIdentifier)) {
			LOG.error(identifyingAttributeName + " attribute not present for calendarAccount " + account + "; this scenario suggests either a problem with the account, or a deployment configuration problem. Please set the 'users.visibleIdentifierAttributeName' appropriately.");
			throw new IllegalStateException(identifyingAttributeName + " attribute not present for calendarAccount " + account);
		}
		return accountIdentifier;
	}
}
