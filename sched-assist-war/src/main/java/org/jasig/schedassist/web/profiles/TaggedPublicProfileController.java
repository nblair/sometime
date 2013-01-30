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

package org.jasig.schedassist.web.profiles;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jasig.schedassist.impl.owner.PublicProfileDao;
import org.jasig.schedassist.model.PublicProfileId;
import org.jasig.schedassist.model.PublicProfileTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * {@link Controller} that is backed by {@link PublicProfileDao#getPublicProfileIdsWithTag(String)}.
 * The tag argument is a {@link PathVariable}.
 * 
 * @author Nicholas Blair
 * @version $Id: TaggedPublicProfileController.java $
 */
@Controller
public class TaggedPublicProfileController {

	private PublicProfileDao publicProfileDao;

	/**
	 * @param publicProfileDao the publicProfileDao to set
	 */
	@Autowired
	public void setPublicProfileDao(PublicProfileDao publicProfileDao) {
		this.publicProfileDao = publicProfileDao;
	}
	
	/**
	 * @return the publicProfileDao
	 */
	public PublicProfileDao getPublicProfileDao() {
		return publicProfileDao;
	}

	@RequestMapping(value="/public/tags/{tag}", method = RequestMethod.GET)
	public String displayProfileIdsByTag(final ModelMap model, @PathVariable("tag") String tag, 
			@RequestParam(value="startIndex",required=false,defaultValue="0") int startIndex) throws UnsupportedEncodingException {
		String decoded = decode(tag);
		List<PublicProfileId> profileIds = this.publicProfileDao.getPublicProfileIdsWithTag(decoded);
		if(profileIds.isEmpty()) {
			// short circuit
			return "profiles/public-listing";
		}
		
		Collections.sort(profileIds);
		ProfilePageInformation pageInfo = new ProfilePageInformation(profileIds, startIndex);
		List<PublicProfileId> sublist = pageInfo.getSublist();
		Map<PublicProfileId, List<PublicProfileTag>> profileMap = publicProfileDao.getProfileTagsBatch(sublist);
		model.addAttribute("titleSuffix", buildPageTitleSuffix(tag, pageInfo.getStartIndex(), pageInfo.getEndIndex()));
		model.addAttribute("profileIds", sublist);
		model.addAttribute("profileMap", profileMap);
		model.addAttribute("showPrev", pageInfo.isShowPrev());
		model.addAttribute("showPrevIndex", pageInfo.getShowPrevIndex());
		model.addAttribute("showNext", pageInfo.isShowNext());
		model.addAttribute("showNextIndex", pageInfo.getShowNextIndex());
		return "profiles/public-listing";
	}
	
	/**
	 * Create a string appended to the document title, e.g.:
	 * 
	 * "Public Profiles tagged with 'tag' - 1 - 10".
	 * 
	 * @param startIndex
	 * @param endIndex
	 * @return
	 */
	protected String buildPageTitleSuffix(String tag, int startIndex, int endIndex) {
		StringBuilder title = new StringBuilder();
		title.append(" labeled with '");
		title.append(tag);
		title.append("' - ");
		title.append(startIndex == 0 ? 1 : startIndex + 1);
		title.append(" - ");
		title.append(endIndex);
		return title.toString();
	}
	
	/**
	 * 
	 * @param tag
	 * @return
	 */
	private String decode(String tag) {
		try {
			String decoded = URLDecoder.decode(tag, "UTF-8");
			return decoded;
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("caught unexpected UnsupportedEncodingException for UTF-8", e);
		}
	}
}
