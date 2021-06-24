import {InfoModel} from "../models/info/InfoModel";
import {RemoteNode} from "./RemoteNode";
import {InstanceMethodCallTransactionRequestModel} from "../models/requests/InstanceMethodCallTransactionRequestModel";
import {StorageReferenceModel} from "../models/values/StorageReferenceModel";
import {TransactionReferenceModel} from "../models/values/TransactionReferenceModel";
import {CodeSignature} from "./lang/CodeSignature";
import {HotmokaException} from "./exception/HotmokaException";
import {MethodSignatureModel} from "../models/signatures/MethodSignatureModel";
import {GameteInfo} from "../models/info/GameteInfo";
import {GasStation} from "../models/info/GasStation";
import {NonVoidMethodSignatureModel} from "../models/signatures/NonVoidMethodSignatureModel";
import {ClassType} from "./lang/ClassType";
import {BasicType} from "./lang/BasicType";
import {Validators} from "../models/info/Validators";
import {Validator} from "../models/info/Validator";

export class ManifestHelper {
    private remoteNode: RemoteNode

    constructor(remoteNode: RemoteNode) {
        this.remoteNode = remoteNode
    }

    async info(): Promise<InfoModel> {
        const takamakaCode = await this.remoteNode.getTakamakaCode()
        const manifest = await this.remoteNode.getManifest()

        if (!takamakaCode) {
            throw new HotmokaException("TakamakaCode not found")
        }

        if (!manifest) {
            throw new HotmokaException("Manifest not found")
        }

        const validators = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.GET_VALIDATORS, manifest))
        const gasStation = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.GET_GAS_STATION, manifest))
        const versions = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.GET_VERSIONS, manifest))
        const gamete = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.GET_GAMETE, manifest))

        if (!gamete.reference) {
            throw new HotmokaException("Gamete not found")
        }

        if (!gasStation.reference) {
            throw new HotmokaException("GasStation not found")
        }

        if (!validators.reference) {
            throw new HotmokaException("Validators not found")
        }

        const chainId = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))
        const maxErrorLength = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.GET_MAX_ERROR_LENGTH, manifest))
        const maxDependencies = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.GET_MAX_DEPENDENCIES, manifest))
        const maxCumulativeSizeOfDependencies = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(gamete.reference, takamakaCode, CodeSignature.GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES, manifest))
        const allowsSelfCharged = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.ALLOWS_SELF_CHARGED, manifest))
        const allowsUnsignedFaucet = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.ALLOWS_UNSIGNED_FAUCET, manifest))
        const skipsVerification = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.SKIPS_VERIFICATION, manifest))
        const signature = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.GET_SIGNATURE, manifest))

        // gamete info
        const balanceOfGamete = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.BALANCE, gamete.reference))
        const redBalanceOfGamete = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.BALANCE_RED, gamete.reference))
        const maxFaucet = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.GET_MAX_FAUCET, gamete.reference))
        const maxRedFaucet = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.GET_MAX_RED_FAUCET, gamete.reference))

        // gasStation info
        const gasPrice = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.GET_GAS_PRICE, gasStation.reference))
        const maxGasPerTransaction = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.GET_MAX_GAS_PER_TRANSACTION, gasStation.reference))
        const ignoresGasPrice = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.IGNORES_GAS_PRICE, gasStation.reference))
        const targetGasAtReward = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.GET_TARGET_GAS_AT_REWARD, gasStation.reference))
        const inflation = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.GET_INFLATION, gasStation.reference))
        const oblivion = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.GET_OBLIVION, gasStation.reference))

        // validators
        const shares = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, new NonVoidMethodSignatureModel("getShares", ClassType.VALIDATORS.name,  [], ClassType.STORAGE_MAP.name), validators.reference))
        if (!shares.reference) {
            throw new HotmokaException("Shares not found")
        }
        const numOfValidators = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, new NonVoidMethodSignatureModel("size", ClassType.STORAGE_MAP.name,  [], BasicType.INT.name), shares.reference))
        const height = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.GET_HEIGHT, validators.reference))
        const numberOfTransactions = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.GET_NUMBER_OF_TRANSACTIONS, validators.reference))
        const ticketForNewPoll = await this.remoteNode.runInstanceMethodCallTransaction(this.buildInstanceMethodCallTransactionModel(manifest, takamakaCode, CodeSignature.GET_TICKET_FOR_NEW_POLL, validators.reference))


        const info = new InfoModel()
        info.takamakaCode = takamakaCode
        info.manifest = manifest
        info.chainId = chainId.value ?? ''
        info.maxErrorLength = maxErrorLength.value ? Number(maxErrorLength.value) : 0
        info.maxDependencies = maxDependencies.value ? Number(maxDependencies.value) : 0
        info.maxCumulativeSizeOfDependencies = maxCumulativeSizeOfDependencies.value ? Number(maxCumulativeSizeOfDependencies.value) : 0
        info.allowsSelfCharged = allowsSelfCharged.value ? 'true' === allowsSelfCharged.value : false
        info.allowsUnsignedFaucet = allowsUnsignedFaucet.value ? 'true' === allowsUnsignedFaucet.value : false
        info.skipsVerification = skipsVerification.value ? 'true' === skipsVerification.value : false
        info.signature = signature.value ?? ''
        info.gameteInfo = new GameteInfo(balanceOfGamete.value, redBalanceOfGamete.value, maxFaucet.value, maxRedFaucet.value)
        info.gasStation = new GasStation(gasPrice.value, maxGasPerTransaction.value, ignoresGasPrice.value, targetGasAtReward.value, inflation.value, oblivion.value)
        info.validators = new Validators(numOfValidators.value, height.value, numberOfTransactions.value, ticketForNewPoll.value)

        const numOfValidatorsValue = Number(numOfValidators.value) ?? 0
        for (let i = 0; i < numOfValidatorsValue; i++) {
            info.validators.validators.push(new Validator())
        }

        return Promise.resolve(info)
    }

    private buildInstanceMethodCallTransactionModel(caller: StorageReferenceModel, classPath: TransactionReferenceModel, methodsSignatureModel: MethodSignatureModel, receiver: StorageReferenceModel): InstanceMethodCallTransactionRequestModel {
        return new InstanceMethodCallTransactionRequestModel(
            caller,
            "0",
            classPath,
            "100000",
            "0",
            methodsSignatureModel,
            [],
            receiver,
            ""
        )
    }
}