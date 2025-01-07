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

package io.hotmoka.examples.javacollections;

import java.util.HashSet;
import java.util.Set;

import io.takamaka.code.lang.View;

/**
 * This class defines methods that test the use of the Java HashSet class.
 */
public class HashSetTests {

	public static @View String testToString1() {
		Set<Object> set = new HashSet<>();
		Object[] keys = { "hello", "how", "are", "you", "?", "are", "how" };
		for (Object key: keys)
			set.add(key);

		Set<Object> copy = new HashSet<>();
		copy.addAll(set);
		return toString(copy);
	}

	public static @View String testToString2() {
		Set<Object> set = new HashSet<>();
		Object[] keys = { "hello", "how", new Object(), "are", "you", "?", "are", "how" };
		for (Object key: keys)
			set.add(key);

		Set<Object> copy = new HashSet<>();
		copy.addAll(set);
		return toString(copy);
	}

	public static @View String testToString3() {
		Set<Object> set = new HashSet<>();
		Object[] keys = { "hello", "how", new C(), "are", "you", "?", "are", "how" };
		for (Object key: keys)
			set.add(key);

		Set<Object> copy = new HashSet<>();
		copy.addAll(set);
		return toString(copy);
	}

	private static String toString(Set<?> objects) {
		// we cannot call toString() directly on strings, since its run-time
		// white-listing condition requires that its receiver must be an object
		// that can be held in store, hence not a Set
		String result = "";
		for (Object s: objects)
			if (result.isEmpty())
				result += s.toString();
			else
				result += ", " + s.toString();

		return "[" + result + "]";
	}
}