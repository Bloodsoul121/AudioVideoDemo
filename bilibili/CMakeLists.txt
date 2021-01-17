cmake_minimum_required(VERSION 3.4.1)

add_subdirectory(src/main/cpp/librtmp)

add_library(
        bilibili-lib
        SHARED
        src/main/cpp/bilibili-lib.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        bilibili-lib
        ${log-lib}
)