#ifndef TORNADO_CUDA_MACROS_DATA_COPIES
#define TORNADO_CUDA_MACROS_DATA_COPIES

#define COPY_ARRAY_D_TO_H(SIG,NATIVE_J_TYPE,J_TYPE) \
JNIEXPORT jbyteArray JNICALL Java_uk_ac_manchester_tornado_drivers_cuda_CUDAStream_writeArrayDtoH__JJJ_3##SIG##J_3B \
  (JNIEnv *env, jclass clazz, jlong device_ptr, jlong offset, jlong length, NATIVE_J_TYPE## Array array, jlong host_offset, jbyteArray stream_wrapper) { \
    CUdeviceptr start_ptr = (CUdeviceptr) device_ptr + (unsigned int) offset; \
    CUstream stream; \
    stream_from_array(env, &stream, stream_wrapper); \
\
    StagingAreaList *staging_list = get_first_free_staging_area(length); \
    CUresult result = cuMemcpyDtoHAsync(staging_list->staging_area, start_ptr, (size_t) length, stream); \
    if (result != 0) { \
        printf("Failed to copy memory from device to host! (%d)\n", result); fflush(stdout); \
    } \
 \
    CUevent event; \
    result = cuEventCreate(&event, CU_EVENT_DEFAULT); \
    if (result != 0) { \
        printf("Failed to create event! (%d)\n", result); fflush(stdout); \
    } \
 \
    result = cuEventRecord(event, stream); \
    if (result != 0) { \
        printf("Failed to record event! (%d)\n", result); fflush(stdout); \
    } \
    if (cuEventQuery(event) != 0) cuEventSynchronize(event); \
 \
    (*env)->Set ## J_TYPE ## ArrayRegion(env, array, host_offset, length / sizeof(NATIVE_J_TYPE), staging_list->staging_area); \
    set_to_unused(stream, result, staging_list); \
 \
    return array_from_event(env, &event); \
} \
 \
JNIEXPORT jint JNICALL Java_uk_ac_manchester_tornado_drivers_cuda_CUDAStream_writeArrayDtoHAsync__JJJ_3##SIG##J_3B \
  (JNIEnv *env, jclass clazz, jlong device_ptr, jlong offset, jlong length, NATIVE_J_TYPE## Array array, jlong host_offset, jbyteArray stream_wrapper) { \
    CUdeviceptr start_ptr = (CUdeviceptr) device_ptr + (unsigned int) offset; \
\
    void *native_array = (*env)->GetPrimitiveArrayCritical(env, array, 0); \
    CUstream stream; \
    stream_from_array(env, &stream, stream_wrapper); \
    CUresult result = cuMemcpyDtoHAsync(native_array + host_offset, start_ptr, (size_t) length, stream); \
 \
    cuMemFreeHost(native_array); \
    (*env)->ReleasePrimitiveArrayCritical(env, array, native_array, 0); \
 \
    return (jint) -1; \
}

#define COPY_ARRAY_H_TO_D(SIG,NATIVE_J_TYPE,J_TYPE) \
JNIEXPORT void JNICALL Java_uk_ac_manchester_tornado_drivers_cuda_CUDAStream_writeArrayHtoD__JJJ_3##SIG##J_3B \
  (JNIEnv *env, jclass clazz, jlong device_ptr, jlong offset, jlong length, NATIVE_J_TYPE## Array array, jlong host_offset, jbyteArray stream_wrapper) { \
    CUdeviceptr start_ptr = (CUdeviceptr) device_ptr + (unsigned int) offset; \
    CUstream stream; \
    stream_from_array(env, &stream, stream_wrapper); \
 \
    StagingAreaList *staging_list = get_first_free_staging_area(length); \
    (*env)->Get##J_TYPE##ArrayRegion(env, array, host_offset / sizeof(NATIVE_J_TYPE), length / sizeof(NATIVE_J_TYPE), staging_list->staging_area); \
 \
    CUresult result = cuMemcpyHtoDAsync(start_ptr, staging_list->staging_area, (size_t) length, stream); \
 \
    result = cuStreamAddCallback(stream, set_to_unused, staging_list, 0); \
    if (result != 0) { \
        printf("Failed to queue memory free! (%d)\n", result); fflush(stdout); \
    } \
    return; \
} \
 \
JNIEXPORT jbyteArray JNICALL Java_uk_ac_manchester_tornado_drivers_cuda_CUDAStream_writeArrayHtoDAsync__JJJ_3##SIG##J_3B \
  (JNIEnv *env, jclass clazz, jlong device_ptr, jlong offset, jlong length, NATIVE_J_TYPE## Array array, jlong host_offset, jbyteArray stream_wrapper) { \
    CUdeviceptr start_ptr = (CUdeviceptr) device_ptr + (unsigned int) offset; \
 \
    StagingAreaList *staging_list = get_first_free_staging_area(length); \
    (*env)->Get##J_TYPE##ArrayRegion(env, array, host_offset / sizeof(NATIVE_J_TYPE), length / sizeof(NATIVE_J_TYPE), staging_list->staging_area); \
 \
    CUstream stream; \
    stream_from_array(env, &stream, stream_wrapper); \
    CUresult result = cuMemcpyHtoDAsync(start_ptr, staging_list->staging_area, (size_t) length, stream); \
 \
    CUevent event; \
    result = cuEventCreate(&event, CU_EVENT_DEFAULT); \
    if (result != 0) { \
        printf("Failed to create event! (%d)\n", result); fflush(stdout); \
    } \
 \
    result = cuEventRecord(event, stream); \
    if (result != 0) { \
        printf("Failed to record event! (%d)\n", result); fflush(stdout); \
    } \
 \
    result = cuStreamAddCallback(stream, set_to_unused, staging_list, 0); \
    if (result != 0) { \
        printf("Failed to queue memory free! (%d)\n", result); fflush(stdout); \
    } \
 \
    return array_from_event(env, &event); \
}

#endif