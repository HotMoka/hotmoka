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

package io.hotmoka.examples.collections;

import java.math.BigInteger;
import java.util.Random;

import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageTreeArray;
import io.takamaka.code.util.StorageTreeByteArray;

/**
 * This class defines methods that test the storage array implementation.
 */
public class ArrayTests {

	public static @View int testRandomInitialization() {
		StorageTreeArray<BigInteger> array = new StorageTreeArray<>(100);
		Random random = new Random(12345L);

		for (int i = 0; i < 50; i++)
			while (array.setIfAbsent(random.nextInt(100), BigInteger.valueOf(i)) != null);

		class WrappedInt {
			int i;
		}

		WrappedInt wi = new WrappedInt();

		array.stream().filter(bi -> bi != null).mapToInt(BigInteger::intValue).forEachOrdered(i -> wi.i += i);

		return wi.i;
	}

	public static @View long countNullsAfterRandomInitialization() {
		StorageTreeArray<BigInteger> array = new StorageTreeArray<>(100);
		Random random = new Random(12345L);

		for (int i = 0; i < 50; i++)
			while (array.setIfAbsent(random.nextInt(100), BigInteger.valueOf(i)) != null);

		class WrappedLong {
			long l;
		}

		WrappedLong wl = new WrappedLong();

		// 50 elements of the array should still be null
		array.stream().filter(bi -> bi == null).forEachOrdered(__ -> wl.l++);

		return wl.l;
	}

	public static @View int testUpdateWithDefault1() {
		StorageTreeArray<BigInteger> array = new StorageTreeArray<>(100);
		Random random = new Random(12345L);

		for (int i = 0; i < 50; i++)
			while (array.setIfAbsent(random.nextInt(array.length), BigInteger.valueOf(i)) != null);

		for (int i = 0; i < array.length; i++)
			array.update(i, BigInteger.ZERO, BigInteger.ONE::add);

		class WrappedInt {
			int i;
		}

		WrappedInt wi = new WrappedInt();

		array.stream().mapToInt(BigInteger::intValue).forEachOrdered(i -> wi.i += i);;

		return wi.i;
	}

	public static @View int testUpdateWithDefault2() {
		StorageTreeArray<BigInteger> array = new StorageTreeArray<>(100);
		Random random = new Random(12345L);

		for (int i = 0; i < 50; i++)
			array.update(random.nextInt(100), BigInteger.ZERO, BigInteger.valueOf(i)::add);

		class WrappedInt {
			int i;
		}

		WrappedInt wi = new WrappedInt();

		array.stream().filter(bi -> bi != null).mapToInt(BigInteger::intValue).forEachOrdered(i -> wi.i += i);

		return wi.i;
	}

	public static @View int testGetOrDefault() {
		StorageTreeArray<BigInteger> array = new StorageTreeArray<>(100);
		Random random = new Random(12345L);

		for (int i = 0; i < 50; i++)
			while (array.setIfAbsent(random.nextInt(array.length), BigInteger.valueOf(i)) != null);

		BigInteger sum = BigInteger.ZERO;
		for (int i = 0; i < array.length; i++)
			sum = sum.add(array.getOrDefault(i, BigInteger.ZERO));

		return sum.intValue();
	}

	public static @View int testByteArrayThenIncrease() {
		StorageTreeByteArray array = new StorageTreeByteArray(100);
		Random random = new Random(12345L);

		for (byte i = 1; i <= 50; i++) {
			int index;

			do {
				index = random.nextInt(array.length);
			}
			while (array.get(index) != 0);
			
			array.set(index, i);
		}

		for (int i = 0; i < array.length; i++)
			array.set(i, (byte) (array.get(i) + 1));

		class WrappedInt {
			int i;
		}

		WrappedInt wi = new WrappedInt();

		array.stream().forEachOrdered(b -> wi.i += b);

		return wi.i;
	}
}