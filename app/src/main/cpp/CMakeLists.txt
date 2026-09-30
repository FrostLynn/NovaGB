cmake_minimum_required(VERSION 3.22.1)
project("novagb-native")

add_library(
    novagb-native
    SHARED
    native-lib.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    novagb-native
    ${log-lib}
)