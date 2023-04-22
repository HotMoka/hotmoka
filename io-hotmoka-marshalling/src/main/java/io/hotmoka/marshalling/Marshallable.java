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

package io.hotmoka.marshalling;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import io.hotmoka.marshalling.internal.MarshallingContextImpl;

/**
 * An object that can be marshaled into a stream, in a way
 * more compact than standard Java serialization. Typically,
 * this works because of context information about the structure
 * of the object.
 */
public abstract class Marshallable {

	/**
	 * Marshals this object into a given stream. This method in general
	 * performs better than standard Java serialization, wrt the size of the marshalled data.
	 * 
	 * @param context the context holding the stream
	 * @throws IOException if this object cannot be marshalled
	 */
	public abstract void into(MarshallingContext context) throws IOException;

	/**
	 * Marshals this object into a byte array.
	 * 
	 * @return the byte array resulting from marshalling this object
	 */
	public final byte[] toByteArray() {
		try (var baos = new ByteArrayOutputStream(); var context = createMarshallingContext(baos)) {
			into(context);
			context.flush();
			return baos.toByteArray();
		}
		catch (IOException e) {
			// impossible for a ByteArrayOutputStream
			throw new RuntimeException("unexpected exception", e);
		}
	}

	/**
	 * Marshals an array of marshallables into a given stream.
	 * 
	 * @param marshallables the array of marshallables
	 * @param context the context holding the stream
	 * @throws IOException if some element could not be marshalled
	 */
	protected static void intoArray(Marshallable[] marshallables, MarshallingContext context) throws IOException {
		context.writeCompactInt(marshallables.length);

		for (Marshallable marshallable: marshallables)
			marshallable.into(context);
	}

	/**
	 * Creates a marshalling context for this object.
	 * 
	 * @param os the output stream of the context
	 * @return the marshalling context
	 * @throws IOException if the marshalling context cannot be created
	 */
	protected MarshallingContext createMarshallingContext(OutputStream os) throws IOException {
		return new MarshallingContextImpl(os);
	}
}