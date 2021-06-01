import {SignatureModel} from "./SignatureModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {BasicType} from "../../internal/lang/BasicType";
import {Utils} from "../../internal/Utils";
import {ClassType} from "../../internal/lang/ClassType";


/**
 * The model of the signature of a field, method or constructor.
 */
export abstract class CodeSignatureModel extends SignatureModel {
    /**
     * The name of the class defining the field, method or constructor.
     */
    formals: Array<string>

    protected constructor(definingClass: string, formals: Array<string>) {
        super(definingClass)
        this.formals = formals
    }

    protected equals(other: any): boolean {
        return (other as CodeSignatureModel).definingClass === this.definingClass &&
             Utils.arrayEquals((other as CodeSignatureModel).formals, this.formals)
    }

    protected into(context: MarshallingContext): void {
        super.into(context)
        context.writeCompactInt(this.formals.length)
        this.formals.forEach(formal => {
            if (BasicType.isBasicType(formal)) {
                new BasicType(formal).into(context)
            } else {
                new ClassType(formal).into(context)
            }
        })
    }
}