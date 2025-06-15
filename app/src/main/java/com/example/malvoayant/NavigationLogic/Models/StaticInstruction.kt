package com.example.malvoayant.NavigationLogic.Models

data class StaticInstruction(
    val instruction: String,
    val distance: Float?,
    val type: String? = "Straight",
)
