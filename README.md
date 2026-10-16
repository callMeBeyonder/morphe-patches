# 👋🧩 Beyonder Morphe Patches

## ❓ About

Custom patches by beyonder for apps I like.

> [!IMPORTANT]
> **StrongLifts requires root.** The app is shielded by Google Play PairIP, so the patched build must be mounted over the genuine Play-signed install — a normal sideload will not run.

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=callMeBeyonder/morphe-patches

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.2.1](https://github.com/callMeBeyonder/morphe-patches/releases/tag/v1.2.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;3 patches total
<details open>
<summary>📦 CRICFy TV&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 7.3 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Remove ads](#remove-ads) | Removes the Unity full-screen ad, the sponsor banner, the sponsor redirects shown when opening a channel and the self-hosted full-screen ad shown on app open. The locked channel dialog is kept. |  |

</details>

<details open>
<summary>📦 MacroFactor Workouts&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 1.4.1 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Unlock Premium (Root)](#unlock-premium-root) | Unlocks all premium features by injecting an active RevenueCat entitlement into the customer info. Requires root: this app is protected by Firebase App Check with Play Integrity, so the patched build must be mounted over the genuine Play-signed install (e.g. morphe-cli patch --mount) — a re-signed sideload will fail server-side attestation. |  |

</details>

<details open>
<summary>📦 StrongLifts&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 4.4.1 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Unlock Pro (Root)](#unlock-pro-root) | Unlocks all Pro and PowerPack premium features by forcing the entitlement gates to return true. Requires root: this app is shielded by Google Play PairIP, so the patched build must be mounted over the genuine Play-signed install (e.g. morphe-cli patch --mount) — a normal sideload will not run. |  |

</details>

<!-- PATCHES_END -->

### 🛠️ Building locally

- Run `./gradlew buildAndroid`
- The built patches .mpp file is found in `patches/build/libs/patches-*.mpp`
- Patch the mpp file using [Morphe-Desktop](https://github.com/MorpheApp/morphe-desktop)
  like any other patch bundle.

See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for more information.

## 📜 License

Beyonder Morphe Patches are licensed under the [GNU General Public License v3.0](LICENSE)
