#
# Copyright (C) 2025 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit_only.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit some common Halcyon stuff.
$(call inherit-product, vendor/halcyon/config/common.mk)

# Inherit from tanzanite device
$(call inherit-product, device/xiaomi/tanzanite/device.mk)

PRODUCT_DEVICE := tanzanite
PRODUCT_NAME := halcyon_tanzanite
PRODUCT_BRAND := Redmi
PRODUCT_MODEL := 24117RN76O
PRODUCT_MANUFACTURER := xiaomi

PRODUCT_SYSTEM_NAME := tanzanite_n_gl
PRODUCT_SYSTEM_DEVICE := tanzanite

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildFingerprint=Redmi/tanzanite_n_global/tanzanite:15/AP3A.240905.015.A2/OS2.0.213.0.VOGEUXM:user/release-keys \
    DeviceName=$(PRODUCT_SYSTEM_DEVICE) \
    DeviceProduct=$(PRODUCT_SYSTEM_NAME)
