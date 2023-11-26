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

package io.hotmoka.beans.marshalling.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.marshalling.AbstractObjectMarshaller;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * Knowledge about how a field signature can be marshalled.
 */
public class FieldSignatureMarshaller extends AbstractObjectMarshaller<FieldSignature> {
	
	private final Map<FieldSignature, Integer> memory = new HashMap<>();

	public FieldSignatureMarshaller() {
		super(FieldSignature.class);
	}

	@Override
	public void write(FieldSignature field, MarshallingContext context) throws IOException {
		Integer index = memory.get(field);
		if (index != null) {
			if (index < 254)
				context.writeByte(index);
			else {
				context.writeByte(254);
				context.writeInt(index);
			}
		}
		else {
			int next = memory.size();
			if (next == Integer.MAX_VALUE) // irrealistic
				throw new IllegalStateException("Too many field signatures in the same context");

			memory.put(field, next);

			context.writeByte(255);
			field.definingClass.into(context);
			context.writeStringUnshared(field.name);
			field.type.into(context);
		}
	}
}