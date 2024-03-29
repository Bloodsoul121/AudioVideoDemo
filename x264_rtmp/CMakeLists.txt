cmake_minimum_required(VERSION 3.4.1)

add_subdirectory(src/main/cpp/librtmp)

#将所有cpp文件定义为一个变量
file(GLOB cpp_source src/main/cpp/*.cpp src/main/cpp/util/*.cpp)

#头文件成功引入
include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp/x264/${ANDROID_ABI}/include)
include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp/faac/${ANDROID_ABI}/include)
include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp/util)

#静态库 FFmpeg
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/src/main/cpp/x264/${ANDROID_ABI}/lib")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/src/main/cpp/faac/${ANDROID_ABI}/lib")

add_library(
        x264rtmp-lib
        SHARED
        ${cpp_source}
)

find_library(
        log-lib
        log
)

target_link_libraries(
        x264rtmp-lib
        ${log-lib}
        rtmp
        x264
        faac
)