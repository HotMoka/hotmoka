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

package io.hotmoka.node.local.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.instrumentation.api.InstrumentationFields;
import io.hotmoka.node.DeserializationError;
import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.Updates;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.internal.transactions.AbstractResponseBuilder;

/**
 * An extractor of the updates to the state reachable, in RAM, from some storage objects.
 * This object is used after a transaction, to collect the fields that have changed
 * their value during the transaction.
 */
public class UpdatesExtractorFromRAM {

	/**
	 * The builder of the transaction for which this extractor works.
	 */
	private final AbstractResponseBuilder<?,?> builder;

	/**
	 * Builds an extractor of the updates to the state reachable from some storage objects.
	 * 
	 * @param builder the builder of the transaction for which the extraction is performed
	 */
	public UpdatesExtractorFromRAM(AbstractResponseBuilder<?,?> builder) {
		this.builder = builder;
	}

	/**
	 * Yields the updates extracted from the given storage objects and from the objects
	 * reachable from them, recursively.
	 * 
	 * @param objects the storage objects whose updates must be computed (for them and
	 *                for the objects recursively reachable from them)
	 * @return the updates, sorted
	 */
	public Stream<Update> extractUpdatesFrom(Stream<Object> objects) {
		return new Processor(objects).updates.stream();
	}

	/**
	 * Internal scope for extracting the updates to some objects.
	 */
	private class Processor {

		/**
		 * The class loader for the transaction that uses this extractor.
		 */
		private final EngineClassLoader classLoader;

		/**
		 * The set of objects to process. This gets expanded as soon as new objects are found to be reachable.
		 */
		private final List<Object> workingSet;

		/**
		 * The set of all objects processed so far. This is needed to avoid processing the same object twice.
		 */
		private final Set<StorageReference> seen = new HashSet<>();

		/**
		 * The extracted updates.
		 */
		private final SortedSet<Update> updates = new TreeSet<>();

		/**
		 * Builds an internal scope to extract the updates to the given objects,
		 * and those reachable from them, recursively.
		 * 
		 * @param objects the storage objects whose updates must be computed (for them and
		 *                for the objects recursively reachable from them)
		 */
		private Processor(Stream<Object> objects) {
			this.classLoader = builder.classLoader;
			this.workingSet = objects
				.filter(object -> seen.add(classLoader.getStorageReferenceOf(object)))
				.collect(Collectors.toList());

			do {
				// removes the next storage object to scan for updates and continues recursively
				// with the objects that can be reached from it, until no new object can be reached
				new ExtractedUpdatesSingleObject(workingSet.remove(workingSet.size() - 1));
			}
			while (!workingSet.isEmpty());
		}

		/**
		 * The internal scope to extract the updates to a given object.
		 */
		private class ExtractedUpdatesSingleObject {

			/**
			 * The reference of the object.
			 */
			private final StorageReference storageReference;

			/**
			 * True if and only if the object was already in storage.
			 */
			private final boolean inStorage;

			/**
			 * Builds the scope to extract the updates to a given object.
			 * 
			 * @param object the object
			 */
			private ExtractedUpdatesSingleObject(Object object) {
				Class<?> clazz = object.getClass();
				this.storageReference = classLoader.getStorageReferenceOf(object);
				this.inStorage = classLoader.getInStorageOf(object);

				if (!inStorage)
					updates.add(Updates.classTag(storageReference, StorageTypes.classOf(clazz), classLoader.transactionThatInstalledJarFor(clazz)));

				Class<?> previous = null;
				while (previous != classLoader.getStorage()) {
					addUpdatesForFieldsDefinedInClass(clazz, object);
					previous = clazz;
					clazz = clazz.getSuperclass();
				}
			}

			/**
			 * Utility method called for update extraction to recur on the old value of fields of reference type.
			 * 
			 * @param s the storage objects whose fields are considered
			 */
			private void recursiveExtract(Object s) {
				if (s != null) {
					Class<?> clazz = s.getClass();
					if (classLoader.getStorage().isAssignableFrom(clazz)) {
						if (seen.add(classLoader.getStorageReferenceOf(s)))
							workingSet.add(s);
					}
					else if (classLoader.isLazilyLoaded(clazz)) // eager types are not recursively followed
						throw new DeserializationError("a field of a storage object cannot hold a " + clazz.getName());
				}
			}

			/**
			 * Takes note that a field of lazy type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of the storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param fieldClassName the name of the type of the field
			 * @param o the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, String fieldClassName, Object o) {
				FieldSignature field = FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.classNamed(fieldClassName));

				if (o == null)
					// the field has been set to null
					updates.add(Updates.toNull(storageReference, field, false));
				else if (classLoader.getStorage().isAssignableFrom(o.getClass())) {
					// the field has been set to a storage object
					StorageReference storageReference2 = classLoader.getStorageReferenceOf(o);
					updates.add(Updates.ofStorage(storageReference, field, storageReference2));

					// if the new value has not yet been considered, we put in the list of object still to be processed
					if (seen.add(storageReference2))
						workingSet.add(o);
				}
				// the following cases occur if the declared type of the field is Object but it is updated
				// to an object whose type is allowed in storage
				else if (o instanceof String s)
					updates.add(Updates.ofString(storageReference, field, s));
				else if (o instanceof BigInteger bi)
					updates.add(Updates.ofBigInteger(storageReference, field, bi));
				else if (o instanceof Enum<?> e) {
					var clazz = e.getClass();
					if (hasInstanceFields(clazz))
						throw new DeserializationError("Field " + field + " of a storage object cannot hold an enumeration of class " + clazz.getName() + ": it has instance non-transient fields");

					updates.add(Updates.ofEnum(storageReference, field, clazz.getName(), e.name(), false));
				}
				else
					throw new DeserializationError("Field " + field + " of a storage object cannot hold a " + o.getClass().getName());
			}

			/**
			 * Determines if the given enumeration type has at least an instance, non-transient field.
			 * 
			 * @param clazz the class
			 * @return true only if that condition holds
			 */
			private boolean hasInstanceFields(Class<?> clazz) {
				return Stream.of(clazz.getDeclaredFields())
					.map(Field::getModifiers)
					.anyMatch(modifiers -> !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers));
			}

			/**
			 * Takes note that a field of {@code boolean} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, boolean s) {
				updates.add(Updates.ofBoolean(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.BOOLEAN), s));
			}

			/**
			 * Takes note that a field of {@code byte} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, byte s) {
				updates.add(Updates.ofByte(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.BYTE), s));
			}

			/**
			 * Takes note that a field of {@code char} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, char s) {
				updates.add(Updates.ofChar(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.CHAR), s));
			}

			/**
			 * Takes note that a field of {@code double} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, double s) {
				updates.add(Updates.ofDouble(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.DOUBLE), s));
			}

			/**
			 * Takes note that a field of {@code float} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, float s) {
				updates.add(Updates.ofFloat(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.FLOAT), s));
			}

			/**
			 * Takes note that a field of {@code int} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, int s) {
				updates.add(Updates.ofInt(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.INT), s));
			}

			/**
			 * Takes note that a field of {@code long} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, long s) {
				updates.add(Updates.ofLong(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.LONG), s));
			}

			/**
			 * Takes note that a field of {@code short} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, short s) {
				updates.add(Updates.ofShort(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.SHORT), s));
			}

			/**
			 * Takes note that a field of {@link java.lang.String} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, String s) {
				if (s == null)
					updates.add(Updates.toNull(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.STRING), true));
				else
					updates.add(Updates.ofString(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.STRING), s));
			}

			/**
			 * Takes note that a field of {@link java.math.BigInteger} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param bi the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, BigInteger bi) {
				FieldSignature field = FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.BIG_INTEGER);
				if (bi == null)
					updates.add(Updates.toNull(storageReference, field, true));
				else
					updates.add(Updates.ofBigInteger(storageReference, field, bi));
			}

			/**
			 * Takes note that a field of enumeration type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param fieldClassName the name of the type of the field
			 * @param element the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, String fieldClassName, Enum<?> element) {
				FieldSignature field = FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.classNamed(fieldClassName));
				if (element == null)
					updates.add(Updates.toNull(storageReference, field, true));
				else
					updates.add(Updates.ofEnum(storageReference, field, element.getClass().getName(), element.name(), true));
			}

			/**
			 * Takes note of updates to the fields of the given object, defined in the given class.
			 * 
			 * @param clazz the class
			 * @param object the object
			 */
			private void addUpdatesForFieldsDefinedInClass(Class<?> clazz, Object object) {
				for (Field field: clazz.getDeclaredFields())
					if (!isStaticOrTransient(field)) {
						field.setAccessible(true); // it might be private
						Object currentValue, oldValue;

						try {
							currentValue = field.get(object);
						}
						catch (IllegalArgumentException | IllegalAccessException e) {
							throw new IllegalStateException("cannot access field " + field.getDeclaringClass().getName() + "." + field.getName(), e);
						}

						String oldName = InstrumentationFields.OLD_PREFIX + field.getName();
						try {
							Field oldField = field.getDeclaringClass().getDeclaredField(oldName);
							oldField.setAccessible(true); // it is always private
							oldValue = oldField.get(object);
						}
						catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
							throw new IllegalStateException("cannot access old value for field " + field.getDeclaringClass().getName() + "." + field.getName(), e);
						}

						if (!inStorage || !Objects.equals(oldValue, currentValue))
							addUpdateFor(field, currentValue);

						if (inStorage && classLoader.isLazilyLoaded(field.getType()))
							recursiveExtract(oldValue);
					}
			}

			/**
			 * Takes note that a field has been updated to a new current value.
			 * 
			 * @param field the field
			 * @param currentValue the current value of the field
			 */
			private void addUpdateFor(Field field, Object currentValue) {
				Class<?> fieldType = field.getType();
				String fieldDefiningClass = field.getDeclaringClass().getName();
				String fieldName = field.getName();

				if (fieldType == char.class)
					addUpdateFor(fieldDefiningClass, fieldName, (char) currentValue);
				else if (fieldType == boolean.class)
					addUpdateFor(fieldDefiningClass, fieldName, (boolean) currentValue);
				else if (fieldType == byte.class)
					addUpdateFor(fieldDefiningClass, fieldName, (byte) currentValue);
				else if (fieldType == short.class)
					addUpdateFor(fieldDefiningClass, fieldName, (short) currentValue);
				else if (fieldType == int.class)
					addUpdateFor(fieldDefiningClass, fieldName, (int) currentValue);
				else if (fieldType == long.class)
					addUpdateFor(fieldDefiningClass, fieldName, (long) currentValue);
				else if (fieldType == float.class)
					addUpdateFor(fieldDefiningClass, fieldName, (float) currentValue);
				else if (fieldType == double.class)
					addUpdateFor(fieldDefiningClass, fieldName, (double) currentValue);
				else if (fieldType == BigInteger.class)
					addUpdateFor(fieldDefiningClass, fieldName, (BigInteger) currentValue);
				else if (fieldType == String.class)
					addUpdateFor(fieldDefiningClass, fieldName, (String) currentValue);
				else if (fieldType.isEnum())
					addUpdateFor(fieldDefiningClass, fieldName, fieldType.getName(), (Enum<?>) currentValue);
				else if (classLoader.isLazilyLoaded(fieldType))
					addUpdateFor(fieldDefiningClass, fieldName, fieldType.getName(), currentValue);
				else
					throw new IllegalStateException("unexpected field in storage object: " + fieldDefiningClass + '.' + fieldName);
			}

			/**
			 * Determines if the given field is static or transient, hence its updates are not extracted.
			 * 
			 * @param field the field
			 * @return true if and only if that condition holds
			 */
			private boolean isStaticOrTransient(Field field) {
				int modifiers = field.getModifiers();
				return Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers);
			}
		}
	}
}