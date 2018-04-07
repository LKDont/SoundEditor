cmake_minimum_required(VERSION 3.4.1)

add_library( soundeditor
             SHARED
             src/main/jni/decoder.c
             src/main/jni/jni_decoder.c
             src/main/jni/infoloader.c
             src/main/jni/jni_infoloader.c
             src/main/jni/convertor.c
             src/main/jni/jni_convertor.c)

find_library( log-lib
              log )

add_library(avutil
            SHARED
            IMPORTED )
set_target_properties(
    avutil
    PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi/libavutil.so
    )

add_library(swresample
            SHARED
            IMPORTED )
set_target_properties(
    swresample
    PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi/libswresample.so
    )

add_library(avcodec
            SHARED
            IMPORTED )
set_target_properties(
    avcodec
    PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi/libavcodec.so
    )

add_library(avformat
            SHARED
            IMPORTED )
set_target_properties(
    avformat
    PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi/libavformat.so
    )

include_directories(${CMAKE_SOURCE_DIR}/src/main/jniLibs/includes)

target_link_libraries( soundeditor
                       avutil
                       swresample
                       avcodec
                       avformat
                       ${log-lib} )