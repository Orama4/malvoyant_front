package com.example.malvoayant.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.R as AndroidR
import com.example.malvoayant.R
import com.example.malvoayant.ui.theme.AppColors
import com.example.malvoayant.ui.theme.PlusJakartaSans

/**
 * Enhanced header bar component that adapts based on the page type
 * Can fill the entire height and shows different layouts based on page type
 */
@Composable
fun HeaderBar(
    pageType: String, // "home", "login", or "register"
    onSpeakHelp: () -> Unit,
    modifier: Modifier = Modifier,
    fullHeight: Boolean = true
) {
    val backgroundModifier = if (fullHeight) {
        modifier
            .fillMaxWidth()
            .fillMaxHeight(0.3f)
            .background(AppColors.darkBlue)
    } else {
        modifier
            .fillMaxWidth()
            .background(AppColors.darkBlue)
            .padding(16.dp)
    }

    Box(modifier = backgroundModifier) {
        when (pageType.lowercase()) {
            "home" -> {
                // Centered logo and welcome text for home page
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "WELCOME TO ",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier.clickable { onSpeakHelp() }
                            .offset(y = (50).dp) // Move closer to the Text


                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Image(
                        painter = painterResource(id = R.drawable.irchad_logo), // Replace with your actual logo resource
                        contentDescription = "Irchad Logo",
                        modifier = Modifier.size(200.dp)

                    )




                }
            }

            "login" -> {
                // Logo in left corner with Register text for register page
                Column (
                    modifier=Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ){
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.irchad_logo), // Replace with your actual logo resource
                            contentDescription = "Irchad Logo",
                            modifier = Modifier.size(80.dp)
                        )


                    }
                    Spacer(modifier = Modifier.height(50.dp))
                    Text(
                        text = "LOGIN",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier.clickable { onSpeakHelp() }
                    )}
            }

            "register" -> {
                // Logo in left corner with Register text for register page
                Column (
                    modifier=Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ){
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.irchad_logo), // Replace with your actual logo resource
                        contentDescription = "Irchad Logo",
                        modifier = Modifier.size(80.dp)
                    )




                }
                    Spacer(modifier = Modifier.height(50.dp))
                    Text(
                        text = "REGISTRATION",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier.clickable { onSpeakHelp() }
                    )}

            }
        }

        // Voice guide icon always in the top right corner
        IconButton(
            onClick = onSpeakHelp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(
                painter = painterResource(id = AndroidR.drawable.ic_btn_speak_now),
                contentDescription = "Voice Guide",
                tint = Color.White
            )
        }
    }
}


