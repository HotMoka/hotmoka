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

package io.hotmoka.tools.internal.cli;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.AbstractJarStoreTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;
import io.hotmoka.verification.TakamakaClassLoader;

class ClassLoaderHelper {
	private final Node node;
	private final StorageReference manifest;
	private final TransactionReference takamakaCode;
	private final StorageReference versions;

	ClassLoaderHelper(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		this.node = node;
		this.manifest = node.getManifest();
		this.takamakaCode = node.getTakamakaCode();
		this.versions = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, AbstractCommand._100_000, takamakaCode, CodeSignature.GET_VERSIONS, manifest));
		
	}

	public TakamakaClassLoader classloaderFor(TransactionReference jar) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		List<TransactionReference> ws = new ArrayList<>();
		Set<TransactionReference> seen = new HashSet<>();
		List<byte[]> jars = new ArrayList<>();
		ws.add(jar);
		seen.add(jar);

		do {
			TransactionReference current = ws.remove(ws.size() - 1);
			AbstractJarStoreTransactionRequest request = (AbstractJarStoreTransactionRequest) node.getRequest(current);
			jars.add(request.getJar());
			request.getDependencies().filter(seen::add).forEachOrdered(ws::add);
		}
		while (!ws.isEmpty());

		int verificationVersion = ((IntValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, AbstractCommand._100_000, takamakaCode, CodeSignature.GET_VERIFICATION_VERSION, versions))).value;

		return TakamakaClassLoader.of(jars.stream(), verificationVersion);
	}
}