/*
Copyright 2023 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.whitelisting.internal.checks;

import io.hotmoka.whitelisting.api.WhiteListingPredicate;
import io.hotmoka.whitelisting.api.WhiteListingWizard;

/**
 * A white-listing predicate stating that value is false.
 */
public class MustBeFalseCheck implements WhiteListingPredicate {

	@Override
	public boolean test(Object value, WhiteListingWizard wizard) {
		return Boolean.FALSE.equals(value);
	}

	@Override
	public String messageIfFailed(String methodName) {
		return "the actual parameter of " + methodName + " must be false";
	}
}