package uk.ac.manchester.tornado.drivers.cuda.graal;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import org.graalvm.compiler.core.common.spi.ForeignCallsProvider;
import org.graalvm.compiler.core.common.type.ObjectStamp;
import org.graalvm.compiler.core.common.type.Stamp;
import org.graalvm.compiler.nodes.CompressionNode;
import org.graalvm.compiler.nodes.FixedNode;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.spi.LoweringTool;
import org.graalvm.compiler.replacements.DefaultJavaLoweringProvider;

public class PTXLoweringProvider extends DefaultJavaLoweringProvider {

    public PTXLoweringProvider(MetaAccessProvider metaAccess,
            ForeignCallsProvider foreignCalls,
            TargetDescription target,
            boolean useCompressedOops) {
        super(metaAccess, foreignCalls, target, useCompressedOops);
    }

    @Override
    protected JavaKind getStorageKind(ResolvedJavaField field) {
        return null;
    }

    @Override
    public int fieldOffset(ResolvedJavaField field) {
        return 0;
    }

    @Override
    public ValueNode staticFieldBase(StructuredGraph graph, ResolvedJavaField field) {
        return null;
    }

    @Override
    public int arrayLengthOffset() {
        return 0;
    }

    @Override
    protected Stamp loadCompressedStamp(ObjectStamp stamp) {
        return null;
    }

    @Override
    protected ValueNode newCompressionNode(CompressionNode.CompressionOp op, ValueNode value) {
        return null;
    }

    @Override
    protected ValueNode createReadHub(StructuredGraph graph, ValueNode object, LoweringTool tool) {
        return null;
    }

    @Override
    protected ValueNode createReadArrayComponentHub(StructuredGraph graph, ValueNode arrayHub, FixedNode anchor) {
        return null;
    }

    @Override
    public Integer smallestCompareWidth() {
        return null;
    }

    @Override
    public boolean supportsBulkZeroing() {
        return false;
    }
}
