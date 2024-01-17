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

package io.hotmoka.beans.internal.signatures;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.FieldSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.api.signatures.FieldSignature;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.types.StorageType;
import io.hotmoka.beans.marshalling.BeanMarshallingContext;
import io.hotmoka.constants.Constants;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * The signature of a field of a class.
 */
@Immutable
public final class FieldSignatureImpl extends AbstractMarshallable implements FieldSignature {

	/**
	 * The field that holds the balance in contracts.
	 */
	public final static FieldSignature BALANCE_FIELD = FieldSignatures.of(StorageTypes.CONTRACT, "balance", StorageTypes.BIG_INTEGER);

	/**
	 * The field that holds the red balance in contracts.
	 */
	public final static FieldSignature RED_BALANCE_FIELD = FieldSignatures.of(StorageTypes.CONTRACT, "balanceRed", StorageTypes.BIG_INTEGER);

	/**
	 * The field that holds the nonce in externally owned accounts.
	 */
	public final static FieldSignature EOA_NONCE_FIELD = FieldSignatures.of(StorageTypes.EOA, "nonce", StorageTypes.BIG_INTEGER);

	/**
	 * The field that holds the public key in externally owned accounts.
	 */
	public final static FieldSignature EOA_PUBLIC_KEY_FIELD = FieldSignatures.of(StorageTypes.EOA, "publicKey", StorageTypes.STRING);

	/**
	 * The field of the manifest that holds the contract of the validators of the node.
	 */
	public final static FieldSignature MANIFEST_VALIDATORS_FIELD = FieldSignatures.of(Constants.MANIFEST_NAME, "validators", StorageTypes.VALIDATORS);

	/**
	 * The field of the manifest that holds the object that keeps track
	 * of the versions of the modules of the node.
	 */
	public final static FieldSignature MANIFEST_VERSIONS_FIELD = FieldSignatures.of(Constants.MANIFEST_NAME, "versions", StorageTypes.VERSIONS);

	/**
	 * The field of the manifest that holds the gas station.
	 */
	public final static FieldSignature MANIFEST_GAS_STATION_FIELD = FieldSignatures.of(StorageTypes.MANIFEST, "gasStation", StorageTypes.GAS_STATION);

	/**
	 * The field of the manifest that holds the gamete account of the node.
	 */
	public final static FieldSignature MANIFEST_GAMETE_FIELD = FieldSignatures.of(Constants.MANIFEST_NAME, "gamete", StorageTypes.GAMETE);

	/**
	 * The field that holds the creator of an event.
	 */
	public final static FieldSignature EVENT_CREATOR_FIELD = FieldSignatures.of(StorageTypes.EVENT, "creator", StorageTypes.CONTRACT);

	/**
	 * The field that holds the gas price inside a {@code io.takamaka.code.governance.GenericGasStation}.
	 */
	public final static FieldSignature GENERIC_GAS_STATION_GAS_PRICE_FIELD = FieldSignatures.of(StorageTypes.GENERIC_GAS_STATION, "gasPrice", StorageTypes.BIG_INTEGER);

	/**
	 * The field that holds the current supply inside a {@code io.takamaka.code.governance.AbstractValidators}.
	 */
	public final static FieldSignature ABSTRACT_VALIDATORS_CURRENT_SUPPLY_FIELD = FieldSignatures.of(StorageTypes.ABSTRACT_VALIDATORS, "currentSupply", StorageTypes.BIG_INTEGER);

	/**
	 * The field that holds the gas price inside a {@code io.takamaka.code.math.UnsignedBigInteger.value}.
	 */
	public final static FieldSignature UNSIGNED_BIG_INTEGER_VALUE_FIELD = FieldSignatures.of(StorageTypes.UNSIGNED_BIG_INTEGER, "value", StorageTypes.BIG_INTEGER);

	/**
	 * The field that holds the root of a {@code io.takamaka.code.util.StorageTreeMap}.
	 */
	public final static FieldSignature STORAGE_TREE_MAP_ROOT_FIELD = FieldSignatures.of(StorageTypes.STORAGE_TREE_MAP, "root", StorageTypes.STORAGE_TREE_MAP_NODE);

	/**
	 * The field that holds the root of a {@code io.takamaka.code.util.StorageIntTreeMap}.
	 */
	public final static FieldSignature STORAGE_TREE_INTMAP_ROOT_FIELD = FieldSignatures.of(StorageTypes.STORAGE_TREE_INTMAP, "root", StorageTypes.STORAGE_TREE_INTMAP_NODE);

	/**
	 * The field that holds the size of a {@code io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_MAP_NODE_SIZE_FIELD = FieldSignatures.of(StorageTypes.STORAGE_TREE_MAP_NODE, "size", StorageTypes.INT);

	/**
	 * The field that holds the size of a {@code io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_INTMAP_NODE_SIZE_FIELD = FieldSignatures.of(StorageTypes.STORAGE_TREE_INTMAP_NODE, "size", StorageTypes.INT);

	/**
	 * The field that holds the value of a {@code io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_INTMAP_NODE_VALUE_FIELD = FieldSignatures.of(StorageTypes.STORAGE_TREE_INTMAP_NODE, "value", StorageTypes.OBJECT);

	/**
	 * The field that holds the left child of a {@code io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_INTMAP_NODE_LEFT_FIELD = FieldSignatures.of(StorageTypes.STORAGE_TREE_INTMAP_NODE, "left", StorageTypes.STORAGE_TREE_INTMAP_NODE);

	/**
	 * The field that holds the right child of a {@code io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_INTMAP_NODE_RIGHT_FIELD = FieldSignatures.of(StorageTypes.STORAGE_TREE_INTMAP_NODE, "right", StorageTypes.STORAGE_TREE_INTMAP_NODE);

	/**
	 * The field that holds the key of a {@code io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_INTMAP_NODE_KEY_FIELD = FieldSignatures.of(StorageTypes.STORAGE_TREE_INTMAP_NODE, "key", StorageTypes.INT);

	/**
	 * The field that holds the left tree of a {@code io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_MAP_NODE_LEFT_FIELD = FieldSignatures.of(StorageTypes.STORAGE_TREE_MAP_NODE, "left", StorageTypes.STORAGE_TREE_MAP_NODE);

	/**
	 * The field that holds the right tree of a {@code io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_MAP_NODE_RIGHT_FIELD = FieldSignatures.of(StorageTypes.STORAGE_TREE_MAP_NODE, "right", StorageTypes.STORAGE_TREE_MAP_NODE);

	/**
	 * The field that holds the key of a {@code io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_MAP_NODE_KEY_FIELD = FieldSignatures.of(StorageTypes.STORAGE_TREE_MAP_NODE, "key", StorageTypes.OBJECT);

	/**
	 * The field that holds the value of a {@code io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_MAP_NODE_VALUE_FIELD = FieldSignatures.of(StorageTypes.STORAGE_TREE_MAP_NODE, "value", StorageTypes.OBJECT);

	/**
	 * The class of the field.
	 */
	private final ClassType definingClass;

	/**
	 * The name of the field.
	 */
	private final String name;

	/**
	 * The type of the field.
	 */
	private final StorageType type;

	/**
	 * Builds the signature of a field.
	 * 
	 * @param definingClass the class of the field
	 * @param name the name of the field
	 * @param type the type of the field
	 */
	public FieldSignatureImpl(ClassType definingClass, String name, StorageType type) {
		this.definingClass = Objects.requireNonNull(definingClass, "definingClass cannot be null");
		this.name = Objects.requireNonNull(name, "name cannot be null");
		this.type = Objects.requireNonNull(type, "type cannot be null");
	}

	/**
	 * Builds the signature of a field.
	 * 
	 * @param definingClass the name of the class of the field
	 * @param name the name of the field
	 * @param type the type of the field
	 */
	public FieldSignatureImpl(String definingClass, String name, StorageType type) {
		this(StorageTypes.classNamed(definingClass), name, type);
	}

	@Override
	public ClassType getDefiningClass() {
		return definingClass;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public StorageType getType() {
		return type;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof FieldSignature fs && fs.getDefiningClass().equals(definingClass)
			&& fs.getName().equals(name) && fs.getType().equals(type);
	}

	@Override
	public int hashCode() {
		return definingClass.hashCode() ^ name.hashCode() ^ type.hashCode();
	}

	@Override
	public String toString() {
		return definingClass + "." + name + ":" + type;
	}

	@Override
	public int compareTo(FieldSignature other) {
		int diff = definingClass.compareTo(other.getDefiningClass());
		if (diff != 0)
			return diff;

		diff = name.compareTo(other.getName());
		return diff != 0 ? diff : type.compareTo(other.getType());
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeObject(FieldSignature.class, this);
	}

	/**
	 * Factory method that unmarshals a field signature from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the field signature
	 * @throws IOException if the field signature could not be unmarshalled
	 */
	public static FieldSignature from(UnmarshallingContext context) throws IOException {
		return context.readObject(FieldSignature.class);
	}

	@Override
	protected final MarshallingContext createMarshallingContext(OutputStream os) throws IOException {
		return new BeanMarshallingContext(os);
	}
}