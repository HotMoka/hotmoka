package io.hotmoka.tools.internal.cli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import io.takamaka.code.verification.TakamakaClassLoader;
import io.takamaka.code.verification.VerifiedJar;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "verify",
	description = "Verifies a jar",
	showDefaultValues = true)
public class Verify extends AbstractCommand {

	@Parameters(index = "0", description = "the jar to verify")
	private Path jar;

	@Option(names = { "--libs" }, description = "the already instrumented dependencies of the jar")
	private List<Path> libs;

	@Option(names = { "--init" }, description = "verifies as during node initialization")
	private boolean init;

	@Option(names = { "--allow-self-charged" }, description = "assumes that @SelfCharged methods are allowed")
	private boolean allowSelfCharged;

	@Option(names = { "--version" }, description = "uses the given version of the verification rules", defaultValue = "0")
	private int version;

	@Override
	protected void execute() throws Exception {
		byte[] bytesOfOrigin = readAllBytes(jar);
		Stream<byte[]> classpath = Stream.of(bytesOfOrigin);
		if (libs != null)
			classpath = Stream.concat(classpath, libs.stream().map(this::readAllBytes));

		TakamakaClassLoader classLoader = TakamakaClassLoader.of(classpath, version);
		VerifiedJar verifiedJar = VerifiedJar.of(bytesOfOrigin, classLoader, init, allowSelfCharged, false);
		verifiedJar.issues().forEach(System.err::println);
		if (verifiedJar.hasErrors())
			throw new CommandException("verification failed because of errors");
		else
			System.out.println("verification succeeded");
	}

	private byte[] readAllBytes(Path jar) {
		try {
			return Files.readAllBytes(jar);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}