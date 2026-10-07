/*
 * Copyright 2020 Bloomberg Finance L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef SELEKT_SECURE_ZERO_H
#define SELEKT_SECURE_ZERO_H

#include <cstddef>
#include <cstring>

// explicit_bzero arrived in glibc 2.25. Using it on older glibc would raise the library's runtime glibc floor
// beyond what some Linux distributions (e.g. RHEL 7, glibc 2.17) provide, so fall back for those.
#if defined(__GLIBC__)
#if __GLIBC_PREREQ(2, 25)
#define SELEKT_HAVE_EXPLICIT_BZERO 1
#endif
#elif defined(__ANDROID_API__) && __ANDROID_API__ >= 28
#define SELEKT_HAVE_EXPLICIT_BZERO 1
#endif

#if defined(SELEKT_HAVE_EXPLICIT_BZERO)
#include <strings.h>
#endif

namespace selekt {
    inline void secure_zero(unsigned char* p, size_t n) {
#if defined(__cpp_lib_memset_explicit) || (defined(__STDC_VERSION__) && __STDC_VERSION__ >= 202311L)
        memset_explicit(p, 0, n);
#elif defined(SELEKT_HAVE_EXPLICIT_BZERO)
        explicit_bzero(p, n);
#else
        volatile unsigned char* pp = p;
        while (n--) { *pp++ = 0; }
#endif
    }
}

#endif //SELEKT_SECURE_ZERO_H
