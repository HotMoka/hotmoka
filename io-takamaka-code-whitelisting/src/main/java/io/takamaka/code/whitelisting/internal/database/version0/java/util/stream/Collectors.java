package io.takamaka.code.whitelisting.internal.database.version0.java.util.stream;

public interface Collectors {
	java.util.stream.Collector<java.lang.CharSequence, ?, java.lang.String> joining(java.lang.CharSequence delimiter, java.lang.CharSequence prefix, java.lang.CharSequence suffix);
	java.util.stream.Collector<java.lang.CharSequence, ?, java.lang.String> joining(java.lang.CharSequence delimiter);
	java.util.stream.Collector<java.lang.CharSequence, ?, java.lang.String> joining();
	<T> java.util.stream.Collector<T, ?, java.util.List<T>> toList();
}