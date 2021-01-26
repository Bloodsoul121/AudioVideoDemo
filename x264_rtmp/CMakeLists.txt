cmake_minimum_required(VERSION 3.4.1)

add_subdirectory(src/main/cpp/librtmp)

add_library(
        x264rtmp-lib
        SHARED
        src/main/cpp/x264rtmp-lib.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        x264rtmp-lib
        ${log-lib}
        rtmp
)