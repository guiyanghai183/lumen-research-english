package com.lumen.researchenglish.network

import androidx.core.content.FileProvider

/** A concrete provider avoids device-specific issues with declaring FileProvider directly. */
class UpdateFileProvider : FileProvider()
