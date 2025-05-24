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
    "hardware/mediatek/libmtkperf_client",
    "hardware/lineage/compat"
]


lib_fixups: lib_fixups_user_type = {
    libs_clang_rt_ubsan: lib_fixup_remove_arch_suffix,
    libs_proto_3_9_1: lib_fixup_vendorcompat,
    libs_proto_21_12: lib_fixup_remove_proto_version_suffix,
}


def fixup_ndk_platform(libname: str) -> tuple[str, str]:
    """
    Replace -ndk_platform with -ndk
    """
    return (libname, libname.replace("-ndk_platform.so", "-ndk.so"))


patchelf_version = "0_17_2"

blob_fixups: blob_fixups_user_type = {
    "vendor/bin/hw/android.hardware.security.keymint@1.0-service.mitee": blob_fixup()
    .patchelf_version(patchelf_version)
    .replace_needed(
        "android.hardware.security.keymint-V1-ndk_platform.so",
        "android.hardware.security.keymint-V3-ndk.so",
    )
    .add_needed("android.hardware.security.rkp-V3-ndk.so")
    .replace_needed(
        *fixup_ndk_platform("android.hardware.security.secureclock-V1-ndk_platform.so")
    )
    .replace_needed(
        *fixup_ndk_platform("android.hardware.security.sharedsecret-V1-ndk_platform.so")
    ),
    "vendor/etc/init/android.hardware.graphics.allocator@4.0-service-mediatek.rc": blob_fixup().regex_replace(
        "android.hardware.graphics.allocator@4.0-service-mediatek",
        "mt6789/android.hardware.graphics.allocator@4.0-service-mediatek.mt6789",
    ),
    (
        "vendor/lib/libwvhidl.so",
        "vendor/lib/mediadrm/libwvdrmengine.so",
        "vendor/lib64/libwvhidl.so",
        "vendor/lib64/mediadrm/libwvdrmengine.so",
    ): blob_fixup()
    .patchelf_version(patchelf_version)
    .replace_needed("libprotobuf-cpp-lite-3.9.1.so", "libprotobuf-cpp-full-3.9.1.so"),
    (
        "vendor/lib64/hw/android.hardware.sensors@2.X-subhal-mediatek.so",
        "vendor/lib64/mt6789/libaalservice.so",
        "vendor/lib64/mt6789/libcam.utils.sensorprovider.so",
    ): blob_fixup()
    .patchelf_version(patchelf_version)
    .add_needed("libshim_sensors.so"),
    "vendor/bin/hw/android.hardware.media.c2@1.2-mediatek-64b": blob_fixup()
    .patchelf_version(patchelf_version)
    .replace_needed("libavservices_minijail_vendor.so", "libavservices_minijail.so")
    .add_needed("libstagefright_foundation-v33.so"),
    "vendor/etc/init/android.hardware.media.c2@1.2-mediatek.rc": blob_fixup().regex_replace(
        "@1.2-mediatek", "@1.2-mediatek-64b"
    ),
    "vendor/etc/init/android.hardware.bluetooth@1.1-service-mediatek.rc": blob_fixup().regex_replace(
        "on property:vts(.|\n)*", ""
    ),
    "vendor/etc/init/android.hardware.neuralnetworks-shim-service-mtk.rc": blob_fixup().regex_replace(
        "start", "enable"
    ),
    (
        "vendor/lib64/libteei_daemon_vfs.so",
        "vendor/lib64/mt6789/lib3a.flash.so",
        "vendor/lib64/mt6789/libaaa_ltm.so",
        "vendor/lib64/mt6789/lib3a.ae.stat.so",
        "vendor/lib64/mt6789/lib3a.sensors.color.so",
        "vendor/lib64/mt6789/lib3a.sensors.flicker.so",
        "vendor/lib64/libSQLiteModule_VER_ALL.so",
    ): blob_fixup()
    .patchelf_version(patchelf_version)
    .add_needed("liblog.so"),
    (
        "vendor/lib64/mt6789/libmtkcam_stdutils.so",
        "vendor/lib64/hw/mt6789/android.hardware.camera.provider@2.6-impl-mediatek.so"
    ): blob_fixup()
    .patchelf_version(patchelf_version)
    .replace_needed("libutils.so", "libutils-v32.so"),
    "vendor/lib64/libmorpho_video_stabilizer.so": blob_fixup()
    .add_needed("libutils.so"),
    "vendor/lib64/hw/mt6789/vendor.mediatek.hardware.pq@2.15-impl.so": blob_fixup()
    .patchelf_version(patchelf_version)
    .add_needed("libshim_sensors.so")
    .replace_needed("libutils.so", "libutils-v32.so"),
    (
        'vendor/lib64/libmorpho_Ldc.so',
        'vendor/lib64/libTrueSight.so',
	    'vendor/lib64/libMiVideoFilter.so',
    ): blob_fixup()
        .clear_symbol_version('AHardwareBuffer_acquire')
        .clear_symbol_version('AHardwareBuffer_allocate')
        .clear_symbol_version('AHardwareBuffer_describe')
        .clear_symbol_version('AHardwareBuffer_lock')
        .clear_symbol_version('AHardwareBuffer_lockPlanes')
        .clear_symbol_version('AHardwareBuffer_release')
        .clear_symbol_version('AHardwareBuffer_unlock'),
    ('vendor/lib64/hw/android.hardware.gnss-impl-mediatek.so', 'vendor/bin/hw/android.hardware.gnss-service.mediatek'): blob_fixup()
        .replace_needed('android.hardware.gnss-V1-ndk_platform.so', 'android.hardware.gnss-V1-ndk.so'),
}  # fmt: skip

module = ExtractUtilsModule(
    'tanzanite',
    'xiaomi',
    blob_fixups=blob_fixups,
    lib_fixups=lib_fixups,
    namespace_imports=namespace_imports,
    check_elf=True,
    add_firmware_proprietary_file=True,
)

if __name__ == "__main__":
    utils = ExtractUtils.device(module)
    utils.run()