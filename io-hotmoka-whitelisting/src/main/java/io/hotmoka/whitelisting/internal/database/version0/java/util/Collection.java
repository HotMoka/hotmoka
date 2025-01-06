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

package io.hotmoka.whitelisting.internal.database.version0.java.util;

import io.hotmoka.whitelisting.HasDeterministicTerminatingEqualsAndHashCode;
import io.hotmoka.whitelisting.MustBeSafeLibraryCollection;

public interface Collection<E> {
	int size();
	boolean isEmpty();
	boolean contains(@HasDeterministicTerminatingEqualsAndHashCode java.lang.Object o);
	java.lang.Object[] toArray();
	<T> T[] toArray(T[] a);
	<T> T[] toArray(java.util.function.IntFunction<T[]> generator);
	boolean add(@HasDeterministicTerminatingEqualsAndHashCode E e);
	boolean remove(@HasDeterministicTerminatingEqualsAndHashCode java.lang.Object o);
	boolean removeIf(java.util.function.Predicate<? super E> filter);
	void clear();
	java.util.stream.Stream<E> stream();
	// we do not allow any collection as parameter, because we don't know
	// if their elements have deterministic equals() and hashCode()
	boolean containsAll(@MustBeSafeLibraryCollection java.util.Collection<?> c);
	boolean addAll(@MustBeSafeLibraryCollection java.util.Collection<? extends E> c);
	boolean removeAll(@MustBeSafeLibraryCollection java.util.Collection<?> c);
	boolean retainAll(@MustBeSafeLibraryCollection java.util.Collection<?> c);
}