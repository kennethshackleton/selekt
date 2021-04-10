# Copyright 2021 Bloomberg Finance L.P.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

cmake_minimum_required (VERSION 3.4.0 FATAL_ERROR)

set(PROJECT_NAME Selekt)

project(${PROJECT_NAME})

set(CMAKE_CXX_STANDARD 14)

add_subdirectory(sqlite3)

find_package(JNI QUIET)
if(JNI_INCLUDE_DIRS)
  target_include_directories(selekt SYSTEM PUBLIC ${JNI_INCLUDE_DIRS})
endif()

target_sources(selekt PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/sqlite3_jni.cpp)
#target_compile_options(selekt PRIVATE "-Werror")
