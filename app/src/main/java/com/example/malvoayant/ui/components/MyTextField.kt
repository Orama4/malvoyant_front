package com.example.malvoayant.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.malvoayant.ui.theme.AppColors
import com.example.malvoayant.ui.theme.PlusJakartaSans

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