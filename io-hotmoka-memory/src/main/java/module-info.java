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

module io.hotmoka.memory {
	exports io.hotmoka.memory;
	requires io.hotmoka.constants;
	requires io.hotmoka.annotations;
	requires io.hotmoka.node.local;
	requires io.hotmoka.stores;
	requires transitive io.hotmoka.node;
	requires io.hotmoka.beans;
	requires toml4j;
	requires java.logging;
	requires io.hotmoka.marshalling.api;
}