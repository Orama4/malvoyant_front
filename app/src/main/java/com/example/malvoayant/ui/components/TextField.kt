package com.example.malvoayant.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.malvoayant.ui.theme.AppColors

// --- Reusable Modern Text Fields ---
@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector? = null, // Optional icon
    isError: Boolean = false

) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small, // Consistent rounding
        leadingIcon = leadingIcon?.let {
            { Icon(it, contentDescription = null, tint = AppColors.primary) }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(0xFF1D1D1F),
            unfocusedTextColor = Color(0xFF1D1D1F),
            focusedBorderColor = AppColors.primary, // Orange focus
            unfocusedBorderColor = Color(0xFFEAEAEA), // Light gray border
            cursorColor = AppColors.primary,
            focusedLabelColor = AppColors.primary, // Orange label focus
            unfocusedLabelColor = Color(0xFFF5F5F7), // Gray label
            errorBorderColor = Color(0xFFFF3B30),
            errorLabelColor = Color(0xFFFF3B30),
            errorLeadingIconColor = Color(0xFFFF3B30)
        ),
        singleLine = true,
        isError = isError

    )
}

@Composable
fun ModernPasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        leadingIcon = { // Lock icon
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = AppColors.primary)
        },
        trailingIcon = {
            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            val description = if (passwordVisible) "Hide password" else "Show password"
            IconButton(onClick = {passwordVisible = !passwordVisible}){
                Icon(imageVector  = image, description, tint = Color(0xFFF5F5F7))
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(0xFF1D1D1F),
            unfocusedTextColor = Color(0xFF1D1D1F),
            focusedBorderColor = if(isError) Color(0xFFFF3B30) else AppColors.primary,
            unfocusedBorderColor = if(isError) Color(0xFFFF3B30) else Color(0xFFEAEAEA),
            cursorColor = AppColors.primary,
            focusedLabelColor = if(isError) Color(0xFFFF3B30) else AppColors.primary,
            unfocusedLabelColor = Color(0xFFF5F5F7),
            errorBorderColor = Color(0xFFFF3B30),
            errorLabelColor = Color(0xFFFF3B30),
            errorLeadingIconColor = Color(0xFFFF3B30),
            errorTrailingIconColor = Color(0xFFFF3B30)
        ),
        isError = isError
    )
}