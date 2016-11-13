package tornado.drivers.opencl;

import com.oracle.graal.compiler.common.LIRKind;
import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.JavaKind;
import tornado.drivers.opencl.graal.OCLArchitecture;
import tornado.drivers.opencl.graal.lir.OCLKind;

import static tornado.common.exceptions.TornadoInternalError.shouldNotReachHere;

public class OCLTargetDescription extends TargetDescription {

    private static final int STACK_ALIGNMENT = 8;
    private static final boolean INLINE_OBJECTS = true;

    public OCLTargetDescription(Architecture arch) {
        this(arch, false, STACK_ALIGNMENT, 4096, INLINE_OBJECTS);
    }

    protected OCLTargetDescription(Architecture arch, boolean isMP, int stackAlignment, int implicitNullCheckLimit, boolean inlineObjects) {
        super(arch, isMP, stackAlignment, implicitNullCheckLimit, inlineObjects);
    }

    //@formatter:off
    private static final OCLKind[][] VECTOR_LOOKUP_TABLE = new OCLKind[][]{
        {OCLKind.UCHAR2, OCLKind.UCHAR3, OCLKind.UCHAR4, OCLKind.UCHAR8, OCLKind.UCHAR16},
        {OCLKind.CHAR2, OCLKind.CHAR3, OCLKind.CHAR4, OCLKind.CHAR8, OCLKind.CHAR16},
        {OCLKind.USHORT2, OCLKind.USHORT3, OCLKind.USHORT4, OCLKind.USHORT8, OCLKind.USHORT16},
        {OCLKind.SHORT2, OCLKind.SHORT3, OCLKind.SHORT4, OCLKind.SHORT8, OCLKind.SHORT16},
        {OCLKind.USHORT2, OCLKind.USHORT3, OCLKind.USHORT4, OCLKind.USHORT8, OCLKind.USHORT16},
        {OCLKind.INT2, OCLKind.INT3, OCLKind.INT4, OCLKind.INT8, OCLKind.INT16},
        {OCLKind.UINT2, OCLKind.UINT3, OCLKind.UINT4, OCLKind.UINT8, OCLKind.UINT16},
        {OCLKind.LONG2, OCLKind.LONG3, OCLKind.LONG4, OCLKind.LONG8, OCLKind.LONG16},
        {OCLKind.ULONG2, OCLKind.ULONG3, OCLKind.ULONG4, OCLKind.ULONG8, OCLKind.ULONG16},
        {OCLKind.FLOAT2, OCLKind.FLOAT3, OCLKind.FLOAT4, OCLKind.FLOAT8, OCLKind.FLOAT16},
        {OCLKind.DOUBLE2, OCLKind.DOUBLE3, OCLKind.DOUBLE4, OCLKind.DOUBLE8, OCLKind.DOUBLE16}
    };
//@formatter:on

    public OCLArchitecture getArch() {
        return (OCLArchitecture) arch;
    }

    // should use OCLKind.lookupLengthIndex instead
    private final static int lookupLengthIndex(int vectorLength) {
        switch (vectorLength) {
            case 2:
                return 0;
            case 3:
                return 1;
            case 4:
                return 2;
            case 8:
                return 3;
            case 16:
                return 4;
            default:
                shouldNotReachHere();
        }

        return -1;
    }

    public final static int lookupTypeIndex(OCLKind kind) {
        switch (kind) {
            case UCHAR:
                return 0;
            case CHAR:
                return 1;
            case USHORT:
                return 2;
            case SHORT:
                return 3;
            case UINT:
                return 4;
            case INT:
                return 5;
            case ULONG:
                return 6;
            case LONG:
                return 7;
            case FLOAT:
                return 8;
            case DOUBLE:
                return 9;
            default:
                return -1;
        }
    }

    public OCLKind getVectorByLength(OCLKind oclKind, int vectorLength) {
        if (lookupTypeIndex(oclKind) == -1) {
            return OCLKind.ILLEGAL;
        }

        if (vectorLength == 1) {
            return oclKind;
        }

        return VECTOR_LOOKUP_TABLE[lookupTypeIndex(oclKind)][lookupLengthIndex(vectorLength)];
    }

    public OCLKind getOCLKind(JavaKind javaKind, int vectorLength) {
        int index = -1;
        switch (javaKind) {
            case Byte:
                index = (javaKind.isUnsigned()) ? 0 : 1;
                break;
            case Short:
                index = (javaKind.isUnsigned()) ? 2 : 3;
                break;
            case Char:
                index = 2;
                break;
            case Int:
                index = (javaKind.isUnsigned()) ? 4 : 5;
                break;
            case Long:
                index = (javaKind.isUnsigned()) ? 6 : 7;
                break;
            case Float:
                index = 8;
                break;
            case Double:
                index = 9;
                break;
            case Boolean:
            case Object:
                shouldNotReachHere("invalid vector type");
                break;
        }

        return VECTOR_LOOKUP_TABLE[index][lookupLengthIndex(vectorLength)];
    }

    public LIRKind getLIRKind(JavaKind javaKind, int vectorLength) {
        return LIRKind.value(getOCLKind(javaKind, vectorLength));
    }

    public OCLKind getOCLKind(JavaKind javaKind) {
        return (OCLKind) arch.getPlatformKind(javaKind);
    }

//    @Override
//    public LIRKind getLIRKind(JavaKind javaKind) {
//        if (javaKind == JavaKind.Void) {
//            return LIRKind.fromJavaKind(arch, javaKind);
//        }
//        return LIRKind.value(getOCLKind(javaKind));
//    }
//
//    @Override
//    public ReferenceMap createReferenceMap(boolean hasRegisters, int stackSlotCount) {
//        unimplemented();
//        return null;
//    }
}