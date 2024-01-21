/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.beans;

import java.io.IOException;
import java.io.OutputStream;

import io.hotmoka.beans.internal.marshalling.BeanMarshallingContext;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * Providers of bean marshalling contexts.
 */
public abstract class BeanMarshallingContexts {

	private BeanMarshallingContexts() {}

	/**
	 * Yields a marshalling context for beans, more optimized than
	 * a normal context, since it shares subcomponents of the beans.
	 * 
	 * @param oos the stream where bytes are marshalled.
	 * @throws IOException if the context cannot be created
	 * @return the context
	 */
	public static MarshallingContext of(OutputStream oos) throws IOException {
		return new BeanMarshallingContext(oos);
	}
}