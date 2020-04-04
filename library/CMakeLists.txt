cmake_minimum_required(VERSION 3.4.1)

find_library(log-lib log)
find_library(LIB-Z z)

include_directories(${CMAKE_SOURCE_DIR}/src/main/jniLibs/include)

add_library(libijkffmpeg SHARED IMPORTED)
set_target_properties(libijkffmpeg PROPERTIES
        IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libijkffmpeg.so)

add_library( soundeditor SHARED
             src/main/jni/decoder.c
             src/main/jni/jni_decoder.c
             src/main/jni/infoloader.c
             src/main/jni/jni_infoloader.c
             src/main/jni/convertor.c
             src/main/jni/jni_convertor.c
             src/main/jni/encoder.c
             src/main/jni/jni_encoder.c)

target_link_libraries(
        soundeditor
        ${log-lib}
        ${LIB-Z}
        libijkffmpeg
)