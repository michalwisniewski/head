package org.mifos.application.util.helpers;

import org.mifos.config.LocalizedTextLookup;

/**

 * YesNoFlag.java    version: 1.0

 

 * Copyright (c) 2005-2006 Grameen Foundation USA

 * 1029 Vermont Avenue, NW, Suite 400, Washington DC 20005

 * All rights reserved.

 

 * Apache License 
 * Copyright (c) 2005-2006 Grameen Foundation USA 
 * 

 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 
 *

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the 

 * License. 
 * 
 * See also http://www.apache.org/licenses/LICENSE-2.0.html for an explanation of the license 

 * and how it is applied. 

 *

 */


public enum YesNoFlag implements LocalizedTextLookup {
	NO((short) 0), YES((short) 1);

	Short value;

	YesNoFlag(Short value) {
		this.value = value;
	}

	public Short getValue() {
		return value;
	}
	
	public static YesNoFlag fromInt(int value) {
		for (YesNoFlag candidate : values()) {
			if (candidate.getValue() == value) {
				return candidate;
			}
		}
		throw new RuntimeException("no field type " + value);
	}
	
	public String getPropertiesKey() {
		return "YesNoFlag." + toString();
	}
	
}
