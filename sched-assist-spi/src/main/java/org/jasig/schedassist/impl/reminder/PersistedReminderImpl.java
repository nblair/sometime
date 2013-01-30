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

import java.util.Date;

import net.fortuna.ical4j.model.component.VEvent;

import org.jasig.schedassist.model.AvailableBlock;
import org.jasig.schedassist.model.AvailableBlockBuilder;
import org.jasig.schedassist.model.ICalendarAccount;
import org.jasig.schedassist.model.IScheduleOwner;

/**
 * Represents a persisted {@link IReminder}.
 * {@link #getRecipient()}, {@link #getScheduleOwner()}, and {@link #getEvent()} 
 * intentionally always return null.
 *
 * @see DefaultReminderServiceImpl#complete(PersistedReminderImpl)
 * @author Nicholas Blair
 * @version $Id: PersistedReminderImpl.java $
 */
class PersistedReminderImpl implements IReminder {

	private long reminderId;
	private long ownerId;
	private String recipientId;
	private Date sendTime;
	private Date blockStartTime;
	private Date blockEndTime;
		
	/**
	 * @return the reminderId
	 */
	public long getReminderId() {
		return reminderId;
	}
	/**
	 * @param reminderId the reminderId to set
	 */
	public void setReminderId(long reminderId) {
		this.reminderId = reminderId;
	}
	/**
	 * @return the ownerId
	 */
	public long getOwnerId() {
		return ownerId;
	}
	/**
	 * @param ownerId the ownerId to set
	 */
	public void setOwnerId(long ownerId) {
		this.ownerId = ownerId;
	}
	/**
	 * @return the recipientId
	 */
	public String getRecipientId() {
		return recipientId;
	}
	/**
	 * @param recipientId the recipientId to set
	 */
	public void setRecipientId(String recipientId) {
		this.recipientId = recipientId;
	}
	/**
	 * @return the sendTime
	 */
	public Date getSendTime() {
		return sendTime;
	}
	/**
	 * @param sendTime the sendTime to set
	 */
	public void setSendTime(Date sendTime) {
		this.sendTime = sendTime;
	}
	/**
	 * @return the blockStartTime
	 */
	public Date getBlockStartTime() {
		return blockStartTime;
	}
	/**
	 * @param blockStartTime the blockStartTime to set
	 */
	public void setBlockStartTime(Date blockStartTime) {
		this.blockStartTime = blockStartTime;
	}
	/**
	 * @return the blockEndTime
	 */
	public Date getBlockEndTime() {
		return blockEndTime;
	}
	/**
	 * @param blockEndTime the blockEndTime to set
	 */
	public void setBlockEndTime(Date blockEndTime) {
		this.blockEndTime = blockEndTime;
	}
	/**
	 * 
	 * @return the {@link AvailableBlock} 
	 */
	public AvailableBlock getTargetBlock() {
		return AvailableBlockBuilder.createBlock(blockStartTime, blockEndTime);
	}
	
	
	@Override
	public IScheduleOwner getScheduleOwner() {
		return null;
	}
	
	@Override
	public ICalendarAccount getRecipient() {
		return null;
	}
	
	@Override
	public VEvent getEvent() {
		return null;
	}

}
