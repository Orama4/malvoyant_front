package com.example.malvoayant.navigation

sealed class Destination(val route: String) {
    object Home : Destination("home")
    object Registration : Destination("registration")
    object Login : Destination("login")

}