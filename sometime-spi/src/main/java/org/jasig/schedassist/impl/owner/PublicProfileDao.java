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

package org.jasig.schedassist.impl.owner;

import java.util.List;
import java.util.Map;

import org.jasig.schedassist.model.IScheduleOwner;
import org.jasig.schedassist.model.PublicProfile;
import org.jasig.schedassist.model.PublicProfileId;
import org.jasig.schedassist.model.PublicProfileTag;

/**
 * Interface defining operations for creating/retrieving/updating/deleting {@link PublicProfile}s.
 *  
 * @author Nicholas Blair, nblair@doit.wisc.edu
 * @version $Id: PublicProfileDao.java 2128 2010-05-19 18:52:44Z npblair $
 */
public interface PublicProfileDao {

	/**
	 * Create a public profile for the owner.
	 * 
	 * @param owner
	 * @param profileDescription
	 * @return the new public profile
	 * @throws PublicProfileAlreadyExistsException 
	 */
	PublicProfile createPublicProfile(IScheduleOwner owner, String profileDescription) throws PublicProfileAlreadyExistsException;
	
	/**
	 * Update an existing public profile.
	 * 
	 * @param profileId
	 * @param profileDescription
	 * @return the updated profile
	 */
	PublicProfile updatePublicProfileDescription(PublicProfileId profileId, String profileDescription);
	
	/**
	 * Remove an existing public profile.
	 * 
	 * @param profileId
	 */
	void removePublicProfile(PublicProfileId profileId);
	
	/**
	 * Attempt to locate an existing {@link PublicProfile} by  
	 * the value of it's profile key.
	 * Returns null if not found.
	 * 
	 * @param profileKey
	 * @return the correspoding public profile, or null if not found
	 */
	PublicProfile locatePublicProfileByKey(String profileKey);
	
	/**
	 * Attempt to locate an existing {@link PublicProfile}
	 * for the owner argument.
	 * Returns null if not found.
	 * 
	 * @param owner
	 * @return the correspoding public profile, or null if not found
	 */
	PublicProfile locatePublicProfileByOwner(IScheduleOwner owner);
	
	/**
	 * Return a {@link List} of all {@link PublicProfileId}s.
	 * 
	 * @return a possible empty, but never null {@link List}
	 */
	List<PublicProfileId> getPublicProfileIds();
	
	/**
	 * Return a subset of {@link PublicProfileId}s.
	 * Implementation will order {@link PublicProfileId}s by the owner display name.
	 * 
	 * @param sortComparator
	 * @param indexStart
	 * @param indexEnd
	 * @return a possible empty, but never null {@link List}
	 */
	List<PublicProfileId> getPublicProfileIds(int indexStart, int indexEnd);
	
	/**
	 * Return a subset of {@link PublicProfileId}s for Academic Advisors.
	 * Implementation will order {@link PublicProfileId}s by the owner display name.
	 * 
	 * @return a possibly empty, but never null {@link List}
	 */
	List<PublicProfileId> getAdvisorPublicProfileIds();
	
	/**
	 * Return the {@link List} of {@link PublicProfileTag}s for the specified profile.
	 * 
	 * @param profileId
	 * @return a possibly empty, but never null {@link List}
	 */
	List<PublicProfileTag> getProfileTags(PublicProfileId profileId);
	
	/**
	 * Convenience method to return the {@link List}s of {@link PublicProfileTag}s for
	 * multiple {@link PublicProfileId}s.
	 * 
	 * @param profileIds
	 * @return a possibly empty, but never null {@link Map}
	 */
	Map<PublicProfileId, List<PublicProfileTag>> getProfileTagsBatch(List<PublicProfileId> profileIds);
	/**
	 * Set the {@link PublicProfileTag}s for the {@link PublicProfileId}. This method is
	 * intended to replace any existing tags when called and store solely the argument.
	 * 
	 * @param tags
	 * @param profileId
	 * @return the new {@link List} of {@link PublicProfileTag}s for the profile; never null, but possibly empty (if the argument was empty)
	 */
	List<PublicProfileTag> setProfileTags(List<String> tags, PublicProfileId profileId);
	
	/**
	 * Return a {@link List} of {@link PublicProfileId}s with the specified tag.
	 * 
	 * @param tag
	 * @return a possibly empty, but never null {@link List}
	 */
	List<PublicProfileId> getPublicProfileIdsWithTag(String tag);

	/**
	 * Return a subset of {@link PublicProfileId}s for Instructors.
	 * Implementation will order {@link PublicProfileId}s by the owner display name.
	 * 
	 * @return a possibly empty, but never null {@link List}
	 */
	List<PublicProfileId> getInstructorPublicProfileIds();
}
