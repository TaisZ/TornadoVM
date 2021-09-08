package uk.ac.manchester.tornado.drivers.spirv.graal.lir;

import org.graalvm.compiler.core.common.cfg.AbstractBlockBase;
import org.graalvm.compiler.lir.LIRInstruction;
import org.graalvm.compiler.lir.LIRInstructionClass;
import org.graalvm.compiler.lir.LabelRef;
import org.graalvm.compiler.lir.Opcode;
import org.graalvm.compiler.lir.SwitchStrategy;
import org.graalvm.compiler.lir.Variable;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;
import uk.ac.manchester.spirvbeehivetoolkit.lib.SPIRVInstScope;
import uk.ac.manchester.spirvbeehivetoolkit.lib.instructions.SPIRVOpBranch;
import uk.ac.manchester.spirvbeehivetoolkit.lib.instructions.SPIRVOpBranchConditional;
import uk.ac.manchester.spirvbeehivetoolkit.lib.instructions.SPIRVOpLabel;
import uk.ac.manchester.spirvbeehivetoolkit.lib.instructions.SPIRVOpLoad;
import uk.ac.manchester.spirvbeehivetoolkit.lib.instructions.SPIRVOpSwitch;
import uk.ac.manchester.spirvbeehivetoolkit.lib.instructions.operands.SPIRVId;
import uk.ac.manchester.spirvbeehivetoolkit.lib.instructions.operands.SPIRVLiteralInteger;
import uk.ac.manchester.spirvbeehivetoolkit.lib.instructions.operands.SPIRVMemoryAccess;
import uk.ac.manchester.spirvbeehivetoolkit.lib.instructions.operands.SPIRVMultipleOperands;
import uk.ac.manchester.spirvbeehivetoolkit.lib.instructions.operands.SPIRVOptionalOperand;
import uk.ac.manchester.spirvbeehivetoolkit.lib.instructions.operands.SPIRVPairLiteralIntegerIdRef;
import uk.ac.manchester.tornado.drivers.spirv.common.SPIRVLogger;
import uk.ac.manchester.tornado.drivers.spirv.graal.asm.SPIRVAssembler;
import uk.ac.manchester.tornado.drivers.spirv.graal.compiler.SPIRVCompilationResultBuilder;

/**
 * SPIR-V Code Generation for all control-flow constructs.
 */
public class SPIRVControlFlow {

    public abstract static class BaseControlFlow extends SPIRVLIRStmt.AbstractInstruction {

        public BaseControlFlow(LIRInstructionClass<? extends LIRInstruction> c) {
            super(c);
        }

        // We only declare the IDs
        protected SPIRVId getIfOfBranch(String blockName, SPIRVAssembler asm) {
            SPIRVId branch = asm.labelTable.get(blockName);
            if (branch == null) {
                branch = asm.registerBlockLabel(blockName);
            }
            return branch;
        }

        // We only declare the IDs
        protected SPIRVId getIdForBranch(LabelRef ref, SPIRVAssembler asm) {
            AbstractBlockBase<?> targetBlock = ref.getTargetBlock();
            String blockName = targetBlock.toString();
            SPIRVId branch = asm.labelTable.get(blockName);
            if (branch == null) {
                branch = asm.registerBlockLabel(blockName);
            }
            return branch;
        }
    }

    public static class LoopBeginLabel extends BaseControlFlow {

        public static final LIRInstructionClass<LoopBeginLabel> TYPE = LIRInstructionClass.create(LoopBeginLabel.class);

        private final String blockId;

        public LoopBeginLabel(String blockName) {
            super(TYPE);
            this.blockId = blockName;
        }

        @Override
        protected void emitCode(SPIRVCompilationResultBuilder crb, SPIRVAssembler asm) {
            SPIRVLogger.traceCodeGen("LoopLabel : blockID " + blockId);
            SPIRVId branchId = getIfOfBranch(blockId, asm);
            SPIRVInstScope newScope = asm.currentBlockScope().add(new SPIRVOpBranch(branchId));
            asm.pushScope(newScope);
            SPIRVInstScope newScope2 = newScope.add(new SPIRVOpLabel(branchId));
            asm.pushScope(newScope2);
        }
    }

    public static class BranchConditional extends BaseControlFlow {

        public static final LIRInstructionClass<BranchConditional> TYPE = LIRInstructionClass.create(BranchConditional.class);

        @Use
        protected Value condition;

        private LabelRef lirTrueBlock;
        private LabelRef lirFalseBlock;

        public BranchConditional(Value condition, LabelRef lirTrueBlock, LabelRef lirFalseBlock) {
            super(TYPE);
            this.condition = condition;
            this.lirTrueBlock = lirTrueBlock;
            this.lirFalseBlock = lirFalseBlock;
        }

        /**
         * It emits the following pattern:
         * 
         * <code>
         *     OpBranchConditional %condition %trueBranch %falseBranch
         * </code>
         * 
         * @param crb
         *            {@link SPIRVCompilationResultBuilder crb}
         * @param asm
         *            {@link SPIRVAssembler}
         */
        @Override
        protected void emitCode(SPIRVCompilationResultBuilder crb, SPIRVAssembler asm) {

            SPIRVId conditionId = asm.lookUpLIRInstructions(condition);

            SPIRVId trueBranch = getIdForBranch(lirTrueBlock, asm);
            SPIRVId falseBranch = getIdForBranch(lirFalseBlock, asm);

            SPIRVLogger.traceCodeGen("emit SPIRVOpBranchConditional: " + condition + "? " + lirTrueBlock + ":" + lirFalseBlock);

            SPIRVId bool = asm.primitives.getTypePrimitive(SPIRVKind.OP_TYPE_BOOL);
            // SPIRVId resultLoad = asm.module.getNextId();
            //
            // asm.currentBlockScope().add(new SPIRVOpLoad( //
            // bool, //
            // resultLoad, //
            // conditionId, //
            // new SPIRVOptionalOperand<>(SPIRVMemoryAccess.Aligned(new
            // SPIRVLiteralInteger(condition.getPlatformKind().getSizeInBytes())))));

            asm.currentBlockScope().add(new SPIRVOpBranchConditional( //
                    conditionId, //
                    trueBranch, //
                    falseBranch, //
                    new SPIRVMultipleOperands<>()));

            // Note: we do not need to register a new ID, since this operation does not
            // generate one.

        }
    }

    public static class Branch extends BaseControlFlow {

        public static final LIRInstructionClass<Branch> TYPE = LIRInstructionClass.create(Branch.class);

        @Use
        private LabelRef branch;

        public Branch(LabelRef branch) {
            super(TYPE);
            this.branch = branch;
        }

        /**
         * It emits the following pattern:
         *
         * <code>
         *     SPIRVOpBranch %branch
         * </code>
         *
         * @param crb
         * @param asm
         */
        @Override
        protected void emitCode(SPIRVCompilationResultBuilder crb, SPIRVAssembler asm) {
            SPIRVId branchId = getIdForBranch(branch, asm);
            SPIRVLogger.traceCodeGen("emit SPIRVOpBranch: " + branch);
            asm.currentBlockScope().add(new SPIRVOpBranch(branchId));

        }
    }

    public static class BranchIf extends BaseControlFlow {

        public static final LIRInstructionClass<BranchIf> TYPE = LIRInstructionClass.create(BranchIf.class);

        @Use
        private LabelRef branch;
        private final boolean isConditional;
        private final boolean isLoopEdgeBack;

        public BranchIf(LabelRef branch, boolean isConditional, boolean isLoopEdgeBack) {
            super(TYPE);
            this.branch = branch;
            this.isConditional = isConditional;
            this.isLoopEdgeBack = isLoopEdgeBack;
        }

        /**
         * It emits the following pattern:
         *
         * <code>
         *     SPIRVOpBranch %branch
         * </code>
         *
         * @param crb
         * @param asm
         */
        @Override
        protected void emitCode(SPIRVCompilationResultBuilder crb, SPIRVAssembler asm) {
            SPIRVId branchId = getIdForBranch(branch, asm);
            SPIRVLogger.traceCodeGen("emit IF_CASE SPIRVOpBranch: " + branch);
            asm.currentBlockScope().add(new SPIRVOpBranch(branchId));

        }
    }

    public static class BranchLoopConditional extends BranchIf {

        public BranchLoopConditional(LabelRef branch, boolean isConditional, boolean isLoopEdgeBack) {
            super(branch, isConditional, isLoopEdgeBack);
        }
    }

    @Opcode("Switch")
    public static class SwitchStatement extends BaseControlFlow {

        public static final LIRInstructionClass<SwitchStatement> TYPE = LIRInstructionClass.create(SwitchStatement.class);

        @Use
        private Variable key;

        private SwitchStrategy strategy;

        @Use
        private LabelRef[] keytargets;

        @Use
        private LabelRef defaultTarget;

        public SwitchStatement(Variable key, SwitchStrategy strategy, LabelRef[] keyTargets, LabelRef defaultTarget) {
            super(TYPE);
            this.key = key;
            this.strategy = strategy;
            this.keytargets = keyTargets;
            this.defaultTarget = defaultTarget;
        }

        @Override
        protected void emitCode(SPIRVCompilationResultBuilder crb, SPIRVAssembler asm) {
            SPIRVLogger.traceCodeGen("emit SWITCH(" + key + ")");

            SPIRVId valueKey = asm.lookUpLIRInstructions(key);

            SPIRVKind spirvKind = (SPIRVKind) key.getPlatformKind();
            SPIRVId typeKind = asm.primitives.getTypePrimitive(spirvKind);

            // Perform a Load of the key value
            SPIRVId loadId = asm.module.getNextId();
            asm.currentBlockScope().add(new SPIRVOpLoad(//
                    typeKind, //
                    loadId, //
                    valueKey, //
                    new SPIRVOptionalOperand<>( //
                            SPIRVMemoryAccess.Aligned( //
                                    new SPIRVLiteralInteger(spirvKind.getSizeInBytes())))//
            ));

            SPIRVId defaultSelector = getIdForBranch(defaultTarget, asm);

            SPIRVPairLiteralIntegerIdRef[] cases = new SPIRVPairLiteralIntegerIdRef[strategy.getKeyConstants().length];
            int i = 0;
            for (Constant keyConstant : strategy.getKeyConstants()) {
                SPIRVId labelCase = getIdForBranch(keytargets[i], asm);
                int caseIntValue = Integer.parseInt(keyConstant.toValueString());
                SPIRVPairLiteralIntegerIdRef pairId = new SPIRVPairLiteralIntegerIdRef(new SPIRVLiteralInteger(caseIntValue), labelCase);
                cases[i] = pairId;
                i++;
            }

            asm.currentBlockScope().add(new SPIRVOpSwitch(loadId, defaultSelector, new SPIRVMultipleOperands<>(cases)));
        }
    }
}
