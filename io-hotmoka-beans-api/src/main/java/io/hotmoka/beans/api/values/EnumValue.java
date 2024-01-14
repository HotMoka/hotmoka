/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.beans.api.values;

import io.hotmoka.annotations.Immutable;

/**
 * An element of an enumeration stored in blockchain.
 */
@Immutable
public interface EnumValue extends StorageValue {

	/**
	 * Yields the name of the class of the enumeration.
	 * 
	 * @return the name of the class of the enumeration
	 */
	String getEnumClassName();

	/**
	 * Yields the name of the enumeration element.
	 * 
	 * @return the name of the enumeration element
	 */
	String getName();
}