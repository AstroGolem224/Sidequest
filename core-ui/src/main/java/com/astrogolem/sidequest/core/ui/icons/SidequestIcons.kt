package com.astrogolem.sidequest.core.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object SidequestIcons {
    val Dashboard: ImageVector
        get() = dashboardIcon
    val QuestLog: ImageVector
        get() = questLogIcon
    val Camera: ImageVector
        get() = cameraIcon
    val Profile: ImageVector
        get() = profileIcon
    val Back: ImageVector
        get() = backIcon
    val Search: ImageVector
        get() = searchIcon
    val Coin: ImageVector
        get() = coinIcon
    val Gallery: ImageVector
        get() = galleryIcon
    val Save: ImageVector
        get() = saveIcon
    val Intel: ImageVector
        get() = intelIcon
    val Retake: ImageVector
        get() = retakeIcon
    val Clean: ImageVector
        get() = cleanIcon
    val Study: ImageVector
        get() = studyIcon
    val Sprint: ImageVector
        get() = sprintIcon
    val Compass: ImageVector
        get() = compassIcon
    val Spark: ImageVector
        get() = sparkIcon
}

private val strokeBrush = SolidColor(Color.Black)

private fun ImageVector.Builder.outlinePath(block: PathBuilder.() -> Unit) {
    path(
        fill = null,
        stroke = strokeBrush,
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero,
        pathBuilder = block,
    )
}

private val dashboardIcon by lazy {
    ImageVector.Builder(
        name = "SqDashboard",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        outlinePath {
            moveTo(4f, 5f)
            lineTo(10f, 5f)
            lineTo(10f, 10f)
            lineTo(4f, 10f)
            close()
            moveTo(14f, 5f)
            lineTo(20f, 5f)
            lineTo(20f, 10f)
            lineTo(14f, 10f)
            close()
            moveTo(4f, 14f)
            lineTo(10f, 14f)
            lineTo(10f, 19f)
            lineTo(4f, 19f)
            close()
            moveTo(14f, 14f)
            lineTo(20f, 14f)
            lineTo(20f, 19f)
            lineTo(14f, 19f)
            close()
        }
    }.build()
}

private val questLogIcon by lazy {
    ImageVector.Builder("SqQuestLog", 24.dp, 24.dp, 24f, 24f).apply {
        outlinePath {
            moveTo(7f, 4f)
            lineTo(7f, 20f)
            moveTo(7f, 5f)
            lineTo(16f, 5f)
            lineTo(14.5f, 8f)
            lineTo(16f, 11f)
            lineTo(7f, 11f)
            moveTo(10f, 15f)
            lineTo(17f, 15f)
            moveTo(10f, 18f)
            lineTo(15f, 18f)
        }
    }.build()
}

private val cameraIcon by lazy {
    ImageVector.Builder("SqCamera", 24.dp, 24.dp, 24f, 24f).apply {
        outlinePath {
            moveTo(5f, 8f)
            lineTo(9f, 8f)
            lineTo(10.5f, 6f)
            lineTo(13.5f, 6f)
            lineTo(15f, 8f)
            lineTo(19f, 8f)
            lineTo(19f, 18f)
            lineTo(5f, 18f)
            close()
            moveTo(12f, 10f)
            curveTo(14.2f, 10f, 16f, 11.8f, 16f, 14f)
            curveTo(16f, 16.2f, 14.2f, 18f, 12f, 18f)
            curveTo(9.8f, 18f, 8f, 16.2f, 8f, 14f)
            curveTo(8f, 11.8f, 9.8f, 10f, 12f, 10f)
            close()
        }
    }.build()
}

private val profileIcon by lazy {
    ImageVector.Builder("SqProfile", 24.dp, 24.dp, 24f, 24f).apply {
        outlinePath {
            moveTo(12f, 5f)
            curveTo(14.2f, 5f, 16f, 6.8f, 16f, 9f)
            curveTo(16f, 11.2f, 14.2f, 13f, 12f, 13f)
            curveTo(9.8f, 13f, 8f, 11.2f, 8f, 9f)
            curveTo(8f, 6.8f, 9.8f, 5f, 12f, 5f)
            close()
            moveTo(5f, 19f)
            curveTo(6.8f, 16.3f, 9.1f, 15f, 12f, 15f)
            curveTo(14.9f, 15f, 17.2f, 16.3f, 19f, 19f)
        }
    }.build()
}

private val backIcon by lazy {
    ImageVector.Builder("SqBack", 24.dp, 24.dp, 24f, 24f).apply {
        outlinePath {
            moveTo(14f, 6f)
            lineTo(8f, 12f)
            lineTo(14f, 18f)
            moveTo(9f, 12f)
            lineTo(18f, 12f)
        }
    }.build()
}

private val searchIcon by lazy {
    ImageVector.Builder("SqSearch", 24.dp, 24.dp, 24f, 24f).apply {
        outlinePath {
            moveTo(10.5f, 6f)
            curveTo(13.5f, 6f, 16f, 8.5f, 16f, 11.5f)
            curveTo(16f, 14.5f, 13.5f, 17f, 10.5f, 17f)
            curveTo(7.5f, 17f, 5f, 14.5f, 5f, 11.5f)
            curveTo(5f, 8.5f, 7.5f, 6f, 10.5f, 6f)
            close()
            moveTo(15f, 16f)
            lineTo(19f, 20f)
        }
    }.build()
}

private val coinIcon by lazy {
    ImageVector.Builder("SqCoin", 24.dp, 24.dp, 24f, 24f).apply {
        outlinePath {
            moveTo(12f, 5f)
            curveTo(15.9f, 5f, 19f, 8.1f, 19f, 12f)
            curveTo(19f, 15.9f, 15.9f, 19f, 12f, 19f)
            curveTo(8.1f, 19f, 5f, 15.9f, 5f, 12f)
            curveTo(5f, 8.1f, 8.1f, 5f, 12f, 5f)
            close()
            moveTo(10f, 10f)
            lineTo(14f, 10f)
            moveTo(12f, 8f)
            lineTo(12f, 16f)
        }
    }.build()
}

private val galleryIcon by lazy {
    ImageVector.Builder("SqGallery", 24.dp, 24.dp, 24f, 24f).apply {
        outlinePath {
            moveTo(5f, 6f)
            lineTo(19f, 6f)
            lineTo(19f, 18f)
            lineTo(5f, 18f)
            close()
            moveTo(8f, 10f)
            curveTo(8.8f, 10f, 9.5f, 9.3f, 9.5f, 8.5f)
            curveTo(9.5f, 7.7f, 8.8f, 7f, 8f, 7f)
            curveTo(7.2f, 7f, 6.5f, 7.7f, 6.5f, 8.5f)
            curveTo(6.5f, 9.3f, 7.2f, 10f, 8f, 10f)
            close()
            moveTo(7f, 17f)
            lineTo(11f, 12f)
            lineTo(14f, 15f)
            lineTo(16f, 13f)
            lineTo(19f, 17f)
        }
    }.build()
}

private val saveIcon by lazy {
    ImageVector.Builder("SqSave", 24.dp, 24.dp, 24f, 24f).apply {
        outlinePath {
            moveTo(12f, 5f)
            lineTo(12f, 14f)
            moveTo(8.5f, 11f)
            lineTo(12f, 14.5f)
            lineTo(15.5f, 11f)
            moveTo(6f, 17f)
            lineTo(18f, 17f)
            lineTo(18f, 19f)
            lineTo(6f, 19f)
            close()
        }
    }.build()
}

private val intelIcon by lazy {
    ImageVector.Builder("SqIntel", 24.dp, 24.dp, 24f, 24f).apply {
        outlinePath {
            moveTo(7f, 5f)
            lineTo(15f, 5f)
            lineTo(18f, 8f)
            lineTo(18f, 19f)
            lineTo(7f, 19f)
            close()
            moveTo(15f, 5f)
            lineTo(15f, 9f)
            lineTo(18f, 9f)
            moveTo(9f, 12f)
            lineTo(15f, 12f)
            moveTo(9f, 15f)
            lineTo(13f, 15f)
        }
    }.build()
}

private val retakeIcon by lazy {
    ImageVector.Builder("SqRetake", 24.dp, 24.dp, 24f, 24f).apply {
        outlinePath {
            moveTo(9f, 8f)
            lineTo(5f, 8f)
            lineTo(5f, 4f)
            moveTo(5f, 8f)
            curveTo(6.8f, 5.5f, 9.1f, 4.2f, 12f, 4.2f)
            curveTo(16.4f, 4.2f, 19.8f, 7.6f, 19.8f, 12f)
            curveTo(19.8f, 16.4f, 16.4f, 19.8f, 12f, 19.8f)
            curveTo(8.7f, 19.8f, 6f, 17.9f, 4.8f, 15f)
        }
    }.build()
}

private val cleanIcon by lazy {
    ImageVector.Builder("SqClean", 24.dp, 24.dp, 24f, 24f).apply {
        outlinePath {
            moveTo(14f, 5f)
            lineTo(10f, 9f)
            moveTo(9f, 10f)
            lineTo(6f, 19f)
            moveTo(9f, 10f)
            lineTo(14f, 15f)
            moveTo(12.5f, 13.5f)
            lineTo(17.5f, 18.5f)
        }
    }.build()
}

private val studyIcon by lazy {
    ImageVector.Builder("SqStudy", 24.dp, 24.dp, 24f, 24f).apply {
        outlinePath {
            moveTo(6f, 6f)
            lineTo(11f, 5f)
            lineTo(11f, 18f)
            lineTo(6f, 19f)
            close()
            moveTo(18f, 6f)
            lineTo(13f, 5f)
            lineTo(13f, 18f)
            lineTo(18f, 19f)
            close()
        }
    }.build()
}

private val sprintIcon by lazy {
    ImageVector.Builder("SqSprint", 24.dp, 24.dp, 24f, 24f).apply {
        outlinePath {
            moveTo(7f, 15f)
            lineTo(11f, 10f)
            lineTo(9.5f, 10f)
            lineTo(13.5f, 5f)
            lineTo(12.5f, 10f)
            lineTo(15.5f, 10f)
            lineTo(10f, 18f)
            lineTo(11f, 13f)
            close()
        }
    }.build()
}

private val compassIcon by lazy {
    ImageVector.Builder("SqCompass", 24.dp, 24.dp, 24f, 24f).apply {
        outlinePath {
            moveTo(12f, 4f)
            lineTo(18f, 10f)
            lineTo(12f, 20f)
            lineTo(6f, 10f)
            close()
            moveTo(10.5f, 11.5f)
            lineTo(15f, 9f)
            lineTo(12.5f, 14f)
            lineTo(9f, 15f)
            close()
        }
    }.build()
}

private val sparkIcon by lazy {
    ImageVector.Builder("SqSpark", 24.dp, 24.dp, 24f, 24f).apply {
        outlinePath {
            moveTo(12f, 4f)
            lineTo(13.8f, 9.2f)
            lineTo(19f, 11f)
            lineTo(13.8f, 12.8f)
            lineTo(12f, 18f)
            lineTo(10.2f, 12.8f)
            lineTo(5f, 11f)
            lineTo(10.2f, 9.2f)
            close()
        }
    }.build()
}
