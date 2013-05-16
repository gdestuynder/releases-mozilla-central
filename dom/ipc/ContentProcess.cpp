/* -*- Mode: C++; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 * vim: set sw=4 ts=8 et tw=80 : 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#include "mozilla/ipc/IOThreadChild.h"

#include "ContentProcess.h"

#include <sys/mman.h>
#include <stdio.h>
#define MADV_MERGEABLE 12

using mozilla::ipc::IOThreadChild;

namespace mozilla {
namespace dom {
bool
ContentProcess::Init()
{
    FILE *fp;
    char section[100];
    char perms[5];
    unsigned long start, end, misc;
    int ch, offset;

    mContent.Init(IOThreadChild::message_loop(),
                         ParentHandle(),
                         IOThreadChild::channel());
    mXREEmbed.Start();
    mContent.InitXPCOM();

    fp = fopen("/proc/self/maps", "r");
    if (fp != NULL) {
        while (fscanf(fp, "%lx-%lx %4s %lx %lx:%lx %ld",
            &start, &end, perms, &misc, &misc, &misc, &misc) == 7)
        {
             section[0] = 0;
             offset = 0;
             while(1)
             {
                 ch = fgetc(fp);
                 if (ch == '\n' || ch == EOF)
                     break;
                 if ((offset == 0) && (ch == ' '))
                     continue;
                 if ((offset + 1) < 100) {
                     section[offset]=ch;
                     section[offset+1]=0;
                     offset++;
                 }
             }
            if (( section[0] == 0) || (strcmp(section,"[stack]") == 0) || (strcmp(section,"[heap]") == 0))
                madvise((void*) start, (size_t) end-start, MADV_MERGEABLE);
            if (ch == EOF)
                break;
        }
        fclose(fp);
    }
    
    return true;
}

void
ContentProcess::CleanUp()
{
    mXREEmbed.Stop();
}

} // namespace tabs
} // namespace mozilla
