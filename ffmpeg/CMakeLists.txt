cmake_minimum_required(VERSION 3.4.1)

include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp/include)
link_directories(${CMAKE_SOURCE_DIR}/src/main/cpp/lib/${ANDROID_ABI})

add_library(
        ffmpeg-lib
        SHARED
        src/main/cpp/ffmpeg-lib.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        ffmpeg-lib
        ${log-lib}
        avcodec
        avdevice
        avfilter
        avformat
        avutil
        swresample
        swscale
)