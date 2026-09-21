package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

// ==========================================
// SAHAYAK AI SHARED DESIGN SYSTEM COMPONENTS
// ==========================================

enum class AppButtonVariant {
    PRIMARY,
    SECONDARY,
    OUTLINED,
    TEXT,
    SUCCESS,
    ERROR
}

/**
 * Standard Accessible App Button adhering to 48dp minimum touch target
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.PRIMARY,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    contentDescription: String? = null,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    val buttonColors = when (variant) {
        AppButtonVariant.PRIMARY -> ButtonDefaults.buttonColors(
            containerColor = PrimaryDeepBlue,
            contentColor = PureWhite,
            disabledContainerColor = Slate300,
            disabledContentColor = Slate500
        )
        AppButtonVariant.SECONDARY -> ButtonDefaults.buttonColors(
            containerColor = PrimaryBlue50,
            contentColor = PrimaryDeepBlue,
            disabledContainerColor = Slate100,
            disabledContentColor = Slate400
        )
        AppButtonVariant.OUTLINED -> ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceColor,
            contentColor = PrimaryDeepBlue,
            disabledContentColor = Slate400
        )
        AppButtonVariant.TEXT -> ButtonDefaults.textButtonColors(
            contentColor = PrimaryDeepBlue,
            disabledContentColor = Slate400
        )
        AppButtonVariant.SUCCESS -> ButtonDefaults.buttonColors(
            containerColor = SuccessGreen,
            contentColor = PureWhite,
            disabledContainerColor = Slate300,
            disabledContentColor = Slate500
        )
        AppButtonVariant.ERROR -> ButtonDefaults.buttonColors(
            containerColor = ErrorRed,
            contentColor = PureWhite,
            disabledContainerColor = Slate300,
            disabledContentColor = Slate500
        )
    }

    val border = when (variant) {
        AppButtonVariant.OUTLINED -> BorderStroke(1.dp, if (enabled) BorderColor else Slate300)
        AppButtonVariant.SECONDARY -> BorderStroke(1.dp, PrimaryBlue100)
        else -> null
    }

    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else Modifier

    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .then(semanticsModifier),
        enabled = enabled && !isLoading,
        colors = buttonColors,
        shape = shape,
        border = border,
        elevation = if (variant == AppButtonVariant.PRIMARY && enabled) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = if (variant == AppButtonVariant.PRIMARY || variant == AppButtonVariant.SUCCESS || variant == AppButtonVariant.ERROR) PureWhite else PrimaryDeepBlue,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        )

        if (trailingIcon != null && !isLoading) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Standard High-Contrast App Text Field with explicit text colors and border styles
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    testTag: String? = null
) {
    val tagModifier = if (testTag != null) Modifier.testTag(testTag) else Modifier

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(tagModifier),
            label = {
                Text(
                    text = label,
                    color = if (isError) ErrorRed else TextSecondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            },
            placeholder = {
                if (placeholder.isNotBlank()) {
                    Text(
                        text = placeholder,
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = if (isError) ErrorRed else if (value.isNotBlank()) PrimaryDeepBlue else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            trailingIcon = trailingIcon,
            textStyle = TextStyle(
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp
            ),
            isError = isError,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            maxLines = maxLines,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                disabledTextColor = TextSecondary,
                errorTextColor = TextPrimary,
                focusedContainerColor = SurfaceColor,
                unfocusedContainerColor = SurfaceColor,
                disabledContainerColor = Slate100,
                errorContainerColor = SurfaceColor,
                cursorColor = PrimaryDeepBlue,
                errorCursorColor = ErrorRed,
                focusedBorderColor = PrimaryDeepBlue,
                unfocusedBorderColor = BorderColor,
                disabledBorderColor = Slate300,
                errorBorderColor = ErrorRed,
                focusedLabelColor = PrimaryDeepBlue,
                unfocusedLabelColor = TextSecondary,
                disabledLabelColor = Slate400,
                errorLabelColor = ErrorRed
            )
        )

        AnimatedVisibility(visible = isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage.orEmpty(),
                color = ErrorRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Standard Elevated App Card container with 12dp corner radius
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = SurfaceColor,
    shape: Shape = RoundedCornerShape(12.dp),
    elevation: Dp = 1.dp,
    border: BorderStroke? = BorderStroke(1.dp, BorderColor),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else Modifier

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier),
        shape = shape,
        color = backgroundColor,
        tonalElevation = elevation,
        shadowElevation = elevation,
        border = border
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * Standard Honest Empty State Component (Not Fake Data)
 */
@Composable
fun AppEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    emoji: String? = null,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    actionIcon: ImageVector? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(PrimaryBlue50),
            contentAlignment = Alignment.Center
        ) {
            if (emoji != null) {
                Text(emoji, fontSize = 32.sp)
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryDeepBlue,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (!actionButtonText.isNullOrBlank() && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            AppButton(
                text = actionButtonText,
                onClick = onActionClick,
                icon = actionIcon,
                variant = AppButtonVariant.PRIMARY
            )
        }
    }
}

/**
 * Clean Error / Alert Banner
 */
@Composable
fun AppErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    isWarning: Boolean = false,
    onDismiss: (() -> Unit)? = null
) {
    val bgColor = if (isWarning) WarningAmberBg else ErrorRedBg
    val borderColor = if (isWarning) WarningAmber else ErrorRed
    val textColor = if (isWarning) WarningAmber else ErrorRed

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isWarning) Icons.Filled.Warning else Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = textColor
                    )
                }
                Text(
                    text = message,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    lineHeight = 16.sp
                )
            }
            if (onDismiss != null) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Standard Loading State
 */
@Composable
fun AppLoadingIndicator(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = PrimaryDeepBlue,
            strokeWidth = 3.dp,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/**
 * Explicit "Demo Data" badge for simulated / sandbox features (Part C1)
 */
@Composable
fun DemoDataBadge(
    text: String = "Demo Data / Sandbox",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = WarningAmberBg,
        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = WarningAmber
            )
        }
    }
}
