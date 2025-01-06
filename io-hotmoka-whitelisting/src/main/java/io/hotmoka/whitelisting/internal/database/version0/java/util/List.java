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

public interface List<E> {
	E get(int index);
	E remove(int index);
	boolean remove(java.lang.Object o);
	boolean contains(java.lang.Object o);
	void sort(java.util.Comparator<? super E> c);
	E set(int index, @HasDeterministicTerminatingEqualsAndHashCode E element);
	void add(int index, @HasDeterministicTerminatingEqualsAndHashCode E element);
	int indexOf(java.lang.Object o);
	int lastIndexOf(java.lang.Object o);
	java.util.ListIterator<E> listIterator();
	java.util.ListIterator<E> listIterator(int index);
	java.util.List<E> subList(int fromIndex, int toIndex);
	java.util.Spliterator<E> spliterator();
	static <E> java.util.List<E> of() { return null; }
	static <E> java.util.List<E> of(@HasDeterministicTerminatingEqualsAndHashCode E e1) { return null; }
	static <E> java.util.List<E> of(@HasDeterministicTerminatingEqualsAndHashCode E e1, @HasDeterministicTerminatingEqualsAndHashCode E e2) { return null; }
	static <E> java.util.List<E> of(@HasDeterministicTerminatingEqualsAndHashCode E e1, @HasDeterministicTerminatingEqualsAndHashCode E e2, @HasDeterministicTerminatingEqualsAndHashCode E e3) { return null; }
	static <E> java.util.List<E> of(@HasDeterministicTerminatingEqualsAndHashCode E e1, @HasDeterministicTerminatingEqualsAndHashCode E e2, @HasDeterministicTerminatingEqualsAndHashCode E e3, @HasDeterministicTerminatingEqualsAndHashCode E e4) { return null; }
	static <E> java.util.List<E> of(@HasDeterministicTerminatingEqualsAndHashCode E e1, @HasDeterministicTerminatingEqualsAndHashCode E e2, @HasDeterministicTerminatingEqualsAndHashCode E e3, @HasDeterministicTerminatingEqualsAndHashCode E e4, @HasDeterministicTerminatingEqualsAndHashCode E e5) { return null; }
	static <E> java.util.List<E> of(@HasDeterministicTerminatingEqualsAndHashCode E e1, @HasDeterministicTerminatingEqualsAndHashCode E e2, @HasDeterministicTerminatingEqualsAndHashCode E e3, @HasDeterministicTerminatingEqualsAndHashCode E e4, @HasDeterministicTerminatingEqualsAndHashCode E e5, @HasDeterministicTerminatingEqualsAndHashCode E e6) { return null; }
	static <E> java.util.List<E> of(@HasDeterministicTerminatingEqualsAndHashCode E e1, @HasDeterministicTerminatingEqualsAndHashCode E e2, @HasDeterministicTerminatingEqualsAndHashCode E e3, @HasDeterministicTerminatingEqualsAndHashCode E e4, @HasDeterministicTerminatingEqualsAndHashCode E e5, @HasDeterministicTerminatingEqualsAndHashCode E e6, @HasDeterministicTerminatingEqualsAndHashCode E e7) { return null; }
	static <E> java.util.List<E> of(@HasDeterministicTerminatingEqualsAndHashCode E e1, @HasDeterministicTerminatingEqualsAndHashCode E e2, @HasDeterministicTerminatingEqualsAndHashCode E e3, @HasDeterministicTerminatingEqualsAndHashCode E e4, @HasDeterministicTerminatingEqualsAndHashCode E e5, @HasDeterministicTerminatingEqualsAndHashCode E e6, @HasDeterministicTerminatingEqualsAndHashCode E e7, @HasDeterministicTerminatingEqualsAndHashCode E e8) { return null; }
	static <E> java.util.List<E> of(@HasDeterministicTerminatingEqualsAndHashCode E e1, @HasDeterministicTerminatingEqualsAndHashCode E e2, @HasDeterministicTerminatingEqualsAndHashCode E e3, @HasDeterministicTerminatingEqualsAndHashCode E e4, @HasDeterministicTerminatingEqualsAndHashCode E e5, @HasDeterministicTerminatingEqualsAndHashCode E e6, @HasDeterministicTerminatingEqualsAndHashCode E e7, @HasDeterministicTerminatingEqualsAndHashCode E e8, @HasDeterministicTerminatingEqualsAndHashCode E e9) { return null; }
	static <E> java.util.List<E> of(@HasDeterministicTerminatingEqualsAndHashCode E e1, @HasDeterministicTerminatingEqualsAndHashCode E e2, @HasDeterministicTerminatingEqualsAndHashCode E e3, @HasDeterministicTerminatingEqualsAndHashCode E e4, @HasDeterministicTerminatingEqualsAndHashCode E e5, @HasDeterministicTerminatingEqualsAndHashCode E e6, @HasDeterministicTerminatingEqualsAndHashCode E e7, @HasDeterministicTerminatingEqualsAndHashCode E e8, @HasDeterministicTerminatingEqualsAndHashCode E e9, @HasDeterministicTerminatingEqualsAndHashCode E e10) { return null; }
	static <E> java.util.List<E> copyOf(@MustBeSafeLibraryCollection java.util.Collection<? extends E> coll) { return null; }
}