package io.takamaka.code.util;

import java.util.NoSuchElementException;

/**
 * A list of elements. It is possible to access elements at both sides of the list.
 * A list can hold {@code null} elements.
 * This interface has access methods and modification methods.
 *
 * @param <E> the type of the elements. This type must be allowed in storage
 */
public interface ModifiableStorageList<E> extends StorageList<E> {

	/**
	 * Adds the given element as first element of this list.
	 * 
	 * @param element the element, possibly {@code null}
	 */
	void addFirst(E element);

	/**
	 * Adds the given element as last element of this list.
	 * 
	 * @param element the element, possibly {@code null}
	 */
	void addLast(E element);

	/**
	 * Adds the given element as first element of this list.
	 * This is synonym of {@link io.takamaka.code.util.ModifiableStorageList#addLast(E)}.
	 * 
	 * @param element the element, possibly {@code null}
	 */
	void add(E element);

	/**
	 * Clears this list, removing all its elements.
	 */
	void clear();

	/**
	 * Removes and yields the first element of this list, if any.
	 * 
	 * @return the first element, removed from this list
	 * @throws NoSuchElementException if this list is empty
	 */
	E removeFirst();

	/**
	 * Removes the first occurrence of the specified element from this list, if it is present.
	 * If this list does not contain the element, it is unchanged. More formally, removes
	 * the element with the lowest index {@code i} such that
	 * {@code e==null ? get(i)==null : e.equals(get(i))}
	 * (if such an element exists). Returns true if this list contained the specified
	 * element (or equivalently, if this list changed as a result of the call).
	 * 
	 * @param e the element to remove, possibly {@code null}
	 * @return true if and only if the list was modified as result of this call
	 */
	boolean remove(Object e);

	/**
	 * Yields a view of this list. The view reflects the elements in this list:
	 * any future modification of this list will be seen also through the view.
	 * A view is always {@link io.takamaka.code.lang.Exported}.
	 * 
	 * @return a view of this list
	 */
	StorageList<E> view();

	/**
	 * Yields a snapshot of this list. The snapshot contains the elements in this list
	 * but is independent from this list: any future modification of this list will
	 * not be seen through the snapshot. A snapshot is always
	 * {@link io.takamaka.code.lang.Exported}.
	 * 
	 * @return a snapshot of this list
	 */
	StorageList<E> snapshot();
}