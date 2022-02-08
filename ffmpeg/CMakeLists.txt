cmake_minimum_required(VERSION 3.4.1)

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
)