/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

#ifndef mozilla_system_nsvolumemountlock_h__
#define mozilla_system_nsvolumemountlock_h__

#include "nsIVolumeMountLock.h"

#include "nsIDOMWakeLock.h"
#include "nsIObserver.h"
#include "nsString.h"
#include "nsTArray.h"
#include "nsWeakReference.h"

namespace mozilla {
namespace system {

/* The VolumeMountLock is designed so that it can be used in the Child or
 * Parent process. While the VolumeMountLock object exists, then the
 * VolumeManager/AutoMounter will prevent a mounted volume from being
 * shared with the PC.
 */

class nsVolumeMountLock MOZ_FINAL : public nsIVolumeMountLock,
                                    public nsIObserver,
                                    public nsSupportsWeakReference
{
public:
  NS_DECL_ISUPPORTS
  NS_DECL_NSIOBSERVER
  NS_DECL_NSIVOLUMEMOUNTLOCK

  static already_AddRefed<nsIVolumeMountLock> Create(const nsAString& volumeName);

  const nsString& VolumeName() const  { return mVolumeName; }

private:
  nsVolumeMountLock(const nsAString& aVolumeName);
  ~nsVolumeMountLock();

  nsresult Init();

  nsString                    mVolumeName;
  int32_t                     mVolumeGeneration;
  nsCOMPtr<nsIDOMMozWakeLock> mWakeLock;
  bool                        mUnlocked;
};

} // namespace system
} // namespace mozilla

#endif  // mozilla_system_nsvolumemountlock_h__
