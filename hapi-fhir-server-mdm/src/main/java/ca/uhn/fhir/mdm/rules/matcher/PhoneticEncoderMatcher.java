package ca.uhn.fhir.mdm.rules.matcher;

/*-
 * #%L
 * HAPI FHIR - Master Data Management
 * %%
 * Copyright (C) 2014 - 2023 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import ca.uhn.fhir.context.phonetic.IPhoneticEncoder;
import ca.uhn.fhir.context.phonetic.PhoneticEncoderEnum;
import ca.uhn.fhir.util.PhoneticEncoderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhoneticEncoderMatcher implements IMdmStringMatcher {
	private static final Logger ourLog = LoggerFactory.getLogger(PhoneticEncoderMatcher.class);

	private final IPhoneticEncoder myStringEncoder;

	public PhoneticEncoderMatcher(PhoneticEncoderEnum thePhoneticEnum) {
		myStringEncoder = PhoneticEncoderUtil.getEncoder(thePhoneticEnum.name());
	}

	@Override
	public boolean matches(String theLeftString, String theRightString) {
		return myStringEncoder.encode(theLeftString).equals(myStringEncoder.encode(theRightString));
	}
}
