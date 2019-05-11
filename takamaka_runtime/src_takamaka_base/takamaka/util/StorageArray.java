package takamaka.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import takamaka.lang.Storage;
import takamaka.lang.View;

/**
 * A map between storage objects.
 *
 * This code is derived from Sedgewick and Wayne's code for
 * red-black trees, with some adaptation. It implements an associative
 * map from keys to values. The map can be kept in storage. Keys
 * and values must have types allowed in storage. Keys are kept in
 * comparable order, if they implement {@link java.lang.Comparable}.
 * Otherwise, they must extend {@link takamaka.lang.Storage} and
 * are kept in chronological order.
 *
 * This class represents an ordered symbol table of generic key-value pairs.
 * It supports the usual <em>put</em>, <em>get</em>, <em>contains</em>,
 * <em>delete</em>, <em>size</em>, and <em>is-empty</em> methods.
 * It also provides ordered methods for finding the <em>minimum</em>,
 * <em>maximum</em>, <em>floor</em>, and <em>ceiling</em>.
 * A symbol table implements the <em>associative array</em> abstraction:
 * when associating a value with a key that is already in the symbol table,
 * the convention is to replace the old value with the new value.
 * Unlike {@link java.util.Map}, this class uses the convention that
 * values cannot be {@code null}—setting the
 * value associated with a key to {@code null} is equivalent to deleting the key
 * from the symbol table.
 * <p>
 * This implementation uses a left-leaning red-black BST. It requires that
 * the key type implements the {@code Comparable} interface and calls the
 * {@code compareTo()} and method to compare two keys. It does not call either
 * {@code equals()} or {@code hashCode()}.
 * The <em>put</em>, <em>contains</em>, <em>remove</em>, <em>minimum</em>,
 * <em>maximum</em>, <em>ceiling</em>, and <em>floor</em> operations each take
 * logarithmic time in the worst case, if the tree becomes unbalanced.
 * The <em>size</em>, and <em>is-empty</em> operations take constant time.
 * Construction takes constant time.
 * <p>
 * For additional documentation, see <a href="https://algs4.cs.princeton.edu/33balanced">Section 3.3</a> of
 * <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *
 * @author Robert Sedgewick
 * @author Kevin Wayne
 * @param <K> the type of the keys
 * @param <V> the type of the elements
 */

public class StorageArray<V> extends Storage implements Iterable<V> {
	private static final boolean RED   = true;
	private static final boolean BLACK = false;

	/**
	 * The root of the tree.
	 */
	private Node<V> root;

	public final int length;

	/**
	 * A node of the binary search tree that implements the map.
	 */
	private static class Node<V> extends Storage {
		private int key;
		private V value; // possibly null
		private Node<V> left, right;
		private boolean color;

		private Node(int key, V value, boolean color, int size) {
			this.key = key;
			this.value = value;
			this.color = color;
		}
	}

	/**
	 * Builds an empty map.
	 */
	public StorageArray(int length) {
		if (length < 0)
			throw new NegativeArraySizeException();

		this.length = length;
	}

	/**
	 * Determines if the given node is red.
	 * 
	 * @param x the node
	 * @return true if and only if {@code x} is red
	 */
	private static <V> boolean isRed(Node<V> x) {
		return x != null && x.color == RED;
	}

	/**
	 * Determines if the given node is black.
	 * 
	 * @param x the node
	 * @return true if and only if {@code x} is black
	 */
	private static <V> boolean isBlack(Node<V> x) {
		return x == null || x.color == BLACK;
	}

	private static int compareTo(int key1, int key2) {
		return key1 - key2;
	}

	/**
	 * Yields the value associated with the given key, if any.
	 * 
	 * @param key the key
	 * @return the value associated with the given key if the key is in the symbol table
	 *         and {@code null} if the key is not in the symbol table
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public @View V get(int key) {
		return get(root, key);
	}

	/**
	 * Yields the value associated with the given key in subtree rooted at x;
	 * 
	 * @param x the root of the subtree
	 * @param key the key
	 * @return the value. Yields {@code null} if the key is not found
	 */
	private static <V> V get(Node<V> x, int key) {
		while (x != null) {
			int cmp = compareTo(key, x.key);
			if      (cmp < 0) x = x.left;
			else if (cmp > 0) x = x.right;
			else              return x.value;
		}
		return null;
	}

	/**
	 * Returns the value associated with the given key.
	 * 
	 * @param key the key
	 * @return the value associated with the given key if the key is in the symbol table.
	 *         Yields {@code _default} if the key is not in the symbol table
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public @View V getOrDefault(int key, V _default) {
		return getOrDefault(root, key, _default);
	}

	private static <V> V getOrDefault(Node<V> x, int key, V _default) {
		while (x != null) {
			int cmp = compareTo(key, x.key);
			if      (cmp < 0) x = x.left;
			else if (cmp > 0) x = x.right;
			else              return x.value;
		}
		return _default;
	}

	/**
	 * Yields the value associated with the given key.
	 * 
	 * @param key the key
	 * @return the value associated with the given key if the key is in the symbol table.
	 *         Yields {@code _default.get()} if the key is not in the symbol table
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public V getOrDefault(int key, Supplier<V> _default) {
		return getOrDefault(root, key, _default);
	}

	// value associated with the given key in subtree rooted at x; uses supplier if no such key is found
	private static <V> V getOrDefault(Node<V> x, int key, Supplier<V> _default) {
		while (x != null) {
			int cmp = compareTo(key, x.key);
			if      (cmp < 0) x = x.left;
			else if (cmp > 0) x = x.right;
			else              return x.value;
		}
		return _default.get();
	}

	/**
	 * Inserts the specified key-value pair into this symbol table, overwriting the old 
	 * value with the new value if the symbol table already contains the specified key.
	 *
	 * @param key the key
	 * @param value the value
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public void put(int key, V value) {
		root = put(root, key, value);
		root.color = BLACK;
		// assert check();
	}

	// insert the key-value pair in the subtree rooted at h
	private static <V> Node<V> put(Node<V> h, int key, V value) { 
		if (h == null) return new Node<>(key, value, RED, 1);

		int cmp = compareTo(key, h.key);
		if      (cmp < 0) h.left  = put(h.left,  key, value); 
		else if (cmp > 0) h.right = put(h.right, key, value); 
		else              h.value = value;

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = rotateLeft(h);
		if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h);
		if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);

		return h;
	}

	// make a left-leaning link lean to the right
	private static <V> Node<V> rotateRight(Node<V> h) {
		// assert (h != null) && isRed(h.left);
		Node<V> x = h.left;
		h.left = x.right;
		x.right = h;
		x.color = h.color;
		h.color = RED;
		return x;
	}

	// make a right-leaning link lean to the left
	private static <V> Node<V> rotateLeft(Node<V> h) {
		// assert (h != null) && isRed(h.right);
		Node<V> x = h.right;
		h.right = x.left;
		x.left = h;
		x.color = h.color;
		h.color = RED;
		return x;
	}

	// flip the colors of a node and its two children
	private static <K,V> void flipColors(Node<V> h) {
		// h must have opposite color of its two children
		// assert (h != null) && (h.left != null) && (h.right != null);
		// assert (isBlack(h) &&  isRed(h.left) &&  isRed(h.right))
		//    || (isRed(h)  && isBlack(h.left) && isBlack(h.right));
		h.color = !h.color;
		h.left.color = !h.left.color;
		h.right.color = !h.right.color;
	}

	/**
	 * Replaces the old value {@code e} at {@code key} with {@code how.apply(e)}.
	 * If {@code key} was unmapped, it will be replaced with {@code how.apply(null)},
	 * which might well lead to a run-time exception.
	 *
	 * @param key the key whose value must be replaced
	 * @param how the replacement function
	 */
	public void update(int key, UnaryOperator<V> how) {
		root = update(root, key, how);
		root.color = BLACK;
	}

	private static <V> Node<V> update(Node<V> h, int key, UnaryOperator<V> how) { 
		if (h == null) return new Node<>(key, how.apply(null), RED, 1);

		int cmp = compareTo(key, h.key);
		if      (cmp < 0) h.left  = update(h.left,  key, how); 
		else if (cmp > 0) h.right = update(h.right, key, how); 
		else              h.value = how.apply(h.value);

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = rotateLeft(h);
		if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h);
		if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);

		return h;
	}

	/**
	 * Replaces the old value {@code e} at {@code key} with {@code how.apply(e)}.
	 * If {@code key} was unmapped, it will be replaced with {@code how.apply(_default)}.
	 *
	 * @param key the key whose value must be replaced
	 * @param _default the default value
	 * @param how the replacement function
	 */
	public void update(int key, V _default, UnaryOperator<V> how) {
		root = update(root, key, _default, how);
		root.color = BLACK;
	}

	private static <V> Node<V> update(Node<V> h, int key, V _default, UnaryOperator<V> how) { 
		if (h == null) return new Node<>(key, how.apply(_default), RED, 1);

		int cmp = compareTo(key, h.key);
		if      (cmp < 0) h.left  = update(h.left, key, _default, how); 
		else if (cmp > 0) h.right = update(h.right, key, _default, how); 
		else if (h.value == null)
			h.value = how.apply(_default);
		else
			h.value = how.apply(h.value);

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = rotateLeft(h);
		if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h);
		if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);

		return h;
	}

	/**
	 * Replaces the old value {@code e} at {@code key} with {@code how.apply(e)}.
	 * If {@code key} was unmapped, it will be replaced with {@code how.apply(_default.get())}.
	 *
	 * @param key the key whose value must be replaced
	 * @param _default the supplier of the default value
	 * @param how the replacement function
	 */
	public void update(int key, Supplier<V> _default, UnaryOperator<V> how) {
		root = update(root, key, _default, how);
		root.color = BLACK;
	}

	private static <V> Node<V> update(Node<V> h, int key, Supplier<V> _default, UnaryOperator<V> how) { 
		if (h == null) return new Node<>(key, how.apply(_default.get()), RED, 1);

		int cmp = compareTo(key, h.key);
		if      (cmp < 0) h.left  = update(h.left, key, _default, how); 
		else if (cmp > 0) h.right = update(h.right, key, _default, how); 
		else if (h.value == null)
			h.value = how.apply(_default.get());
		else
			h.value = how.apply(h.value);

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = rotateLeft(h);
		if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h);
		if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);

		return h;
	}

	/**
	 * If the given key is unmapped or is mapped to {@code null}, map it to the given value.
	 * 
	 * @param key the key
	 * @param value the value
	 * @return the previous value at the given key. Yields {@code null} if {@code key} was previously unmapped
	 *         or was mapped to {@code null}
	 */
	public V putIfAbsent(int key, V value) {
		class PutIfAbsent {
			private V result;

			private Node<V> putIfAbsent(Node<V> h) {
				// not found: result remains null
				if (h == null)
					// not found
					return new Node<>(key, value, RED, 1);

				int cmp = compareTo(key, h.key);
				if      (cmp < 0) h.left  = putIfAbsent(h.left);
				else if (cmp > 0) h.right = putIfAbsent(h.right);
				else if (h.value == null) {
					// found but was bound to null: result remains null
					h.value = value;
					return h;
				}
				else {
					// found and was bound to a non-null value
					result = h.value;
					return h;
				}

				// fix-up any right-leaning links
				if (isRed(h.right) && isBlack(h.left))     h = rotateLeft(h);
				if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h);
				if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);

				return h;
			}
		}

		PutIfAbsent pia = new PutIfAbsent();
		root = pia.putIfAbsent(root);
		root.color = BLACK;

		return pia.result;
	}

	/**
	 * If the given key is unmapped or is mapped to {@code null}, map it to the value given by a supplier.
	 * 
	 * @param key the key
	 * @param supplier the supplier
	 * @return the previous value at the given key, if it was already mapped to a non-{@code null} value.
	 *         If the key was unmapped or was mapped to {@code null}, yields the new value
	 */
	public V computeIfAbsent(int key, Supplier<V> supplier) {
		class ComputeIfAbsent {
			private V result;

			private Node<V> computeIfAbsent(Node<V> h) { 
				if (h == null)
					// not found
					return new Node<>(key, result = supplier.get(), RED, 1);

				int cmp = compareTo(key, h.key);
				if      (cmp < 0) h.left  = computeIfAbsent(h.left);
				else if (cmp > 0) h.right = computeIfAbsent(h.right);
				else if (h.value == null) {
					// found but was bound to null
					result = h.value = supplier.get();
					return h;
				}
				else {
					// found and was bound to a non-null value
					result = h.value;
					return h;
				}

				// fix-up any right-leaning links
				if (isRed(h.right) && isBlack(h.left))     h = rotateLeft(h);
				if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h);
				if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);

				return h;
			}
		}

		ComputeIfAbsent cia = new ComputeIfAbsent();
		root = cia.computeIfAbsent(root);
		root.color = BLACK;

		return cia.result;
	}

	/**
	 * If the given key is unmapped or is mapped to {@code null}, map it to the value given by a supplier.
	 * 
	 * @param key the key
	 * @param supplier the supplier
	 * @return the previous value at the given key, if it was already mapped to a non-{@code null} value.
	 *         If the key was unmapped or was mapped to {@code null}, yields the new value
	 */
	public V computeIfAbsent(int key, IntFunction<V> supplier) {
		class ComputeIfAbsent {
			private V result;

			private Node<V> computeIfAbsent(Node<V> h) { 
				if (h == null)
					// not found
					return new Node<>(key, result = supplier.apply(key), RED, 1);

				int cmp = compareTo(key, h.key);
				if      (cmp < 0) h.left  = computeIfAbsent(h.left);
				else if (cmp > 0) h.right = computeIfAbsent(h.right);
				else if (h.value == null) {
					// found but was bound to null
					result = h.value = supplier.apply(key);
					return h;
				}
				else {
					// found and was bound to a non-null value
					result = h.value;
					return h;
				}

				// fix-up any right-leaning links
				if (isRed(h.right) && isBlack(h.left))     h = rotateLeft(h);
				if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h);
				if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);

				return h;
			}
		}

		ComputeIfAbsent cia = new ComputeIfAbsent();
		root = cia.computeIfAbsent(root);
		root.color = BLACK;

		return cia.result;
	}

	@Override
	public Iterator<V> iterator() {
		return new StorageMapIterator<V>(root);
	}

	private static class StorageMapIterator<V> implements Iterator<V> {
		// the path under enumeration; it holds that the left children
		// have already been enumerated
		private List<Node<V>> stack = new ArrayList<>();

		private StorageMapIterator(Node<V> root) {
			// initially, the stack contains the leftmost path of the tree
			for (Node<V> cursor = root; cursor != null; cursor = cursor.left)
				stack.add(cursor);
		}

		@Override
		public boolean hasNext() {
			return !stack.isEmpty();
		}

		@Override
		public V next() {
			Node<V> topmost = stack.remove(stack.size() - 1);

			// we add the leftmost path of the right child of topmost
			for (Node<V> cursor = topmost.right; cursor != null; cursor = cursor.left)
				stack.add(cursor);

			return topmost.value;
		}
	}

	/**
	 * Yields an ordered stream of the entries (key/value) in this map, in
	 * increasing order of keys.
	 * 
	 * @return the stream
	 */
	public Stream<V> stream() {
		return StreamSupport.stream(spliterator(), false);
	}
}