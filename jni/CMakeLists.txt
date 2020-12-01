cmake_minimum_required(VERSION 3.4.1)

# 很奇怪，只有 STATUS 才能输出日志，在 build_output.txt 中查看日志
message(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
message("CMAKE_SOURCE_DIR = ${CMAKE_SOURCE_DIR}")
message(STATUS "PROJECT_SOURCE_DIR = ${PROJECT_SOURCE_DIR}")
message(WARNING "CMAKE_BINARY_DIR = ${CMAKE_BINARY_DIR}")
message(STATUS ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> hello")

add_library(
        jni
        SHARED
        src/main/cpp/native-lib.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        jni
        ${log-lib}
)

message(STATUS ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> hello hello")

##为了确保 CMake 可以在编译时定位到我们的头文件，我们需要使用 include_directories() 命令，并包含 头文件的路
#include_directories(imported-lib/include/)
#
#add_library(
#        # 指定目标导入库.
#        imported-lib
#        # 设置导入库的类型（静态或动态） 为 shared library.
#        SHARED
#        # 告知 CMake imported-lib 是导入的库
#        IMPORTED
#)
#set_target_properties(
#        # 指定目标导入库
#        imported-lib
#        # 指定属性（本地导入的已有库）
#        PROPERTIES IMPORTED_LOCATION
#        # 指定你要导入库的路径.
#        # ${CMAKE_SOURCE_DIR}
#        imported-lib/src/${ANDROID_ABI}/libimported-lib.so
#)
#
##要将预构建库关联到我们的原生库，请将其添加到 CMake 构建脚本的 target_link_libraries() 命令中
#target_link_libraries(
#        # 指定了三个库，分别是native-lib、 imported-lib和log-lib.
#        native-lib
#        imported-lib
#        # log-lib是包含在 NDK 中的一个日志库
#        ${log-lib}
#)