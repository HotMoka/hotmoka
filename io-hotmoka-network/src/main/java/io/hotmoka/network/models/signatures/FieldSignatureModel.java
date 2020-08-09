package io.hotmoka.network.models.signatures;

import io.hotmoka.beans.signatures.FieldSignature;

/**
 * The model of the signature of a field of a class.
 */
public final class FieldSignatureModel extends SignatureModel {

	/**
	 * The name of the field.
	 */
	public String name;

	/**
	 * The type of the field.
	 */
	public String type;

	/**
	 * Builds the model of the signature of a field.
	 * 
	 * @param field the signature of the field
	 */
	public FieldSignatureModel(FieldSignature field) {
		super(field.definingClass.name);

		this.name = field.name;
		this.type = nameOf(field.type);
	}

	public FieldSignatureModel() {}

	/**
	 * Yields the signature having this model.
	 * 
	 * @return the signature
	 */
	public FieldSignature toBean() {
		return new FieldSignature(definingClass, name, typeWithName(type));
	}
}