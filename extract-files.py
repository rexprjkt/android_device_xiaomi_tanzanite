#!/usr/bin/env -S PYTHONPATH=../../../tools/extract-utils python3
#
# SPDX-FileCopyrightText: 2025 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

from extract_utils.fixups_blob import blob_fixups_user_type, blob_fixup
from extract_utils.fixups_lib import (
    lib_fixup_remove_arch_suffix,
    lib_fixup_remove_proto_version_suffix,
    lib_fixup_vendorcompat,
    lib_fixups_user_type,
    libs_clang_rt_ubsan,
    libs_proto_3_9_1,
    libs_proto_21_12,
)
from extract_utils.main import (
    ExtractUtils,
    ExtractUtilsModule,
)

namespace_imports = [
    'device/xiaomi/tanzanite',
    "hardware/mediatek",
    "hardware/mediatek/libmtkperf_client"
]


lib_fixups: lib_fixups_user_type = {
    libs_clang_rt_ubsan: lib_fixup_remove_arch_suffix,
    libs_proto_3_9_1: lib_fixup_vendorcompat,
    libs_proto_21_12: lib_fixup_remove_proto_version_suffix,
}


blob_fixups: blob_fixups_user_type = {
    "vendor/etc/init/android.hardware.graphics.allocator@4.0-service-mediatek.rc": blob_fixup().regex_replace(
        "android.hardware.graphics.allocator@4.0-service-mediatek",
        "mt6789/android.hardware.graphics.allocator@4.0-service-mediatek.mt6789",
    ),
    "vendor/etc/init/android.hardware.media.c2@1.2-mediatek.rc": blob_fixup().regex_replace(
        "@1.2-mediatek", "@1.2-mediatek-64b"
    ),
    "vendor/etc/init/android.hardware.bluetooth@1.1-service-mediatek.rc": blob_fixup().regex_replace(
        "on property:vts(.|\n)*", ""
    ),
    "vendor/etc/init/android.hardware.neuralnetworks-shim-service-mtk.rc": blob_fixup().regex_replace(
        "start", "enable"
    ),
}  # fmt: skip

module = ExtractUtilsModule(
    'tanzanite',
    'xiaomi',
    blob_fixups=blob_fixups,
    lib_fixups=lib_fixups,
    namespace_imports=namespace_imports,
    check_elf=True,
)

if __name__ == "__main__":
    utils = ExtractUtils.device(module)
    utils.run()