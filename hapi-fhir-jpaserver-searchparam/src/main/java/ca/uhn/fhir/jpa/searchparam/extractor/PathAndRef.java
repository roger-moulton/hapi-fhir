package ca.uhn.fhir.jpa.searchparam.extractor;

/*
 * #%L
 * HAPI FHIR Search Parameters
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

import org.hl7.fhir.instance.model.api.IBaseReference;

public class PathAndRef {

	private final String myPath;
	private final IBaseReference myRef;
	private final String mySearchParamName;
	private final boolean myCanonical;

	/**
	 * Constructor
	 */
	public PathAndRef(String theSearchParamName, String thePath, IBaseReference theRef, boolean theCanonical) {
		super();
		mySearchParamName = theSearchParamName;
		myPath = thePath;
		myRef = theRef;
		myCanonical = theCanonical;
	}

	public boolean isCanonical() {
		return myCanonical;
	}

	public String getSearchParamName() {
		return mySearchParamName;
	}

	public String getPath() {
		return myPath;
	}

	public IBaseReference getRef() {
		return myRef;
	}

}
