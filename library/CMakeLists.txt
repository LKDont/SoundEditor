cmake_minimum_required(VERSION 3.4.1)

find_library(log-lib log)
find_library(LIB-Z z)

include_directories(${CMAKE_SOURCE_DIR}/src/main/jniLibs/includes)

# avcodec
add_library(avcodec
            STATIC
            IMPORTED )
set_target_properties(
    avcodec
    PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libavcodec.a
    )

# filter
add_library(avfilter
            STATIC
            IMPORTED )
set_target_properties(
    avfilter
    PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libavfilter.a
    )

# format
add_library(avformat
            STATIC
            IMPORTED )
set_target_properties(
    avformat
    PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libavformat.a
    )

# util
add_library(avutil
            STATIC
            IMPORTED )
set_target_properties(
    avutil
    PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libavutil.a
    )

# swresample
add_library(swresample
            STATIC
            IMPORTED )
set_target_properties(
    swresample
    PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libswresample.a
    )

# swscale
add_library(swscale
            STATIC
            IMPORTED )
set_target_properties(
    swscale
    PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libswscale.a
    )

# fdk-aac
add_library(fdk-aac
            STATIC
            IMPORTED )
set_target_properties(
    fdk-aac
    PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libfdk-aac.a
    )

# mp3lame
add_library(mp3lame
            STATIC
            IMPORTED )
set_target_properties(
    mp3lame
    PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libmp3lame.a
    )

add_library( soundeditor
             SHARED
             src/main/jni/decoder.c
             src/main/jni/jni_decoder.c
             src/main/jni/infoloader.c
             src/main/jni/jni_infoloader.c
             src/main/jni/convertor.c
             src/main/jni/jni_convertor.c
             src/main/jni/encoder.c
             src/main/jni/jni_encoder.c)

target_link_libraries( soundeditor
                       ${log-lib}
                       ${LIB-Z}
                       avfilter
                       avformat
                       avcodec
                       avutil
                       swresample
                       swscale
                       fdk-aac
                       mp3lame )