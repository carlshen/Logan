cmake_minimum_required(VERSION 3.5)

set(SOURCE_FILES
        clogan_core.c
        mmap_util.c mmap_util.c cJSON.c
        construct_data.c construct_data.c
        zlib_util.h zlib_util.c logan_config.h
        json_util.h json_util.c aes_util.h aes_util.c
        clogan_status.h
        directory_util.h directory_util.c base_util.h base_util.c console_util.h console_util.c)

add_library (clogan ${SOURCE_FILES})
