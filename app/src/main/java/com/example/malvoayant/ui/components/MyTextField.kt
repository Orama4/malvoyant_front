package com.example.malvoayant.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.malvoayant.ui.theme.AppColors
import com.example.malvoayant.ui.theme.PlusJakartaSans
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff


@Composable
fun MyTextField(
    text: String,
    content: String,
    placeHolder: String,
    icon: Painter,
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    isPassword: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
            text = text,
            fontSize = 24.sp,
            fontFamily = PlusJakartaSans,
            color = AppColors.writingBlue,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeHolder,
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.writingBlue.copy(alpha = 0.6f)
                    )
                )
            },
            leadingIcon = {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Gray
                )
            },
            trailingIcon = {
                if (isPassword) {
                    IconButton(onClick = { passwordVisible = !passwordVisible },modifier = Modifier.padding(end = 8.dp)) {

                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                }
            },
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = when (content) {
                "Email" -> KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email)
                "Password" -> KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password)
                "Phone Number" -> KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone)
                else -> KeyboardOptions.Default
            },
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White,
                cursorColor = Color.Black,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = AppColors.darkBlue,
                unfocusedTextColor = AppColors.darkBlue
            ),
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .fillMaxWidth()
                .height(120.dp)
                .border(
                    width = 4.dp,
                    color = AppColors.primary,
                    shape = RoundedCornerShape(20.dp)
                ),
            textStyle = TextStyle(
                fontSize = 24.sp,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.writingBlue
            ),
            singleLine = true
        )
    }
}
