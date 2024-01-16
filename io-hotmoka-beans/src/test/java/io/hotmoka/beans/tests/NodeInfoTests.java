/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.beans.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.NodeInfos;
import io.hotmoka.testing.AbstractLoggedTests;
import jakarta.websocket.DecodeException;
import jakarta.websocket.EncodeException;

public class NodeInfoTests extends AbstractLoggedTests {

	@Test
	@DisplayName("node informations are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForNodeInfo() throws EncodeException, DecodeException {
		var info1 = NodeInfos.of("type", "1.2.3", "ID");
		String encoded = new NodeInfos.Encoder().encode(info1);
		var info2 = new NodeInfos.Decoder().decode(encoded);
		assertEquals(info1, info2);
	}
}