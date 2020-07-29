package uk.ac.manchester.tornado.drivers.ptx.graal.lir;

import jdk.vm.ci.meta.Value;
import org.graalvm.compiler.core.common.LIRKind;
import org.graalvm.compiler.lir.LIRInstruction;
import org.graalvm.compiler.lir.Variable;
import org.graalvm.compiler.nodes.DirectCallTargetNode;
import uk.ac.manchester.tornado.drivers.ptx.graal.PTXCodeUtil;
import uk.ac.manchester.tornado.drivers.ptx.graal.asm.PTXAssembler;
import uk.ac.manchester.tornado.drivers.ptx.graal.compiler.PTXCompilationResultBuilder;

import static uk.ac.manchester.tornado.drivers.ptx.graal.asm.PTXAssemblerConstants.TAB;

public class PTXDirectCall extends PTXLIROp {

    protected DirectCallTargetNode target;
    @LIRInstruction.Def protected Value result;
    @LIRInstruction.Use protected Value[] parameters;

    public PTXDirectCall(DirectCallTargetNode target, Value result, Value[] parameters) {
        super(LIRKind.value(result.getPlatformKind()));
        this.result = result;
        this.parameters = parameters;
        this.target = target;
    }

    @Override
    public void emit(PTXCompilationResultBuilder crb, PTXAssembler asm, Variable dest) {
        final String methodName = PTXCodeUtil.makeMethodName(target.targetMethod());

        asm.emitSymbol(TAB);
        asm.emitSymbol("call ");
        asm.emit(methodName);
        asm.emit(", (");
        int i = 0;
        for (Value param : parameters) {
            asm.emit(PTXAssembler.toString(param));
            if (i < parameters.length - 1) {
                asm.emit(", ");
            }

            i++;
        }
        asm.emit(")");

        crb.addNonInlinedMethod(target.targetMethod());
    }
}
